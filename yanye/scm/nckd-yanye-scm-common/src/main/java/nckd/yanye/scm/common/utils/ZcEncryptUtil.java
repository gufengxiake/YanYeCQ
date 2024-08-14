package nckd.yanye.scm.common.utils;

import cn.hutool.core.date.DatePattern;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import nckd.yanye.scm.common.ZcPlatformConst;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 招采平台回调消息体签名及解密工具类
 *
 * @author liuxiao
 */
public class ZcEncryptUtil {

    /**
     * 签名校验
     *
     * @param signature
     * @param timestamp
     * @param nonce
     * @return true-校验成功，false-校验失败
     */
    public static boolean checkSignature(String signature,
                                         String timestamp,
                                         String nonce) {
        String rawString = Stream.of(nonce, timestamp, ZcPlatformConst.SECRET)
                .sorted(String::compareTo)
                .collect(Collectors.joining());
        return Objects.equals(signature, SecureUtil.sha1(rawString));
    }

    /**
     * 加密消息体
     *
     * @param body
     * @return
     */
    public static String encryptBody(Object body) {
        String rawJson = JSONUtil.parseObj(body)
                .setDateFormat(DatePattern.NORM_DATETIME_PATTERN)
                .toString();

        String encryptBody = SecureUtil.aes(
                        SecureUtil.md5(ZcPlatformConst.SECRET).toLowerCase()
                                .getBytes())
                .encryptBase64(rawJson);
        return encryptBody;
    }

    /**
     * 解密消息体
     *
     * @param encryptBody
     * @return
     */
    public static String decryptBody(String encryptBody) {
        String decryptStr = SecureUtil.aes(
                        SecureUtil.md5(ZcPlatformConst.SECRET).toLowerCase()
                                .getBytes())
                .decryptStr(encryptBody);
        return decryptStr;
    }
}