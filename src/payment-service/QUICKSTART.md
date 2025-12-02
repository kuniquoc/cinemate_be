# Quick Start Guide

## Prerequisites

- Java 21 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher

## Setup Steps

### 1. Database Setup

```sql
-- Connect to PostgreSQL and create database
CREATE DATABASE cinemate_payment;

-- Grant permissions (if needed)
GRANT ALL PRIVILEGES ON DATABASE cinemate_payment TO postgres;
```

### 2. Configure Application

Update `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cinemate_payment
    username: postgres # Your PostgreSQL username
    password: postgres # Your PostgreSQL password

vnpay:
  tmn-code: YOUR_TMN_CODE # Replace with your VNPay merchant code
  hash-secret: YOUR_HASH_SECRET # Replace with your VNPay hash secret
```

### 3. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The service will start on http://localhost:8996

### 4. Verify Installation

Test that the service is running:

```bash
# Get available subscription plans
curl http://localhost:8996/api/subscription-plans
```

Expected response:

```json
[
  {
    "id": 1,
    "name": "Premium",
    "description": "Premium monthly subscription with unlimited access to all content",
    "price": 79000,
    "durationDays": 30,
    "maxDevices": 4,
    "features": {
      "hd_streaming": true,
      "offline_download": true,
      "multiple_devices": true,
      "ad_free": true
    },
    "isActive": true
  }
]
```

## Testing the Complete Flow

### 1. Create a Subscription

```bash
curl -X POST http://localhost:8996/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "planId": 1,
    "autoRenew": true
  }'
```

Save the `id` from the response (e.g., subscriptionId = 1)

### 2. Generate Payment URL

```bash
curl -X POST http://localhost:8996/api/payments/create-url \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "subscriptionId": 1,
    "amount": 79000,
    "paymentMethod": "VNPAY",
    "orderInfo": "Premium subscription payment",
    "ipAddress": "127.0.0.1"
  }'
```

Response:

```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
  "vnpTxnRef": "VNPAY_20251116211500_1234",
  "message": "Payment URL created successfully"
}
```

### 3. Register a Device

```bash
curl -X POST http://localhost:8996/api/devices/register \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "deviceName": "My Laptop",
    "deviceType": "WEB",
    "deviceId": "device-uuid-123",
    "browserInfo": "Chrome 120",
    "osInfo": "Windows 11",
    "ipAddress": "192.168.1.100"
  }'
```

### 4. Check User Devices

```bash
curl http://localhost:8996/api/devices/user/1
```

### 5. View Payment History

```bash
curl http://localhost:8996/api/payments/history/1
```

### 6. Check Current Subscription

```bash
curl http://localhost:8996/api/subscriptions/current/1
```

## Common Issues

### Database Connection Error

```
Error: Could not connect to database
```

**Solution**: Ensure PostgreSQL is running and credentials in `application.yml` are correct.

### Flyway Migration Error

```
Error: Migration checksum mismatch
```

**Solution**: Drop and recreate the database:

```sql
DROP DATABASE cinemate_payment;
CREATE DATABASE cinemate_payment;
```

### Port Already in Use

```
Error: Port 8996 is already in use
```

**Solution**: Change the port in `application.yml`:

```yaml
server:
  port: 8997
```

## VNPay Sandbox Testing

To test with VNPay sandbox:

1. Register for VNPay sandbox account at https://sandbox.vnpayment.vn/
2. Get your `TMN_CODE` and `HASH_SECRET`
3. Update `application.yml` with these credentials
4. Use the test card details provided by VNPay for testing

## API Documentation

Once running, you can test all endpoints using tools like:

- Postman
- cURL
- REST Client (VS Code extension)

See `IMPLEMENTATION.md` for complete API documentation.
