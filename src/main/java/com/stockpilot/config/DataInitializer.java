package com.stockpilot.config;

import com.stockpilot.model.*;
import com.stockpilot.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           WarehouseRepository warehouseRepository,
                           ProductRepository productRepository,
                           InventoryRepository inventoryRepository,
                           StockMovementRepository stockMovementRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (warehouseRepository.count() > 0 && userRepository.count() > 0) {
            log.info("StockPilot database already contains data; verifying admin password...");
            userRepository.findByUsername("stock_admin").ifPresent(admin -> {
                admin.setPasswordHash(passwordEncoder.encode("Password123!"));
                userRepository.save(admin);
            });
            userRepository.findByUsername("wh_manager_east").ifPresent(u -> {
                u.setPasswordHash(passwordEncoder.encode("Password123!"));
                userRepository.save(u);
            });
            userRepository.findByUsername("customer_bob").ifPresent(u -> {
                u.setPasswordHash(passwordEncoder.encode("Password123!"));
                userRepository.save(u);
            });
            return;
        }

        log.info("Initializing StockPilot demo data...");

        // 1. Warehouses
        Warehouse whEast = warehouseRepository.save(new Warehouse("WH-EAST", "Central Hub East", "100 Logistics Blvd, New York, NY"));
        Warehouse whWest = warehouseRepository.save(new Warehouse("WH-WEST", "Pacific Distribution Center", "500 Harbor Way, Los Angeles, CA"));

        // 2. Users
        String encodedPass = passwordEncoder.encode("Password123!");
        User admin = userRepository.save(new User("stock_admin", encodedPass, "Alexander Vance (Admin)", "admin@stockpilot.io", Role.ADMIN, null));
        User mgrEast = userRepository.save(new User("wh_manager_east", encodedPass, "Elena Rostova (East Mgr)", "elena@stockpilot.io", Role.WAREHOUSE_MANAGER, whEast));
        User mgrWest = userRepository.save(new User("wh_manager_west", encodedPass, "Marcus Chen (West Mgr)", "marcus@stockpilot.io", Role.WAREHOUSE_MANAGER, whWest));
        User bob = userRepository.save(new User("customer_bob", encodedPass, "Bob Martinez (Buyer)", "bob@acme-enterprises.com", Role.CUSTOMER, null));
        User alice = userRepository.save(new User("customer_alice", encodedPass, "Alice Sterling (Retail)", "alice@starlight-tech.com", Role.CUSTOMER, null));

        // 3. Products
        Product pLaptop = productRepository.save(new Product("SKU-LAPTOP-01", "Dell XPS 15 Laptop", "15.6-inch OLED, 32GB RAM, 1TB SSD", new BigDecimal("1499.00"), 5));
        Product pMonitor = productRepository.save(new Product("SKU-MONITOR-02", "UltraWide 34\" Curved Monitor", "3440x1440 WQHD, 144Hz, HDR400", new BigDecimal("499.00"), 10));
        Product pKeyboard = productRepository.save(new Product("SKU-KEYBOARD-03", "Pro Mechanical Keyboard RGB", "Hot-swappable switches, PBT keycaps", new BigDecimal("129.00"), 15));
        Product pHeadset = productRepository.save(new Product("SKU-HEADSET-04", "Wireless ANC Studio Headset", "Active Noise Cancelling, 40h battery", new BigDecimal("199.00"), 8));
        Product pMouse = productRepository.save(new Product("SKU-MOUSE-05", "Ergonomic Precision Mouse", "4000 DPI sensor, Bluetooth + 2.4G", new BigDecimal("79.00"), 20));

        // 4. Initial Inventory
        // WH-EAST
        inventoryRepository.save(new Inventory(pLaptop, whEast, 25, 0));
        inventoryRepository.save(new Inventory(pMonitor, whEast, 40, 0));
        inventoryRepository.save(new Inventory(pKeyboard, whEast, 100, 0));
        inventoryRepository.save(new Inventory(pHeadset, whEast, 30, 0));
        inventoryRepository.save(new Inventory(pMouse, whEast, 150, 0));

        // WH-WEST (Headset is 4 on hand -> triggers low stock alert!)
        inventoryRepository.save(new Inventory(pLaptop, whWest, 15, 0));
        inventoryRepository.save(new Inventory(pMonitor, whWest, 20, 0));
        inventoryRepository.save(new Inventory(pKeyboard, whWest, 60, 0));
        inventoryRepository.save(new Inventory(pHeadset, whWest, 4, 0));
        inventoryRepository.save(new Inventory(pMouse, whWest, 80, 0));

        // 5. Initial Stock Movements
        stockMovementRepository.save(new StockMovement(pLaptop, whEast, MovementType.RECEIPT, 25, 0, 25, 0, 0, "PO-INIT-001", "PURCHASE_RECEIPT", mgrEast, "Initial stocking"));
        stockMovementRepository.save(new StockMovement(pMonitor, whEast, MovementType.RECEIPT, 40, 0, 40, 0, 0, "PO-INIT-001", "PURCHASE_RECEIPT", mgrEast, "Initial stocking"));
        stockMovementRepository.save(new StockMovement(pHeadset, whWest, MovementType.RECEIPT, 4, 0, 4, 0, 0, "PO-INIT-002", "PURCHASE_RECEIPT", mgrWest, "Initial stocking (low inventory)"));

        log.info("StockPilot demo data initialization complete!");
    }
}
