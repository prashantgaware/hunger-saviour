# API Documentation - Hunger Saviour

## Overview
This document provides detailed information about all REST API endpoints in the Hunger Saviour platform.

## Base URLs
- **API Gateway**: `http://localhost:8080` (All requests should go through the gateway)
- **User Service**: `http://localhost:8081` (Direct access)
- **Restaurant Service**: `http://localhost:8082` (Direct access)
- **Order Service**: `http://localhost:8083` (Direct access)
- **Payment Service**: `http://localhost:8084` (Direct access)

## Authentication
Most endpoints require JWT authentication. Include the token in the Authorization header:
```
Authorization: Bearer <your-jwt-token>
```

---

## User Service APIs

### 1. Register User
Create a new user account.

**Endpoint:** `POST /api/auth/register`  
**Authentication:** Not required

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "phoneNumber": "+1234567890",
  "role": "CUSTOMER"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com",
  "fullName": "John Doe",
  "role": "CUSTOMER"
}
```

**Roles:** `CUSTOMER`, `RESTAURANT_OWNER`, `ADMIN`

---

### 2. Login
Authenticate and receive JWT token.

**Endpoint:** `POST /api/auth/login`  
**Authentication:** Not required

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "user@example.com",
  "fullName": "John Doe",
  "role": "CUSTOMER"
}
```

---

### 3. Health Check
Check if user service is running.

**Endpoint:** `GET /api/auth/health`  
**Authentication:** Not required

**Response:** `200 OK`
```
User Service is running
```

---

## Restaurant Service APIs

### 4. Create Restaurant
Create a new restaurant (typically by restaurant owners).

**Endpoint:** `POST /api/restaurants`  
**Authentication:** Not required (should be protected in production)

**Request Body:**
```json
{
  "name": "Pizza Palace",
  "address": "123 Main St, New York, NY",
  "cuisine": "Italian",
  "description": "Best pizza in town",
  "phoneNumber": "+1234567890",
  "ownerId": 1
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "Pizza Palace",
  "address": "123 Main St, New York, NY",
  "cuisine": "Italian",
  "description": "Best pizza in town",
  "phoneNumber": "+1234567890",
  "ownerId": 1,
  "isActive": true,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

---

### 5. Get Restaurant by ID
Retrieve details of a specific restaurant.

**Endpoint:** `GET /api/restaurants/{id}`  
**Authentication:** Not required

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "Pizza Palace",
  "address": "123 Main St, New York, NY",
  "cuisine": "Italian",
  "description": "Best pizza in town",
  "phoneNumber": "+1234567890",
  "ownerId": 1,
  "isActive": true,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

---

### 6. Get All Restaurants
Retrieve list of all active restaurants.

**Endpoint:** `GET /api/restaurants`  
**Authentication:** Not required

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Pizza Palace",
    "address": "123 Main St, New York, NY",
    "cuisine": "Italian",
    "description": "Best pizza in town",
    "phoneNumber": "+1234567890",
    "ownerId": 1,
    "isActive": true,
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
]
```

---

### 7. Update Restaurant
Update restaurant information.

**Endpoint:** `PUT /api/restaurants/{id}`  
**Authentication:** Not required (should be protected in production)

**Request Body:**
```json
{
  "name": "Pizza Palace Deluxe",
  "address": "123 Main St, New York, NY",
  "cuisine": "Italian",
  "description": "Best pizza in town with new menu",
  "phoneNumber": "+1234567890"
}
```

**Response:** `200 OK` (Updated restaurant object)

---

### 8. Delete Restaurant
Soft delete a restaurant (sets isActive to false).

**Endpoint:** `DELETE /api/restaurants/{id}`  
**Authentication:** Not required (should be protected in production)

**Response:** `200 OK`

---

### 9. Add Menu Item
Add a menu item to a restaurant.

**Endpoint:** `POST /api/restaurants/{restaurantId}/menu`  
**Authentication:** Not required (should be protected in production)

**Request Body:**
```json
{
  "name": "Margherita Pizza",
  "description": "Classic pizza with tomato and mozzarella",
  "price": 12.99,
  "category": "Pizza",
  "isAvailable": true
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "Margherita Pizza",
  "description": "Classic pizza with tomato and mozzarella",
  "price": 12.99,
  "category": "Pizza",
  "isAvailable": true,
  "createdAt": "2024-01-15T10:35:00",
  "updatedAt": "2024-01-15T10:35:00"
}
```

---

### 10. Get Menu Items
Get all available menu items for a restaurant.

**Endpoint:** `GET /api/restaurants/{restaurantId}/menu`  
**Authentication:** Not required

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Margherita Pizza",
    "description": "Classic pizza with tomato and mozzarella",
    "price": 12.99,
    "category": "Pizza",
    "isAvailable": true,
    "createdAt": "2024-01-15T10:35:00",
    "updatedAt": "2024-01-15T10:35:00"
  }
]
```

---

### 11. Update Menu Item
Update a menu item.

**Endpoint:** `PUT /api/restaurants/menu/{id}`  
**Authentication:** Not required (should be protected in production)

**Request Body:**
```json
{
  "name": "Margherita Pizza Deluxe",
  "description": "Classic pizza with premium ingredients",
  "price": 14.99,
  "category": "Pizza",
  "isAvailable": true
}
```

**Response:** `200 OK` (Updated menu item object)

---

### 12. Delete Menu Item
Soft delete a menu item.

**Endpoint:** `DELETE /api/restaurants/menu/{id}`  
**Authentication:** Not required (should be protected in production)

**Response:** `200 OK`

---

## Order Service APIs

### 13. Create Order
Create a new order.

**Endpoint:** `POST /api/orders`  
**Authentication:** Not required (should be protected in production)

**Request Body:**
```json
{
  "userId": 1,
  "restaurantId": 1,
  "deliveryAddress": "456 Oak Ave, New York, NY",
  "items": [
    {
      "menuItemId": 1,
      "menuItemName": "Margherita Pizza",
      "quantity": 2,
      "price": 12.99
    },
    {
      "menuItemId": 2,
      "menuItemName": "Caesar Salad",
      "quantity": 1,
      "price": 8.99
    }
  ]
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "userId": 1,
  "restaurantId": 1,
  "status": "PENDING",
  "totalAmount": 34.97,
  "deliveryAddress": "456 Oak Ave, New York, NY",
  "orderItems": [
    {
      "id": 1,
      "menuItemId": 1,
      "menuItemName": "Margherita Pizza",
      "quantity": 2,
      "price": 12.99,
      "subtotal": 25.98
    },
    {
      "id": 2,
      "menuItemId": 2,
      "menuItemName": "Caesar Salad",
      "quantity": 1,
      "price": 8.99,
      "subtotal": 8.99
    }
  ],
  "createdAt": "2024-01-15T11:00:00",
  "updatedAt": "2024-01-15T11:00:00"
}
```

**Order Status Values:** `PENDING`, `CONFIRMED`, `PREPARING`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`

---

### 14. Get Order by ID
Retrieve order details.

**Endpoint:** `GET /api/orders/{id}`  
**Authentication:** Not required (should be protected in production)

**Response:** `200 OK` (Order object)

---

### 15. Get Orders by User
Get all orders for a specific user.

**Endpoint:** `GET /api/orders/user/{userId}`  
**Authentication:** Not required (should be protected in production)

**Response:** `200 OK` (Array of order objects)

---

### 16. Get Orders by Restaurant
Get all orders for a specific restaurant.

**Endpoint:** `GET /api/orders/restaurant/{restaurantId}`  
**Authentication:** Not required (should be protected in production)

**Response:** `200 OK` (Array of order objects)

---

### 17. Update Order Status
Update the status of an order.

**Endpoint:** `PUT /api/orders/{id}/status`  
**Authentication:** Not required (should be protected in production)

**Request Body:**
```json
{
  "status": "CONFIRMED"
}
```

**Response:** `200 OK` (Updated order object)

---

### 18. Cancel Order
Cancel an order.

**Endpoint:** `DELETE /api/orders/{id}`  
**Authentication:** Not required (should be protected in production)

**Response:** `200 OK`

---

## Payment Service APIs

### 19. Process Payment
Process a payment using Stripe.

**Endpoint:** `POST /api/payments`  
**Authentication:** Not required (should be protected in production)

**Request Body:**
```json
{
  "orderId": 1,
  "userId": 1,
  "amount": 34.97,
  "paymentMethodId": "pm_card_visa",
  "currency": "usd"
}
```

**Response:** `200 OK`
```json
{
  "paymentId": 1,
  "status": "SUCCESS",
  "message": "Payment processed successfully",
  "stripePaymentIntentId": "pi_1234567890"
}
```

**Payment Status Values:** `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`

**Note:** Requires valid Stripe API key and payment method ID from Stripe.js

---

### 20. Get Payment by ID
Retrieve payment details.

**Endpoint:** `GET /api/payments/{id}`  
**Authentication:** Not required (should be protected in production)

**Response:** `200 OK`
```json
{
  "id": 1,
  "orderId": 1,
  "userId": 1,
  "amount": 34.97,
  "status": "SUCCESS",
  "stripePaymentIntentId": "pi_1234567890",
  "stripeChargeId": "ch_1234567890",
  "paymentMethod": "CARD",
  "createdAt": "2024-01-15T11:05:00",
  "updatedAt": "2024-01-15T11:05:00"
}
```

---

### 21. Get Payment by Order ID
Retrieve payment for a specific order.

**Endpoint:** `GET /api/payments/order/{orderId}`  
**Authentication:** Not required (should be protected in production)

**Response:** `200 OK` (Payment object)

---

### 22. Get Payments by User
Get all payments for a specific user.

**Endpoint:** `GET /api/payments/user/{userId}`  
**Authentication:** Not required (should be protected in production)

**Response:** `200 OK` (Array of payment objects)

---

### 23. Refund Payment
Process a refund for a payment.

**Endpoint:** `POST /api/payments/{id}/refund`  
**Authentication:** Not required (should be protected in production)

**Response:** `200 OK`
```json
{
  "paymentId": 1,
  "status": "REFUNDED",
  "message": "Payment refunded successfully",
  "stripePaymentIntentId": "pi_1234567890"
}
```

---

## Error Responses

All endpoints may return the following error responses:

### 400 Bad Request
```json
{
  "timestamp": "2024-01-15T11:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register"
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2024-01-15T11:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/api/orders"
}
```

### 404 Not Found
```json
{
  "timestamp": "2024-01-15T11:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Resource not found",
  "path": "/api/restaurants/999"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2024-01-15T11:00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "/api/payments"
}
```

---

## Rate Limiting
Currently not implemented. Recommended to implement at API Gateway level in production.

## Versioning
Current version: v1 (implicit in URLs)

## Testing with cURL

### Register and Login
```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User",
    "phoneNumber": "+1234567890",
    "role": "CUSTOMER"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### Create Restaurant and Menu
```bash
# Create Restaurant
curl -X POST http://localhost:8080/api/restaurants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pizza Palace",
    "address": "123 Main St",
    "cuisine": "Italian",
    "description": "Best pizza in town",
    "phoneNumber": "+1234567890",
    "ownerId": 1
  }'

# Add Menu Item
curl -X POST http://localhost:8080/api/restaurants/1/menu \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Margherita Pizza",
    "description": "Classic pizza",
    "price": 12.99,
    "category": "Pizza"
  }'
```

### Create Order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "restaurantId": 1,
    "deliveryAddress": "456 Oak Ave",
    "items": [
      {
        "menuItemId": 1,
        "menuItemName": "Margherita Pizza",
        "quantity": 2,
        "price": 12.99
      }
    ]
  }'
```

---

## Postman Collection
A Postman collection with all endpoints is available in the repository: `postman/hunger-saviour.postman_collection.json`

## Support
For API issues or questions, please create an issue on GitHub: https://github.com/prashantgaware/hunger-saviour/issues
