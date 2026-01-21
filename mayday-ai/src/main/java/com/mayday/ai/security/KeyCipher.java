package com.mayday.ai.security;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * KeyCipher：用于 API Key 的加/解密（AES-GCM）
 *
 * 约束：
 * 1) 数据库只存密文（base64），不存明文
 * 2) master-key 由环境变量或配置注入（建议 32字节随机密钥后 base64）
 * 3) 日志严禁输出明文 key
 *
 * 密文格式：
 * - out = iv(12 bytes) + ciphertext_with_tag
 * - 再 base64 存入 key_cipher
 */
@Component
public class KeyCipher {

    private static final int IV_LEN = 12;      // GCM 推荐 12 bytes IV
    private static final int TAG_BITS = 128;   // GCM Tag 128 bits

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public KeyCipher(@Value("${mayday.ai.master-key}") String masterKeyBase64) {
        byte[] raw = Base64.getDecoder().decode(masterKeyBase64);
        this.secretKey = new SecretKeySpec(raw, "AES");
    }

    /** 加密：明文 -> base64(iv + cipherTextWithTag) */
    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LEN];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));

            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);

            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("AI key encrypt failed", e);
        }
    }

    /** 解密：base64(iv + cipherTextWithTag) -> 明文 */
    public String decrypt(String cipherBase64) {
        try {
            byte[] in = Base64.getDecoder().decode(cipherBase64);
            if (in.length <= IV_LEN) {
                throw new IllegalArgumentException("Invalid cipher text");
            }

            byte[] iv = Arrays.copyOfRange(in, 0, IV_LEN);
            byte[] ct = Arrays.copyOfRange(in, IV_LEN, in.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));

            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AI key decrypt failed", e);
        }
    }
}
