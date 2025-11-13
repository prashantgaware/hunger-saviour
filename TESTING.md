# Testing Guide

This guide covers integration testing and end-to-end testing for the Hunger Saviour microservices platform.

## Overview

The testing strategy includes:
- Unit tests (existing)
- Integration tests (testing service components together)
- End-to-end tests (testing complete user flows)
- Contract tests (API contracts between services)

## Prerequisites

- Java 17+
- Maven 3.6+
- Docker and Docker Compose
- Testcontainers (for integration tests)

## Integration Testing

### Dependencies

Add to service `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.1</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>rabbitmq</artifactId>
    <version>1.19.1</version>
    <scope>test</scope>
</dependency>
```

### Example Integration Test - User Service

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_users")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "password123",
            "Test User",
            "+1234567890",
            "CUSTOMER"
        );

        ResponseEntity<AuthResponse> response = restTemplate
                .postForEntity("/api/auth/register", request, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getToken()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldLoginWithValidCredentials() {
        // First register
        RegisterRequest registerRequest = new RegisterRequest(
            "login@example.com",
            "password123",
            "Login User",
            "+1234567890",
            "CUSTOMER"
        );
        restTemplate.postForEntity("/api/auth/register", registerRequest, AuthResponse.class);

        // Then login
        LoginRequest loginRequest = new LoginRequest("login@example.com", "password123");
        ResponseEntity<AuthResponse> response = restTemplate
                .postForEntity("/api/auth/login", loginRequest, AuthResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isNotNull();
    }
}
```

### Example Integration Test - Restaurant Service with Redis

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class RestaurantServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_restaurants")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void shouldCacheRestaurantQueries() {
        // Create restaurant
        Restaurant restaurant = new Restaurant();
        restaurant.setName("Test Restaurant");
        restaurant.setAddress("123 Test St");
        restaurant.setCuisine("Italian");
        restaurant.setOwnerId(1L);
        
        Restaurant saved = restaurantService.createRestaurant(restaurant);

        // First call - should hit database
        Restaurant first = restaurantService.getRestaurantById(saved.getId());
        
        // Second call - should hit cache
        Restaurant second = restaurantService.getRestaurantById(saved.getId());

        // Verify cache is being used
        Cache cache = cacheManager.getCache("restaurants");
        assertThat(cache.get(saved.getId())).isNotNull();
        assertThat(first).isEqualTo(second);
    }
}
```

### Example Integration Test - Order Service with RabbitMQ

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test_orders")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void shouldPublishEventWhenOrderStatusChanges() {
        // Create order
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(1L);
        request.setRestaurantId(1L);
        request.setDeliveryAddress("123 Test St");
        
        Order order = orderService.createOrder(request);

        // Update order status
        orderService.updateOrderStatus(order.getId(), OrderStatus.CONFIRMED);

        // Verify message was sent to queue
        // (In real tests, you'd use @RabbitListener to capture the message)
    }
}
```

## End-to-End Testing

### Setup

Create a test configuration that starts all services:

```yaml
# docker-compose.test.yml
version: '3.8'

services:
  # All services with test configurations
  # Use test databases and test credentials
```

### E2E Test Example - Complete Order Flow

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {
    "server.port=8080"
})
class EndToEndOrderFlowTest {

    private RestTemplate restTemplate;
    private String baseUrl = "http://localhost:8080";

    @BeforeEach
    void setup() {
        restTemplate = new RestTemplate();
    }

    @Test
    void completeOrderFlow() {
        // 1. Register user
        RegisterRequest registerRequest = new RegisterRequest(
            "e2e@example.com", "password123", "E2E User", "+1234567890", "CUSTOMER"
        );
        
        ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity(
            baseUrl + "/api/auth/register", registerRequest, AuthResponse.class
        );
        String token = authResponse.getBody().getToken();

        // 2. Create restaurant (as owner)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        
        Restaurant restaurant = new Restaurant();
        restaurant.setName("E2E Restaurant");
        restaurant.setAddress("123 E2E St");
        restaurant.setCuisine("Italian");
        
        HttpEntity<Restaurant> restaurantEntity = new HttpEntity<>(restaurant, headers);
        ResponseEntity<Restaurant> restaurantResponse = restTemplate.postForEntity(
            baseUrl + "/api/restaurants", restaurantEntity, Restaurant.class
        );
        Long restaurantId = restaurantResponse.getBody().getId();

        // 3. Add menu item
        MenuItem menuItem = new MenuItem();
        menuItem.setName("Test Pizza");
        menuItem.setPrice(12.99);
        
        HttpEntity<MenuItem> menuEntity = new HttpEntity<>(menuItem, headers);
        ResponseEntity<MenuItem> menuResponse = restTemplate.postForEntity(
            baseUrl + "/api/restaurants/" + restaurantId + "/menu",
            menuEntity, MenuItem.class
        );
        Long menuItemId = menuResponse.getBody().getId();

        // 4. Create order
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setRestaurantId(restaurantId);
        orderRequest.setDeliveryAddress("456 Customer St");
        orderRequest.setItems(List.of(
            new OrderItemRequest(menuItemId, "Test Pizza", 2, 12.99)
        ));
        
        HttpEntity<CreateOrderRequest> orderEntity = new HttpEntity<>(orderRequest, headers);
        ResponseEntity<Order> orderResponse = restTemplate.postForEntity(
            baseUrl + "/api/orders", orderEntity, Order.class
        );
        Long orderId = orderResponse.getBody().getId();
        
        assertThat(orderResponse.getBody().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(orderResponse.getBody().getTotalAmount()).isEqualTo(25.98);

        // 5. Process payment
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(orderId);
        paymentRequest.setAmount(25.98);
        paymentRequest.setPaymentMethodId("pm_card_visa");
        
        HttpEntity<PaymentRequest> paymentEntity = new HttpEntity<>(paymentRequest, headers);
        ResponseEntity<PaymentResponse> paymentResponse = restTemplate.postForEntity(
            baseUrl + "/api/payments", paymentEntity, PaymentResponse.class
        );
        
        assertThat(paymentResponse.getBody().getStatus()).isEqualTo("succeeded");

        // 6. Verify order status updated
        ResponseEntity<Order> finalOrder = restTemplate.exchange(
            baseUrl + "/api/orders/" + orderId,
            HttpMethod.GET,
            new HttpEntity<>(headers),
            Order.class
        );
        
        assertThat(finalOrder.getBody().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
}
```

## Contract Testing

### Using Spring Cloud Contract

```groovy
// Add to build.gradle or equivalent in pom.xml
testImplementation 'org.springframework.cloud:spring-cloud-starter-contract-verifier'
```

### Define Contract

```groovy
// src/test/resources/contracts/shouldReturnRestaurantById.groovy
Contract.make {
    description "should return restaurant by id"
    request {
        method GET()
        url("/api/restaurants/1")
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            name: "Test Restaurant",
            cuisine: "Italian",
            address: "123 Test St"
        ])
    }
}
```

## Running Tests

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify -Pintegration-tests
```

### E2E Tests
```bash
# Start services
docker-compose -f docker-compose.test.yml up -d

# Run E2E tests
mvn verify -Pe2e-tests

# Stop services
docker-compose -f docker-compose.test.yml down
```

### All Tests
```bash
mvn clean verify
```

## Test Coverage

### Generate Coverage Report
```bash
mvn jacoco:report
```

View report at: `target/site/jacoco/index.html`

### Minimum Coverage Requirements
- Line coverage: 80%
- Branch coverage: 70%
- Service layer: 90%
- Controller layer: 85%

## Best Practices

1. **Isolation**: Each test should be independent
2. **Data**: Use test data builders for consistent test data
3. **Cleanup**: Clean up test data after each test
4. **Containers**: Use Testcontainers for external dependencies
5. **Mocking**: Mock external service calls
6. **Assertions**: Use meaningful assertions with clear messages
7. **Performance**: Keep tests fast (< 30 seconds for integration tests)
8. **Documentation**: Document complex test scenarios

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run tests
        run: mvn clean verify
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

## Troubleshooting

### Testcontainers Issues
- Ensure Docker daemon is running
- Check Docker permissions
- Increase Docker memory limits

### Slow Tests
- Use test profiles to skip slow tests during development
- Optimize database initialization
- Use parallel test execution

### Flaky Tests
- Check for timing issues
- Ensure proper test isolation
- Use deterministic test data

## References

- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Testcontainers](https://www.testcontainers.org/)
- [Spring Cloud Contract](https://spring.io/projects/spring-cloud-contract)
