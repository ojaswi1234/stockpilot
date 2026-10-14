package com.stockpilot.service;

import com.stockpilot.dto.CreateOrderRequest;
import com.stockpilot.dto.OrderDto;
import com.stockpilot.dto.OrderItemDto;
import com.stockpilot.dto.OrderItemRequest;
import com.stockpilot.event.LowStockAlertEvent;
import com.stockpilot.event.OrderEvent;
import com.stockpilot.event.StockChangedEvent;
import com.stockpilot.exception.IllegalOrderStateException;
import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.exception.ResourceNotFoundException;
import com.stockpilot.model.*;
import com.stockpilot.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final Duration DEFAULT_RESERVATION_TIMEOUT = Duration.ofMinutes(15);

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;
    private final Random random = new Random();

    public OrderService(OrderRepository orderRepository,
                        InventoryRepository inventoryRepository,
                        ProductRepository productRepository,
                        WarehouseRepository warehouseRepository,
                        StockMovementRepository stockMovementRepository,
                        ApplicationEventPublisher eventPublisher,
                        AuditService auditService) {
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersByWarehouse(Long warehouseId) {
        return orderRepository.findByWarehouseIdOrderByCreatedAtDesc(warehouseId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        return toDto(order);
    }

    @Transactional(readOnly = true)
    public OrderDto getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));
        return toDto(order);
    }

    /**
     * Atomically creates an order and reserves inventory.
     * Prevents overselling using row-level pessimistic write locking (SELECT ... FOR UPDATE)
     * and ascending Product ID ordering to prevent database deadlocks.
     */
    @Transactional
    public OrderDto createAndReserveOrder(CreateOrderRequest req, User customer) {
        Warehouse warehouse = warehouseRepository.findById(req.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with ID: " + req.getWarehouseId()));

        String orderNumber = "ORD-" + Instant.now().toEpochMilli() + "-" + String.format("%04d", random.nextInt(10000));
        Order order = new Order(orderNumber, customer, warehouse);

        // Sort items by Product ID to ensure deterministic lock acquisition order (prevents deadlocks)
        List<OrderItemRequest> sortedItems = req.getItems().stream()
                .sorted(Comparator.comparing(OrderItemRequest::getProductId))
                .collect(Collectors.toList());

        for (OrderItemRequest itemReq : sortedItems) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + itemReq.getProductId()));

            // Acquire pessimistic write lock on the inventory row
            Inventory inventory = inventoryRepository.findForUpdate(product.getId(), warehouse.getId())
                    .orElseThrow(() -> new InsufficientStockException("No inventory record exists for product " + product.getSku() + " in warehouse " + warehouse.getCode()));

            if (!inventory.canReserve(itemReq.getQuantity())) {
                throw new InsufficientStockException("Insufficient stock for product " + product.getSku() +
                        ": requested " + itemReq.getQuantity() + ", but available is " + inventory.getAvailableQuantity());
            }

            int prevOnHand = inventory.getQuantityOnHand();
            int prevReserved = inventory.getQuantityReserved();

            // Increment reserved quantity
            inventory.reserve(itemReq.getQuantity());
            Inventory savedInv = inventoryRepository.save(inventory);

            // Record movement ledger entry
            StockMovement movement = new StockMovement(
                    product, warehouse, MovementType.RESERVATION,
                    itemReq.getQuantity(), prevOnHand, savedInv.getQuantityOnHand(),
                    prevReserved, savedInv.getQuantityReserved(),
                    orderNumber, "ORDER_RESERVATION", customer, "Reserved for order " + orderNumber
            );
            stockMovementRepository.save(movement);

            // Add order item
            OrderItem orderItem = new OrderItem(product, itemReq.getQuantity(), product.getUnitPrice());
            order.addItem(orderItem);

            eventPublisher.publishEvent(new StockChangedEvent(savedInv, "Reserved " + itemReq.getQuantity() + " for order " + orderNumber));
        }

        order.setStatus(OrderStatus.RESERVED);
        order.setReservationExpiresAt(Instant.now().plus(DEFAULT_RESERVATION_TIMEOUT));
        Order savedOrder = orderRepository.save(order);

        auditService.record(customer, "ORDER_RESERVED", "ORDER",
                savedOrder.getId().toString(), "Created order " + orderNumber + " with total amount: " + savedOrder.getTotalAmount());

        log.info("Order successfully created and reserved: {} by {}", orderNumber, customer.getUsername());
        eventPublisher.publishEvent(new OrderEvent(savedOrder, "ORDER_RESERVED"));

        return toDto(savedOrder);
    }

    /**
     * Marks a reserved order as paid.
     */
    @Transactional
    public OrderDto payOrder(Long orderId, User user) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalOrderStateException("Cannot pay for order " + order.getOrderNumber() + " with status: " + order.getStatus());
        }

        if (order.getReservationExpiresAt() != null && order.getReservationExpiresAt().isBefore(Instant.now())) {
            expireOrder(order.getId());
            throw new IllegalOrderStateException("Order reservation has expired for order " + order.getOrderNumber());
        }

        order.setStatus(OrderStatus.PAID);
        Order saved = orderRepository.save(order);

        auditService.record(user, "ORDER_PAID", "ORDER",
                saved.getId().toString(), "Marked order " + saved.getOrderNumber() + " as PAID");

        log.info("Order paid: {}", saved.getOrderNumber());
        eventPublisher.publishEvent(new OrderEvent(saved, "ORDER_PAID"));

        return toDto(saved);
    }

    /**
     * Fulfills an order, physically deducting on-hand stock and releasing reservation.
     */
    @Transactional
    public OrderDto fulfillOrder(Long orderId, User performedByUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.RESERVED) {
            throw new IllegalOrderStateException("Cannot fulfill order " + order.getOrderNumber() + " with status: " + order.getStatus());
        }

        // Sort items deterministically
        List<OrderItem> sortedItems = order.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .collect(Collectors.toList());

        for (OrderItem item : sortedItems) {
            Inventory inventory = inventoryRepository.findForUpdate(item.getProduct().getId(), order.getWarehouse().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found for product " + item.getProduct().getSku()));

            int prevOnHand = inventory.getQuantityOnHand();
            int prevReserved = inventory.getQuantityReserved();

            // Deduct on-hand and reserved
            inventory.fulfill(item.getQuantity());
            Inventory savedInv = inventoryRepository.save(inventory);

            StockMovement movement = new StockMovement(
                    item.getProduct(), order.getWarehouse(), MovementType.FULFILLMENT,
                    -item.getQuantity(), prevOnHand, savedInv.getQuantityOnHand(),
                    prevReserved, savedInv.getQuantityReserved(),
                    order.getOrderNumber(), "ORDER_FULFILLMENT", performedByUser, "Order fulfilled and shipped"
            );
            stockMovementRepository.save(movement);

            eventPublisher.publishEvent(new StockChangedEvent(savedInv, "Fulfilled order " + order.getOrderNumber()));

            if (savedInv.getQuantityOnHand() <= item.getProduct().getLowStockThreshold()) {
                eventPublisher.publishEvent(new LowStockAlertEvent(savedInv));
            }
        }

        order.setStatus(OrderStatus.FULFILLED);
        Order saved = orderRepository.save(order);

        auditService.record(performedByUser, "ORDER_FULFILLED", "ORDER",
                saved.getId().toString(), "Fulfilled order " + saved.getOrderNumber());

        log.info("Order fulfilled: {} by {}", saved.getOrderNumber(), performedByUser.getUsername());
        eventPublisher.publishEvent(new OrderEvent(saved, "ORDER_FULFILLED"));

        return toDto(saved);
    }

    /**
     * Cancels an order and restores reserved inventory back to available.
     */
    @Transactional
    public OrderDto cancelOrder(Long orderId, User user, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (order.getStatus() == OrderStatus.FULFILLED || order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED) {
            throw new IllegalOrderStateException("Cannot cancel order " + order.getOrderNumber() + " with status: " + order.getStatus());
        }

        boolean wasReserved = (order.getStatus() == OrderStatus.RESERVED || order.getStatus() == OrderStatus.PAID);

        if (wasReserved) {
            List<OrderItem> sortedItems = order.getItems().stream()
                    .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                    .collect(Collectors.toList());

            for (OrderItem item : sortedItems) {
                Inventory inventory = inventoryRepository.findForUpdate(item.getProduct().getId(), order.getWarehouse().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

                int prevOnHand = inventory.getQuantityOnHand();
                int prevReserved = inventory.getQuantityReserved();

                inventory.releaseReservation(item.getQuantity());
                Inventory savedInv = inventoryRepository.save(inventory);

                StockMovement movement = new StockMovement(
                        item.getProduct(), order.getWarehouse(), MovementType.CANCELLATION_RELEASE,
                        item.getQuantity(), prevOnHand, savedInv.getQuantityOnHand(),
                        prevReserved, savedInv.getQuantityReserved(),
                        order.getOrderNumber(), "ORDER_CANCELLATION", user, "Cancellation: " + (reason != null ? reason : "User cancelled")
                );
                stockMovementRepository.save(movement);

                eventPublisher.publishEvent(new StockChangedEvent(savedInv, "Released reservation for " + order.getOrderNumber()));
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        auditService.record(user, "ORDER_CANCELLED", "ORDER",
                saved.getId().toString(), "Cancelled order " + saved.getOrderNumber() + " (" + reason + ")");

        log.info("Order cancelled: {} (reason: {})", saved.getOrderNumber(), reason);
        eventPublisher.publishEvent(new OrderEvent(saved, "ORDER_CANCELLED"));

        return toDto(saved);
    }

    /**
     * Automatically expires an unfulfilled reservation and releases inventory back to available stock.
     */
    @Transactional
    public OrderDto expireOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (order.getStatus() != OrderStatus.RESERVED) {
            return toDto(order);
        }

        List<OrderItem> sortedItems = order.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .collect(Collectors.toList());

        for (OrderItem item : sortedItems) {
            Inventory inventory = inventoryRepository.findForUpdate(item.getProduct().getId(), order.getWarehouse().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

            int prevOnHand = inventory.getQuantityOnHand();
            int prevReserved = inventory.getQuantityReserved();

            inventory.releaseReservation(item.getQuantity());
            Inventory savedInv = inventoryRepository.save(inventory);

            StockMovement movement = new StockMovement(
                    item.getProduct(), order.getWarehouse(), MovementType.CANCELLATION_RELEASE,
                    item.getQuantity(), prevOnHand, savedInv.getQuantityOnHand(),
                    prevReserved, savedInv.getQuantityReserved(),
                    order.getOrderNumber(), "ORDER_EXPIRED", null, "Automatic reservation timeout expired"
            );
            stockMovementRepository.save(movement);

            eventPublisher.publishEvent(new StockChangedEvent(savedInv, "Reservation expired for " + order.getOrderNumber()));
        }

        order.setStatus(OrderStatus.EXPIRED);
        Order saved = orderRepository.save(order);

        auditService.record(null, "ORDER_EXPIRED", "ORDER",
                saved.getId().toString(), "Expired stale reservation for order " + saved.getOrderNumber());

        log.info("Order reservation expired: {}", saved.getOrderNumber());
        eventPublisher.publishEvent(new OrderEvent(saved, "ORDER_EXPIRED"));

        return toDto(saved);
    }

    public OrderDto toDto(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(i -> new OrderItemDto(
                        i.getId(),
                        i.getProduct().getId(),
                        i.getProduct().getSku(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getSubtotal()
                ))
                .collect(Collectors.toList());

        return new OrderDto(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomer().getId(),
                order.getCustomer().getUsername(),
                order.getCustomer().getFullName(),
                order.getWarehouse().getId(),
                order.getWarehouse().getCode(),
                order.getWarehouse().getName(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getReservationExpiresAt(),
                itemDtos,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
