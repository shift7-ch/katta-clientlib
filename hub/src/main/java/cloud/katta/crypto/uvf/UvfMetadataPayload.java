/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;


import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.cryptomator.random.FastSecureRandomProvider;

import org.apache.commons.lang3.StringUtils;
import org.cryptomator.cryptolib.api.Cryptor;
import org.cryptomator.cryptolib.api.CryptorProvider;
import org.cryptomator.cryptolib.api.DirectoryContentCryptor;
import org.cryptomator.cryptolib.api.DirectoryMetadata;
import org.cryptomator.cryptolib.api.UVFMasterkey;
import org.cryptomator.cryptolib.common.P384KeyPair;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import cloud.katta.crypto.exceptions.NotECKeyException;
import cloud.katta.model.JWEPayload;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObjectJSON;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MultiDecrypter;
import com.nimbusds.jose.crypto.MultiEncrypter;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.gen.OctetSequenceKeyGenerator;

import static cloud.katta.crypto.KeyHelper.decodePrivateKey;

/**
 * Represents payload of <a href="https://github.com/encryption-alliance/unified-vault-format/blob/develop/vault%20metadata/README.md"><code>vault.uvf</code> metadata</a>.
 * Counterpart of <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/universalVaultFormat.ts"><code>MetadataPayload</code></a>.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UvfMetadataPayload extends JWEPayload {

    @JsonProperty(value = "fileFormat", required = true)
    String fileFormat;

    @JsonProperty(value = "nameFormat", required = true)
    String nameFormat;

    @JsonProperty(value = "seeds", required = true)
    Map<String, String> seeds;

    @JsonProperty(value = "initialSeed", required = true)
    String initialSeed;

    @JsonProperty(value = "latestSeed", required = true)
    String latestSeed;

    @JsonProperty(value = "kdf", required = true)
    String kdf;

    @JsonProperty(value = "kdfSalt", required = true)
    String kdfSalt;

    @JsonProperty(value = "org.cryptomator.automaticAccessGrant", required = true)
    VaultMetadataJWEAutomaticAccessGrantDto automaticAccessGrant;

    @JsonProperty(value = "cloud.katta.storage", required = true)
    VaultMetadataJWEBackendDto storage;


    public static UvfMetadataPayload fromJWE(final String jwe) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        return mapper.readValue(jwe, UvfMetadataPayload.class);
    }

    public String toJSON() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        return mapper.writeValueAsString(this);
    }

    public static UvfMetadataPayload create() {
        final String kid = Base64.getUrlEncoder().encodeToString(new AlphanumericRandomStringService(4).random().getBytes(StandardCharsets.UTF_8));
        final byte[] rawSeed = new byte[32];
        FastSecureRandomProvider.get().provide().nextBytes(rawSeed);
        final byte[] kdfSalt = new byte[32];
        FastSecureRandomProvider.get().provide().nextBytes(kdfSalt);
        return new UvfMetadataPayload()
                .withFileFormat("AES-256-GCM-32k")
                .withNameFormat("AES-SIV-512-B64URL")
                .withSeeds(new HashMap<String, String>() {{
                    put(kid, Base64.getUrlEncoder().encodeToString(rawSeed));
                }})
                .withLatestSeed(kid)
                .withinitialSeed(kid)
                .withKdf("HKDF-SHA512")
                .withKdfSalt(Base64.getUrlEncoder().encodeToString(kdfSalt));
    }

    public String computeRootDirIdHash() throws JsonProcessingException {
        final UVFMasterkey masterKey = UVFMasterkey.fromDecryptedPayload(this.toJSON());
        final CryptorProvider provider = CryptorProvider.forScheme(CryptorProvider.Scheme.UVF_DRAFT);
        final Cryptor cryptor = provider.provide(masterKey, FastSecureRandomProvider.get().provide());
        final byte[] rootDirId = masterKey.rootDirId();
        return cryptor.fileNameCryptor(masterKey.firstRevision()).hashDirectoryId(rootDirId);
    }

    public byte[] computeRootDirUvf() throws JsonProcessingException {
        final UVFMasterkey masterKey = UVFMasterkey.fromDecryptedPayload(this.toJSON());
        final CryptorProvider provider = CryptorProvider.forScheme(CryptorProvider.Scheme.UVF_DRAFT);
        final Cryptor cryptor = provider.provide(masterKey, FastSecureRandomProvider.get().provide());
        DirectoryMetadata rootDirMetadata = cryptor.directoryContentCryptor().rootDirectoryMetadata();
        DirectoryContentCryptor dirContentCryptor = cryptor.directoryContentCryptor();
        byte[] rootDirUvfFileContents = dirContentCryptor.encryptDirectoryMetadata(rootDirMetadata);
        return rootDirUvfFileContents;
    }

    public static final class UniversalVaultFormatJWKS {
        private final ECKey recoveryKeyJWK;
        private final P384KeyPair recoveryKey;

        public ECKey recoveryKey() {
            return recoveryKeyJWK;
        }

        public OctetSequenceKey memberKey() {
            return memberKey;
        }

        private final OctetSequenceKey memberKey;

        private UniversalVaultFormatJWKS() throws JOSEException {
            memberKey = new OctetSequenceKeyGenerator(256)
                    .keyID("org.cryptomator.hub.memberkey")
                    .algorithm(JWEAlgorithm.A256KW)
                    .generate();

            recoveryKey = P384KeyPair.generate();

            final ECKey recoveryKeyJWKWithoutThumbprint = new ECKey.Builder(Curve.P_384,
                    recoveryKey.getPublic())
                    .build();

            recoveryKeyJWK = new ECKey.Builder(Curve.P_384,
                    recoveryKey.getPublic())
                    .algorithm(JWEAlgorithm.ECDH_ES_A256KW)
                    .keyID(String.format("org.cryptomator.hub.recoverykey.%s", recoveryKeyJWKWithoutThumbprint.computeThumbprint()))
                    .privateKey(recoveryKey.getPrivate())
                    .build();
        }

        public JWKSet toJWKSet() {
            return new JWKSet(Arrays.asList(memberKey, recoveryKeyJWK));
        }

        public String serializePublicRecoverykey() {
            return new JWKSet(recoveryKeyJWK).toPublicJWKSet().toString();
        }

        public UvfAccessTokenPayload toAccessToken() {
            return new UvfAccessTokenPayload().withKey(Base64.getEncoder().encodeToString(memberKey.toByteArray()));
        }

        public UvfAccessTokenPayload toOwnerAccessToken() {
            return new UvfAccessTokenPayload()
                    .withKey(Base64.getEncoder().encodeToString(memberKey.toByteArray()))
                    .withRecoveryKey(Base64.getEncoder().encodeToString(recoveryKey.getPrivate().getEncoded()));
        }

        public static OctetSequenceKey memberKeyFromRawKey(final byte[] raw) {
            return new OctetSequenceKey.Builder(raw)
                    .keyID("org.cryptomator.hub.memberkey")
                    .algorithm(JWEAlgorithm.A256KW)
                    .build();
        }

        public static ECKey recoveryKeyFromRawKey(final ECKey publicRecoveryKey, final byte[] raw) throws NoSuchAlgorithmException, InvalidKeySpecException, NotECKeyException {
            return new ECKey.Builder(publicRecoveryKey).privateKey(decodePrivateKey(Base64.getEncoder().encodeToString(raw))).build();
        }
    }

    public static UniversalVaultFormatJWKS createKeys() throws JOSEException {
        return new UniversalVaultFormatJWKS();
    }

    public UvfMetadataPayload() {
    }

    public String fileFormat() {
        return fileFormat;
    }

    public UvfMetadataPayload withFileFormat(final String fileFormat) {
        this.fileFormat = fileFormat;
        return this;
    }

    public String nameFormat() {
        return nameFormat;
    }

    public UvfMetadataPayload withNameFormat(final String nameFormat) {
        this.nameFormat = nameFormat;
        return this;
    }

    public Map<String, String> seeds() {
        return seeds;
    }

    public UvfMetadataPayload withSeeds(final Map<String, String> keys) {
        this.seeds = keys;
        return this;
    }

    public String initialSeed() {
        return initialSeed;
    }

    public UvfMetadataPayload withinitialSeed(final String initialSeed) {
        this.initialSeed = initialSeed;
        return this;
    }

    public String latestSeed() {
        return latestSeed;
    }

    public UvfMetadataPayload withLatestSeed(final String latestSeed) {
        this.latestSeed = latestSeed;
        return this;
    }


    public String kdf() {
        return kdf;
    }

    public UvfMetadataPayload withKdf(final String kdf) {
        this.kdf = kdf;
        return this;
    }

    public String kdfSalt() {
        return kdfSalt;
    }

    public UvfMetadataPayload withKdfSalt(final String kdfSalt) {
        this.kdfSalt = kdfSalt;
        return this;
    }

    public VaultMetadataJWEAutomaticAccessGrantDto automaticAccessGrant() {
        return automaticAccessGrant;
    }

    public UvfMetadataPayload withAutomaticAccessGrant(final VaultMetadataJWEAutomaticAccessGrantDto vaultMetadataJWEAutomaticAccessGrantDto) {
        this.automaticAccessGrant = vaultMetadataJWEAutomaticAccessGrantDto;
        return this;
    }

    public VaultMetadataJWEBackendDto storage() {
        return storage;
    }

    public UvfMetadataPayload withStorage(final VaultMetadataJWEBackendDto backend) {
        this.storage = backend;
        return this;
    }

    /**
     * Decrypt for recipient.
     *
     * @param jwe The jwe
     * @param jwk The jwk
     */
    public static UvfMetadataPayload decryptWithJWK(final String jwe, final JWK jwk) throws ParseException, JOSEException, JsonProcessingException {
        final JWEObjectJSON jweObject = JWEObjectJSON.parse(jwe);
        jweObject.decrypt(new MultiDecrypter(jwk));
        final Payload payload = jweObject.getPayload();
        return UvfMetadataPayload.fromJWE(payload.toString());
    }

    /**
     * Encrypt for recipients.
     *
     * @param apiURL  api URL that goes into custom header param "origin"
     * @param vaultId the vault ID that goes into custom header param "origin"
     * @param keys    recipient keys for whom to encrypt
     */
    public String encrypt(final String apiURL, final UUID vaultId, final JWKSet keys) throws JOSEException {
        final JWEObjectJSON builder = new JWEObjectJSON(
                new JWEHeader.Builder(EncryptionMethod.A256GCM)
                        .customParam("origin", String.format("%s/vaults/%s/uvf/vault.uvf", apiURL, vaultId.toString()))
                        .jwkURL(URI.create("jwks.json"))
                        .build(),
                new Payload(new HashMap<String, Object>() {{
                    put("fileFormat", fileFormat);
                    put("nameFormat", nameFormat);
                    put("seeds", seeds);
                    put("initialSeed", initialSeed);
                    put("latestSeed", latestSeed);
                    put("kdf", kdf);
                    put("kdfSalt", kdfSalt);
                    put("org.cryptomator.automaticAccessGrant", automaticAccessGrant);
                    put("cloud.katta.storage", storage);
                }})
        );
        builder.encrypt(new MultiEncrypter(keys));
        return builder.serializeGeneral();
    }

    @Override
    public String toString() {
        return "UvfMetadataPayload{" +
                "fileFormat='" + fileFormat + '\'' +
                ", nameFormat='" + nameFormat + '\'' +
                ", seeds={" + seeds.entrySet().stream().map(e -> e.getKey() + "=" + StringUtils.repeat("*", Integer.min(8, StringUtils.length(e.getValue())))).collect(Collectors.joining(", ")) + "}" +
                ", initialSeed='" + initialSeed + '\'' +
                ", latestSeed='" + latestSeed + '\'' +
                ", kdf='" + kdf + '\'' +
                ", kdfSalt='" + StringUtils.repeat("*", Integer.min(8, StringUtils.length(kdf))) + '\'' +
                ", automaticAccessGrant=" + automaticAccessGrant +
                ", storage=" + storage +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }

        final UvfMetadataPayload that = (UvfMetadataPayload) o;
        return Objects.equals(fileFormat, that.fileFormat) && Objects.equals(nameFormat, that.nameFormat) && Objects.equals(seeds, that.seeds) && Objects.equals(initialSeed, that.initialSeed) && Objects.equals(latestSeed, that.latestSeed) && Objects.equals(kdf, that.kdf) && Objects.equals(kdfSalt, that.kdfSalt) && Objects.equals(automaticAccessGrant, that.automaticAccessGrant) && Objects.equals(storage, that.storage);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(fileFormat);
        result = 31 * result + Objects.hashCode(nameFormat);
        result = 31 * result + Objects.hashCode(seeds);
        result = 31 * result + Objects.hashCode(initialSeed);
        result = 31 * result + Objects.hashCode(latestSeed);
        result = 31 * result + Objects.hashCode(kdf);
        result = 31 * result + Objects.hashCode(kdfSalt);
        result = 31 * result + Objects.hashCode(automaticAccessGrant);
        result = 31 * result + Objects.hashCode(storage);
        return result;
    }
}
