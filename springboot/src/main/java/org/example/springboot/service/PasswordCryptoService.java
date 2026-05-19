package org.example.springboot.service;

import jakarta.annotation.PostConstruct;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.security.SecurityProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

@Service
public class PasswordCryptoService {
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private final SecurityProperties securityProperties;

    public PasswordCryptoService(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public boolean isEncryptionEnabled() {
        return securityProperties.isPasswordEncryptionEnabled();
    }

    @PostConstruct
    public void init() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();
        } catch (Exception e) {
            throw new ServiceException("初始化登录加密密钥失败");
        }
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String decryptPassword(String encryptedPassword) {
        if (!StringUtils.hasText(encryptedPassword)) {
            throw new ServiceException("密码不能为空");
        }
        if (!isEncryptionEnabled()) {
            return encryptedPassword;
        }
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaepParams = new OAEPParameterSpec(
                    "SHA-256",
                    "MGF1",
                    MGF1ParameterSpec.SHA256,
                    PSource.PSpecified.DEFAULT
            );
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParams);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ServiceException("密码解密失败，请刷新页面后重试");
        }
    }
}
