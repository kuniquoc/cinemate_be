# Family Plan Feature - Implementation Guide

## Overview

The Family Plan feature allows subscription owners to share their plan with up to 6 members, with support for parental controls for child accounts.

## Database Schema

### New Tables

#### 1. family_members

Tracks all members belonging to a family subscription.

```sql
- id: BIGSERIAL PRIMARY KEY
- subscription_id: BIGINT (FK to subscriptions)
- user_id: BIGINT
- is_owner: BOOLEAN
- joined_at: TIMESTAMP
- created_at: TIMESTAMP
```

#### 2. parent_control

Manages parental control settings for child accounts.

```sql
- id: BIGSERIAL PRIMARY KEY
- parent_id: BIGINT
- kid_id: BIGINT
- subscription_id: BIGINT (FK to subscriptions)
- blocked_categories: TEXT (Comma-separated category names)
- watch_time_limit_minutes: INTEGER
- created_at: TIMESTAMP
- updated_at: TIMESTAMP
```

#### 3. family_invitations

Stores invitation tokens for new family members.

```sql
- id: BIGSERIAL PRIMARY KEY
- subscription_id: BIGINT (FK to subscriptions)
- invitation_token: VARCHAR(255) UNIQUE
- mode: VARCHAR(10) (ADULT or KID)
- status: VARCHAR(20) (PENDING, ACCEPTED, EXPIRED, CANCELLED)
- invited_by: BIGINT
- invited_user_id: BIGINT
- expires_at: TIMESTAMP
- accepted_at: TIMESTAMP
- created_at: TIMESTAMP
```

### Updated Tables

#### subscription_plans

Added family plan support:

```sql
- max_members: INTEGER (null for individual plans, 6 for family)
- is_family_plan: BOOLEAN DEFAULT false
```

## Migration Files

The following Flyway migrations have been created:

- `V5__add_family_plan_support.sql` - Adds family plan fields and inserts Family plan
- `V6__create_family_members_table.sql` - Creates family_members table
- `V7__create_parent_control_table.sql` - Creates parent_control table
- `V8__create_family_invitations_table.sql` - Creates family_invitations table

## API Endpoints

### Family Plan Management

#### 1. Create Invitation

```
POST /api/family-plans/invitations?userId={ownerId}
Body: {
  "subscriptionId": 1,
  "mode": "ADULT" | "KID"
}
Response: {
  "id": 1,
  "invitationToken": "uuid-token",
  "invitationLink": "http://localhost:3000/family/join?token=uuid-token",
  "mode": "ADULT",
  "status": "PENDING",
  "invitedBy": 1,
  "expiresAt": "2025-12-02T00:00:00",
  "createdAt": "2025-11-25T00:00:00"
}
```

#### 2. Accept Invitation

```
POST /api/family-plans/invitations/accept?userId={userId}
Body: {
  "invitationToken": "uuid-token"
}
Response: {
  "id": 1,
  "userId": 2,
  "isOwner": false,
  "joinedAt": "2025-11-25T00:00:00"
}
```

#### 3. Get Family Members

```
GET /api/family-plans/subscriptions/{subscriptionId}/members
Response: [{
  "id": 1,
  "userId": 1,
  "isOwner": true,
  "joinedAt": "2025-11-01T00:00:00"
}, ...]
```

#### 4. Remove Member

```
DELETE /api/family-plans/subscriptions/{subscriptionId}/members/{memberUserId}?ownerId={ownerId}
Response: {
  "message": "Member removed successfully"
}
```

#### 5. Get Invitations

```
GET /api/family-plans/subscriptions/{subscriptionId}/invitations
Response: [{
  "id": 1,
  "invitationToken": "uuid-token",
  "invitationLink": "http://localhost:3000/family/join?token=uuid-token",
  "mode": "ADULT",
  "status": "PENDING",
  ...
}]
```

#### 6. Cancel Invitation

```
DELETE /api/family-plans/invitations/{invitationId}?userId={ownerId}
Response: {
  "message": "Invitation cancelled successfully"
}
```

### Parent Control

#### 1. Get Parent Control

```
GET /api/family-plans/parent-control?parentId={parentId}&kidId={kidId}
Response: {
  "id": 1,
  "parentId": 1,
  "kidId": 2,
  "subscriptionId": 1,
  "blockedCategories": ["Horror", "Thriller", "Adult"],
  "watchTimeLimitMinutes": 120,
  "createdAt": "2025-11-25T00:00:00",
  "updatedAt": "2025-11-25T00:00:00"
}
```

#### 2. Update Parent Control

```
PUT /api/family-plans/parent-control?parentId={parentId}&kidId={kidId}
Body: {
  "blockedCategories": ["Horror", "Thriller", "Adult"],
  "watchTimeLimitMinutes": 60
}
Response: {
  "id": 1,
  "parentId": 1,
  "kidId": 2,
  "blockedCategories": ["Horror", "Thriller", "Adult"],
  "watchTimeLimitMinutes": 60,
  ...
}
```

#### 3. Get Kids for Parent

```
GET /api/family-plans/parent-control/kids?parentId={parentId}
Response: [{
  "id": 1,
  "parentId": 1,
  "kidId": 2,
  "blockedCategories": ["Horror", "Thriller", "Adult"],
  "watchTimeLimitMinutes": 120,
  ...
}]
```

## Business Logic

### Invitation Flow

1. **Owner Creates Invitation**

   - Owner selects mode (ADULT or KID)
   - System generates unique token
   - Token expires in 7 days by default
   - Returns shareable link

2. **Recipient Accepts Invitation**
   - User clicks invitation link
   - System validates token (not expired, pending status)
   - Checks member limit not exceeded
   - Adds user as family member
   - If KID mode: creates parent_control record with default settings

### Parent Control

- Automatically created when KID invitation is accepted
- Parent is the invitation creator
- Default blocked categories: Horror, Thriller, Adult
- Parent can update blocked categories list and watch time limits
- Blocked categories are matched against movie category names
- Categories are stored as comma-separated values in database

### Member Limits

- Individual plans: 1 member (the owner)
- Family plan: up to 6 members
- Owner cannot be removed
- Only owner can invite/remove members

## Configuration

Add to `application.yml`:

```yaml
app:
  frontend:
    url: ${FRONTEND_URL:http://localhost:3000}
```

## Entities

### New Entities

- `FamilyMember` - Represents a member in a family subscription
- `ParentControl` - Stores parental control settings
- `FamilyInvitation` - Manages invitation lifecycle

### New Enums

- `InvitationMode` - ADULT, KID
- `InvitationStatus` - PENDING, ACCEPTED, EXPIRED, CANCELLED

## Repositories

- `FamilyMemberRepository` - CRUD for family members
- `ParentControlRepository` - CRUD for parent controls
- `FamilyInvitationRepository` - CRUD for invitations

## Services

- `FamilyPlanService` - Core business logic for family plans

## Controllers

- `FamilyPlanController` - REST API endpoints

## Error Handling

All endpoints use the standard `ResponseData` wrapper and throw:

- `ResourceNotFoundException` - Entity not found
- `SubscriptionException` - Business logic violations
- `DeviceLimitException` - Member limit exceeded

## TODO

- [ ] Integrate with authentication system to get userId from JWT token
- [ ] Add email notifications for invitations
- [ ] Implement invitation expiration cleanup job
- [ ] Add analytics tracking for family plan usage
- [ ] Create frontend components for family plan management
