package com.bsi.utility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

public class RequestIdGenerator {
    public static String generateRequestID(){
        String data = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        long random = ThreadLocalRandom.current().nextLong(10_000_000L,99_999_999L);
        return data+random;
    }

    public static String currentRequestDate(){
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date());
    }
}
