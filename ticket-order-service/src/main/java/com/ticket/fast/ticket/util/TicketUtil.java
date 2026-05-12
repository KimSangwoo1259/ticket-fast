package com.ticket.fast.ticket.util;

public class TicketUtil {

    public static String createPerformanceRedisKey(Long performanceId){
        return "performance:" + performanceId + ":available_seats";
    }
    public static String createDetailKey(String redisKey){
        return redisKey.concat(":details");
    }
}
