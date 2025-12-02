# API Testing Examples

Complete test scenarios for all endpoints.

## Setup

Base URL: `http://localhost:8996`

## 1. Subscription Plans

### Get All Plans

```bash
curl -X GET http://localhost:8996/api/subscription-plans \
  -H "Accept: application/json"
```

**Expected Response:**

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
    "isActive": true,
    "createdAt": "2025-11-16T21:00:00",
    "updatedAt": "2025-11-16T21:00:00"
  }
]
```

### Get Plan by ID

```bash
curl -X GET http://localhost:8996/api/subscription-plans/1 \
  -H "Accept: application/json"
```

## 2. Subscriptions

### Create Subscription

```bash
curl -X POST http://localhost:8996/api/subscriptions \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "userId": 1,
    "planId": 1,
    "autoRenew": true
  }'
```

**Expected Response:**

```json
{
  "id": 1,
  "userId": 1,
  "plan": {
    "id": 1,
    "name": "Premium",
    "price": 79000,
    "durationDays": 30,
    "maxDevices": 4
  },
  "status": "PENDING",
  "startDate": null,
  "endDate": null,
  "autoRenew": true,
  "createdAt": "2025-11-16T21:00:00",
  "updatedAt": "2025-11-16T21:00:00"
}
```

### Get Current Subscription

```bash
curl -X GET http://localhost:8996/api/subscriptions/current/1 \
  -H "Accept: application/json"
```

### Get Subscription History

```bash
curl -X GET http://localhost:8996/api/subscriptions/history/1 \
  -H "Accept: application/json"
```

### Cancel Subscription

```bash
curl -X PUT "http://localhost:8996/api/subscriptions/1/cancel?userId=1" \
  -H "Accept: application/json"
```

### Renew Subscription

```bash
curl -X POST "http://localhost:8996/api/subscriptions/renew?userId=1&planId=1" \
  -H "Accept: application/json"
```

## 3. Payments

### Create Payment URL

```bash
curl -X POST http://localhost:8996/api/payments/create-url \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "userId": 1,
    "subscriptionId": 1,
    "amount": 79000,
    "paymentMethod": "VNPAY",
    "orderInfo": "Premium subscription payment",
    "ipAddress": "127.0.0.1"
  }'
```

**Expected Response:**

```json
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=7900000&vnp_Command=pay&...",
  "vnpTxnRef": "VNPAY_20251116210000_1234",
  "message": "Payment URL created successfully"
}
```

### Get Payment History

```bash
curl -X GET http://localhost:8996/api/payments/history/1 \
  -H "Accept: application/json"
```

### Get Payment by ID

```bash
curl -X GET http://localhost:8996/api/payments/1 \
  -H "Accept: application/json"
```

**Expected Response:**

```json
{
  "id": 1,
  "userId": 1,
  "subscriptionId": 1,
  "amount": 79000,
  "paymentMethod": "VNPAY",
  "status": "PENDING",
  "transactionId": null,
  "vnpTxnRef": "VNPAY_20251116210000_1234",
  "vnpTransactionNo": null,
  "vnpBankCode": null,
  "vnpCardType": null,
  "vnpOrderInfo": "Premium subscription payment",
  "vnpPayDate": null,
  "vnpResponseCode": null,
  "paymentDate": null,
  "createdAt": "2025-11-16T21:00:00",
  "updatedAt": "2025-11-16T21:00:00"
}
```

### Simulate VNPay Return (Manual Test)

After payment on VNPay, you'll be redirected to:

```
http://localhost:8996/api/payments/vnpay-return?vnp_Amount=7900000&vnp_ResponseCode=00&...
```

## 4. Device Management

### Register Device

```bash
curl -X POST http://localhost:8996/api/devices/register \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "userId": 1,
    "deviceName": "My Laptop",
    "deviceType": "WEB",
    "deviceId": "device-uuid-123",
    "browserInfo": "Chrome 120.0.0",
    "osInfo": "Windows 11",
    "ipAddress": "192.168.1.100"
  }'
```

**Expected Response:**

```json
{
  "id": 1,
  "userId": 1,
  "deviceName": "My Laptop",
  "deviceType": "WEB",
  "deviceId": "device-uuid-123",
  "browserInfo": "Chrome 120.0.0",
  "osInfo": "Windows 11",
  "ipAddress": "192.168.1.100",
  "lastAccessed": "2025-11-16T21:00:00",
  "isActive": true,
  "createdAt": "2025-11-16T21:00:00",
  "updatedAt": "2025-11-16T21:00:00"
}
```

### Get User Devices

```bash
curl -X GET http://localhost:8996/api/devices/user/1 \
  -H "Accept: application/json"
```

### Remove Device

```bash
curl -X DELETE "http://localhost:8996/api/devices/1?userId=1" \
  -H "Accept: application/json"
```

**Expected Response:**

```json
{
  "message": "Device removed successfully"
}
```

### Verify Device

```bash
curl -X GET "http://localhost:8996/api/devices/verify?userId=1&deviceId=device-uuid-123" \
  -H "Accept: application/json"
```

**Expected Response:**

```json
{
  "isRegistered": true,
  "userId": 1,
  "deviceId": "device-uuid-123"
}
```

## 5. Error Scenarios

### Resource Not Found

```bash
curl -X GET http://localhost:8996/api/subscription-plans/999 \
  -H "Accept: application/json"
```

**Expected Response (404):**

```json
{
  "timestamp": "2025-11-16T21:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "SubscriptionPlan not found with id: '999'",
  "path": "/api/subscription-plans/999"
}
```

### Validation Error

```bash
curl -X POST http://localhost:8996/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": null,
    "planId": null
  }'
```

**Expected Response (400):**

```json
{
  "timestamp": "2025-11-16T21:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid input parameters",
  "path": "/api/subscriptions",
  "validationErrors": {
    "userId": "User ID is required",
    "planId": "Plan ID is required"
  }
}
```

### Device Limit Exceeded

```bash
# Register 5 devices for the same user
# The 5th request should fail

curl -X POST http://localhost:8996/api/devices/register \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "deviceName": "Device 5",
    "deviceType": "MOBILE",
    "deviceId": "device-5"
  }'
```

**Expected Response (403):**

```json
{
  "timestamp": "2025-11-16T21:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Device limit reached. Maximum 4 devices allowed. Please remove a device before adding a new one.",
  "path": "/api/devices/register"
}
```

### Active Subscription Already Exists

```bash
# Try to create another subscription while one is active

curl -X POST http://localhost:8996/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "planId": 1,
    "autoRenew": false
  }'
```

**Expected Response (400):**

```json
{
  "timestamp": "2025-11-16T21:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "User already has an active subscription",
  "path": "/api/subscriptions"
}
```

## 6. Complete Flow Test

### Step-by-Step Integration Test

```bash
# 1. Get available plans
curl http://localhost:8996/api/subscription-plans

# 2. Create subscription (save the ID)
SUBSCRIPTION_ID=$(curl -X POST http://localhost:8996/api/subscriptions \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "planId": 1, "autoRenew": true}' \
  | jq -r '.id')

echo "Created subscription: $SUBSCRIPTION_ID"

# 3. Create payment URL
curl -X POST http://localhost:8996/api/payments/create-url \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": 1,
    \"subscriptionId\": $SUBSCRIPTION_ID,
    \"amount\": 79000,
    \"paymentMethod\": \"VNPAY\",
    \"orderInfo\": \"Premium subscription payment\"
  }" | jq '.'

# 4. Register devices
for i in {1..4}; do
  curl -X POST http://localhost:8996/api/devices/register \
    -H "Content-Type: application/json" \
    -d "{
      \"userId\": 1,
      \"deviceName\": \"Device $i\",
      \"deviceType\": \"WEB\",
      \"deviceId\": \"device-$i\"
    }"
done

# 5. Verify device limit (this should fail)
curl -X POST http://localhost:8996/api/devices/register \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "deviceName": "Device 5",
    "deviceType": "WEB",
    "deviceId": "device-5"
  }'

# 6. Check user devices
curl http://localhost:8996/api/devices/user/1 | jq '.'

# 7. Check payment history
curl http://localhost:8996/api/payments/history/1 | jq '.'

# 8. Check subscription history
curl http://localhost:8996/api/subscriptions/history/1 | jq '.'
```

## 7. PowerShell Testing Script

```powershell
# Test all endpoints

$baseUrl = "http://localhost:8996"

# 1. Get Plans
Write-Host "1. Getting subscription plans..." -ForegroundColor Green
Invoke-RestMethod -Uri "$baseUrl/api/subscription-plans" -Method Get

# 2. Create Subscription
Write-Host "2. Creating subscription..." -ForegroundColor Green
$subscription = Invoke-RestMethod -Uri "$baseUrl/api/subscriptions" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"userId": 1, "planId": 1, "autoRenew": true}'

Write-Host "Subscription ID: $($subscription.id)" -ForegroundColor Yellow

# 3. Create Payment
Write-Host "3. Creating payment URL..." -ForegroundColor Green
$payment = Invoke-RestMethod -Uri "$baseUrl/api/payments/create-url" `
  -Method Post `
  -ContentType "application/json" `
  -Body "{`"userId`": 1, `"subscriptionId`": $($subscription.id), `"amount`": 79000, `"paymentMethod`": `"VNPAY`"}"

Write-Host "Payment URL: $($payment.paymentUrl)" -ForegroundColor Yellow

# 4. Register Device
Write-Host "4. Registering device..." -ForegroundColor Green
$device = Invoke-RestMethod -Uri "$baseUrl/api/devices/register" `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"userId": 1, "deviceName": "Test Device", "deviceType": "WEB", "deviceId": "test-123"}'

Write-Host "Device ID: $($device.id)" -ForegroundColor Yellow

Write-Host "`nAll tests completed successfully!" -ForegroundColor Green
```

## Notes

- Replace `userId` values with actual user IDs from your user service
- For production, add authentication headers to all requests
- VNPay payment URLs must be opened in a browser for testing
- Test with VNPay sandbox credentials before going to production
