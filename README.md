# StockPilot — Real-Time Multi-Warehouse Inventory & Order Platform

StockPilot is an enterprise-grade Inventory and Order Management platform engineered with **Spring Boot 3.3.4**, **Java 21**, **PostgreSQL 16**, **Hibernate/JPA**, and **Spring WebSocket STOMP**.

It solves the classic e-commerce overselling problem under high concurrency using row-level pessimistic write locking, deterministic resource acquisition to prevent deadlocks, an immutable double-entry movement ledger, automated reservation timeout reapers, and formula injection-safe CSV exports.

---

## 1. Key Architectural Features

- **Pessimistic Concurrency Control**: Guarantees zero overselling when multiple concurrent orders compete for limited stock via `SELECT ... FOR UPDATE` row locks.
- **Deadlock-Free Resource Ordering**: Multi-item orders sort required product IDs in strict ascending order before acquiring pessimistic locks, preventing circular wait conditions.
- **Double-Entry Stock Movement Ledger**: Every addition, reservation, fulfillment, cancellation, or adjustment creates an immutable audit record in `stock_movements`.
- **Automated Reservation Expiry**: Stale unfulfilled reservations automatically expire via a background `@Scheduled` reaper after 15 minutes, returning stock to the available pool.
- **Formula Injection Defense**: All exported CSV files neutralize spreadsheet formula execution (`=`, `+`, `-`, `@`, `\t`, `\r` prefixes) via RFC-compliant quoting and prefixing.
- **Real-Time STOMP WebSockets**: Broadcasts live stock level fluctuations and low-stock alerts to warehouse operators without full-page reloads.

---

## 2. Order Lifecycle State Machine

```
              ┌──────────────┐
              │   PENDING    │
              └──────┬───────┘
                     │ (Atomic stock reservation)
                     ▼
              ┌──────────────┐  (15 min timeout)  ┌──────────────┐
              │   RESERVED   ├───────────────────►│   EXPIRED    │
              └───┬──────┬───┘                    └──────────────┘
                  │      │ (User cancels)                ▲
     (Customer    │      └────────────────────────┐      │ (Stock released)
      pays)       │                               ▼      │
                  ▼                        ┌─────────────┴┐
              ┌──────────────┐             │  CANCELLED   │
              │     PAID     ├────────────►└──────────────┘
              └───┬──────────┘ (Cancellation before fulfillment)
                  │
   (Warehouse Mgr │ Fulfill & deduct on-hand)
                  ▼
              ┌──────────────┐
              │  FULFILLED   │
              └──────────────┘
```

---

## 3. Preloaded Demo Accounts

All demo accounts share the password: `Password123!`

| Username | Role | Scope | Description |
|---|---|---|---|
| `stock_admin` | `ADMIN` | Global | Can manage products, warehouses, and full audit logs |
| `wh_manager_east` | `WAREHOUSE_MANAGER` | `WH-EAST` (New York) | Can receive shipments and adjust East hub inventory |
| `wh_manager_west` | `WAREHOUSE_MANAGER` | `WH-WEST` (Los Angeles)| Can receive shipments and adjust West hub inventory |
| `customer_bob` | `CUSTOMER` | Buyer | Places orders and reviews order history |
| `customer_alice` | `CUSTOMER` | Retail Buyer | Places orders and reviews order history |

---

## 4. Quickstart Guide

### Option A: Running with Docker Compose (Recommended)

```bash
# Modern Docker Compose V2 (recommended, standard in Cloud Shell & modern Linux/Mac/Windows)
docker compose up --build

# Or legacy Docker Compose V1
docker-compose up --build
```
The application will launch on `http://localhost:8083`.

### Option B: Running Locally with Maven

```bash
# Ensure PostgreSQL is running on port 5432
export DB_URL=jdbc:postgresql://localhost:5432/stockpilotdb
export DB_USERNAME=stockpilot_user
export DB_PASSWORD=stockpilot_pass

./mvnw spring-boot:run
```

---

## 5. Running Automated Tests

Run the full test suite including the high-concurrency race condition test and CSV sanitization tests:

```bash
./mvnw test
```
