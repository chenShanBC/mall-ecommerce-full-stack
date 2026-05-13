package com.mallfei.pay.infrastructure.channel;

import com.mallfei.common.exception.BusinessException;

import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class AlipayRsa2Support {

    private AlipayRsa2Support() {
    }

    public static String sign(String content, String privateKeyText, Charset charset) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(loadPrivateKey(privateKeyText));
            signature.update(content.getBytes(charset));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception exception) {
            throw BusinessException.badRequest("支付宝RSA2签名失败: " + exception.getMessage());
        }
    }

    public static boolean verify(String content, String sign, String publicKeyText, Charset charset) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(loadPublicKey(publicKeyText));
            signature.update(content.getBytes(charset));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception exception) {
            return false;
        }
    }

    private static PrivateKey loadPrivateKey(String key) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(normalize(key));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private static PublicKey loadPublicKey(String key) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(normalize(key));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    private static String normalize(String key) {
        return key.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
    }
}
