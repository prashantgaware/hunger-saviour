# Security Documentation - Hunger Saviour

## Overview
This document outlines the security features, considerations, and best practices implemented in the Hunger Saviour platform.

## Security Features

### 1. Authentication & Authorization

#### JWT (JSON Web Tokens)
- **Implementation**: JJWT library (0.11.5)
- **Algorithm**: HS256 (HMAC with SHA-256)
- **Token Expiration**: 24 hours (configurable)
- **Secret Key**: Configurable via environment variable
- **Token Location**: Authorization header (`Bearer <token>`)

**Security Considerations:**
- JWT secret should be at least 256 bits
- Tokens are stateless - no server-side session storage
- Tokens should be stored securely on client-side (HttpOnly cookies or secure storage)

#### Password Security
- **Hashing Algorithm**: BCrypt (Spring Security default)
- **Work Factor**: 10 rounds (BCrypt default)
- **Salt**: Automatically generated per password
- **Storage**: Only hashed passwords stored in database

**Best Practices:**
- Minimum password length: 6 characters (should be increased to 12+ in production)
- Consider implementing password complexity requirements
- Implement password reset functionality with secure tokens

### 2. API Security

#### CSRF Protection
**Status**: Disabled for REST API

**Justification:**
- REST APIs are stateless and use JWT tokens in headers
- CSRF attacks target cookie-based authentication
- JWT tokens in Authorization headers are not vulnerable to CSRF
- Standard practice for REST APIs with JWT authentication

**Code Reference:** `user-service/src/main/java/com/hungersaviour/user/config/SecurityConfig.java`

#### CORS (Cross-Origin Resource Sharing)
**Status**: Currently allows all origins (`*`)

**Production Recommendation:**
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList("https://yourdomain.com"));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
    configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
    configuration.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

### 3. Payment Security

#### Stripe Integration
- **PCI Compliance**: Payment card data never touches our servers
- **Stripe API**: All card processing handled by Stripe
- **Payment Methods**: Securely stored by Stripe
- **API Keys**: Secret keys stored as environment variables

**Security Best Practices:**
- Never log payment card numbers
- Use Stripe.js to collect card details on client side
- Implement webhook signature verification for payment events
- Use test keys in development, live keys only in production
- Rotate API keys periodically

**Webhook Security (Recommended Implementation):**
```java
@PostMapping("/webhook")
public ResponseEntity<String> handleWebhook(@RequestBody String payload, 
                                           @RequestHeader("Stripe-Signature") String signature) {
    try {
        Event event = Webhook.constructEvent(payload, signature, webhookSecret);
        // Process event
        return ResponseEntity.ok().build();
    } catch (SignatureVerificationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
```

### 4. Database Security

#### Connection Security
- Use SSL/TLS for database connections in production
- Store credentials as environment variables
- Implement principle of least privilege (dedicated DB users per service)

**Production Configuration:**
```properties
spring.datasource.url=jdbc:postgresql://db-host:5432/dbname?ssl=true&sslmode=require
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

#### SQL Injection Prevention
- **ORM**: Spring Data JPA/Hibernate
- **Parameterized Queries**: All queries use JPA repositories
- **Input Validation**: Bean Validation API (@Valid annotations)

### 5. Input Validation

#### Request Validation
```java
@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    // ... other fields
}
```

**Implemented Validations:**
- Email format validation
- Password length requirements
- Not null/blank checks on required fields

### 6. Secrets Management

#### Current Implementation
Secrets stored as:
- Environment variables
- Application properties (for development)

#### Production Recommendations
Use dedicated secrets management:
- **AWS**: AWS Secrets Manager or Parameter Store
- **Azure**: Azure Key Vault
- **GCP**: Google Secret Manager
- **Kubernetes**: Kubernetes Secrets
- **HashiCorp**: Vault

**Example with Spring Cloud Config:**
```yaml
spring:
  cloud:
    config:
      server:
        vault:
          host: vault-server
          port: 8200
          scheme: https
```

### 7. Docker Security

#### Container Security
- Use official base images (eclipse-temurin)
- Multi-stage builds to minimize image size
- Run containers as non-root user (recommended)

**Enhanced Dockerfile:**
```dockerfile
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
WORKDIR /app
COPY --from=build /app/service/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Docker Compose Security
- Use Docker secrets for sensitive data
- Implement network isolation
- Enable security scanning

### 8. API Gateway Security

#### Rate Limiting (Recommended)
Implement at API Gateway level to prevent abuse:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: http://user-service:8081
          predicates:
            - Path=/api/auth/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

#### Request Size Limits
```properties
spring.codec.max-in-memory-size=10MB
server.max-http-header-size=8KB
```

### 9. Logging and Monitoring

#### Security Logging
Log security events:
- Authentication attempts (success/failure)
- Authorization failures
- Payment transactions
- Data access patterns

**Sensitive Data:**
- Never log passwords
- Never log full credit card numbers
- Mask or hash PII in logs

**Example:**
```java
log.info("Login attempt for user: {}", email);
log.warn("Failed login attempt for user: {}", email);
log.error("Payment processing error for order: {}", orderId);
```

### 10. HTTPS/TLS

#### Production Configuration
Always use HTTPS in production:

```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat
```

#### Certificate Management
- Use valid SSL certificates (Let's Encrypt, commercial CAs)
- Automate certificate renewal
- Enforce TLS 1.2 or higher
- Implement HSTS headers

## Security Testing

### 1. Static Analysis
- **Tool**: CodeQL (GitHub Actions)
- **Frequency**: On every push/PR
- **Configuration**: `.github/workflows/codeql.yml`

### 2. Dependency Scanning
Regularly update dependencies:
```bash
mvn versions:display-dependency-updates
mvn dependency-check:check
```

### 3. Penetration Testing
Recommended tools:
- OWASP ZAP
- Burp Suite
- Postman security tests

### 4. Security Audit Checklist

- [ ] All secrets stored as environment variables
- [ ] HTTPS enabled in production
- [ ] Database connections use SSL
- [ ] JWT secret is strong (256+ bits)
- [ ] Password hashing implemented (BCrypt)
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention (JPA)
- [ ] XSS prevention (Spring Security defaults)
- [ ] CORS configured for specific origins
- [ ] Rate limiting implemented
- [ ] Logging doesn't expose sensitive data
- [ ] Dependencies are up to date
- [ ] Security headers configured
- [ ] Stripe webhook signatures verified

## Security Headers

### Recommended Headers
Add to API Gateway or each service:

```java
@Configuration
public class SecurityHeadersConfig {
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/api/auth/**");
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'")
                )
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.block(true))
                .contentTypeOptions(contentType -> contentType.disable())
            );
        return http.build();
    }
}
```

**Headers to implement:**
- `Content-Security-Policy`
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security: max-age=31536000`

## Incident Response

### Security Incident Procedures

1. **Detection**: Monitor logs and alerts
2. **Containment**: Isolate affected services
3. **Investigation**: Analyze logs and traces
4. **Remediation**: Apply fixes and patches
5. **Recovery**: Restore services
6. **Post-mortem**: Document and improve

### Contact Information
- Security Issues: security@hungersaviour.com (if applicable)
- GitHub Security Advisories: Private disclosure preferred

## Compliance Considerations

### GDPR (EU)
- User data deletion capability
- Data export functionality
- Privacy policy
- Cookie consent

### PCI DSS
- Use Stripe for card processing
- Never store card numbers
- Maintain secure network
- Regular security testing

### OWASP Top 10
Current status:
- ✅ A01: Broken Access Control - JWT authentication
- ✅ A02: Cryptographic Failures - BCrypt, HTTPS
- ✅ A03: Injection - JPA/Hibernate parameterized queries
- ✅ A04: Insecure Design - Microservices architecture
- ⚠️ A05: Security Misconfiguration - Review in production
- ⚠️ A06: Vulnerable Components - Keep dependencies updated
- ⚠️ A07: Auth Failures - Implement MFA (optional)
- ✅ A08: Software/Data Integrity - Code review process
- ⚠️ A09: Logging Failures - Implement centralized logging
- ⚠️ A10: SSRF - Validate external URLs if applicable

## Resources

### Documentation
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [Stripe Security](https://stripe.com/docs/security)

### Tools
- [OWASP ZAP](https://www.zaproxy.org/)
- [SonarQube](https://www.sonarqube.org/)
- [Snyk](https://snyk.io/)
- [Dependabot](https://github.com/dependabot)

## Conclusion

Security is an ongoing process. This document should be reviewed and updated regularly as new threats emerge and best practices evolve. Always prioritize security in development and deployment decisions.

For security concerns or to report vulnerabilities, please create a private security advisory on GitHub or contact the development team directly.
