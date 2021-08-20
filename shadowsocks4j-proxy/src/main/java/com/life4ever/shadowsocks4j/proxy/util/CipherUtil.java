package com.life4ever.shadowsocks4j.proxy.util;

import com.life4ever.shadowsocks4j.proxy.exception.Shadowsocks4jProxyException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import static com.life4ever.shadowsocks4j.proxy.util.ConfigUtil.getCipherPassword;
import static com.life4ever.shadowsocks4j.proxy.util.ConfigUtil.getCipherSalt;

public class CipherUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int DEFAULT_AES_GCM_TAG_BITS_LENGTH = 128;

    private static final int DEFAULT_AES_GCM_IV_BYTES_LENGTH = 8;

    private static volatile SecretKey secretKey;

    private CipherUtil() {
    }

    public static byte[] encrypt(byte[] content) throws Shadowsocks4jProxyException {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            // 生成 iv
            byte[] iv = new byte[DEFAULT_AES_GCM_IV_BYTES_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(DEFAULT_AES_GCM_TAG_BITS_LENGTH, iv);

            // 生成密钥
            SecretKey secretKey = getSecretKeyFromPassword(getCipherPassword(), getCipherSalt());
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            // iv + 密文
            byte[] encryptedContent = cipher.doFinal(content);
            return assemblyEncryptedContentWithIv(encryptedContent, iv);
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | InvalidKeySpecException | BadPaddingException | InvalidKeyException e) {
            throw new Shadowsocks4jProxyException(e.getMessage(), e);
        }
    }

    public static byte[] decrypt(byte[] encryptedContent) throws Shadowsocks4jProxyException {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(DEFAULT_AES_GCM_TAG_BITS_LENGTH, encryptedContent, 0, DEFAULT_AES_GCM_IV_BYTES_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, getSecretKeyFromPassword(getCipherPassword(), getCipherSalt()), gcmParameterSpec);
            return cipher.doFinal(encryptedContent, DEFAULT_AES_GCM_IV_BYTES_LENGTH, encryptedContent.length - DEFAULT_AES_GCM_IV_BYTES_LENGTH);
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException | InvalidKeySpecException | BadPaddingException | InvalidKeyException e) {
            throw new Shadowsocks4jProxyException(e.getMessage(), e);
        }
    }

    private static SecretKey getSecretKeyFromPassword(String password, String salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (secretKey == null) {
            synchronized (CipherUtil.class) {
                if (secretKey == null) {
                    SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                    KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 128);
                    secretKey = new SecretKeySpec(secretKeyFactory.generateSecret(keySpec).getEncoded(), "AES");
                }
            }
        }
        return secretKey;
    }

    private static byte[] assemblyEncryptedContentWithIv(byte[] encryptedContent, byte[] iv) {
        byte[] bytes = new byte[encryptedContent.length + DEFAULT_AES_GCM_IV_BYTES_LENGTH];
        System.arraycopy(iv, 0, bytes, 0, iv.length);
        System.arraycopy(encryptedContent, 0, bytes, iv.length, encryptedContent.length);
        return bytes;
    }

}
