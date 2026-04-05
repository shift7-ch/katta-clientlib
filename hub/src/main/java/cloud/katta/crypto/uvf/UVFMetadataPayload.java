/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto.uvf;


import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.cryptomator.random.FastSecureRandomProvider;

import org.openapitools.jackson.nullable.JsonNullableModule;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import cloud.katta.crypto.JWEPayload;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObjectJSON;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MultiDecrypter;
import com.nimbusds.jose.crypto.MultiEncrypter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

/**
 * Represents payload of <a href="https://github.com/encryption-alliance/unified-vault-format/blob/develop/vault%20metadata/README.md"><code>vault.uvf</code> metadata</a>.
 * Counterpart of <a href="https://github.com/shift7-ch/katta-server/blob/feature/cipherduck-uvf/frontend/src/common/universalVaultFormat.ts"><code>MetadataPayload</code></a>.
 * <p>
 * It has two custom fields:
 * <ul>
 *  <li>org.cryptomator.automaticAccessGrant (upstream)</li>
 *  <li>cloud.katta.storage</li>
 * </ul>
 * It has at two recipients:
 * <ul>
 *    <li>org.cryptomator.hub.memberkey shared with vault members (having access to the member key)</li>
 *    <li>org.cryptomator.hub.recoverykey. shared with vault owners (having access to the recovery key)</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UVFMetadataPayload extends JWEPayload {
    private static final String UVF_SPEC_VERSION_KEY_PARAM = "uvf.spec.version";

    private static final String UVF_FILEFORMAT = "AES-256-GCM-32k";
    private static final String UVF_NAME_FORMAT = "AES-SIV-512-B64URL";

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
    VaultMetadataAutomaticAccessGrantDto automaticAccessGrant;

    @JsonProperty(value = "cloud.katta.storage", required = true)
    VaultMetadataStorageDto storage;

    public UVFMetadataPayload() {
    }

    /**
     * Creates a new instance with initialized values. Initially, there is one seed in the map identified by its KID
     * and the initial and latest seed is set to this single seed in the map via its KID
     */
    public static UVFMetadataPayload create() {
        final String kid = Base64.getUrlEncoder().encodeToString(new AlphanumericRandomStringService(4).random().getBytes(StandardCharsets.UTF_8));
        final byte[] rawSeed = new byte[32];
        FastSecureRandomProvider.get().provide().nextBytes(rawSeed);
        final byte[] kdfSalt = new byte[32];
        FastSecureRandomProvider.get().provide().nextBytes(kdfSalt);
        return new UVFMetadataPayload()
                .withFileFormat(UVF_FILEFORMAT)
                .withNameFormat(UVF_NAME_FORMAT)
                .withSeeds(new HashMap<String, String>() {{
                    put(kid, Base64.getUrlEncoder().encodeToString(rawSeed));
                }})
                .withLatestSeed(kid)
                .withinitialSeed(kid)
                .withKdf("HKDF-SHA512")
                .withKdfSalt(Base64.getUrlEncoder().encodeToString(kdfSalt));
    }

    public String fileFormat() {
        return fileFormat;
    }

    public UVFMetadataPayload withFileFormat(final String fileFormat) {
        this.fileFormat = fileFormat;
        return this;
    }

    public String nameFormat() {
        return nameFormat;
    }

    public UVFMetadataPayload withNameFormat(final String nameFormat) {
        this.nameFormat = nameFormat;
        return this;
    }

    public Map<String, String> seeds() {
        return seeds;
    }

    public UVFMetadataPayload withSeeds(final Map<String, String> keys) {
        this.seeds = keys;
        return this;
    }

    public String initialSeed() {
        return initialSeed;
    }

    public UVFMetadataPayload withinitialSeed(final String initialSeed) {
        this.initialSeed = initialSeed;
        return this;
    }

    public String latestSeed() {
        return latestSeed;
    }

    public UVFMetadataPayload withLatestSeed(final String latestSeed) {
        this.latestSeed = latestSeed;
        return this;
    }


    public String kdf() {
        return kdf;
    }

    public UVFMetadataPayload withKdf(final String kdf) {
        this.kdf = kdf;
        return this;
    }

    public String kdfSalt() {
        return kdfSalt;
    }

    public UVFMetadataPayload withKdfSalt(final String kdfSalt) {
        this.kdfSalt = kdfSalt;
        return this;
    }

    public VaultMetadataAutomaticAccessGrantDto automaticAccessGrant() {
        return automaticAccessGrant;
    }

    public UVFMetadataPayload withAutomaticAccessGrant(final VaultMetadataAutomaticAccessGrantDto vaultMetadataJWEAutomaticAccessGrantDto) {
        this.automaticAccessGrant = vaultMetadataJWEAutomaticAccessGrantDto;
        return this;
    }

    public VaultMetadataStorageDto storage() {
        return storage;
    }

    public UVFMetadataPayload withStorage(final VaultMetadataStorageDto backend) {
        this.storage = backend;
        return this;
    }

    /**
     * Decrypt for recipient.
     *
     * @param jwe The jwe
     * @param jwk The jwk
     */
    public static UVFMetadataPayload decrypt(final String jwe, final JWK jwk) throws SecurityFailure {
        try {
            final JWEObjectJSON jweObject = JWEObjectJSON.parse(jwe);
            // https://datatracker.ietf.org/doc/html/rfc7515#section-4.1.11
            // Recipients MAY consider the JWS to be invalid if the critical
            // list contains any Header Parameter names defined by this
            // specification or [JWA] for use with JWS or if any other constraints on its use are violated.
            final Object uvfSpecVersion = jweObject.getHeader().getCustomParams().get(UVF_SPEC_VERSION_KEY_PARAM);
            if(null == uvfSpecVersion || !uvfSpecVersion.equals(1L)) {
                throw new SecurityFailure(String.format("Unexpected value for critical header %s: found %s, expected \"1\"", UVF_SPEC_VERSION_KEY_PARAM, uvfSpecVersion));
            }
            jweObject.decrypt(new MultiDecrypter(jwk, Collections.singleton(UVF_SPEC_VERSION_KEY_PARAM)));
            final Payload payload = jweObject.getPayload();
            final ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JsonNullableModule());
            return mapper.readValue(payload.toString(), UVFMetadataPayload.class);
        }
        catch(ParseException | JOSEException | JsonProcessingException e) {
            throw new SecurityFailure(e);
        }
    }

    public String encrypt(final String apiURL, final UUID vaultId, final JWKSet keys) throws JOSEException {
        final JWEObjectJSON json = this.toJSON(apiURL, vaultId);
        json.encrypt(new MultiEncrypter(keys));
        return json.serializeGeneral();
    }

    /**
     *
     * @param apiURL  api URL that goes into custom header param "cloud.katta.origin"
     * @param vaultId the vault ID that goes into custom header param "cloud.katta.origin"
     * @see <a href="https://github.com/encryption-alliance/unified-vault-format/tree/develop/vault%20metadata#jose-header">UVF Specification of JWE Header</a>
     */
    public JWEObjectJSON toJSON(final String apiURL, final UUID vaultId) {
        final JWEHeader header = new JWEHeader.Builder(EncryptionMethod.A256GCM)
                // kid goes into recipient-specific header
                .customParam("cloud.katta.origin", URI.create(String.format("%s/vaults/%s/uvf/vault.uvf", apiURL, vaultId.toString())).normalize().toString())
                .jwkURL(URI.create("jwks.json"))
                .contentType("json")
                .criticalParams(Collections.singleton(UVF_SPEC_VERSION_KEY_PARAM))
                .customParam(UVF_SPEC_VERSION_KEY_PARAM, 1)
                .build();
        final Payload payload = new Payload(new HashMap<String, Object>() {{
            put("fileFormat", fileFormat);
            put("nameFormat", nameFormat);
            put("seeds", seeds);
            put("initialSeed", initialSeed);
            put("latestSeed", latestSeed);
            put("kdf", kdf);
            put("kdfSalt", kdfSalt);
            put("org.cryptomator.automaticAccessGrant", automaticAccessGrant);
            put("cloud.katta.storage", storage);
        }});
        return new JWEObjectJSON(header, payload);
    }

    public String toJSON() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JsonNullableModule());
        return mapper.writeValueAsString(this);
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }

        final UVFMetadataPayload that = (UVFMetadataPayload) o;
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
