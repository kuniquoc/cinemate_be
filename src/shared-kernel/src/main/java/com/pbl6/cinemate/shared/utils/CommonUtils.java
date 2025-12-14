package com.pbl6.cinemate.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public final class CommonUtils {
    private CommonUtils() {
    }

    public static int getRandomFourDigitNumber() {
        Random random = new Random();
        // Sinh số ngẫu nhiên từ 1000 đến 9999
        return 1000 + random.nextInt(9000); // 9000 vì 9999 - 1000 + 1 = 9000
    }

    public static String toJsonString(Object ob) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(ob);
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
