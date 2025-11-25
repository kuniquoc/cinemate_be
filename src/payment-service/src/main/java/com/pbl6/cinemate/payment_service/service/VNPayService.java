package com.pbl6.cinemate.payment_service.service;

import com.pbl6.cinemate.payment_service.config.VNPayConfig;
import com.pbl6.cinemate.payment_service.dto.response.PaymentUrlResponse;
import com.pbl6.cinemate.payment_service.entity.Payment;
import com.pbl6.cinemate.payment_service.enums.PaymentStatus;
import com.pbl6.cinemate.payment_service.exception.InvalidPaymentException;
import com.pbl6.cinemate.payment_service.exception.PaymentProcessingException;
import com.pbl6.cinemate.payment_service.exception.ResourceNotFoundException;
import com.pbl6.cinemate.payment_service.repository.PaymentRepository;
import com.pbl6.cinemate.payment_service.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {
    
    private final VNPayConfig vnPayConfig;
    private final PaymentRepository paymentRepository;
    
    @Transactional
    public PaymentUrlResponse createPaymentUrl(Payment payment, String ipAddress) {
        try {
            // Create VNPay parameters
            Map<String, String> vnpParams = new HashMap<>();
            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
            vnpParams.put("vnp_Amount", String.valueOf(payment.getAmount().multiply(new BigDecimal(100)).longValue()));
            vnpParams.put("vnp_CurrCode", "VND");
            vnpParams.put("vnp_TxnRef", payment.getVnpTxnRef());
            vnpParams.put("vnp_OrderInfo", payment.getVnpOrderInfo() != null ? payment.getVnpOrderInfo() : "Payment for subscription");
            vnpParams.put("vnp_OrderType", "other");
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
            vnpParams.put("vnp_IpAddr", ipAddress);
            
            // Create date format
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            String createDate = formatter.format(new Date());
            vnpParams.put("vnp_CreateDate", createDate);
            
            // Set expiration time (15 minutes)
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            calendar.add(Calendar.MINUTE, 15);
            String expireDate = formatter.format(calendar.getTime());
            vnpParams.put("vnp_ExpireDate", expireDate);
            
            // Generate secure hash
            String secureHash = VNPayUtil.generateSecureHash(vnpParams, vnPayConfig.getHashSecret());
            vnpParams.put("vnp_SecureHash", secureHash);
            
            // Build payment URL
            String queryString = VNPayUtil.buildQueryString(vnpParams, true);
            String paymentUrl = vnPayConfig.getUrl() + "?" + queryString;
            
            log.info("Created VNPay payment URL for transaction: {}", payment.getVnpTxnRef());
            
            return new PaymentUrlResponse(
                    paymentUrl,
                    payment.getVnpTxnRef(),
                    "Payment URL created successfully"
            );
        } catch (Exception e) {
            log.error("Error creating VNPay payment URL", e);
            throw new PaymentProcessingException("Failed to create payment URL", e);
        }
    }
    
    @Transactional
    public Payment processPaymentCallback(Map<String, String> params) {
        try {
            // Verify secure hash
            String vnpSecureHash = params.get("vnp_SecureHash");
            if (vnpSecureHash == null) {
                throw new InvalidPaymentException("Missing secure hash in callback");
            }
            
            Map<String, String> paramsToVerify = new HashMap<>(params);
            paramsToVerify.remove("vnp_SecureHash");
            paramsToVerify.remove("vnp_SecureHashType");
            
            boolean isValidHash = VNPayUtil.verifySecureHash(paramsToVerify, vnPayConfig.getHashSecret(), vnpSecureHash);
            if (!isValidHash) {
                log.error("Invalid VNPay secure hash");
                throw new InvalidPaymentException("Invalid payment signature");
            }
            
            // Get payment by transaction reference
            String vnpTxnRef = params.get("vnp_TxnRef");
            Payment payment = paymentRepository.findByVnpTxnRef(vnpTxnRef)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", "vnpTxnRef", vnpTxnRef));
            
            // Update payment with VNPay response
            String responseCode = params.get("vnp_ResponseCode");
            payment.setVnpResponseCode(responseCode);
            payment.setVnpTransactionNo(params.get("vnp_TransactionNo"));
            payment.setVnpBankCode(params.get("vnp_BankCode"));
            payment.setVnpCardType(params.get("vnp_CardType"));
            payment.setVnpPayDate(params.get("vnp_PayDate"));
            
            // Update payment status based on response code
            if ("00".equals(responseCode)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaymentDate(LocalDateTime.now());
                payment.setTransactionId(params.get("vnp_TransactionNo"));
                log.info("Payment successful: {}", vnpTxnRef);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                log.warn("Payment failed with response code {}: {}", responseCode, vnpTxnRef);
            }
            
            return paymentRepository.save(payment);
            
        } catch (InvalidPaymentException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing VNPay callback", e);
            throw new PaymentProcessingException("Failed to process payment callback", e);
        }
    }
    
    public String getPaymentStatus(String responseCode) {
        return switch (responseCode) {
            case "00" -> "Payment successful";
            case "07" -> "Transaction suspicious (fraudulent transaction)";
            case "09" -> "Card not registered for Internet Banking";
            case "10" -> "Authentication failed";
            case "11" -> "Payment timeout";
            case "12" -> "Card locked";
            case "13" -> "Invalid OTP";
            case "24" -> "Transaction cancelled";
            case "51" -> "Insufficient balance";
            case "65" -> "Daily transaction limit exceeded";
            case "75" -> "Payment bank is under maintenance";
            case "79" -> "Payment timeout, please try again";
            default -> "Payment failed";
        };
    }
}
