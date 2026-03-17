package com.ticket.fast.common.util;

import com.github.f4b6a3.tsid.TsidCreator;

public class TsidUtil {

    // 64비트 Long 타입 TSID 생성 (DB 저장용)
    public static Long nextLong() {
        return TsidCreator.getTsid().toLong();
    }

    // 13~20자리 문자열 타입 TSID 생성 (프론트 전달용)
    public static String nextString() {
        return TsidCreator.getTsid().toString();
    }


}
