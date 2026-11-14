package com.tianxin.lifeline.blockchain.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 存证哈希工具。
 *
 * <p>优先使用国密 SM3（hutool SmUtil + BouncyCastle Provider），
 * 运行环境缺失 BouncyCastle 时自动降级 JDK MessageDigest SHA-256，
 * 保证任何环境下存证/校验链路可用。
 *
 * <p>存证哈希口径：{@code hash(content | relatedId | relatedType | 时间戳毫秒)}，
 * 各因子以竖线分隔避免拼接歧义，校验时按同一口径重算比对。
 */
@Slf4j
public final class HashUtil {

    /** SM3 算法名 */
    public static final String ALGORITHM_SM3 = "SM3";

    /** SHA-256 算法名（降级方案） */
    public static final String ALGORITHM_SHA256 = "SHA-256";

    /** 运行环境是否具备 SM3 能力（BouncyCastle 在 classpath 即具备） */
    private static final boolean SM3_AVAILABLE = detectSm3();

    private HashUtil() {
    }

    /**
     * 探测 BouncyCastle SM3 摘要类是否存在。
     *
     * @return true 表示可使用国密 SM3
     */
    private static boolean detectSm3() {
        try {
            Class.forName("org.bouncycastle.crypto.digests.SM3Digest");
            return true;
        } catch (Throwable t) {
            log.warn("BouncyCastle 缺失, 存证哈希降级为 SHA-256");
            return false;
        }
    }

    /**
     * 当前生效的哈希算法名。
     *
     * @return SM3 或 SHA-256
     */
    public static String algorithm() {
        return SM3_AVAILABLE ? ALGORITHM_SM3 : ALGORITHM_SHA256;
    }

    /**
     * 对原文计算存证哈希（SM3 优先，SHA-256 兜底），输出十六进制小写。
     *
     * @param raw 原文
     * @return 哈希十六进制串
     */
    public static String digest(String raw) {
        if (SM3_AVAILABLE) {
            return SmUtil.sm3(raw);
        }
        return sha256Hex(raw);
    }

    /**
     * 计算存证哈希：{@code hash(content | relatedId | relatedType | 时间戳毫秒)}。
     *
     * @param content          存证内容（null 视为空串）
     * @param relatedId        关联业务对象ID
     * @param relatedType      关联业务对象类型（null 视为空串）
     * @param timestampMillis  时间戳（毫秒，取上链时间）
     * @return 存证哈希十六进制串
     */
    public static String evidenceHash(String content, Long relatedId, String relatedType, long timestampMillis) {
        String raw = StrUtil.nullToEmpty(content) + "|" + relatedId + "|"
                + StrUtil.nullToEmpty(relatedType) + "|" + timestampMillis;
        return digest(raw);
    }

    /**
     * JDK 原生 SHA-256 摘要（降级方案）。
     *
     * @param raw 原文
     * @return 哈希十六进制串
     */
    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // JDK 规范要求必须提供 SHA-256，理论不可达
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }
}
