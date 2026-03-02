# 🛒 MiNu Marketplace

A full-stack **Mini Marketplace** web application built for a Software Engineering Lab project.

---

## 📋 Table of Contents

- [Tech Stack](#-tech-stack)
- [Architecture & Design Patterns](#-architecture--design-patterns)
- [How to Run](#-how-to-run)
- [Default Login Credentials](#-default-login-credentials)
- [How to Access Admin Panel](#-how-to-access-admin-panel)
- [How Admin Verification Works](#-how-admin-verification-works)
- [Role-Based Access Control (RBAC)](#-role-based-access-control-rbac)
- [Database Schema](#-database-schema)
- [Project Structure](#-project-structure)
- [All Pages & URLs](#-all-pages--urls)
- [What Was Done (Change Log)](#-what-was-done-change-log)

---

## 🔧 Tech Stack

| Component        | Technology                          |
|------------------|-------------------------------------|
| Backend          | Spring Boot 3.2.5 (Java 17)        |
| View Engine      | Thymeleaf + Bootstrap 5             |
| Security         | Spring Security 6 (role-based)      |
| ORM              | Spring Data JPA / Hibernate 6       |
| Database         | PostgreSQL 16 (Docker)              |
| Build Tool       | Maven (via `mvnw` wrapper)          |
| Containerization | Docker Compose                      |
| Annotations      | Lombok                              |

---

## 🏛 Architecture & Design Patterns

| Pattern              | Implementation                                              |
|----------------------|-------------------------------------------------------------|
| **Layered Architecture** | Controller → Service → Repository → Database            |
| **MVC Pattern**      | Controller (routes) + Thymeleaf (views) + Model (entities)  |
| **Repository Pattern** | Spring Data JPA interfaces for each entity                |
| **Service Pattern**  | Business logic separated in `@Service` classes              |
| **DTO Pattern**      | `UserRegistrationDto`, `ProductDto`, `StoreDto`, etc.       |
| **RBAC**             | 3 roles: `ADMIN`, `SELLER`, `CUSTOMER` via Spring Security  |

---

## 🚀 How to Run

### Prerequisites
- **Java 17+** installed
- **Docker Desktop** running
- **Maven** (or use the included `mvnw` wrapper)

### Step 1: Start PostgreSQL via Docker

```bash
cd "E:\3_2\project\SWE project\MiNu"
docker compose down -v
docker compose up -d
```

This starts PostgreSQL 16 on **port 5433** with:
- Database: `minu_db`
- Username: `minu_user`
- Password: `minu_secret`
- Data persisted in Docker volume `postgres_data`

Verify it's running:
```bash
docker exec minu-postgres pg_isready -U minu_user -d minu_db
```
Expected output: `accepting connections`

### Step 2: Kill any old Java processes (if needed)

```bash
taskkill /F /IM java.exe
```

### Step 3: Build and Run Spring Boot

```bash
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run
```

### Step 4: Open Browser

Go to: **http://localhost:8080**

---

## 🔑 Default Login Credentials

These accounts are **auto-created** on first startup by `DataInitializer.java`:

| Role     | Username   | Password     | Verified? |
|----------|------------|--------------|-----------|
| ADMIN    | `admin`    | `admin123`   | ✅ Yes    |
| SELLER   | `seller`   | `seller123`  | ✅ Yes    |
| CUSTOMER | `customer` | `customer123`| ✅ Yes    |

> ⚠️ **New users who register via the registration form start as UNVERIFIED.**
> Only the default seed users above are pre-verified.

---

## 🔐 How to Access Admin Panel

### Step-by-step:

1. **Go to** → `http://localhost:8080/login`
2. **Enter:**
   - Username: `admin`
   - Password: `admin123`
3. **Click Login**
4. You will be **automatically redirected** to the **Admin Dashboard** at `/admin/dashboard`

### Admin Navbar Links (visible only when logged in as admin):

| Link             | URL                    | What it does                         |
|------------------|------------------------|--------------------------------------|
| 📊 Dashboard     | `/admin/dashboard`     | Overview with counts & quick actions |
| Users            | `/admin/users`         | View, verify, reject, delete users   |
| Stores           | `/admin/stores`        | View all seller stores               |
| All Products     | `/admin/products`      | View & delete any product            |
| All Orders       | `/admin/orders`        | View & update order status           |

### Direct URLs (when logged in as admin):

- Dashboard: `http://localhost:8080/admin/dashboard`
- Users: `http://localhost:8080/admin/users`
- Stores: `http://localhost:8080/admin/stores`
- Products: `http://localhost:8080/admin/products`
- Orders: `http://localhost:8080/admin/orders`

---

## ✅ How Admin Verification Works

### The Problem It Solves:
When a new **Seller** or **Customer** registers, they should **NOT** get full access immediately. An Admin must review and verify them first.

### Registration Flow:
```
New User Registers → verified = FALSE → Can login but cannot:
  • Seller: Cannot create store, cannot add products
  • Customer: Cannot place orders
```

### Admin Verification Flow:
```
1. Admin logs in  →  http://localhost:8080/login  (admin / admin123)
2. Goes to        →  http://localhost:8080/admin/users
3. Sees all users in a table with their verification status
4. For UNVERIFIED users, admin sees these buttons:

   [Verify]  → Sets verified = TRUE  → User can now use all features
   [Reject]  → DELETES the user from the database
   [Delete]  → DELETES the user (and cascades: deletes their store & products)

5. For ADMIN users → No action buttons shown (cannot delete yourself)
```

### What Each Button Does:

| Button   | Action                          | When shown                      |
|----------|---------------------------------|---------------------------------|
| **Verify** | Sets `verified = true`        | Only for unverified users       |
| **Reject** | Deletes user from database    | Only for unverified users       |
| **Delete** | Deletes user + store + products | Always (except for ADMIN role) |

### Visual Indicators:
- ✅ **Green badge** `✓ Verified` — user is active
- ⚠️ **Yellow badge** `✗ Unverified` — user is pending review

---

## 👥 Role-Based Access Control (RBAC)

### ADMIN — Full System Control
- ✅ View all users, stores, products, orders
- ✅ Verify / Reject user registrations
- ✅ Delete any seller (cascades: deletes their store & products)
- ✅ Delete any customer
- ✅ Delete any product
- ✅ Change order status (PENDING → PAID → SHIPPED → DELIVERED → CANCELLED)
- ❌ Admin does NOT buy products

### SELLER — Vendor Role
- ❌ Registers as unverified (cannot do anything until admin verifies)
- ✅ After verification: Create **exactly ONE store**
- ✅ Add products to their store
- ✅ Update / Delete their own products only
- ❌ Cannot modify other sellers' products
- ❌ Cannot delete users

### CUSTOMER — Buyer Role
- ❌ Registers as unverified (cannot place orders until admin verifies)
- ✅ Browse all products (even when unverified)
- ✅ After verification: Add to cart, place orders
- ✅ View own order history
- ❌ Cannot manage products or stores

---

## 🗄 Database Schema

### Entity Relationship Diagram (Text)

```
Users (1) ──── (1) Store ──── (N) Products
  │
  │ (1:N)
  │
Orders ──── (N) OrderItems ──── (1) Products
```

### Tables

#### `users`
| Column      | Type         | Notes                      |
|-------------|--------------|----------------------------|
| id          | BIGINT PK    | Auto-generated             |
| username    | VARCHAR(50)  | Unique, not null           |
| email       | VARCHAR(100) | Unique, not null           |
| password    | VARCHAR      | BCrypt encoded             |
| role        | VARCHAR(20)  | ADMIN / SELLER / CUSTOMER  |
| verified    | BOOLEAN      | Default: false             |
| created_at  | TIMESTAMP    | Auto-set                   |
| updated_at  | TIMESTAMP    | Auto-set                   |

#### `stores`
| Column      | Type         | Notes                      |
|-------------|--------------|----------------------------|
| id          | BIGINT PK    | Auto-generated             |
| name        | VARCHAR(200) | Not null                   |
| description | TEXT         |                            |
| seller_id   | BIGINT FK    | Unique → users(id)         |
| created_at  | TIMESTAMP    | Auto-set                   |

#### `products`
| Column         | Type           | Notes                   |
|----------------|----------------|-------------------------|
| id             | BIGINT PK      | Auto-generated          |
| name           | VARCHAR(200)   | Not null                |
| description    | TEXT           |                         |
| price          | DECIMAL(10,2)  | BigDecimal              |
| stock_quantity | INT            | Not null                |
| image_url      | VARCHAR        | Optional                |
| store_id       | BIGINT FK      | → stores(id)            |
| created_at     | TIMESTAMP      | Auto-set                |
| updated_at     | TIMESTAMP      | Auto-set                |

#### `orders`
| Column       | Type           | Notes                    |
|--------------|----------------|--------------------------|
| id           | BIGINT PK      | Auto-generated           |
| customer_id  | BIGINT FK      | → users(id)              |
| status       | VARCHAR(20)    | PENDING/PAID/CONFIRMED/SHIPPED/DELIVERED/CANCELLED |
| total_amount | DECIMAL(12,2)  | BigDecimal               |
| created_at   | TIMESTAMP      | Auto-set                 |
| updated_at   | TIMESTAMP      | Auto-set                 |

#### `order_items`
| Column            | Type          | Notes               |
|-------------------|---------------|----------------------|
| id                | BIGINT PK     | Auto-generated       |
| order_id          | BIGINT FK     | → orders(id)         |
| product_id        | BIGINT FK     | → products(id)       |
| quantity          | INT           | Not null             |
| price_at_purchase | DECIMAL(10,2) | BigDecimal           |

### JPA Relationships
- `User` (1) ↔ (1) `Store` — OneToOne, cascade ALL
- `Store` (1) ↔ (N) `Product` — OneToMany, cascade ALL
- `User` (1) ↔ (N) `Order` — OneToMany, cascade ALL
- `Order` (1) ↔ (N) `OrderItem` — OneToMany, cascade ALL
- `OrderItem` (N) → (1) `Product` — ManyToOne

### Cascade Rules
- Delete a **Seller** → Their **Store** is deleted → All **Products** in that store are deleted
- Delete a **Customer** → All their **Orders** are deleted → All **OrderItems** are deleted

---

## 📁 Project Structure

```
src/main/java/com/minuStore/MiNu/
├── MiNuApplication.java              # Spring Boot entry point
├── config/
│   ├── SecurityConfig.java           # Spring Security config (RBAC rules)
│   ├── CustomLoginSuccessHandler.java # Redirects each role after login
│   └── DataInitializer.java          # Seeds admin/seller/customer on startup
├── controller/
│   ├── HomeController.java           # GET /
│   ├── AuthController.java           # GET/POST /login, /register
│   ├── AdminController.java          # /admin/** (dashboard, users, stores, products, orders)
│   ├── SellerController.java         # /seller/** (store, products CRUD)
│   ├── ProductController.java        # /products (public browsing)
│   └── OrderController.java          # /cart/**, /orders/** (customer cart & checkout)
├── service/
│   ├── UserService.java              # Register, verify, reject, delete users
│   ├── StoreService.java             # Create/update store per seller
│   ├── ProductService.java           # CRUD products (with ownership checks)
│   ├── OrderService.java             # Place orders, update status
│   ├── CartService.java              # Session-scoped shopping cart
│   └── CustomUserDetailsService.java # Spring Security user loader
├── repository/
│   ├── UserRepository.java
│   ├── StoreRepository.java
│   ├── ProductRepository.java
│   ├── OrderRepository.java
│   └── OrderItemRepository.java
├── model/
│   ├── User.java                     # implements UserDetails
│   ├── Store.java
│   ├── Product.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── Role.java                     # enum: ADMIN, SELLER, CUSTOMER
│   └── OrderStatus.java              # enum: PENDING, PAID, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
└── dto/
    ├── UserRegistrationDto.java
    ├── StoreDto.java
    ├── ProductDto.java
    ├── OrderDto.java
    └── OrderItemDto.java

src/main/resources/
├── application.yaml                  # DB config, JPA settings
├── static/css/style.css
└── templates/
    ├── index.html                    # Home page
    ├── login.html                    # Login form
    ├── register.html                 # Registration form (choose CUSTOMER or SELLER)
    ├── fragments/layout.html         # Navbar + footer (role-aware)
    ├── admin/
    │   ├── dashboard.html            # Admin dashboard with counts
    │   ├── users.html                # User table with Verify/Reject/Delete buttons
    │   ├── stores.html               # All stores table
    │   ├── products.html             # All products table with Delete
    │   └── orders.html               # All orders with status update
    ├── seller/
    │   ├── store.html                # Create/view/update store
    │   ├── store-form.html           # Unverified seller message
    │   ├── products.html             # Seller's product list
    │   ├── product-form.html         # Add/edit product form
    │   └── orders.html               # Seller orders info
    ├── products/
    │   ├── list.html                 # Public product listing with search
    │   └── detail.html               # Single product detail + add to cart
    └── orders/
        ├── cart.html                 # Shopping cart
        └── list.html                 # Customer order history
```

---

## 🌐 All Pages & URLs

### Public (No login required)
| URL              | Page                    |
|------------------|-------------------------|
| `/`              | Home page               |
| `/login`         | Login form              |
| `/register`      | Registration form       |
| `/products`      | Browse all products     |
| `/products/{id}` | Product detail          |

### Admin Only (Role: ADMIN)
| URL                            | Page / Action                |
|--------------------------------|------------------------------|
| `/admin/dashboard`             | Dashboard with counts        |
| `/admin/users`                 | User management table        |
| `POST /admin/users/{id}/verify`| Verify a user                |
| `POST /admin/users/{id}/reject`| Reject (delete) a user       |
| `POST /admin/users/{id}/delete`| Delete a user + cascade      |
| `/admin/stores`                | View all stores              |
| `/admin/products`              | View all products            |
| `POST /admin/products/{id}/delete` | Delete any product       |
| `/admin/orders`                | View all orders              |
| `POST /admin/orders/{id}/status`   | Update order status      |

### Seller Only (Role: SELLER)
| URL                              | Page / Action              |
|----------------------------------|----------------------------|
| `/seller/store`                  | View/create/update store   |
| `/seller/products`               | List own products          |
| `/seller/products/new`           | Add new product form       |
| `POST /seller/products`          | Create product             |
| `/seller/products/{id}/edit`     | Edit product form          |
| `POST /seller/products/{id}/edit`| Update product             |
| `POST /seller/products/{id}/delete` | Delete own product      |

### Customer Only (Role: CUSTOMER)
| URL                    | Page / Action            |
|------------------------|--------------------------|
| `POST /cart/add`       | Add product to cart      |
| `/cart`                | View shopping cart       |
| `POST /cart/remove`    | Remove item from cart    |
| `POST /orders/checkout`| Place order              |
| `/orders`              | View order history       |

---

## 📝 What Was Done (Change Log)

### 1. Fixed Critical Build Issues
- **Spring Boot 4.0.3 → 3.2.5** (v4 doesn't exist; was causing total build failure)
- **Java 21 → 17** (per project requirements)
- **`spring-boot-starter-webmvc` → `spring-boot-starter-web`** (former doesn't exist)
- **Removed non-existent test dependencies** (`spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-test`, `spring-boot-starter-thymeleaf-test`, `spring-boot-starter-webmvc-test`) → replaced with correct `spring-boot-starter-test` + `spring-security-test`
- **Added Lombok version** to `maven-compiler-plugin` annotation processor path (was causing `version can neither be null` error)

### 2. Fixed Spring Security Configuration
- **`DaoAuthenticationProvider`** — changed from constructor-based (Spring Boot 4 API) to setter-based `setUserDetailsService()` (Spring Boot 3 API)
- Added **`@EnableMethodSecurity`** for `@PreAuthorize` support
- Created **`CustomLoginSuccessHandler`** — redirects each role to the correct page after login:
  - ADMIN → `/admin/dashboard`
  - SELLER → `/seller/store`
  - CUSTOMER → `/products`

### 3. Added User Verification System
- Added **`verified`** boolean field to `User` entity (default: `false`)
- New users register as **unverified** — cannot use protected features
- Admin can **Verify**, **Reject**, or **Delete** users from `/admin/users`
- Seed users (`admin`, `seller`, `customer`) are pre-verified
- **ADMIN role cannot be self-registered** (registration form only allows CUSTOMER/SELLER)

### 4. Added Store Entity & Logic
- Created **`Store`** entity with `OneToOne` relationship to `User` (seller)
- Created **`StoreRepository`**, **`StoreService`**, **`StoreDto`**
- **`Product`** now belongs to `Store` (not directly to User) — `ManyToOne` to Store
- Seller must **create a store first** before adding products
- Seller can have **exactly one store**
- Deleting a seller **cascades** → deletes store → deletes all products

### 5. Added Admin Dashboard
- Created **`/admin/dashboard`** page with summary counts (users, unverified, stores, orders)
- Quick-action links to all admin pages

### 6. Complete Admin CRUD Operations
- **Verify user** — `POST /admin/users/{id}/verify`
- **Reject user** — `POST /admin/users/{id}/reject` (deletes)
- **Delete user** — `POST /admin/users/{id}/delete` (cascades)
- **Delete any product** — `POST /admin/products/{id}/delete`
- **Update order status** — `POST /admin/orders/{id}/status`
- **View all stores** — `GET /admin/stores`
- **View all products** — `GET /admin/products`

### 7. Updated Seller Flow
- Seller must be verified → then create store → then add products
- Ownership checks: seller can only edit/delete their own products
- Updated `SellerController` to use `Store` entity

### 8. Updated Customer Flow
- Customer must be verified before placing orders
- Cart is session-scoped
- Order reduces product stock on checkout

### 9. Updated All Templates
- **Navbar** — role-aware links (admin sees dashboard/users/stores/products/orders; seller sees store/products; customer sees cart/orders)
- **Admin users page** — table with Verify/Reject/Delete buttons and verification badges
- **Admin dashboard** — card layout with counts
- **Admin stores/products pages** — new templates
- **Seller store page** — create or update store
- **Product detail** — shows store name and seller name

### 10. Docker & Database Configuration
- **`compose.yaml`** — PostgreSQL 16 on port 5433, persistent volume `postgres_data`
- **`application.yaml`** — JDBC URL `localhost:5433/minu_db`, `ddl-auto=update`
- Database auto-creates/updates all tables via Hibernate

---

## 🔄 Quick Start Cheatsheet

```bash
# 1. Start database
docker compose up -d

# 2. Kill old Java processes (if any)
taskkill /F /IM java.exe

# 3. Build & Run
.\mvnw.cmd clean compile
.\mvnw.cmd spring-boot:run

# 4. Open browser
# Home:  http://localhost:8080
# Login: http://localhost:8080/login
# Admin: login as admin/admin123 → auto-redirects to /admin/dashboard
```

---

## ❓ FAQ

**Q: How do I log in as admin?**
> Go to `http://localhost:8080/login`, enter `admin` / `admin123`. You'll be redirected to the admin dashboard.

**Q: How do I verify a new user?**
> Login as admin → click "Users" in navbar → find the unverified user → click the green "Verify" button.

**Q: What happens if I reject a user?**
> The user is permanently deleted from the database.

**Q: What happens if I delete a seller?**
> The seller, their store, and all their products are deleted (cascade).

**Q: Can a user register as ADMIN?**
> No. The registration form only allows CUSTOMER or SELLER. The admin account is seeded automatically.

**Q: Why can't the seller add products?**
> Either: (1) their account is not verified yet, or (2) they haven't created a store yet. Go to `/seller/store` first.

**Q: Port 8080 already in use?**
> Run `taskkill /F /IM java.exe` to kill old Java processes, then restart.

**Q: Database connection refused?**
> Run `docker compose up -d` and wait a few seconds for PostgreSQL to start.
