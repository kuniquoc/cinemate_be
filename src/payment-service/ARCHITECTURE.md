# Payment Service Architecture

## System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                             │
├─────────────────────────────────────────────────────────────────┤
│  Web App  │  Mobile App  │  TV App  │  Tablet App  │  Desktop   │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ HTTP/REST API
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      CONTROLLER LAYER                            │
├─────────────────────────────────────────────────────────────────┤
│  SubscriptionPlan  │  Subscription  │  Payment  │  Device       │
│    Controller      │   Controller   │Controller │ Controller    │
│   (2 endpoints)    │  (5 endpoints) │(5 endpoints)│(4 endpoints)│
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ DTOs (Request/Response)
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                       SERVICE LAYER                              │
├─────────────────────────────────────────────────────────────────┤
│ SubscriptionPlan │ Subscription │  Payment  │ VNPay │  Device   │
│     Service      │   Service    │  Service  │Service│  Service  │
│                  │              │           │       │           │
│ • getAllPlans    │• create      │• create   │• URL  │• register │
│ • getPlanById    │• activate    │• update   │• verify│• remove  │
│                  │• cancel      │• history  │• callback│• list  │
│                  │• renew       │           │       │• verify   │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ Entity Mapping (ModelMapper)
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                    REPOSITORY LAYER                              │
├─────────────────────────────────────────────────────────────────┤
│SubscriptionPlan │ Subscription │  Payment  │  Device           │
│   Repository     │  Repository  │Repository │ Repository        │
│                  │              │           │                   │
│ • JPA Queries    │• Custom      │• Custom   │• Custom           │
│ • Active plans   │  queries     │  queries  │  queries          │
│                  │• findActive  │• byTxnRef │• countActive      │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         │ JPA/Hibernate
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DATABASE LAYER                              │
├─────────────────────────────────────────────────────────────────┤
│              PostgreSQL Database (cinemate_payment)              │
│                                                                  │
│  ┌─────────────┐  ┌──────────────┐  ┌─────────┐  ┌─────────┐  │
│  │subscription_│  │subscriptions │  │payments │  │ devices │  │
│  │   plans     │  │              │  │         │  │         │  │
│  │─────────────│  │──────────────│  │─────────│  │─────────│  │
│  │ id (PK)     │◄─┤ plan_id (FK) │◄─┤sub_id   │  │user_id  │  │
│  │ name        │  │ user_id      │  │user_id  │  │device_id│  │
│  │ price       │  │ status       │  │amount   │  │type     │  │
│  │ duration    │  │ start_date   │  │status   │  │active   │  │
│  │ max_devices │  │ end_date     │  │vnp_*    │  │         │  │
│  └─────────────┘  └──────────────┘  └─────────┘  └─────────┘  │
│                                                                  │
│  Flyway Migrations: V1, V2, V3, V4                              │
└─────────────────────────────────────────────────────────────────┘
```

## Payment Flow Architecture

```
┌─────────────┐
│   Client    │
│ (Web/Mobile)│
└──────┬──────┘
       │
       │ 1. Create subscription
       ▼
┌─────────────────────┐
│SubscriptionController│
└──────┬──────────────┘
       │
       │ 2. Create subscription (PENDING)
       ▼
┌─────────────────┐
│Subscription     │
│   Service       │
└──────┬──────────┘
       │
       │ 3. Save to DB
       ▼
┌─────────────────┐
│ PostgreSQL      │
└─────────────────┘

       ┌─────────────┐
       │   Client    │
       └──────┬──────┘
              │
              │ 4. Request payment URL
              ▼
       ┌─────────────────┐
       │PaymentController│
       └──────┬──────────┘
              │
              │ 5. Create payment
              ▼
       ┌─────────────────┐
       │Payment Service  │
       └──────┬──────────┘
              │
              │ 6. Generate VNPay URL
              ▼
       ┌─────────────────┐
       │  VNPay Service  │
       │                 │
       │• Build params   │
       │• Sign HMACSHA512│
       │• Create URL     │
       └──────┬──────────┘
              │
              │ 7. Return payment URL
              ▼
       ┌─────────────┐
       │   Client    │
       └──────┬──────┘
              │
              │ 8. Redirect to VNPay
              ▼
       ┌─────────────────┐
       │   VNPay Portal  │
       │                 │
       │ User completes  │
       │    payment      │
       └──────┬──────────┘
              │
              │ 9. Payment result + signature
              ▼
       ┌─────────────────┐
       │/vnpay-return    │
       │/vnpay-ipn       │
       └──────┬──────────┘
              │
              │ 10. Verify signature
              ▼
       ┌─────────────────┐
       │  VNPay Service  │
       │                 │
       │• Verify hash    │
       │• Update status  │
       └──────┬──────────┘
              │
              │ 11. If SUCCESS
              ▼
       ┌─────────────────┐
       │Subscription     │
       │   Service       │
       │                 │
       │• Set ACTIVE     │
       │• Set dates      │
       └─────────────────┘
```

## Device Management Flow

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       │ 1. Register device
       ▼
┌─────────────────┐
│DeviceController │
└──────┬──────────┘
       │
       │ 2. Check if exists
       ▼
┌─────────────────┐
│ Device Service  │
└──────┬──────────┘
       │
       ├─► Device exists? → Update last_accessed
       │
       └─► New device?
           │
           ▼
       ┌─────────────────┐
       │Count active     │
       │devices          │
       └──────┬──────────┘
              │
              ├─► >= 4 devices? → Throw DeviceLimitException (403)
              │
              └─► < 4 devices?
                  │
                  ▼
              ┌─────────────────┐
              │ Save new device │
              │ Return response │
              └─────────────────┘
```

## Data Flow Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                        REQUEST FLOW                           │
└──────────────────────────────────────────────────────────────┘

Client Request (JSON)
    │
    ├─► Validation (@Valid)
    │   └─► Fails? → GlobalExceptionHandler → ErrorResponse
    │
    └─► Controller
        │
        ├─► DTO → Service Layer
        │   │
        │   ├─► Business Logic
        │   │   │
        │   │   ├─► Repository → Database
        │   │   │
        │   │   └─► External Service (VNPay)
        │   │
        │   └─► Entity → DTO (ModelMapper)
        │
        └─► Response (JSON)


┌──────────────────────────────────────────────────────────────┐
│                     EXCEPTION FLOW                            │
└──────────────────────────────────────────────────────────────┘

Exception Thrown
    │
    ├─► ResourceNotFoundException → 404 Not Found
    │
    ├─► InvalidPaymentException → 400 Bad Request
    │
    ├─► SubscriptionException → 400 Bad Request
    │
    ├─► DeviceLimitException → 403 Forbidden
    │
    ├─► PaymentProcessingException → 500 Internal Server Error
    │
    ├─► MethodArgumentNotValidException → 400 + Validation Errors
    │
    └─► Generic Exception → 500 Internal Server Error
```

## Component Dependencies

```
┌─────────────────────────────────────────────────────────────┐
│                    DEPENDENCY GRAPH                          │
└─────────────────────────────────────────────────────────────┘

Controllers
    │
    ├─► Services
    │   │
    │   ├─► Repositories (JPA)
    │   │   │
    │   │   └─► Database (PostgreSQL)
    │   │
    │   ├─► ModelMapper
    │   │
    │   └─► External Config (VNPayConfig)
    │
    ├─► DTOs (Request/Response)
    │
    └─► Exception Handler

Utilities
    │
    ├─► VNPayUtil (Crypto)
    │
    └─► DateTimeUtil (Time)

Configuration
    │
    ├─► VNPayConfig (@ConfigurationProperties)
    │
    ├─► ModelMapper (@Bean)
    │
    └─► application.yml
```

## Technology Stack Layers

```
┌─────────────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER                          │
│  Spring MVC Controllers + REST API                          │
│  Jackson (JSON Serialization)                               │
└─────────────────────────────────────────────────────────────┘
                           │
┌─────────────────────────────────────────────────────────────┐
│                   BUSINESS LAYER                             │
│  Services + Business Logic                                  │
│  Transaction Management (@Transactional)                    │
│  ModelMapper (DTO ↔ Entity)                                │
└─────────────────────────────────────────────────────────────┘
                           │
┌─────────────────────────────────────────────────────────────┐
│                   PERSISTENCE LAYER                          │
│  Spring Data JPA Repositories                               │
│  Hibernate ORM                                              │
│  Flyway Migration                                           │
└─────────────────────────────────────────────────────────────┘
                           │
┌─────────────────────────────────────────────────────────────┐
│                    DATABASE LAYER                            │
│  PostgreSQL 12+                                             │
│  ACID Transactions                                          │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  INTEGRATION LAYER                           │
│  VNPay Payment Gateway (HMACSHA512)                         │
│  WebFlux HTTP Client                                        │
└─────────────────────────────────────────────────────────────┘
```

## Security Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    SECURITY LAYERS                           │
└─────────────────────────────────────────────────────────────┘

1. Input Validation
   ├─► Jakarta Validation (@Valid)
   └─► Custom business rules

2. Payment Security
   ├─► HMACSHA512 signature
   ├─► Transaction ID verification
   └─► Amount validation

3. Data Integrity
   ├─► Database constraints
   ├─► Foreign keys
   └─► Check constraints

4. Error Handling
   ├─► No sensitive data in errors
   ├─► Sanitized messages
   └─► Structured error responses

5. Transaction Safety
   ├─► @Transactional
   ├─► Rollback on failure
   └─► ACID compliance
```

## Deployment Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     PRODUCTION SETUP                          │
└──────────────────────────────────────────────────────────────┘

                    ┌─────────────┐
                    │ Load Balancer│
                    └──────┬───────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
    ┌────▼─────┐     ┌────▼─────┐     ┌────▼─────┐
    │ Instance │     │ Instance │     │ Instance │
    │    1     │     │    2     │     │    3     │
    │ (8996)   │     │ (8996)   │     │ (8996)   │
    └────┬─────┘     └────┬─────┘     └────┬─────┘
         │                │                 │
         └─────────────────┼─────────────────┘
                           │
                    ┌──────▼───────┐
                    │  PostgreSQL  │
                    │   Cluster    │
                    │  (Primary +  │
                    │   Replicas)  │
                    └──────────────┘
```

## Monitoring & Logging

```
Application Logs
    │
    ├─► Payment events (INFO)
    ├─► Errors (ERROR)
    └─► Debug info (DEBUG)

Metrics to Monitor
    │
    ├─► Payment success rate
    ├─► API response times
    ├─► Active subscriptions
    ├─► Device registrations
    └─► Database connection pool
```
