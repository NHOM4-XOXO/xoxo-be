package com.nhom4.xoxo.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EncryptionService {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final int AES_KEY_SIZE = 256;
    private static final int RSA_KEY_SIZE = 2048;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;

    /**
     * Generate RSA key pair for user
     */
    public KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(RSA_KEY_SIZE, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    /**
     * Generate AES key for message encryption
     */
    public SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }

    /**
     * Encrypt message with AES
     */
    public EncryptedMessage encryptMessage(String message, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        byte[] iv = generateIV();
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] encryptedData = cipher.doFinal(message.getBytes());
        
        return EncryptedMessage.builder()
                .encryptedData(Base64.getEncoder().encodeToString(encryptedData))
                .iv(Base64.getEncoder().encodeToString(iv))
                .build();
    }

    /**
     * Decrypt message with AES
     */
    public String decryptMessage(EncryptedMessage encryptedMessage, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        byte[] iv = Base64.getDecoder().decode(encryptedMessage.getIv());
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        
        cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
        byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedMessage.getEncryptedData()));
        
        return new String(decryptedData);
    }

    /**
     * Encrypt AES key with RSA public key
     */
    public String encryptAESKey(SecretKey aesKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    /**
     * Decrypt AES key with RSA private key
     */
    public SecretKey decryptAESKey(String encryptedAESKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedKey = cipher.doFinal(Base64.getDecoder().decode(encryptedAESKey));
        return new SecretKeySpec(decryptedKey, "AES");
    }

    /**
     * Generate random IV for GCM
     */
    private byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * Encrypted message wrapper
     */
    public static class EncryptedMessage {
        private String encryptedData;
        private String iv;
        private String encryptedAESKey;

        public EncryptedMessage() {}

        public EncryptedMessage(String encryptedData, String iv) {
            this.encryptedData = encryptedData;
            this.iv = iv;
        }

        // Getters and setters
        public String getEncryptedData() { return encryptedData; }
        public void setEncryptedData(String encryptedData) { this.encryptedData = encryptedData; }
        public String getIv() { return iv; }
        public void setIv(String iv) { this.iv = iv; }
        public String getEncryptedAESKey() { return encryptedAESKey; }
        public void setEncryptedAESKey(String encryptedAESKey) { this.encryptedAESKey = encryptedAESKey; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private EncryptedMessage message = new EncryptedMessage();

            public Builder encryptedData(String encryptedData) {
                message.encryptedData = encryptedData;
                return this;
            }

            public Builder iv(String iv) {
                message.iv = iv;
                return this;
            }

            public Builder encryptedAESKey(String encryptedAESKey) {
                message.encryptedAESKey = encryptedAESKey;
                return this;
            }

            public EncryptedMessage build() {
                return message;
            }
        }
    }
}
