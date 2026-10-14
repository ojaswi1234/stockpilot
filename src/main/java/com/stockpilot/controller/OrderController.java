package com.stockpilot.controller;

import com.stockpilot.dto.CreateOrderRequest;
import com.stockpilot.dto.OrderDto;
import com.stockpilot.dto.OrderItemRequest;
import com.stockpilot.model.Role;
import com.stockpilot.model.User;
import com.stockpilot.repository.ProductRepository;
import com.stockpilot.repository.UserRepository;
import com.stockpilot.repository.WarehouseRepository;
import com.stockpilot.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService,
                           ProductRepository productRepository,
                           WarehouseRepository warehouseRepository,
                           UserRepository userRepository) {
        this.orderService = orderService;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String listOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        List<OrderDto> orders;
        if (currentUser != null && currentUser.getRole() == Role.CUSTOMER) {
            orders = orderService.getOrdersByCustomer(currentUser.getId());
        } else if (currentUser != null && currentUser.getRole() == Role.WAREHOUSE_MANAGER && currentUser.getAssignedWarehouse() != null) {
            orders = orderService.getOrdersByWarehouse(currentUser.getAssignedWarehouse().getId());
        } else {
            orders = orderService.getAllOrders();
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("orders", orders);
        model.addAttribute("products", productRepository.findByActiveTrue());
        model.addAttribute("warehouses", warehouseRepository.findByActiveTrue());

        return "orders";
    }

    @GetMapping("/{id}")
    public String orderDetails(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        OrderDto order = orderService.getOrderById(id);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("order", order);

        return "order-detail";
    }

    @PostMapping("/create")
    public String createOrder(@RequestParam Long warehouseId,
                              @RequestParam Long productId,
                              @RequestParam int quantity,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        User customer = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        CreateOrderRequest req = new CreateOrderRequest(warehouseId,
                Collections.singletonList(new OrderItemRequest(productId, quantity)));

        OrderDto order = orderService.createAndReserveOrder(req, customer);
        redirectAttributes.addFlashAttribute("successMessage", "Order " + order.getOrderNumber() + " placed and stock reserved!");
        return "redirect:/orders/" + order.getId();
    }

    @PostMapping("/{id}/pay")
    public String payOrder(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        OrderDto order = orderService.payOrder(id, user);
        redirectAttributes.addFlashAttribute("successMessage", "Order " + order.getOrderNumber() + " paid successfully!");
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/fulfill")
    public String fulfillOrder(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        OrderDto order = orderService.fulfillOrder(id, user);
        redirectAttributes.addFlashAttribute("successMessage", "Order " + order.getOrderNumber() + " fulfilled and shipped!");
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(defaultValue = "Cancelled by user") String reason,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        OrderDto order = orderService.cancelOrder(id, user, reason);
        redirectAttributes.addFlashAttribute("successMessage", "Order " + order.getOrderNumber() + " cancelled; reserved stock released!");
        return "redirect:/orders/" + id;
    }
}
