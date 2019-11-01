package com.jpmorgan.quorum.encryption.ec;

import com.quorum.tessera.encryption.Encryptor;
import com.quorum.tessera.encryption.EncryptorFactory;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class EllipticalCurveEncryptorFactory implements EncryptorFactory {

    @Override
    public String getType() {
        return "EC";
    }

    @Override
    public Encryptor create(Map<String, String> properties) {
        final Map<String, String> props = Optional.ofNullable(properties).orElse(Collections.emptyMap());
        String symmetricCipher = properties.getOrDefault("symmetricCipher", "AES/GCM/NoPadding");
        String ellipticCurve = properties.getOrDefault("ellipticCurve", "secp256r1");
        int nonceLength = Integer.parseInt(properties.getOrDefault("nonceLength", "24"));
        int sharedKeyLength = Integer.parseInt(properties.getOrDefault("sharedKeyLength", "32"));

        return new EllipticalCurveEncryptor(symmetricCipher, ellipticCurve, nonceLength, sharedKeyLength);
    }
}