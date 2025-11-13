# Hunger Saviour - Microservices-Based Food Ordering Platform

A modern, scalable food ordering platform built with Spring Boot microservices architecture, featuring JWT authentication, Stripe payment integration, and containerized deployment with Docker.

## 🏗️ Architecture

This project implements a microservices architecture with the following components:

### Services

1. **API Gateway** (Port 8080)
   - Central entry point for all client requests
   - Routes requests to appropriate microservices
   - Built with Spring Cloud Gateway

2. **User Service** (Port 8081)
   - User registration and authentication
   - JWT token generation and validation
   - Secured with Spring Security
   - Database: PostgreSQL (hunger_saviour_users)

3. **Restaurant Service** (Port 8082)
   - Restaurant and menu management
   - CRUD operations for restaurants and menu items
   - Database: PostgreSQL (hunger_saviour_restaurants)

4. **Order Service** (Port 8083)
   - Order creation and management
   - Order status tracking
   - Database: PostgreSQL (hunger_saviour_orders)

5. **Payment Service** (Port 8084)
   - Stripe API integration for secure payments
   - Payment processing and refunds
   - Database: PostgreSQL (hunger_saviour_payments)

## 🚀 Features

- **RESTful APIs**: Clean, well-documented REST endpoints for all services
- **JWT Authentication**: Secure user authentication with JSON Web Tokens
- **Stripe Integration**: Industry-standard payment processing with Stripe API
- **Microservices Architecture**: Independent, scalable services
- **Docker Support**: Complete containerization with Docker Compose
- **PostgreSQL**: Robust relational database with Spring Data JPA
- **CI/CD Ready**: Containerized services for consistent deployment

## 🛠️ Technology Stack

- **Backend Framework**: Spring Boot 3.1.5
- **Java Version**: 17
- **Database**: PostgreSQL 15
- **API Gateway**: Spring Cloud Gateway
- **Security**: Spring Security + JWT (JJWT 0.11.5)
- **Payment**: Stripe Java SDK 24.0.0
- **ORM**: Spring Data JPA / Hibernate
- **Build Tool**: Maven
- **Containerization**: Docker & Docker Compose

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- PostgreSQL 15 (if running locally without Docker)
- Stripe API Key (for payment service)

## 🚀 Getting Started

### Option 1: Run with Docker Compose (Recommended)

1. Clone the repository:
```bash
git clone https://github.com/prashantgaware/hunger-saviour.git
cd hunger-saviour
```

2. Set your Stripe API key (optional, defaults to test key):
```bash
export STRIPE_API_KEY=your_stripe_secret_key
```

3. Build and start all services:
```bash
docker-compose up --build
```

All services will be available at:
- API Gateway: http://localhost:8080
- User Service: http://localhost:8081
- Restaurant Service: http://localhost:8082
- Order Service: http://localhost:8083
- Payment Service: http://localhost:8084

### Option 2: Run Locally

1. Start PostgreSQL and create databases:
```sql
CREATE DATABASE hunger_saviour_users;
CREATE DATABASE hunger_saviour_restaurants;
CREATE DATABASE hunger_saviour_orders;
CREATE DATABASE hunger_saviour_payments;
```

2. Build the project:
```bash
mvn clean install
```

3. Run each service:
```bash
# Terminal 1 - User Service
cd user-service
mvn spring-boot:run

# Terminal 2 - Restaurant Service
cd restaurant-service
mvn spring-boot:run

# Terminal 3 - Order Service
cd order-service
mvn spring-boot:run

# Terminal 4 - Payment Service
cd payment-service
mvn spring-boot:run

# Terminal 5 - API Gateway
cd api-gateway
mvn spring-boot:run
```

## 📚 API Documentation

### User Service APIs

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "phoneNumber": "+1234567890",
  "role": "CUSTOMER"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

Response:
```json
{
  "token": "jwt_token_here",
  "email": "user@example.com",
  "fullName": "John Doe",
  "role": "CUSTOMER"
}
```

### Restaurant Service APIs

#### Create Restaurant
```http
POST /api/restaurants
Content-Type: application/json

{
  "name": "Pizza Palace",
  "address": "123 Main St",
  "cuisine": "Italian",
  "description": "Best pizza in town",
  "phoneNumber": "+1234567890",
  "ownerId": 1
}
```

#### Get All Restaurants
```http
GET /api/restaurants
```

#### Add Menu Item
```http
POST /api/restaurants/{restaurantId}/menu
Content-Type: application/json

{
  "name": "Margherita Pizza",
  "description": "Classic pizza with tomato and mozzarella",
  "price": 12.99,
  "category": "Pizza"
}
```

### Order Service APIs

#### Create Order
```http
POST /api/orders
Content-Type: application/json

{
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
}
```

#### Get User Orders
```http
GET /api/orders/user/{userId}
```

#### Update Order Status
```http
PUT /api/orders/{orderId}/status
Content-Type: application/json

{
  "status": "CONFIRMED"
}
```

### Payment Service APIs

#### Process Payment
```http
POST /api/payments
Content-Type: application/json

{
  "orderId": 1,
  "userId": 1,
  "amount": 25.98,
  "paymentMethodId": "pm_card_visa",
  "currency": "usd"
}
```

#### Refund Payment
```http
POST /api/payments/{paymentId}/refund
```

## 🔐 Security

- All user passwords are encrypted using BCrypt
- JWT tokens are required for authenticated endpoints
- Tokens expire after 24 hours (configurable)
- Stripe API key should be kept secure and not committed to version control

## 🧪 Testing

Run tests for all services:
```bash
mvn test
```

Run tests for a specific service:
```bash
cd user-service
mvn test
```

## 🐳 Docker Commands

Build all services:
```bash
docker-compose build
```

Start services in detached mode:
```bash
docker-compose up -d
```

View logs:
```bash
docker-compose logs -f
```

Stop all services:
```bash
docker-compose down
```

Clean up volumes:
```bash
docker-compose down -v
```

## 📝 Configuration

### Environment Variables

- `STRIPE_API_KEY`: Your Stripe secret API key
- `SPRING_DATASOURCE_URL`: Database connection URL
- `JWT_SECRET`: Secret key for JWT token generation
- `JWT_EXPIRATION`: Token expiration time in milliseconds

### Application Properties

Each service has its own `application.properties` file for configuration. Key settings:

- Database connection details
- Server port
- JWT configuration (user-service)
- Stripe API key (payment-service)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is open source and available under the MIT License.

## 👥 Authors

- **Prashant Gaware** - Initial work

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Stripe for payment processing capabilities
- PostgreSQL community