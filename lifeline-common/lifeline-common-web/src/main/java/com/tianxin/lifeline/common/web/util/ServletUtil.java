package com.tianxin.lifeline.common.web.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Servlet 工具类。
 *
 * <p>提供获取客户端真实IP、读取请求头等通用能力。
 */
public final class ServletUtil {

    /** 未知IP占位符 */
    private static final String UNKNOWN = "unknown";

    /** 本地回环IPv4 */
    private static final String LOCAL_IPV4 = "127.0.0.1";

    private ServletUtil() {
    }

    /**
     * 获取客户端真实IP，按代理链常见请求头依次解析。
     *
     * @param request 当前请求
     * @return 客户端IP
     */
    public static String getClientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (StrUtil.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
                // X-Forwarded-For 为逗号分隔的IP链，取第一个非unknown值
                int commaIdx = ip.indexOf(',');
                if (commaIdx > 0) {
                    ip = ip.substring(0, commaIdx).trim();
                }
                if (StrUtil.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
                    return ip;
                }
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 判断IP是否为本地地址。
     *
     * @param ip IP地址
     * @return true 表示本地地址
     */
    public static boolean isLocalIp(String ip) {
        return LOCAL_IPV4.equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip);
    }

    /**
     * 获取请求头，不存在返回空字符串。
     *
     * @param request   当前请求
     * @param headerName 请求头名称
     * @return 请求头值
     */
    public static String getHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return value == null ? StrUtil.EMPTY : value;
    }

    /**
     * 获取 Bearer Token（去除 "Bearer " 前缀）。
     *
     * @param request 当前请求
     * @return 纯token字符串，无token返回 null
     */
    public static String getBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StrUtil.isBlank(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
