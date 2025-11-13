# Hunger Saviour - Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT APPLICATIONS                          │
│                    (Web Browser, Mobile Apps)                        │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                │ HTTPS
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY :8080                            │
│                    (Spring Cloud Gateway)                            │
│                                                                      │
│  • Request Routing                                                   │
│  • Load Balancing                                                    │
│  • CORS Configuration                                                │
└───────┬──────────┬──────────┬──────────┬────────────────────────────┘
        │          │          │          │
        │          │          │          │
        ▼          ▼          ▼          ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│  USER    │ │RESTAURANT│ │  ORDER   │ │ PAYMENT  │
│ SERVICE  │ │ SERVICE  │ │ SERVICE  │ │ SERVICE  │
│  :8081   │ │  :8082   │ │  :8083   │ │  :8084   │
└─────┬────┘ └─────┬────┘ └─────┬────┘ └─────┬────┘
      │            │            │            │
      │            │            │            │ Stripe API
      │            │            │            └────────────►
      │            │            │
      ▼            ▼            ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│PostgreSQL│ │PostgreSQL│ │PostgreSQL│ │PostgreSQL│
│  Users   │ │Restaurant│ │  Orders  │ │ Payments │
│   DB     │ │    DB    │ │    DB    │ │    DB    │
└──────────┘ └──────────┘ └──────────┘ └──────────┘
```

## Service Details

### 1. API Gateway (Port 8080)
**Technology:** Spring Cloud Gateway
**Responsibilities:**
- Route requests to appropriate microservices
- Centralized entry point for all API calls
- CORS management
- Future: Rate limiting, API versioning

**Routes:**
- `/api/auth/**` → User Service
- `/api/users/**` → User Service
- `/api/restaurants/**` → Restaurant Service
- `/api/orders/**` → Order Service
- `/api/payments/**` → Payment Service

---

### 2. User Service (Port 8081)
**Technology:** Spring Boot + Spring Security + JWT
**Database:** PostgreSQL (hunger_saviour_users)

**Features:**
- User registration and authentication
- JWT token generation and validation
- Password hashing with BCrypt
- User profile management

**Key Components:**
- `UserRepository`: JPA repository for user data
- `AuthService`: Business logic for authentication
- `JwtUtil`: JWT token utilities
- `SecurityConfig`: Spring Security configuration

**Entities:**
- User (id, email, password, fullName, phoneNumber, role, timestamps)

---

### 3. Restaurant Service (Port 8082)
**Technology:** Spring Boot + Spring Data JPA
**Database:** PostgreSQL (hunger_saviour_restaurants)

**Features:**
- Restaurant CRUD operations
- Menu management
- Restaurant search and filtering
- Owner-based restaurant management

**Key Components:**
- `RestaurantRepository`: JPA repository for restaurants
- `MenuItemRepository`: JPA repository for menu items
- `RestaurantService`: Business logic

**Entities:**
- Restaurant (id, name, address, cuisine, description, ownerId, isActive, timestamps)
- MenuItem (id, name, description, price, category, isAvailable, restaurantId, timestamps)

**Relationships:**
- One Restaurant → Many MenuItems

---

### 4. Order Service (Port 8083)
**Technology:** Spring Boot + Spring Data JPA
**Database:** PostgreSQL (hunger_saviour_orders)

**Features:**
- Order creation and management
- Order status tracking
- Order history by user/restaurant
- Order calculations

**Key Components:**
- `OrderRepository`: JPA repository for orders
- `OrderService`: Business logic for order processing

**Entities:**
- Order (id, userId, restaurantId, status, totalAmount, deliveryAddress, timestamps)
- OrderItem (id, orderId, menuItemId, menuItemName, quantity, price, subtotal)

**Relationships:**
- One Order → Many OrderItems

**Order Status Flow:**
```
PENDING → CONFIRMED → PREPARING → OUT_FOR_DELIVERY → DELIVERED
                                          ↓
                                     CANCELLED
```

---

### 5. Payment Service (Port 8084)
**Technology:** Spring Boot + Stripe API
**Database:** PostgreSQL (hunger_saviour_payments)
**External API:** Stripe Payment Processing

**Features:**
- Payment processing via Stripe
- Payment status tracking
- Refund processing
- Payment history

**Key Components:**
- `PaymentRepository`: JPA repository for payments
- `PaymentService`: Business logic with Stripe integration
- `StripeConfig`: Stripe API configuration

**Entities:**
- Payment (id, orderId, userId, amount, status, stripePaymentIntentId, stripeChargeId, paymentMethod, timestamps)

**Payment Flow:**
```
1. Client initiates payment with payment method ID
2. Service creates Stripe PaymentIntent
3. Stripe processes payment
4. Service stores payment record
5. Returns payment confirmation
```

---

## Data Flow Examples

### User Registration & Order Flow
```
1. User Registration
   Client → API Gateway → User Service
   User Service → PostgreSQL (Users DB)
   User Service → Client (JWT Token)

2. Browse Restaurants
   Client → API Gateway → Restaurant Service
   Restaurant Service → PostgreSQL (Restaurants DB)
   Restaurant Service → Client (Restaurant List)

3. View Menu
   Client → API Gateway → Restaurant Service
   Restaurant Service → PostgreSQL (Menu Items)
   Restaurant Service → Client (Menu List)

4. Create Order
   Client → API Gateway → Order Service
   Order Service → PostgreSQL (Orders DB)
   Order Service → Client (Order Confirmation)

5. Process Payment
   Client → API Gateway → Payment Service
   Payment Service → Stripe API
   Stripe API → Payment Service (Confirmation)
   Payment Service → PostgreSQL (Payments DB)
   Payment Service → Client (Payment Status)
```

---

## Security Architecture

### Authentication Flow
```
1. User Login
   POST /api/auth/login
   { email, password }
   
2. User Service validates credentials
   - Retrieve user from database
   - Compare hashed password using BCrypt
   
3. Generate JWT Token
   - Create JWT with email and role
   - Sign with HMAC-SHA256
   - Set 24-hour expiration
   
4. Return Token
   { token, email, fullName, role }

5. Subsequent Requests
   Authorization: Bearer <JWT_TOKEN>
   
6. JWT Validation
   - Extract token from header
   - Verify signature
   - Check expiration
   - Grant access
```

### Security Layers
```
┌─────────────────────────────────────┐
│         HTTPS/TLS Layer             │
│   (SSL Certificate in Production)   │
└─────────────────────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│      API Gateway Layer              │
│   • CORS Policy                     │
│   • Rate Limiting (future)          │
└─────────────────────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│    JWT Authentication Layer         │
│   • Token Validation                │
│   • User Authorization              │
└─────────────────────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│     Service Security Layer          │
│   • Input Validation                │
│   • SQL Injection Prevention        │
│   • Business Logic Authorization    │
└─────────────────────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│       Database Layer                │
│   • Connection Encryption           │
│   • Access Control                  │
│   • Data Encryption at Rest         │
└─────────────────────────────────────┘
```

---

## Deployment Architecture

### Docker Compose (Development/Local)
```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Network                           │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ Gateway  │  │   User   │  │Restaurant│  │  Order   │  │
│  │Container │  │Container │  │Container │  │Container │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ Payment  │  │PostgreSQL│  │PostgreSQL│  │PostgreSQL│  │
│  │Container │  │ Users DB │  │Restaurant│  │ Orders   │  │
│  └──────────┘  └──────────┘  │    DB    │  │   DB     │  │
│                               └──────────┘  └──────────┘  │
│                               ┌──────────┐                 │
│                               │PostgreSQL│                 │
│                               │ Payments │                 │
│                               │    DB    │                 │
│                               └──────────┘                 │
└─────────────────────────────────────────────────────────────┘
```

### Production Kubernetes (Example)
```
┌────────────────────────────────────────────────────────────┐
│                    Load Balancer                           │
└────────────────────────────┬───────────────────────────────┘
                             │
                             ▼
┌────────────────────────────────────────────────────────────┐
│              Kubernetes Cluster                            │
│                                                            │
│  ┌─────────────────┐  ┌─────────────────┐                │
│  │  API Gateway    │  │   User Service  │                │
│  │   Deployment    │  │   Deployment    │                │
│  │   (3 replicas)  │  │   (3 replicas)  │                │
│  └─────────────────┘  └─────────────────┘                │
│                                                            │
│  ┌─────────────────┐  ┌─────────────────┐                │
│  │Restaurant Svc   │  │  Order Service  │                │
│  │   Deployment    │  │   Deployment    │                │
│  │   (2 replicas)  │  │   (5 replicas)  │                │
│  └─────────────────┘  └─────────────────┘                │
│                                                            │
│  ┌─────────────────┐                                      │
│  │ Payment Service │                                      │
│  │   Deployment    │                                      │
│  │   (3 replicas)  │                                      │
│  └─────────────────┘                                      │
│                                                            │
│  ┌────────────────────────────────────┐                   │
│  │   Managed PostgreSQL (AWS RDS)     │                   │
│  │   • users_db                        │                   │
│  │   • restaurants_db                  │                   │
│  │   • orders_db                       │                   │
│  │   • payments_db                     │                   │
│  └────────────────────────────────────┘                   │
└────────────────────────────────────────────────────────────┘
```

---

## Technology Stack Summary

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.1.5 |
| API Gateway | Spring Cloud Gateway | 2022.0.4 |
| Security | Spring Security + JWT | 3.1.5 |
| JWT Library | JJWT | 0.11.5 |
| ORM | Spring Data JPA / Hibernate | 3.1.5 |
| Database | PostgreSQL | 15 |
| Payment | Stripe Java SDK | 24.0.0 |
| Build Tool | Maven | 3.6+ |
| Containerization | Docker | Latest |
| Orchestration | Docker Compose | Latest |

---

## Scalability Considerations

### Horizontal Scaling
- All services are stateless (except database)
- Load balancing at API Gateway level
- Independent scaling per service based on load

### Database Scaling
- Read replicas for heavy read operations
- Connection pooling (HikariCP)
- Separate databases per service (data isolation)

### Caching Strategy (Future Enhancement)
```
┌──────────┐
│  Redis   │ ← Cache frequently accessed data
│  Cache   │   • Restaurant listings
└──────────┘   • Menu items
               • User sessions (if needed)
```

---

## Monitoring & Observability (Recommended)

```
┌─────────────────────────────────────────────┐
│         Application Metrics                 │
│      (Spring Boot Actuator)                 │
└───────────────┬─────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────┐
│         Prometheus                          │
│      (Metrics Collection)                   │
└───────────────┬─────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────┐
│         Grafana                             │
│      (Metrics Visualization)                │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│         Application Logs                    │
│      (Logback)                              │
└───────────────┬─────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────┐
│         Elasticsearch                       │
│      (Log Aggregation)                      │
└───────────────┬─────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────┐
│         Kibana                              │
│      (Log Visualization)                    │
└─────────────────────────────────────────────┘
```

---

## Future Enhancements

1. **Service Discovery**: Add Eureka or Consul for dynamic service registration
2. **Message Queue**: Add RabbitMQ/Kafka for async communication
3. **Caching**: Implement Redis for improved performance
4. **API Documentation**: Add Swagger/OpenAPI for interactive API docs
5. **Monitoring**: Implement Prometheus + Grafana
6. **Logging**: Centralized logging with ELK stack
7. **Service Mesh**: Consider Istio for advanced service management
8. **Testing**: Add integration and end-to-end tests
9. **Notification Service**: Email/SMS notifications for orders
10. **Real-time Updates**: WebSocket support for order tracking

---

This architecture provides a solid foundation for a scalable, maintainable food ordering platform that can grow with business needs.
