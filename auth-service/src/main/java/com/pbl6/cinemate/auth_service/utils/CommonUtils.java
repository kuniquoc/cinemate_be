package com.pbl6.cinemate.auth_service.utils;

import java.util.Random;

public final class CommonUtils {
    public static int getRandomFourDigitNumber() {
        Random random = new Random();
        // Sinh số ngẫu nhiên từ 1000 đến 9999
        return 1000 + random.nextInt(9000); // 9000 vì 9999 - 1000 + 1 = 9000
    }

    private CommonUtils() {
    }
}
