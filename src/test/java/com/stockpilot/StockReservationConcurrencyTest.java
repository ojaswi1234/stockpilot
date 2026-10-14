package com.stockpilot;

import com.stockpilot.dto.CreateOrderRequest;
import com.stockpilot.dto.OrderItemRequest;
import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.model.Inventory;
import com.stockpilot.model.Product;
import com.stockpilot.model.User;
import com.stockpilot.model.Warehouse;
import com.stockpilot.repository.InventoryRepository;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.UserRepository;
import com.stockpilot.repository.WarehouseRepository;
import com.stockpilot.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class StockReservationConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private UserRepository userRepository;

    private Product testProduct;
    private Warehouse testWarehouse;
    private User testCustomer;

    @BeforeEach
    void setup() {
        testWarehouse = warehouseRepository.findByCode("WH-CONC").orElseGet(() ->
                warehouseRepository.save(new Warehouse("WH-CONC", "Concurrency Test WH", "Test Location")));
        testProduct = productRepository.findBySku("SKU-CONC-01").orElseGet(() ->
                productRepository.save(new Product("SKU-CONC-01", "Limited Edition Item", "Rare item", new BigDecimal("100.00"), 2)));
        testCustomer = userRepository.findByUsername("customer_bob").orElseGet(() ->
                userRepository.save(new User("customer_bob", "pass", "Bob", "bob@conc.com", com.stockpilot.model.Role.CUSTOMER, null)));

        // Stock exactly 5 units on hand, 0 reserved -> available = 5
        Inventory inventory = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), testWarehouse.getId()).orElse(null);
        if (inventory != null) {
            inventory.setQuantityOnHand(5);
            inventory.setQuantityReserved(0);
            inventoryRepository.save(inventory);
        } else {
            inventoryRepository.save(new Inventory(testProduct, testWarehouse, 5, 0));
        }
    }

    @Test
    @DisplayName("High concurrency: 20 concurrent threads competing for 5 items must result in exactly 5 successes and 15 rejections without overselling")
    void testConcurrentOrderReservationsPreventOverselling() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all threads to fire simultaneously
                    CreateOrderRequest req = new CreateOrderRequest(
                            testWarehouse.getId(),
                            Collections.singletonList(new OrderItemRequest(testProduct.getId(), 1))
                    );
                    orderService.createAndReserveOrder(req, testCustomer);
                    successCount.incrementAndGet();
                } catch (InsufficientStockException ex) {
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    Throwable root = ex;
                    boolean isInsufficient = false;
                    while (root != null) {
                        if (root instanceof InsufficientStockException || (root.getMessage() != null && root.getMessage().contains("Insufficient stock"))) {
                            isInsufficient = true;
                            break;
                        }
                        root = root.getCause();
                    }
                    if (isInsufficient) {
                        failureCount.incrementAndGet();
                    } else {
                        ex.printStackTrace();
                    }
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Fire all threads concurrently
        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        // Assert exact invariants
        assertEquals(5, successCount.get(), "Exactly 5 orders should succeed for 5 available items");
        assertEquals(15, failureCount.get(), "Exactly 15 orders should fail due to insufficient stock");

        // Verify final database state
        Inventory finalInventory = inventoryRepository.findByProductIdAndWarehouseId(testProduct.getId(), testWarehouse.getId())
                .orElseThrow();

        assertEquals(5, finalInventory.getQuantityOnHand(), "On-hand stock must remain 5 until fulfillment");
        assertEquals(5, finalInventory.getQuantityReserved(), "Reserved stock must be exactly 5");
        assertEquals(0, finalInventory.getAvailableQuantity(), "Available stock must be exactly 0 (no overselling)");
    }
}
