package com.quorum.tessera.key.generation;

import com.quorum.tessera.config.ArgonOptions;
import com.quorum.tessera.config.keypairs.AWSKeyPair;
import com.quorum.tessera.config.vault.data.AWSSetSecretData;
import com.quorum.tessera.encryption.Encryptor;
import com.quorum.tessera.encryption.KeyPair;
import com.quorum.tessera.encryption.PrivateKey;
import com.quorum.tessera.encryption.PublicKey;
import com.quorum.tessera.key.vault.KeyVaultService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.UnsupportedCharsetException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.*;

public class AWSSecretManagerKeyGeneratorTest {

    private final String pubStr = "public";
    private final String privStr = "private";
    private final PublicKey pub = PublicKey.from(pubStr.getBytes());
    private final PrivateKey priv = PrivateKey.from(privStr.getBytes());

    private KeyVaultService keyVaultService;
    private AWSSecretManagerKeyGenerator awsSecretManagerKeyGenerator;

    @Before
    public void setUp() {
        final Encryptor encryptor = mock(Encryptor.class);
        this.keyVaultService = mock(KeyVaultService.class);

        final KeyPair keyPair = new KeyPair(pub, priv);

        when(encryptor.generateNewKeys()).thenReturn(keyPair);

        awsSecretManagerKeyGenerator = new AWSSecretManagerKeyGenerator(encryptor, keyVaultService);
    }

    @Test
    public void keysSavedInVaultWithProvidedVaultIdAndCorrectSuffix() {
        final String vaultId = "vaultId";
        final String pubVaultId = vaultId + "Pub";
        final String privVaultId = vaultId + "Key";

        final AWSKeyPair result = awsSecretManagerKeyGenerator.generate(vaultId, null, null);

        final ArgumentCaptor<AWSSetSecretData> captor = ArgumentCaptor.forClass(AWSSetSecretData.class);

        verify(keyVaultService, times(2)).setSecret(captor.capture());

        List<AWSSetSecretData> capturedArgs = captor.getAllValues();
        assertThat(capturedArgs).hasSize(2);

        AWSSetSecretData expectedDataPub = new AWSSetSecretData(pubVaultId, pub.encodeToBase64());
        AWSSetSecretData expectedDataPriv = new AWSSetSecretData(privVaultId, priv.encodeToBase64());

        assertThat(capturedArgs)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expectedDataPub, expectedDataPriv);

        verifyNoMoreInteractions(keyVaultService);

        final AWSKeyPair expected = new AWSKeyPair(pubVaultId, privVaultId);

        assertThat(result).isExactlyInstanceOf(AWSKeyPair.class);
        assertThat(result).isEqualToComparingFieldByField(expected);
    }

    @Test
    public void vaultIdIsFinalComponentOfFilePath() {
        final String vaultId = "vaultId";
        final String pubVaultId = vaultId + "Pub";
        final String privVaultId = vaultId + "Key";
        final String path = "/some/path/" + vaultId;

        awsSecretManagerKeyGenerator.generate(path, null, null);

        final ArgumentCaptor<AWSSetSecretData> captor = ArgumentCaptor.forClass(AWSSetSecretData.class);

        verify(keyVaultService, times(2)).setSecret(captor.capture());

        List<AWSSetSecretData> capturedArgs = captor.getAllValues();
        assertThat(capturedArgs).hasSize(2);

        AWSSetSecretData expectedDataPub = new AWSSetSecretData(pubVaultId, pub.encodeToBase64());
        AWSSetSecretData expectedDataPriv = new AWSSetSecretData(privVaultId, priv.encodeToBase64());

        assertThat(capturedArgs)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(expectedDataPub, expectedDataPriv);

        verifyNoMoreInteractions(keyVaultService);
    }

    @Test
    public void ifNoVaultIdProvidedThenSuffixOnlyIsUsed() {
        awsSecretManagerKeyGenerator.generate(null, null, null);

        final ArgumentCaptor<AWSSetSecretData> captor = ArgumentCaptor.forClass(AWSSetSecretData.class);

        verify(keyVaultService, times(2)).setSecret(captor.capture());

        List<AWSSetSecretData> capturedArgs = captor.getAllValues();
        assertThat(capturedArgs).hasSize(2);

        AWSSetSecretData expectedDataPub = new AWSSetSecretData("Pub", pub.encodeToBase64());
        AWSSetSecretData expectedDataPriv = new AWSSetSecretData("Key", priv.encodeToBase64());

        assertThat(capturedArgs)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(expectedDataPub, expectedDataPriv);

        verifyNoMoreInteractions(keyVaultService);
    }

    @Test
    public void exceptionThrownIfDisallowedCharactersUsedInVaultId() {
        final String invalidId = "/tmp/abc@+!";

        final Throwable throwable = catchThrowable(() -> awsSecretManagerKeyGenerator.generate(invalidId, null, null));

        assertThat(throwable).isInstanceOf(UnsupportedCharsetException.class);
        assertThat(throwable)
                .hasMessageContaining(
                        "Generated key ID for AWS Secret Manager can contain only 0-9, a-z, A-Z and /_+=.@- characters");
    }

    @Test
    public void encryptionIsNotUsedWhenSavingToVault() {
        final ArgonOptions argonOptions = mock(ArgonOptions.class);

        awsSecretManagerKeyGenerator.generate("vaultId", argonOptions, null);

        verifyNoMoreInteractions(argonOptions);
    }
}
