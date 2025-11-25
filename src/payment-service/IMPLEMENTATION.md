# Payment Service - Implementation Documentation

## Overview

Complete payment service implementation with VNPay integration, subscription management, and device tracking.

## Features Implemented

### 1. Database Schema (Flyway Migrations)

- ✅ `subscription_plans` - Plan definitions with Premium plan seed data
- ✅ `subscriptions` - User subscriptions with lifecycle management
- ✅ `payments` - Payment transactions with VNPay integration
- ✅ `devices` - Device registration with 4-device limit

### 2. Domain Layer

- ✅ **Enums**: SubscriptionStatus, PaymentStatus, PaymentMethod, DeviceType
- ✅ **Entities**: SubscriptionPlan, Subscription, Payment, Device
- ✅ **Repositories**: 4 JpaRepository interfaces with custom queries

### 3. API Layer

#### Request DTOs (with Validation)

- ✅ CreateSubscriptionRequest
- ✅ CreatePaymentRequest
- ✅ RegisterDeviceRequest

#### Response DTOs

- ✅ SubscriptionPlanResponse
- ✅ SubscriptionResponse
- ✅ PaymentResponse
- ✅ DeviceResponse
- ✅ PaymentUrlResponse
- ✅ ErrorResponse

### 4. Business Logic Layer

#### Services

- ✅ **SubscriptionPlanService**: Plan management
- ✅ **VNPayService**: VNPay payment URL generation and callback processing
- ✅ **PaymentService**: Payment creation and tracking
- ✅ **SubscriptionService**: Full subscription lifecycle
- ✅ **DeviceService**: Device registration with 4-device limit enforcement

### 5. REST API Endpoints (16 total)

#### Subscription Plans (2 endpoints)

```
GET  /api/subscription-plans        - List all active plans
GET  /api/subscription-plans/{id}   - Get plan by ID
```

#### Subscriptions (5 endpoints)

```
POST /api/subscriptions             - Create subscription
GET  /api/subscriptions/current/{userId}    - Get current subscription
GET  /api/subscriptions/history/{userId}    - Get subscription history
PUT  /api/subscriptions/{id}/cancel         - Cancel subscription
POST /api/subscriptions/renew                - Renew subscription
```

#### Payments (5 endpoints)

```
POST /api/payments/create-url       - Create VNPay payment URL
GET  /api/payments/vnpay-return     - VNPay payment return handler
GET  /api/payments/vnpay-ipn        - VNPay IPN callback handler
GET  /api/payments/history/{userId} - Get payment history
GET  /api/payments/{id}             - Get payment by ID
```

#### Devices (4 endpoints)

```
GET    /api/devices/user/{userId}   - List user devices
POST   /api/devices/register        - Register new device
DELETE /api/devices/{deviceId}      - Remove device
GET    /api/devices/verify          - Verify device registration
```

### 6. Exception Handling

- ✅ GlobalExceptionHandler with @RestControllerAdvice
- ✅ 5 custom exceptions with appropriate HTTP status codes
- ✅ Validation error handling with field-level messages

### 7. VNPay Integration

- ✅ VNPayConfig with @ConfigurationProperties
- ✅ VNPayUtil for HMACSHA512 signature generation/validation
- ✅ Payment URL generation
- ✅ Payment callback processing (return URL and IPN)

### 8. Infrastructure

- ✅ Maven dependencies (Spring Boot 3.5.7, WebFlux, ModelMapper)
- ✅ ModelMapper bean configuration
- ✅ Utility classes (VNPayUtil, DateTimeUtil)
- ✅ Complete database configuration

## Configuration

### Database Setup

1. Create PostgreSQL database:

```sql
CREATE DATABASE cinemate_payment;
```

2. Update credentials in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cinemate_payment
    username: postgres
    password: postgres
```

### VNPay Configuration

Update VNPay credentials in `application.yml`:

```yaml
vnpay:
  tmn-code: YOUR_TMN_CODE # Get from VNPay merchant portal
  hash-secret: YOUR_HASH_SECRET # Get from VNPay merchant portal
```

## Running the Application

1. **Start PostgreSQL** on port 5432

2. **Build the project**:

```bash
mvn clean install
```

3. **Run the application**:

```bash
mvn spring-boot:run
```

The service will start on port **8996**.

## Testing

### 1. Database Migration Test

Start the application and verify Flyway migrations execute successfully. Check logs for:

```
Flyway: Successfully validated 4 migrations
```

### 2. Test Payment Flow

#### Step 1: Get Available Plans

```bash
curl http://localhost:8996/api/subscription-plans
```

#### Step 2: Create Subscription

```bash
curl -X POST http://localhost:8996/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "planId": 1,
    "autoRenew": true
  }'
```

#### Step 3: Create Payment URL

```bash
curl -X POST http://localhost:8996/api/payments/create-url \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "subscriptionId": 1,
    "amount": 79000,
    "paymentMethod": "VNPAY",
    "orderInfo": "Premium subscription payment"
  }'
```

#### Step 4: Complete Payment

- Use the returned `paymentUrl` to complete payment via VNPay
- VNPay will redirect to `/api/payments/vnpay-return`
- Subscription will be activated automatically upon successful payment

### 3. Test Device Management

#### Register Device

```bash
curl -X POST http://localhost:8996/api/devices/register \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "deviceName": "My Laptop",
    "deviceType": "WEB",
    "deviceId": "device-uuid-123",
    "browserInfo": "Chrome 120",
    "osInfo": "Windows 11"
  }'
```

#### List User Devices

```bash
curl http://localhost:8996/api/devices/user/1
```

#### Test 4-Device Limit

Register 5 devices to see the limit enforcement error.

## Key Implementation Details

### VNPay Payment Flow

1. User creates payment → `POST /api/payments/create-url`
2. System generates VNPay URL with HMACSHA512 signature
3. User completes payment on VNPay
4. VNPay redirects to return URL with payment result
5. System verifies signature and updates payment status
6. If successful, subscription is activated automatically

### Subscription Activation

- Subscriptions start as `PENDING` status
- Activated to `ACTIVE` upon successful payment
- Start date and end date are set based on plan duration
- Auto-renew flag controls renewal behavior

### Device Limit Enforcement

- Maximum 4 active devices per user
- Device registration verifies limit before adding
- Existing devices can be updated without counting against limit
- Devices can be removed to free up slots

## Database Schema Highlights

### Premium Plan Default Data

```sql
name: Premium
price: 79000 VND
duration: 30 days
max_devices: 4
features: HD streaming, offline download, multiple devices, ad-free
```

### Payment Status Flow

```
PENDING → SUCCESS (payment completed)
        → FAILED  (payment failed)
        → CANCELLED (user cancelled)
```

### Subscription Status Flow

```
PENDING → ACTIVE (payment successful)
        → CANCELLED (user cancelled)
        → EXPIRED (end date passed)
```

## Error Handling

All endpoints return structured error responses:

```json
{
  "timestamp": "2025-11-16T21:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "SubscriptionPlan not found with id: '999'",
  "path": "/api/subscription-plans/999"
}
```

Validation errors include field-level details:

```json
{
  "timestamp": "2025-11-16T21:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid input parameters",
  "validationErrors": {
    "userId": "User ID is required",
    "amount": "Amount must be positive"
  }
}
```

## Security Notes

⚠️ **Important**: Before deploying to production:

1. **Update VNPay credentials** in `application.yml`
2. **Secure database credentials** using environment variables
3. **Add authentication/authorization** to API endpoints
4. **Enable HTTPS** for all endpoints
5. **Add rate limiting** to prevent abuse
6. **Implement user authentication** to validate userId in requests

## Next Steps

- [ ] Add user authentication/authorization
- [ ] Implement scheduled job to auto-expire subscriptions
- [ ] Add payment retry mechanism
- [ ] Implement refund functionality
- [ ] Add email notifications for payment events
- [ ] Create admin endpoints for plan management
- [ ] Add monitoring and logging
- [ ] Write comprehensive unit and integration tests
