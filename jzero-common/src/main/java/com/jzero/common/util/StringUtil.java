package com.jzero.common.util;

/**
 * @author: ZhouYingAn
 * @date: 2026/3/22
 * @Version: 1.0
 * @description: 字符串工具
 */
public class StringUtil {

    /**
     * 判空
     * @param cs
     * @return
     */
    public static boolean isNotNullAndNotEmpty(String cs){
        int strLen = length(cs);
        if (strLen == 0) {
            return true;
        } else {
            for(int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(cs.charAt(i))) {
                    return false;
                }
            }

            return true;
        }
    }
    public static boolean isNullOrEmpty(String cs){
        return !isNotNullAndNotEmpty(cs);
    }
    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
}
