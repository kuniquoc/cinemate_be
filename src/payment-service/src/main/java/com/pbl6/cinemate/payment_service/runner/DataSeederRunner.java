package com.pbl6.cinemate.payment_service.runner;

import com.pbl6.cinemate.payment_service.entity.*;
import com.pbl6.cinemate.payment_service.enums.*;
import com.pbl6.cinemate.payment_service.repository.*;
import com.pbl6.cinemate.shared.constants.SeedUUIDs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Seeds initial payment data for development/testing purposes.
 * Only runs when app.seed.enabled=true and the subscriptions table is empty.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataSeederRunner implements ApplicationRunner {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final DeviceRepository deviceRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyInvitationRepository familyInvitationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedSubscriptions();
        seedPayments();
        seedDevices();
        seedFamilyMembers();
        seedFamilyInvitations();
    }

    private void seedSubscriptions() {
        if (subscriptionRepository.count() > 0) {
            log.info("Subscriptions table is not empty. Skipping subscription seeding.");
            return;
        }

        log.info("Starting subscription data seeding...");

        // Get subscription plans
        List<SubscriptionPlan> plans = subscriptionPlanRepository.findByIsActiveTrue();
        if (plans.isEmpty()) {
            log.warn("No active subscription plans found. Skipping subscription seeding.");
            return;
        }

        SubscriptionPlan premiumPlan = plans.stream()
                .filter(p -> !p.getIsFamilyPlan())
                .findFirst()
                .orElse(plans.get(0));

        SubscriptionPlan familyPlan = plans.stream()
                .filter(SubscriptionPlan::getIsFamilyPlan)
                .findFirst()
                .orElse(premiumPlan);

        LocalDateTime now = LocalDateTime.now();
        List<Subscription> subscriptions = new ArrayList<>();

        // PENDING subscriptions (3)
        subscriptions.add(createSubscription(SeedUUIDs.Subscriptions.SUB_PENDING_01,
                SeedUUIDs.Users.USER_01, premiumPlan, SubscriptionStatus.PENDING, null, null, now));
        subscriptions.add(createSubscription(SeedUUIDs.Subscriptions.SUB_PENDING_02,
                SeedUUIDs.Users.USER_02, premiumPlan, SubscriptionStatus.PENDING, null, null, now));
        subscriptions.add(createSubscription(SeedUUIDs.Subscriptions.SUB_PENDING_03,
                SeedUUIDs.Users.USER_03, familyPlan, SubscriptionStatus.PENDING, null, null, now));

        // ACTIVE subscriptions (3)
        subscriptions.add(createSubscription(SeedUUIDs.Subscriptions.SUB_ACTIVE_01,
                SeedUUIDs.Users.USER_04, premiumPlan, SubscriptionStatus.ACTIVE,
                now.minusDays(15), now.plusDays(15), now.minusDays(15)));
        subscriptions.add(createSubscription(SeedUUIDs.Subscriptions.SUB_ACTIVE_02,
                SeedUUIDs.Users.USER_05, premiumPlan, SubscriptionStatus.ACTIVE,
                now.minusDays(5), now.plusDays(25), now.minusDays(5)));
        subscriptions.add(createSubscription(SeedUUIDs.Subscriptions.SUB_ACTIVE_03,
                SeedUUIDs.Users.USER_06, familyPlan, SubscriptionStatus.ACTIVE,
                now.minusDays(10), now.plusDays(20), now.minusDays(10)));

        // EXPIRED subscriptions (3)
        subscriptions.add(createSubscription(SeedUUIDs.Subscriptions.SUB_EXPIRED_01,
                SeedUUIDs.Users.USER_07, premiumPlan, SubscriptionStatus.EXPIRED,
                now.minusDays(60), now.minusDays(30), now.minusDays(60)));
        subscriptions.add(createSubscription(SeedUUIDs.Subscriptions.SUB_EXPIRED_02,
                SeedUUIDs.Users.USER_08, premiumPlan, SubscriptionStatus.EXPIRED,
                now.minusDays(45), now.minusDays(15), now.minusDays(45)));
        subscriptions.add(createSubscription(SeedUUIDs.Subscriptions.SUB_EXPIRED_03,
                SeedUUIDs.Users.USER_09, familyPlan, SubscriptionStatus.EXPIRED,
                now.minusDays(90), now.minusDays(60), now.minusDays(90)));

        // CANCELLED subscriptions (3)
        subscriptions.add(createSubscription(SeedUUIDs.Subscriptions.SUB_CANCELLED_01,
                SeedUUIDs.Users.USER_10, premiumPlan, SubscriptionStatus.CANCELLED,
                now.minusDays(20), now.plusDays(10), now.minusDays(20)));
        subscriptions.add(createSubscription(SeedUUIDs.Subscriptions.SUB_CANCELLED_02,
                SeedUUIDs.Users.USER_11, premiumPlan, SubscriptionStatus.CANCELLED,
                now.minusDays(25), now.plusDays(5), now.minusDays(25)));
        subscriptions.add(createSubscription(SeedUUIDs.Subscriptions.SUB_CANCELLED_03,
                SeedUUIDs.Users.USER_12, familyPlan, SubscriptionStatus.CANCELLED,
                now.minusDays(15), now.plusDays(15), now.minusDays(15)));

        subscriptionRepository.saveAll(subscriptions);
        log.info("Successfully seeded {} subscriptions", subscriptions.size());
    }

    private Subscription createSubscription(UUID id, UUID userId, SubscriptionPlan plan,
            SubscriptionStatus status, LocalDateTime startDate,
            LocalDateTime endDate, LocalDateTime createdAt) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setUserId(userId);
        subscription.setPlan(plan);
        subscription.setStatus(status);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setAutoRenew(status == SubscriptionStatus.ACTIVE);
        subscription.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());
        return subscription;
    }

    private void seedPayments() {
        if (paymentRepository.count() > 0) {
            log.info("Payments table is not empty. Skipping payment seeding.");
            return;
        }

        log.info("Starting payment data seeding...");

        List<Subscription> subscriptions = subscriptionRepository.findAll();
        if (subscriptions.isEmpty()) {
            log.warn("No subscriptions found. Skipping payment seeding.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<Payment> payments = new ArrayList<>();

        // All payments use VNPAY as per user requirement
        PaymentMethod paymentMethod = PaymentMethod.VNPAY;
        BigDecimal premiumPrice = new BigDecimal("79000");
        BigDecimal familyPrice = new BigDecimal("149000");

        // Create payments with various statuses
        // PENDING payments
        payments.add(createPayment(SeedUUIDs.Payments.PAYMENT_01, SeedUUIDs.Users.USER_01,
                "user01@example.com", findSubscriptionById(subscriptions, SeedUUIDs.Subscriptions.SUB_PENDING_01),
                premiumPrice, paymentMethod, PaymentStatus.PENDING, now, 1));
        payments.add(createPayment(SeedUUIDs.Payments.PAYMENT_02, SeedUUIDs.Users.USER_02,
                "user02@example.com", findSubscriptionById(subscriptions, SeedUUIDs.Subscriptions.SUB_PENDING_02),
                premiumPrice, paymentMethod, PaymentStatus.PENDING, now, 2));

        // SUCCESS payments
        payments.add(createPayment(SeedUUIDs.Payments.PAYMENT_03, SeedUUIDs.Users.USER_04,
                "user04@example.com", findSubscriptionById(subscriptions, SeedUUIDs.Subscriptions.SUB_ACTIVE_01),
                premiumPrice, paymentMethod, PaymentStatus.SUCCESS, now.minusDays(15), 3));
        payments.add(createPayment(SeedUUIDs.Payments.PAYMENT_04, SeedUUIDs.Users.USER_05,
                "user05@example.com", findSubscriptionById(subscriptions, SeedUUIDs.Subscriptions.SUB_ACTIVE_02),
                premiumPrice, paymentMethod, PaymentStatus.SUCCESS, now.minusDays(5), 4));
        payments.add(createPayment(SeedUUIDs.Payments.PAYMENT_05, SeedUUIDs.Users.USER_06,
                "user06@example.com", findSubscriptionById(subscriptions, SeedUUIDs.Subscriptions.SUB_ACTIVE_03),
                familyPrice, paymentMethod, PaymentStatus.SUCCESS, now.minusDays(10), 5));
        payments.add(createPayment(SeedUUIDs.Payments.PAYMENT_06, SeedUUIDs.Users.USER_07,
                "user07@example.com", findSubscriptionById(subscriptions, SeedUUIDs.Subscriptions.SUB_EXPIRED_01),
                premiumPrice, paymentMethod, PaymentStatus.SUCCESS, now.minusDays(60), 6));
        payments.add(createPayment(SeedUUIDs.Payments.PAYMENT_07, SeedUUIDs.Users.USER_08,
                "user08@example.com", findSubscriptionById(subscriptions, SeedUUIDs.Subscriptions.SUB_EXPIRED_02),
                premiumPrice, paymentMethod, PaymentStatus.SUCCESS, now.minusDays(45), 7));

        // FAILED payments
        payments.add(createPayment(SeedUUIDs.Payments.PAYMENT_08, SeedUUIDs.Users.USER_03,
                "user03@example.com", null, familyPrice, paymentMethod, PaymentStatus.FAILED, now.minusDays(1), 8));
        payments.add(createPayment(SeedUUIDs.Payments.PAYMENT_09, SeedUUIDs.Users.USER_09,
                "user09@example.com", null, familyPrice, paymentMethod, PaymentStatus.FAILED, now.minusDays(2), 9));

        // CANCELLED payments
        payments.add(createPayment(SeedUUIDs.Payments.PAYMENT_10, SeedUUIDs.Users.USER_10,
                "user10@example.com", findSubscriptionById(subscriptions, SeedUUIDs.Subscriptions.SUB_CANCELLED_01),
                premiumPrice, paymentMethod, PaymentStatus.CANCELLED, now.minusDays(20), 10));

        // REFUNDED payments
        payments.add(createPayment(SeedUUIDs.Payments.PAYMENT_11, SeedUUIDs.Users.USER_11,
                "user11@example.com", findSubscriptionById(subscriptions, SeedUUIDs.Subscriptions.SUB_CANCELLED_02),
                premiumPrice, paymentMethod, PaymentStatus.REFUNDED, now.minusDays(25), 11));
        payments.add(createPayment(SeedUUIDs.Payments.PAYMENT_12, SeedUUIDs.Users.USER_12,
                "user12@example.com", findSubscriptionById(subscriptions, SeedUUIDs.Subscriptions.SUB_CANCELLED_03),
                familyPrice, paymentMethod, PaymentStatus.REFUNDED, now.minusDays(15), 12));

        paymentRepository.saveAll(payments);
        log.info("Successfully seeded {} payments", payments.size());
    }

    private Subscription findSubscriptionById(List<Subscription> subscriptions, UUID id) {
        return subscriptions.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    private Payment createPayment(UUID id, UUID userId, String userEmail, Subscription subscription,
            BigDecimal amount, PaymentMethod method, PaymentStatus status,
            LocalDateTime createdAt, int txnIndex) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setUserId(userId);
        payment.setUserEmail(userEmail);
        payment.setSubscription(subscription);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setStatus(status);
        payment.setTransactionId("TXN_SEED_" + String.format("%03d", txnIndex));
        payment.setVnpTxnRef("VNP_SEED_" + String.format("%03d", txnIndex));

        if (status == PaymentStatus.SUCCESS) {
            payment.setVnpTransactionNo("VNP_TXN_" + System.currentTimeMillis() + txnIndex);
            payment.setVnpBankCode("NCB");
            payment.setVnpCardType("ATM");
            payment.setVnpResponseCode("00");
            payment.setPaymentDate(createdAt);
        }

        payment.setCreatedAt(createdAt);
        payment.setUpdatedAt(LocalDateTime.now());
        return payment;
    }

    private void seedDevices() {
        if (deviceRepository.count() > 0) {
            log.info("Devices table is not empty. Skipping device seeding.");
            return;
        }

        log.info("Starting device data seeding...");

        LocalDateTime now = LocalDateTime.now();
        List<Device> devices = new ArrayList<>();

        // WEB devices
        devices.add(createDevice(SeedUUIDs.Devices.DEVICE_WEB_01, SeedUUIDs.Users.USER_04,
                "Chrome on Windows", DeviceType.WEB, "web-device-001", "Chrome 120.0", "Windows 11", now));
        devices.add(createDevice(SeedUUIDs.Devices.DEVICE_WEB_02, SeedUUIDs.Users.USER_05,
                "Firefox on MacOS", DeviceType.WEB, "web-device-002", "Firefox 121.0", "MacOS 14.0", now));

        // MOBILE devices
        devices.add(createDevice(SeedUUIDs.Devices.DEVICE_MOBILE_01, SeedUUIDs.Users.USER_04,
                "iPhone 15 Pro", DeviceType.MOBILE, "mobile-device-001", "Safari", "iOS 17.0", now));
        devices.add(createDevice(SeedUUIDs.Devices.DEVICE_MOBILE_02, SeedUUIDs.Users.USER_06,
                "Samsung Galaxy S24", DeviceType.MOBILE, "mobile-device-002", "Chrome Mobile", "Android 14", now));

        // TABLET device
        devices.add(createDevice(SeedUUIDs.Devices.DEVICE_TABLET, SeedUUIDs.Users.USER_05,
                "iPad Pro", DeviceType.TABLET, "tablet-device-001", "Safari", "iPadOS 17.0", now));

        // TV device
        devices.add(createDevice(SeedUUIDs.Devices.DEVICE_TV, SeedUUIDs.Users.USER_06,
                "Samsung Smart TV", DeviceType.TV, "tv-device-001", "Tizen Browser", "Tizen 7.0", now));

        // DESKTOP devices
        devices.add(createDevice(SeedUUIDs.Devices.DEVICE_DESKTOP_01, SeedUUIDs.Users.USER_07,
                "Desktop App - Windows", DeviceType.DESKTOP, "desktop-device-001", "Cinemate App", "Windows 11", now));
        devices.add(createDevice(SeedUUIDs.Devices.DEVICE_DESKTOP_02, SeedUUIDs.Users.USER_08,
                "Desktop App - MacOS", DeviceType.DESKTOP, "desktop-device-002", "Cinemate App", "MacOS 14.0", now));

        deviceRepository.saveAll(devices);
        log.info("Successfully seeded {} devices", devices.size());
    }

    private Device createDevice(UUID id, UUID userId, String deviceName, DeviceType deviceType,
            String deviceId, String browserInfo, String osInfo, LocalDateTime now) {
        Device device = new Device();
        device.setId(id);
        device.setUserId(userId);
        device.setDeviceName(deviceName);
        device.setDeviceType(deviceType);
        device.setDeviceId(deviceId);
        device.setBrowserInfo(browserInfo);
        device.setOsInfo(osInfo);
        device.setIpAddress("192.168.1." + (100 + Math.abs(deviceId.hashCode() % 100)));
        device.setLastAccessed(now);
        device.setIsActive(true);
        device.setCreatedAt(now);
        device.setUpdatedAt(now);
        return device;
    }

    private void seedFamilyMembers() {
        if (familyMemberRepository.count() > 0) {
            log.info("FamilyMembers table is not empty. Skipping family member seeding.");
            return;
        }

        log.info("Starting family member data seeding...");

        // Get the active family subscription
        Subscription familySubscription = subscriptionRepository.findById(SeedUUIDs.Subscriptions.SUB_ACTIVE_03)
                .orElse(null);

        if (familySubscription == null) {
            log.warn("Active family subscription not found. Skipping family member seeding.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<FamilyMember> members = new ArrayList<>();

        // Owner
        members.add(createFamilyMember(SeedUUIDs.FamilyMembers.MEMBER_OWNER, familySubscription,
                SeedUUIDs.Users.USER_06, true, now.minusDays(10)));

        // Other members
        members.add(createFamilyMember(SeedUUIDs.FamilyMembers.MEMBER_02, familySubscription,
                SeedUUIDs.Users.USER_07, false, now.minusDays(8)));
        members.add(createFamilyMember(SeedUUIDs.FamilyMembers.MEMBER_03, familySubscription,
                SeedUUIDs.Users.USER_08, false, now.minusDays(5)));
        members.add(createFamilyMember(SeedUUIDs.FamilyMembers.MEMBER_04, familySubscription,
                SeedUUIDs.Users.USER_09, false, now.minusDays(3)));
        members.add(createFamilyMember(SeedUUIDs.FamilyMembers.MEMBER_05, familySubscription,
                SeedUUIDs.Users.USER_10, false, now.minusDays(1)));

        familyMemberRepository.saveAll(members);
        log.info("Successfully seeded {} family members", members.size());
    }

    private FamilyMember createFamilyMember(UUID id, Subscription subscription, UUID userId,
            boolean isOwner, LocalDateTime joinedAt) {
        FamilyMember member = new FamilyMember();
        member.setId(id);
        member.setSubscription(subscription);
        member.setUserId(userId);
        member.setIsOwner(isOwner);
        member.setJoinedAt(joinedAt);
        member.setCreatedAt(joinedAt);
        return member;
    }

    private void seedFamilyInvitations() {
        if (familyInvitationRepository.count() > 0) {
            log.info("FamilyInvitations table is not empty. Skipping family invitation seeding.");
            return;
        }

        log.info("Starting family invitation data seeding...");

        Subscription familySubscription = subscriptionRepository.findById(SeedUUIDs.Subscriptions.SUB_ACTIVE_03)
                .orElse(null);

        if (familySubscription == null) {
            log.warn("Active family subscription not found. Skipping family invitation seeding.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<FamilyInvitation> invitations = new ArrayList<>();

        // PENDING invitation (ADULT mode)
        invitations.add(createFamilyInvitation(SeedUUIDs.FamilyInvitations.INVITATION_PENDING,
                familySubscription, InvitationMode.ADULT, InvitationStatus.PENDING,
                SeedUUIDs.Users.USER_06, null, now.plusDays(7), null, now));

        // ACCEPTED invitation
        invitations.add(createFamilyInvitation(SeedUUIDs.FamilyInvitations.INVITATION_ACCEPTED,
                familySubscription, InvitationMode.ADULT, InvitationStatus.ACCEPTED,
                SeedUUIDs.Users.USER_06, SeedUUIDs.Users.USER_07, now.plusDays(5), now.minusDays(2), now.minusDays(8)));

        // EXPIRED invitation
        invitations.add(createFamilyInvitation(SeedUUIDs.FamilyInvitations.INVITATION_EXPIRED,
                familySubscription, InvitationMode.ADULT, InvitationStatus.EXPIRED,
                SeedUUIDs.Users.USER_06, null, now.minusDays(1), null, now.minusDays(8)));

        // CANCELLED invitation
        invitations.add(createFamilyInvitation(SeedUUIDs.FamilyInvitations.INVITATION_CANCELLED,
                familySubscription, InvitationMode.ADULT, InvitationStatus.CANCELLED,
                SeedUUIDs.Users.USER_06, null, now.plusDays(3), null, now.minusDays(5)));

        // KID mode invitation
        invitations.add(createFamilyInvitation(SeedUUIDs.FamilyInvitations.INVITATION_KID,
                familySubscription, InvitationMode.KID, InvitationStatus.ACCEPTED,
                SeedUUIDs.Users.USER_06, SeedUUIDs.Users.USER_10, now.plusDays(4), now.minusDays(1), now.minusDays(3)));

        familyInvitationRepository.saveAll(invitations);
        log.info("Successfully seeded {} family invitations", invitations.size());
    }

    private FamilyInvitation createFamilyInvitation(UUID id, Subscription subscription,
            InvitationMode mode, InvitationStatus status,
            UUID invitedBy, UUID invitedUserId,
            LocalDateTime expiresAt, LocalDateTime acceptedAt,
            LocalDateTime createdAt) {
        FamilyInvitation invitation = new FamilyInvitation();
        invitation.setId(id);
        invitation.setSubscription(subscription);
        invitation.setInvitationToken(UUID.randomUUID().toString());
        invitation.setMode(mode);
        invitation.setStatus(status);
        invitation.setInvitedBy(invitedBy);
        invitation.setInvitedUserId(invitedUserId);
        invitation.setExpiresAt(expiresAt);
        invitation.setAcceptedAt(acceptedAt);
        invitation.setCreatedAt(createdAt);
        return invitation;
    }
}
