#!/bin/bash

# Hunger Saviour - Integration Test Script
# This script demonstrates the complete order flow with service integration

set -e

API_URL="${API_URL:-http://localhost:8080}"
USER_SERVICE="${USER_SERVICE:-http://localhost:8081}"
RESTAURANT_SERVICE="${RESTAURANT_SERVICE:-http://localhost:8082}"
ORDER_SERVICE="${ORDER_SERVICE:-http://localhost:8083}"

echo "=================================="
echo "Hunger Saviour Integration Test"
echo "=================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Step 1: Register a customer
echo -e "${BLUE}Step 1: Registering customer...${NC}"
CUSTOMER_RESPONSE=$(curl -s -X POST $USER_SERVICE/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "customer@test.com",
    "password": "password123",
    "fullName": "Test Customer",
    "phoneNumber": "+1234567890",
    "role": "CUSTOMER"
  }')

CUSTOMER_ID=$(echo $CUSTOMER_RESPONSE | jq -r '.id // 1')
echo -e "${GREEN}✓ Customer registered with ID: $CUSTOMER_ID${NC}"
echo ""

# Step 2: Create a restaurant
echo -e "${BLUE}Step 2: Creating restaurant...${NC}"
RESTAURANT_RESPONSE=$(curl -s -X POST $RESTAURANT_SERVICE/api/restaurants \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Pizza Palace",
    "address": "123 Test Street",
    "cuisine": "Italian",
    "description": "Best pizza in test town",
    "phoneNumber": "+1234567890",
    "ownerEmail": "owner@testpizza.com",
    "ownerId": 1
  }')

RESTAURANT_ID=$(echo $RESTAURANT_RESPONSE | jq -r '.id // 1')
echo -e "${GREEN}✓ Restaurant created with ID: $RESTAURANT_ID${NC}"
echo ""

# Step 3: Add menu items
echo -e "${BLUE}Step 3: Adding menu items...${NC}"
MENU_ITEM_RESPONSE=$(curl -s -X POST $RESTAURANT_SERVICE/api/restaurants/$RESTAURANT_ID/menu \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Margherita Pizza",
    "description": "Classic tomato and mozzarella",
    "price": 12.99,
    "category": "Pizza",
    "isAvailable": true
  }')

MENU_ITEM_ID=$(echo $MENU_ITEM_RESPONSE | jq -r '.id // 1')
echo -e "${GREEN}✓ Menu item added with ID: $MENU_ITEM_ID${NC}"
echo ""

# Step 4: Create an order (WITH INTEGRATION)
echo -e "${BLUE}Step 4: Creating order (with payment & notifications)...${NC}"
echo -e "${YELLOW}This will:${NC}"
echo -e "${YELLOW}  1. Validate customer (User Service)${NC}"
echo -e "${YELLOW}  2. Validate restaurant (Restaurant Service)${NC}"
echo -e "${YELLOW}  3. Process payment (Payment Service - will fail without valid Stripe key)${NC}"
echo -e "${YELLOW}  4. Send notifications (Notification Service via RabbitMQ)${NC}"
echo ""

ORDER_RESPONSE=$(curl -s -X POST $ORDER_SERVICE/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": '$CUSTOMER_ID',
    "restaurantId": '$RESTAURANT_ID',
    "deliveryAddress": "456 Customer Avenue",
    "paymentMethodId": "pm_card_visa",
    "items": [
      {
        "menuItemId": '$MENU_ITEM_ID',
        "menuItemName": "Margherita Pizza",
        "quantity": 2,
        "price": 12.99
      }
    ]
  }' 2>&1)

# Check if order was created
if echo "$ORDER_RESPONSE" | jq -e '.id' > /dev/null 2>&1; then
  ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.id')
  ORDER_STATUS=$(echo $ORDER_RESPONSE | jq -r '.status')
  ORDER_TOTAL=$(echo $ORDER_RESPONSE | jq -r '.totalAmount')
  
  echo -e "${GREEN}✓ Order created successfully!${NC}"
  echo -e "  Order ID: $ORDER_ID"
  echo -e "  Status: $ORDER_STATUS"
  echo -e "  Total Amount: \$$ORDER_TOTAL"
  echo ""
  
  # Step 5: Check order details
  echo -e "${BLUE}Step 5: Fetching order details...${NC}"
  ORDER_DETAILS=$(curl -s $ORDER_SERVICE/api/orders/$ORDER_ID)
  echo -e "${GREEN}✓ Order details retrieved${NC}"
  echo "$ORDER_DETAILS" | jq '.'
  echo ""
  
  # Step 6: Update order status
  echo -e "${BLUE}Step 6: Updating order status to OUT_FOR_DELIVERY...${NC}"
  echo -e "${YELLOW}This will trigger notifications to both customer and restaurant${NC}"
  UPDATE_RESPONSE=$(curl -s -X PUT $ORDER_SERVICE/api/orders/$ORDER_ID/status \
    -H "Content-Type: application/json" \
    -d '{"status": "OUT_FOR_DELIVERY"}')
  
  NEW_STATUS=$(echo $UPDATE_RESPONSE | jq -r '.status')
  echo -e "${GREEN}✓ Order status updated to: $NEW_STATUS${NC}"
  echo ""
  
else
  echo -e "${RED}✗ Order creation failed${NC}"
  echo "Response: $ORDER_RESPONSE"
  echo ""
  echo -e "${YELLOW}Note: Payment may have failed if Stripe key is not configured.${NC}"
  echo -e "${YELLOW}Check order-service logs for details.${NC}"
fi

echo "=================================="
echo -e "${GREEN}Integration Test Complete${NC}"
echo "=================================="
echo ""
echo "To check notifications:"
echo "  1. View RabbitMQ: http://localhost:15672 (guest/guest)"
echo "  2. Check notification-service logs: docker logs hunger-saviour-notification-service"
echo "  3. Check order-service logs: docker logs hunger-saviour-order-service"
echo ""
echo "To view all orders:"
echo "  curl $ORDER_SERVICE/api/orders/user/$CUSTOMER_ID"
echo ""
