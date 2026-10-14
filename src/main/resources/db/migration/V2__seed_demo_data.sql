-- StockPilot Demo Seed Data Migration (V2)

-- 1. Warehouses
INSERT INTO warehouses (id, code, name, location, is_active)
VALUES
    (1, 'WH-EAST', 'Central Hub East', '100 Logistics Blvd, New York, NY', TRUE),
    (2, 'WH-WEST', 'Pacific Distribution Center', '500 Harbor Way, Los Angeles, CA', TRUE);

-- 2. Products
INSERT INTO products (id, sku, name, description, unit_price, low_stock_threshold, is_active)
VALUES
    (1, 'SKU-LAPTOP-01', 'Dell XPS 15 Laptop', '15.6-inch OLED, 32GB RAM, 1TB SSD, Intel Core i7', 1499.00, 5, TRUE),
    (2, 'SKU-MONITOR-02', 'UltraWide 34" Curved Monitor', '3440x1440 WQHD, 144Hz, HDR400, USB-C Hub', 499.00, 10, TRUE),
    (3, 'SKU-KEYBOARD-03', 'Pro Mechanical Keyboard RGB', 'Hot-swappable switches, PBT keycaps, wireless 2.4G', 129.00, 15, TRUE),
    (4, 'SKU-HEADSET-04', 'Wireless ANC Studio Headset', 'Active Noise Cancelling, 40h battery, High-res audio', 199.00, 8, TRUE),
    (5, 'SKU-MOUSE-05', 'Ergonomic Precision Mouse', '4000 DPI sensor, Bluetooth + 2.4G, hyper-scroll wheel', 79.00, 20, TRUE);

-- 3. Users (Password: Password123!)
-- BCrypt: $2a$10$wzVd4h1Yy0F5f7v4zY4K3.lEaJ7FjFfLwzM.E4s0pBwK.Gq2PzBca
INSERT INTO users (id, username, password_hash, full_name, email, role, assigned_warehouse_id, enabled)
VALUES
    (1, 'stock_admin', '$2a$10$wzVd4h1Yy0F5f7v4zY4K3.lEaJ7FjFfLwzM.E4s0pBwK.Gq2PzBca', 'Alexander Vance (Admin)', 'admin@stockpilot.io', 'ADMIN', NULL, TRUE),
    (2, 'wh_manager_east', '$2a$10$wzVd4h1Yy0F5f7v4zY4K3.lEaJ7FjFfLwzM.E4s0pBwK.Gq2PzBca', 'Elena Rostova (East Mgr)', 'elena@stockpilot.io', 'WAREHOUSE_MANAGER', 1, TRUE),
    (3, 'wh_manager_west', '$2a$10$wzVd4h1Yy0F5f7v4zY4K3.lEaJ7FjFfLwzM.E4s0pBwK.Gq2PzBca', 'Marcus Chen (West Mgr)', 'marcus@stockpilot.io', 'WAREHOUSE_MANAGER', 2, TRUE),
    (4, 'customer_bob', '$2a$10$wzVd4h1Yy0F5f7v4zY4K3.lEaJ7FjFfLwzM.E4s0pBwK.Gq2PzBca', 'Bob Martinez (Buyer)', 'bob@acme-enterprises.com', 'CUSTOMER', NULL, TRUE),
    (5, 'customer_alice', '$2a$10$wzVd4h1Yy0F5f7v4zY4K3.lEaJ7FjFfLwzM.E4s0pBwK.Gq2PzBca', 'Alice Sterling (Retail)', 'alice@starlight-tech.com', 'CUSTOMER', NULL, TRUE);

-- 4. Initial Inventory
-- WH-EAST
INSERT INTO inventory (id, product_id, warehouse_id, quantity_on_hand, quantity_reserved, version)
VALUES
    (1, 1, 1, 25, 0, 0), -- Dell XPS: 25 on hand
    (2, 2, 1, 40, 0, 0), -- Monitor: 40 on hand
    (3, 3, 1, 100, 0, 0), -- Keyboard: 100 on hand
    (4, 4, 1, 30, 0, 0), -- Headset: 30 on hand
    (5, 5, 1, 150, 0, 0); -- Mouse: 150 on hand

-- WH-WEST
INSERT INTO inventory (id, product_id, warehouse_id, quantity_on_hand, quantity_reserved, version)
VALUES
    (6, 1, 2, 15, 0, 0), -- Dell XPS: 15 on hand
    (7, 2, 2, 20, 0, 0), -- Monitor: 20 on hand
    (8, 3, 2, 60, 0, 0), -- Keyboard: 60 on hand
    (9, 4, 2, 4, 0, 0), -- Headset: 4 on hand (BELOW THRESHOLD of 8 -> LOW STOCK ALERT DEMO)
    (10, 5, 2, 80, 0, 0); -- Mouse: 80 on hand

-- 5. Initial Stock Movement Audit Records
INSERT INTO stock_movements (product_id, warehouse_id, movement_type, quantity_change, previous_on_hand, new_on_hand, previous_reserved, new_reserved, reference_id, reference_type, performed_by_user_id, notes)
VALUES
    (1, 1, 'RECEIPT', 25, 0, 25, 0, 0, 'PO-INITIAL-001', 'PURCHASE_RECEIPT', 2, 'Initial warehouse stocking'),
    (2, 1, 'RECEIPT', 40, 0, 40, 0, 0, 'PO-INITIAL-001', 'PURCHASE_RECEIPT', 2, 'Initial warehouse stocking'),
    (3, 1, 'RECEIPT', 100, 0, 100, 0, 0, 'PO-INITIAL-001', 'PURCHASE_RECEIPT', 2, 'Initial warehouse stocking'),
    (4, 1, 'RECEIPT', 30, 0, 30, 0, 0, 'PO-INITIAL-001', 'PURCHASE_RECEIPT', 2, 'Initial warehouse stocking'),
    (5, 1, 'RECEIPT', 150, 0, 150, 0, 0, 'PO-INITIAL-001', 'PURCHASE_RECEIPT', 2, 'Initial warehouse stocking'),
    (1, 2, 'RECEIPT', 15, 0, 15, 0, 0, 'PO-INITIAL-002', 'PURCHASE_RECEIPT', 3, 'West coast branch launch stock'),
    (2, 2, 'RECEIPT', 20, 0, 20, 0, 0, 'PO-INITIAL-002', 'PURCHASE_RECEIPT', 3, 'West coast branch launch stock'),
    (3, 2, 'RECEIPT', 60, 0, 60, 0, 0, 'PO-INITIAL-002', 'PURCHASE_RECEIPT', 3, 'West coast branch launch stock'),
    (4, 2, 'RECEIPT', 4, 0, 4, 0, 0, 'PO-INITIAL-002', 'PURCHASE_RECEIPT', 3, 'West coast branch launch stock'),
    (5, 2, 'RECEIPT', 80, 0, 80, 0, 0, 'PO-INITIAL-002', 'PURCHASE_RECEIPT', 3, 'West coast branch launch stock');

-- Update sequence values
SELECT setval('warehouses_id_seq', (SELECT MAX(id) FROM warehouses));
SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('inventory_id_seq', (SELECT MAX(id) FROM inventory));
SELECT setval('stock_movements_id_seq', (SELECT MAX(id) FROM stock_movements));
