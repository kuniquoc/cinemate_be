package com.pbl6.cinemate.payment_service.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class VNPayUtil {
    
    public static String generateSecureHash(Map<String, String> params, String secretKey) {
        try {
            // Sort parameters by key
            Map<String, String> sortedParams = new TreeMap<>(params);
            
            // Build hash data WITH URL encoding (VNPay requirement)
            // Exclude vnp_SecureHash and vnp_SecureHashType
            String hashData = sortedParams.entrySet().stream()
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                    .filter(entry -> !entry.getKey().equals("vnp_SecureHash") && !entry.getKey().equals("vnp_SecureHashType"))
                    .map(entry -> {
                        try {
                            return entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            return entry.getKey() + "=" + entry.getValue();
                        }
                    })
                    .collect(Collectors.joining("&"));
            
            // Generate HMAC SHA512
            Mac hmacSHA512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmacSHA512.init(secretKeySpec);
            byte[] hash = hmacSHA512.doFinal(hashData.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string (uppercase for VNPay)
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException("Error generating VNPay secure hash", e);
        }
    }
    
    public static boolean verifySecureHash(Map<String, String> params, String secretKey, String secureHash) {
        String generatedHash = generateSecureHash(params, secretKey);
        return generatedHash.equalsIgnoreCase(secureHash);
    }
    
    public static String buildQueryString(Map<String, String> params, boolean encode) {
        Map<String, String> sortedParams = new TreeMap<>(params);
        
        return sortedParams.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .map(entry -> {
                    // Don't encode the secure hash itself (it's already in hex format)
                    if (entry.getKey().equals("vnp_SecureHash") || entry.getKey().equals("vnp_SecureHashType")) {
                        return entry.getKey() + "=" + entry.getValue();
                    }
                    String value = encode ? 
                            URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8) : 
                            entry.getValue();
                    return entry.getKey() + "=" + value;
                })
                .collect(Collectors.joining("&"));
    }
    
    public static String getIpAddress(String xForwardedFor, String remoteAddr) {
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return remoteAddr != null ? remoteAddr : "127.0.0.1";
    }
}
