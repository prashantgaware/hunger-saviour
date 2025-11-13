#!/bin/bash

# Hunger Saviour - API Integration Test Script
# This script demonstrates the complete flow of the food ordering platform

echo "========================================"
echo "Hunger Saviour - API Integration Test"
echo "========================================"
echo ""

# Configuration
API_GATEWAY="http://localhost:8080"
USER_SERVICE="http://localhost:8081"
RESTAURANT_SERVICE="http://localhost:8082"
ORDER_SERVICE="http://localhost:8083"
PAYMENT_SERVICE="http://localhost:8084"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print test results
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ $2${NC}"
    else
        echo -e "${RED}✗ $2 FAILED${NC}"
        exit 1
    fi
}

echo "Waiting for services to start..."
sleep 5

# Test 1: Health Checks
echo ""
echo "Test 1: Health Checks"
echo "---------------------"
curl -s "$USER_SERVICE/api/auth/health" > /dev/null 2>&1
print_result $? "User Service is running"

curl -s "$RESTAURANT_SERVICE/api/restaurants/health" > /dev/null 2>&1
print_result $? "Restaurant Service is running"

curl -s "$ORDER_SERVICE/api/orders/health" > /dev/null 2>&1
print_result $? "Order Service is running"

curl -s "$PAYMENT_SERVICE/api/payments/health" > /dev/null 2>&1
print_result $? "Payment Service is running"

# Test 2: User Registration and Authentication
echo ""
echo "Test 2: User Registration"
echo "-------------------------"
REGISTER_RESPONSE=$(curl -s -X POST "$API_GATEWAY/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "fullName": "Test User",
    "phoneNumber": "+1234567890",
    "role": "CUSTOMER"
  }')

if echo "$REGISTER_RESPONSE" | grep -q "token"; then
    print_result 0 "User registered successfully"
    JWT_TOKEN=$(echo "$REGISTER_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    echo "   JWT Token: ${JWT_TOKEN:0:50}..."
else
    # Try login if user already exists
    echo "   User might exist, trying login..."
    LOGIN_RESPONSE=$(curl -s -X POST "$API_GATEWAY/api/auth/login" \
      -H "Content-Type: application/json" \
      -d '{
        "email": "test@example.com",
        "password": "password123"
      }')
    
    if echo "$LOGIN_RESPONSE" | grep -q "token"; then
        print_result 0 "User logged in successfully"
        JWT_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
        echo "   JWT Token: ${JWT_TOKEN:0:50}..."
    else
        print_result 1 "User authentication"
    fi
fi

# Test 3: Create Restaurant
echo ""
echo "Test 3: Create Restaurant"
echo "-------------------------"
RESTAURANT_RESPONSE=$(curl -s -X POST "$API_GATEWAY/api/restaurants" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Pizza Palace",
    "address": "123 Main St, New York, NY",
    "cuisine": "Italian",
    "description": "Best pizza in town",
    "phoneNumber": "+1234567890",
    "ownerId": 1
  }')

if echo "$RESTAURANT_RESPONSE" | grep -q "id"; then
    print_result 0 "Restaurant created"
    RESTAURANT_ID=$(echo "$RESTAURANT_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
    echo "   Restaurant ID: $RESTAURANT_ID"
else
    print_result 1 "Restaurant creation"
fi

# Test 4: Add Menu Item
echo ""
echo "Test 4: Add Menu Item"
echo "---------------------"
MENU_RESPONSE=$(curl -s -X POST "$API_GATEWAY/api/restaurants/${RESTAURANT_ID}/menu" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Margherita Pizza",
    "description": "Classic pizza with tomato and mozzarella",
    "price": 12.99,
    "category": "Pizza"
  }')

if echo "$MENU_RESPONSE" | grep -q "id"; then
    print_result 0 "Menu item added"
    MENU_ITEM_ID=$(echo "$MENU_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
    echo "   Menu Item ID: $MENU_ITEM_ID"
else
    print_result 1 "Menu item creation"
fi

# Test 5: Get All Restaurants
echo ""
echo "Test 5: Get All Restaurants"
echo "---------------------------"
RESTAURANTS_LIST=$(curl -s "$API_GATEWAY/api/restaurants")
if echo "$RESTAURANTS_LIST" | grep -q "Pizza Palace"; then
    print_result 0 "Retrieved restaurants list"
else
    print_result 1 "Getting restaurants"
fi

# Test 6: Create Order
echo ""
echo "Test 6: Create Order"
echo "--------------------"
ORDER_RESPONSE=$(curl -s -X POST "$API_GATEWAY/api/orders" \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": 1,
    \"restaurantId\": ${RESTAURANT_ID},
    \"deliveryAddress\": \"456 Oak Ave, New York, NY\",
    \"items\": [
      {
        \"menuItemId\": ${MENU_ITEM_ID},
        \"menuItemName\": \"Margherita Pizza\",
        \"quantity\": 2,
        \"price\": 12.99
      }
    ]
  }")

if echo "$ORDER_RESPONSE" | grep -q "id"; then
    print_result 0 "Order created"
    ORDER_ID=$(echo "$ORDER_RESPONSE" | grep -o '"id":[0-9]*' | cut -d':' -f2)
    TOTAL_AMOUNT=$(echo "$ORDER_RESPONSE" | grep -o '"totalAmount":[0-9.]*' | cut -d':' -f2)
    echo "   Order ID: $ORDER_ID"
    echo "   Total Amount: \$${TOTAL_AMOUNT}"
else
    print_result 1 "Order creation"
fi

# Test 7: Payment Processing (will fail without real Stripe key, but tests the endpoint)
echo ""
echo "Test 7: Payment Processing Endpoint"
echo "------------------------------------"
PAYMENT_RESPONSE=$(curl -s -X POST "$API_GATEWAY/api/payments" \
  -H "Content-Type: application/json" \
  -d "{
    \"orderId\": ${ORDER_ID},
    \"userId\": 1,
    \"amount\": ${TOTAL_AMOUNT},
    \"paymentMethodId\": \"pm_card_visa\",
    \"currency\": \"usd\"
  }")

# Payment will likely fail without real Stripe credentials, but endpoint should respond
if echo "$PAYMENT_RESPONSE" | grep -q "paymentId\|status"; then
    print_result 0 "Payment endpoint accessible"
    echo "   Note: Payment may fail without valid Stripe API key"
else
    print_result 1 "Payment endpoint"
fi

# Summary
echo ""
echo "========================================"
echo -e "${GREEN}Integration Test Completed!${NC}"
echo "========================================"
echo ""
echo "Summary:"
echo "  ✓ All microservices are running"
echo "  ✓ JWT authentication working"
echo "  ✓ Restaurant management working"
echo "  ✓ Menu management working"
echo "  ✓ Order processing working"
echo "  ✓ Payment endpoints accessible"
echo ""
echo "For full payment testing, configure STRIPE_API_KEY environment variable"
echo ""
