package com.pbl6.cinemate.shared.constants;

import java.util.UUID;

/**
 * Centralized UUID constants for seed data across all microservices.
 * Using fixed UUIDs ensures data consistency and referential integrity.
 */
public final class SeedUUIDs {

    private SeedUUIDs() {
        // Utility class - prevent instantiation
    }

    // ==================== AUTH SERVICE ====================

    /**
     * Role UUIDs - Must match auth-service roles table
     */
    public static final class Roles {
        public static final UUID ADMIN = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        public static final UUID USER = UUID.fromString("a0000000-0000-0000-0000-000000000002");

        private Roles() {
        }
    }

    /**
     * Permission UUIDs
     */
    public static final class Permissions {
        public static final UUID USER_READ = UUID.fromString("b0000000-0000-0000-0000-000000000001");
        public static final UUID USER_WRITE = UUID.fromString("b0000000-0000-0000-0000-000000000002");
        public static final UUID MOVIE_READ = UUID.fromString("b0000000-0000-0000-0000-000000000003");
        public static final UUID MOVIE_WRITE = UUID.fromString("b0000000-0000-0000-0000-000000000004");
        public static final UUID PAYMENT_READ = UUID.fromString("b0000000-0000-0000-0000-000000000005");
        public static final UUID PAYMENT_WRITE = UUID.fromString("b0000000-0000-0000-0000-000000000006");
        public static final UUID ADMIN_ACCESS = UUID.fromString("b0000000-0000-0000-0000-000000000007");
        public static final UUID CUSTOMER_READ = UUID.fromString("b0000000-0000-0000-0000-000000000008");
        public static final UUID CUSTOMER_WRITE = UUID.fromString("b0000000-0000-0000-0000-000000000009");

        private Permissions() {
        }
    }

    /**
     * User UUIDs (15 users)
     * Users 1-2: ADMIN role
     * Users 3-15: USER role
     */
    public static final class Users {
        // Admin users
        public static final UUID ADMIN_01 = UUID.fromString("c0000000-0000-0000-0000-000000000001");
        public static final UUID ADMIN_02 = UUID.fromString("c0000000-0000-0000-0000-000000000002");

        // Regular users
        public static final UUID USER_01 = UUID.fromString("c0000000-0000-0000-0000-000000000003");
        public static final UUID USER_02 = UUID.fromString("c0000000-0000-0000-0000-000000000004");
        public static final UUID USER_03 = UUID.fromString("c0000000-0000-0000-0000-000000000005");
        public static final UUID USER_04 = UUID.fromString("c0000000-0000-0000-0000-000000000006");
        public static final UUID USER_05 = UUID.fromString("c0000000-0000-0000-0000-000000000007");
        public static final UUID USER_06 = UUID.fromString("c0000000-0000-0000-0000-000000000008");
        public static final UUID USER_07 = UUID.fromString("c0000000-0000-0000-0000-000000000009");
        public static final UUID USER_08 = UUID.fromString("c0000000-0000-0000-0000-000000000010");
        public static final UUID USER_09 = UUID.fromString("c0000000-0000-0000-0000-000000000011");
        public static final UUID USER_10 = UUID.fromString("c0000000-0000-0000-0000-000000000012");
        public static final UUID USER_11 = UUID.fromString("c0000000-0000-0000-0000-000000000013");
        public static final UUID USER_12 = UUID.fromString("c0000000-0000-0000-0000-000000000014");
        public static final UUID USER_13 = UUID.fromString("c0000000-0000-0000-0000-000000000015");

        // User with disabled account (for testing)
        public static final UUID USER_DISABLED = UUID.fromString("c0000000-0000-0000-0000-000000000099");

        private Users() {
        }
    }

    // ==================== CUSTOMER SERVICE ====================

    /**
     * Customer UUIDs (mapped 1:1 with Users via accountId)
     */
    public static final class Customers {
        public static final UUID CUSTOMER_ADMIN_01 = UUID.fromString("d0000000-0000-0000-0000-000000000001");
        public static final UUID CUSTOMER_ADMIN_02 = UUID.fromString("d0000000-0000-0000-0000-000000000002");
        public static final UUID CUSTOMER_01 = UUID.fromString("d0000000-0000-0000-0000-000000000003");
        public static final UUID CUSTOMER_02 = UUID.fromString("d0000000-0000-0000-0000-000000000004");
        public static final UUID CUSTOMER_03 = UUID.fromString("d0000000-0000-0000-0000-000000000005");
        public static final UUID CUSTOMER_04 = UUID.fromString("d0000000-0000-0000-0000-000000000006");
        public static final UUID CUSTOMER_05 = UUID.fromString("d0000000-0000-0000-0000-000000000007");
        public static final UUID CUSTOMER_06 = UUID.fromString("d0000000-0000-0000-0000-000000000008");
        public static final UUID CUSTOMER_07 = UUID.fromString("d0000000-0000-0000-0000-000000000009");
        public static final UUID CUSTOMER_08 = UUID.fromString("d0000000-0000-0000-0000-000000000010");
        public static final UUID CUSTOMER_09 = UUID.fromString("d0000000-0000-0000-0000-000000000011");
        public static final UUID CUSTOMER_10 = UUID.fromString("d0000000-0000-0000-0000-000000000012");
        public static final UUID CUSTOMER_11 = UUID.fromString("d0000000-0000-0000-0000-000000000013");
        public static final UUID CUSTOMER_12 = UUID.fromString("d0000000-0000-0000-0000-000000000014");
        public static final UUID CUSTOMER_13 = UUID.fromString("d0000000-0000-0000-0000-000000000015");

        private Customers() {
        }
    }

    // ==================== MOVIE SERVICE ====================

    /**
     * Category UUIDs (12 categories)
     */
    public static final class Categories {
        public static final UUID ACTION = UUID.fromString("e0000000-0000-0000-0000-000000000001");
        public static final UUID COMEDY = UUID.fromString("e0000000-0000-0000-0000-000000000002");
        public static final UUID DRAMA = UUID.fromString("e0000000-0000-0000-0000-000000000003");
        public static final UUID HORROR = UUID.fromString("e0000000-0000-0000-0000-000000000004");
        public static final UUID SCI_FI = UUID.fromString("e0000000-0000-0000-0000-000000000005");
        public static final UUID ROMANCE = UUID.fromString("e0000000-0000-0000-0000-000000000006");
        public static final UUID THRILLER = UUID.fromString("e0000000-0000-0000-0000-000000000007");
        public static final UUID ANIMATION = UUID.fromString("e0000000-0000-0000-0000-000000000008");
        public static final UUID DOCUMENTARY = UUID.fromString("e0000000-0000-0000-0000-000000000009");
        public static final UUID FANTASY = UUID.fromString("e0000000-0000-0000-0000-000000000010");
        public static final UUID ADVENTURE = UUID.fromString("e0000000-0000-0000-0000-000000000011");
        public static final UUID MYSTERY = UUID.fromString("e0000000-0000-0000-0000-000000000012");

        private Categories() {
        }
    }

    /**
     * Actor UUIDs (10 actors)
     */
    public static final class Actors {
        public static final UUID ACTOR_01 = UUID.fromString("f0000000-0000-0000-0000-000000000001");
        public static final UUID ACTOR_02 = UUID.fromString("f0000000-0000-0000-0000-000000000002");
        public static final UUID ACTOR_03 = UUID.fromString("f0000000-0000-0000-0000-000000000003");
        public static final UUID ACTOR_04 = UUID.fromString("f0000000-0000-0000-0000-000000000004");
        public static final UUID ACTOR_05 = UUID.fromString("f0000000-0000-0000-0000-000000000005");
        public static final UUID ACTOR_06 = UUID.fromString("f0000000-0000-0000-0000-000000000006");
        public static final UUID ACTOR_07 = UUID.fromString("f0000000-0000-0000-0000-000000000007");
        public static final UUID ACTOR_08 = UUID.fromString("f0000000-0000-0000-0000-000000000008");
        public static final UUID ACTOR_09 = UUID.fromString("f0000000-0000-0000-0000-000000000009");
        public static final UUID ACTOR_10 = UUID.fromString("f0000000-0000-0000-0000-000000000010");

        private Actors() {
        }
    }

    /**
     * Director UUIDs (8 directors)
     */
    public static final class Directors {
        public static final UUID DIRECTOR_01 = UUID.fromString("f1000000-0000-0000-0000-000000000001");
        public static final UUID DIRECTOR_02 = UUID.fromString("f1000000-0000-0000-0000-000000000002");
        public static final UUID DIRECTOR_03 = UUID.fromString("f1000000-0000-0000-0000-000000000003");
        public static final UUID DIRECTOR_04 = UUID.fromString("f1000000-0000-0000-0000-000000000004");
        public static final UUID DIRECTOR_05 = UUID.fromString("f1000000-0000-0000-0000-000000000005");
        public static final UUID DIRECTOR_06 = UUID.fromString("f1000000-0000-0000-0000-000000000006");
        public static final UUID DIRECTOR_07 = UUID.fromString("f1000000-0000-0000-0000-000000000007");
        public static final UUID DIRECTOR_08 = UUID.fromString("f1000000-0000-0000-0000-000000000008");

        private Directors() {
        }
    }

    /**
     * Movie UUIDs (15 movies)
     * Movies 1-12: PUBLIC status
     * Movies 13-15: PRIVATE status
     */
    public static final class Movies {
        // Public movies
        public static final UUID MOVIE_01 = UUID.fromString("f2000000-0000-0000-0000-000000000001");
        public static final UUID MOVIE_02 = UUID.fromString("f2000000-0000-0000-0000-000000000002");
        public static final UUID MOVIE_03 = UUID.fromString("f2000000-0000-0000-0000-000000000003");
        public static final UUID MOVIE_04 = UUID.fromString("f2000000-0000-0000-0000-000000000004");
        public static final UUID MOVIE_05 = UUID.fromString("f2000000-0000-0000-0000-000000000005");
        public static final UUID MOVIE_06 = UUID.fromString("f2000000-0000-0000-0000-000000000006");
        public static final UUID MOVIE_07 = UUID.fromString("f2000000-0000-0000-0000-000000000007");
        public static final UUID MOVIE_08 = UUID.fromString("f2000000-0000-0000-0000-000000000008");
        public static final UUID MOVIE_09 = UUID.fromString("f2000000-0000-0000-0000-000000000009");
        public static final UUID MOVIE_10 = UUID.fromString("f2000000-0000-0000-0000-000000000010");
        public static final UUID MOVIE_11 = UUID.fromString("f2000000-0000-0000-0000-000000000011");
        public static final UUID MOVIE_12 = UUID.fromString("f2000000-0000-0000-0000-000000000012");

        // Private movies
        public static final UUID MOVIE_13 = UUID.fromString("f2000000-0000-0000-0000-000000000013");
        public static final UUID MOVIE_14 = UUID.fromString("f2000000-0000-0000-0000-000000000014");
        public static final UUID MOVIE_15 = UUID.fromString("f2000000-0000-0000-0000-000000000015");

        private Movies() {
        }
    }

    // ==================== PAYMENT SERVICE ====================

    /**
     * Subscription Plan UUIDs
     */
    public static final class SubscriptionPlans {
        public static final UUID PREMIUM = UUID.fromString("10000000-0000-0000-0000-000000000001");
        public static final UUID FAMILY = UUID.fromString("10000000-0000-0000-0000-000000000002");

        private SubscriptionPlans() {
        }
    }

    /**
     * Subscription UUIDs (12 subscriptions with various statuses)
     */
    public static final class Subscriptions {
        // PENDING subscriptions
        public static final UUID SUB_PENDING_01 = UUID.fromString("11000000-0000-0000-0000-000000000001");
        public static final UUID SUB_PENDING_02 = UUID.fromString("11000000-0000-0000-0000-000000000002");
        public static final UUID SUB_PENDING_03 = UUID.fromString("11000000-0000-0000-0000-000000000003");

        // ACTIVE subscriptions
        public static final UUID SUB_ACTIVE_01 = UUID.fromString("11000000-0000-0000-0000-000000000004");
        public static final UUID SUB_ACTIVE_02 = UUID.fromString("11000000-0000-0000-0000-000000000005");
        public static final UUID SUB_ACTIVE_03 = UUID.fromString("11000000-0000-0000-0000-000000000006");

        // EXPIRED subscriptions
        public static final UUID SUB_EXPIRED_01 = UUID.fromString("11000000-0000-0000-0000-000000000007");
        public static final UUID SUB_EXPIRED_02 = UUID.fromString("11000000-0000-0000-0000-000000000008");
        public static final UUID SUB_EXPIRED_03 = UUID.fromString("11000000-0000-0000-0000-000000000009");

        // CANCELLED subscriptions
        public static final UUID SUB_CANCELLED_01 = UUID.fromString("11000000-0000-0000-0000-000000000010");
        public static final UUID SUB_CANCELLED_02 = UUID.fromString("11000000-0000-0000-0000-000000000011");
        public static final UUID SUB_CANCELLED_03 = UUID.fromString("11000000-0000-0000-0000-000000000012");

        private Subscriptions() {
        }
    }

    /**
     * Payment UUIDs (12 payments with various statuses)
     */
    public static final class Payments {
        public static final UUID PAYMENT_01 = UUID.fromString("12000000-0000-0000-0000-000000000001");
        public static final UUID PAYMENT_02 = UUID.fromString("12000000-0000-0000-0000-000000000002");
        public static final UUID PAYMENT_03 = UUID.fromString("12000000-0000-0000-0000-000000000003");
        public static final UUID PAYMENT_04 = UUID.fromString("12000000-0000-0000-0000-000000000004");
        public static final UUID PAYMENT_05 = UUID.fromString("12000000-0000-0000-0000-000000000005");
        public static final UUID PAYMENT_06 = UUID.fromString("12000000-0000-0000-0000-000000000006");
        public static final UUID PAYMENT_07 = UUID.fromString("12000000-0000-0000-0000-000000000007");
        public static final UUID PAYMENT_08 = UUID.fromString("12000000-0000-0000-0000-000000000008");
        public static final UUID PAYMENT_09 = UUID.fromString("12000000-0000-0000-0000-000000000009");
        public static final UUID PAYMENT_10 = UUID.fromString("12000000-0000-0000-0000-000000000010");
        public static final UUID PAYMENT_11 = UUID.fromString("12000000-0000-0000-0000-000000000011");
        public static final UUID PAYMENT_12 = UUID.fromString("12000000-0000-0000-0000-000000000012");

        private Payments() {
        }
    }

    /**
     * Device UUIDs (8 devices with various types)
     */
    public static final class Devices {
        public static final UUID DEVICE_WEB_01 = UUID.fromString("13000000-0000-0000-0000-000000000001");
        public static final UUID DEVICE_WEB_02 = UUID.fromString("13000000-0000-0000-0000-000000000002");
        public static final UUID DEVICE_MOBILE_01 = UUID.fromString("13000000-0000-0000-0000-000000000003");
        public static final UUID DEVICE_MOBILE_02 = UUID.fromString("13000000-0000-0000-0000-000000000004");
        public static final UUID DEVICE_TABLET = UUID.fromString("13000000-0000-0000-0000-000000000005");
        public static final UUID DEVICE_TV = UUID.fromString("13000000-0000-0000-0000-000000000006");
        public static final UUID DEVICE_DESKTOP_01 = UUID.fromString("13000000-0000-0000-0000-000000000007");
        public static final UUID DEVICE_DESKTOP_02 = UUID.fromString("13000000-0000-0000-0000-000000000008");

        private Devices() {
        }
    }

    /**
     * Family Invitation UUIDs (5 invitations)
     */
    public static final class FamilyInvitations {
        public static final UUID INVITATION_PENDING = UUID.fromString("14000000-0000-0000-0000-000000000001");
        public static final UUID INVITATION_ACCEPTED = UUID.fromString("14000000-0000-0000-0000-000000000002");
        public static final UUID INVITATION_EXPIRED = UUID.fromString("14000000-0000-0000-0000-000000000003");
        public static final UUID INVITATION_CANCELLED = UUID.fromString("14000000-0000-0000-0000-000000000004");
        public static final UUID INVITATION_KID = UUID.fromString("14000000-0000-0000-0000-000000000005");

        private FamilyInvitations() {
        }
    }

    /**
     * Family Member UUIDs (5 members)
     */
    public static final class FamilyMembers {
        public static final UUID MEMBER_OWNER = UUID.fromString("15000000-0000-0000-0000-000000000001");
        public static final UUID MEMBER_02 = UUID.fromString("15000000-0000-0000-0000-000000000002");
        public static final UUID MEMBER_03 = UUID.fromString("15000000-0000-0000-0000-000000000003");
        public static final UUID MEMBER_04 = UUID.fromString("15000000-0000-0000-0000-000000000004");
        public static final UUID MEMBER_05 = UUID.fromString("15000000-0000-0000-0000-000000000005");

        private FamilyMembers() {
        }
    }
}
