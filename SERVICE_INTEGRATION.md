# Service Integration Guide

This document explains how the Hunger Saviour microservices interact with each other to create a complete food ordering and delivery flow, similar to apps like Swiggy and Uber Eats.

## Overview

The system now implements a fully integrated order flow where:
1. Orders are automatically validated against user and restaurant data
2. Payments are processed through the Payment Service (Stripe integration)
3. Notifications are sent to both customers and restaurant owners
4. Order status updates trigger real-time notifications
5. The complete order lifecycle is managed end-to-end

## Service Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────────────────────┐
│                        API Gateway :8080                         │
└────────┬───────────────────────────────────────────────┬────────┘
         │                                                │
         ▼                                                ▼
┌─────────────────┐                            ┌──────────────────┐
│  Order Service  │◄──────────────────────────►│  User Service    │
│     :8083       │                            │     :8081        │
└────┬────┬───────┘                            └──────────────────┘
     │    │
     │    └──────────────────────┐
     │                           │
     ▼                           ▼
┌──────────────────┐    ┌──────────────────┐
│ Payment Service  │    │Restaurant Service│
│     :8084        │    │     :8082        │
└────────┬─────────┘    └──────┬───────────┘
         │                     │
         │                     │
         └──────┬──────────────┘
                │
                ▼
         ┌─────────────┐
         │  RabbitMQ   │
         └──────┬──────┘
                │
                ▼
         ┌─────────────────┐
         │Notification Svc │
         │     :8085       │
         └─────────────────┘
```

## Complete Order Flow

### Step-by-Step Process

#### 1. Order Creation (POST /api/orders)

**Request:**
```json
{
  "userId": 1,
  "restaurantId": 1,
  "deliveryAddress": "123 Main St, City",
  "paymentMethodId": "pm_card_visa",
  "items": [
    {
      "menuItemId": 1,
      "menuItemName": "Margherita Pizza",
      "quantity": 2,
      "price": 12.99
    }
  ]
}
```

**What Happens:**

1. **User Validation**
   - Order Service calls User Service: `GET /api/users/{userId}`
   - Validates user exists and retrieves email for notifications
   ```java
   UserResponse user = userServiceClient.getUserById(request.getUserId());
   ```

2. **Restaurant Validation**
   - Order Service calls Restaurant Service: `GET /api/restaurants/{restaurantId}`
   - Validates restaurant exists and retrieves owner email
   ```java
   RestaurantResponse restaurant = restaurantServiceClient.getRestaurantById(request.getRestaurantId());
   ```

3. **Order Creation**
   - Creates order with status: `PENDING`
   - Calculates total amount from order items
   - Saves order to database
   - **Publishes event** to RabbitMQ: "ORDER_PLACED"
   
4. **Payment Processing**
   - Updates order status to: `PAYMENT_PROCESSING`
   - Order Service calls Payment Service: `POST /api/payments`
   ```java
   PaymentResponse payment = paymentServiceClient.processPayment(paymentRequest);
   ```
   - Payment Service processes payment via Stripe API
   - Returns payment status (SUCCESS/FAILED)

5. **Order Confirmation** (if payment succeeds)
   - Updates order status to: `CONFIRMED`
   - Stores payment ID in order
   - **Publishes event** to RabbitMQ: "ORDER_CONFIRMED"
   - Both customer and restaurant receive notification

6. **Preparation Started**
   - Automatically updates status to: `PREPARING`
   - **Publishes event** to RabbitMQ: "PREPARING"
   - Restaurant owner receives notification to start preparing

#### 2. Notification Flow

**RabbitMQ Configuration:**
- Exchange: `order.exchange` (Topic)
- Queue: `order.status.queue`
- Routing Key: `order.status`

**Event Structure:**
```json
{
  "orderId": 1,
  "userId": 1,
  "userEmail": "customer@example.com",
  "restaurantId": 1,
  "restaurantName": "Pizza Palace",
  "restaurantEmail": "owner@pizzapalace.com",
  "status": "CONFIRMED",
  "totalAmount": 25.98,
  "deliveryAddress": "123 Main St"
}
```

**Notification Service Processing:**
1. Listens to `order.status.queue`
2. For each event:
   - Sends email to **customer** with order status update
   - Sends email to **restaurant owner** with order details
3. Different email templates for different statuses

**Customer Email Example (CONFIRMED):**
```
Dear Customer,

Great news! Your order has been confirmed and payment was successful.

Order Details:
Order ID: #1
Restaurant: Pizza Palace
Status: Order Confirmed
Total Amount: $25.98
Delivery Address: 123 Main St

Thank you for choosing Hunger Saviour!
```

**Restaurant Email Example (PREPARING):**
```
Dear Restaurant Partner,

You have a new order to prepare!

Order Details:
Order ID: #1
Status: Preparing Your Order
Total Amount: $25.98
Delivery Address: 123 Main St

⚠️ Please start preparing this order immediately.

Please log in to your dashboard to view full order details.
```

#### 3. Status Updates (PUT /api/orders/{id}/status)

Restaurant or delivery personnel can update order status:

**Request:**
```json
{
  "status": "OUT_FOR_DELIVERY"
}
```

**What Happens:**
1. Order Service updates status in database
2. Fetches user and restaurant details
3. **Publishes event** to RabbitMQ: "STATUS_UPDATE"
4. Both customer and restaurant receive notification

**Possible Status Transitions:**
```
PENDING 
  → PAYMENT_PROCESSING 
    → CONFIRMED 
      → PREPARING 
        → OUT_FOR_DELIVERY 
          → DELIVERED

OR

Any status → CANCELLED (with notifications)
```

#### 4. Order Cancellation (DELETE /api/orders/{id})

**What Happens:**
1. Order Service updates status to: `CANCELLED`
2. Fetches user and restaurant details
3. **Publishes event** to RabbitMQ: "ORDER_CANCELLED"
4. Both parties receive cancellation notification

## API Endpoints

### Order Service (8083)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/orders | Create new order (with payment) |
| GET | /api/orders/{id} | Get order details |
| GET | /api/orders/user/{userId} | Get all orders by user |
| GET | /api/orders/restaurant/{restaurantId} | Get all orders for restaurant |
| PUT | /api/orders/{id}/status | Update order status |
| DELETE | /api/orders/{id} | Cancel order |

### User Service (8081)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/users/{id} | Get user details |
| POST | /api/auth/register | Register new user |
| POST | /api/auth/login | User login |

### Restaurant Service (8082)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/restaurants/{id} | Get restaurant details |
| GET | /api/restaurants | Get all restaurants |
| POST | /api/restaurants | Create restaurant |
| GET | /api/restaurants/{id}/menu | Get menu items |

### Payment Service (8084)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/payments | Process payment |
| GET | /api/payments/{id} | Get payment details |
| GET | /api/payments/order/{orderId} | Get payment by order |
| POST | /api/payments/{id}/refund | Refund payment |

## Configuration

### Order Service Configuration

Add to `application.properties`:
```properties
# Service URLs (Docker)
services.payment.url=http://payment-service:8084
services.restaurant.url=http://restaurant-service:8082
services.user.url=http://user-service:8081

# Service URLs (Local)
# services.payment.url=http://localhost:8084
# services.restaurant.url=http://localhost:8082
# services.user.url=http://localhost:8081
```

### Restaurant Model Update

Restaurants now include owner email for notifications:
```java
private String ownerEmail; // Required for notifications
```

When creating a restaurant, include the owner's email:
```json
{
  "name": "Pizza Palace",
  "address": "123 Main St",
  "ownerEmail": "owner@pizzapalace.com",
  ...
}
```

## Error Handling

### Payment Failures

If payment fails:
1. Order status updated to: `PAYMENT_FAILED`
2. Error message returned to client
3. Notification sent to customer
4. Restaurant NOT notified (order not confirmed)

### Service Unavailability

If a service is unavailable:
- User/Restaurant Service: Throws `RuntimeException` - order creation fails
- Payment Service: Order marked as `PAYMENT_FAILED`
- Notification Service: Order continues (notifications are not critical)

## Testing the Integration

### 1. Start All Services

Using Docker Compose:
```bash
docker-compose up -d
```

### 2. Create a User

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@test.com",
    "password": "password123",
    "fullName": "John Doe",
    "phoneNumber": "+1234567890",
    "role": "CUSTOMER"
  }'
```

### 3. Create a Restaurant

```bash
curl -X POST http://localhost:8080/api/restaurants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Restaurant",
    "address": "123 Restaurant St",
    "cuisine": "Italian",
    "ownerEmail": "owner@test.com",
    "phoneNumber": "+1234567890",
    "ownerId": 1
  }'
```

### 4. Add Menu Items

```bash
curl -X POST http://localhost:8080/api/restaurants/1/menu \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pizza",
    "description": "Delicious pizza",
    "price": 12.99,
    "category": "Main"
  }'
```

### 5. Place an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "restaurantId": 1,
    "deliveryAddress": "456 Customer St",
    "paymentMethodId": "pm_card_visa",
    "items": [
      {
        "menuItemId": 1,
        "menuItemName": "Pizza",
        "quantity": 2,
        "price": 12.99
      }
    ]
  }'
```

### 6. Check Order Status

```bash
curl http://localhost:8080/api/orders/1
```

### 7. Update Order Status (Restaurant)

```bash
curl -X PUT http://localhost:8080/api/orders/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "OUT_FOR_DELIVERY"}'
```

### 8. Check Logs

**Order Service Logs:**
```bash
docker logs hunger-saviour-order-service
```

**Notification Service Logs:**
```bash
docker logs hunger-saviour-notification-service
```

**RabbitMQ Management UI:**
- URL: http://localhost:15672
- Username: guest
- Password: guest

## Monitoring

### Check Service Health

```bash
# Order Service
curl http://localhost:8083/actuator/health

# Payment Service
curl http://localhost:8084/actuator/health

# Notification Service
curl http://localhost:8085/actuator/health
```

### RabbitMQ Monitoring

Access RabbitMQ Management UI to see:
- Message flow through queues
- Consumer status
- Message rates
- Queue depth

### Prometheus Metrics

```bash
# Order Service metrics
curl http://localhost:8083/actuator/prometheus
```

## Best Practices

1. **Always provide payment method** when creating orders for automatic processing
2. **Set restaurant owner email** when creating restaurants for notifications
3. **Handle payment failures gracefully** - inform users clearly
4. **Monitor RabbitMQ** to ensure notifications are being processed
5. **Use proper error handling** in client applications
6. **Log all service interactions** for debugging
7. **Test notification delivery** before production deployment

## Troubleshooting

### Orders stuck in PENDING

- Check if payment service is running
- Verify paymentMethodId is valid
- Check payment service logs for errors

### No notifications received

- Verify RabbitMQ is running and accessible
- Check notification service logs
- Ensure email configuration is correct
- Verify restaurant has ownerEmail set

### Service communication errors

- Check all services are running: `docker ps`
- Verify service URLs in application.properties
- Check Eureka dashboard: http://localhost:8761
- Ensure network connectivity between containers

## Future Enhancements

- Add real-time order tracking with WebSocket
- Implement delivery partner assignment
- Add order rating and feedback
- Implement loyalty points system
- Add promotional codes and discounts
- Integrate SMS notifications
- Add push notifications for mobile apps
- Implement order scheduling
- Add restaurant preparation time estimates
