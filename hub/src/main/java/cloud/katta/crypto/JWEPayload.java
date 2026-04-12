/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.crypto;

import org.openapitools.jackson.nullable.JsonNullableModule;

import java.util.HashMap;
import java.util.Map;

import cloud.katta.client.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class JWEPayload {

    private static final ObjectMapper mapper = new JSON().getMapper();

    static {
        mapper.registerModule(new JsonNullableModule());
    }

    public static <T extends JWEPayload> T fromJSON(final String jwe, final Class<T> valueType) throws JsonProcessingException {
        return mapper.readValue(jwe, valueType);
    }

    public Map<String, Object> toJSONObject() throws JsonProcessingException {
        return mapper.readValue(mapper.writeValueAsString(this), HashMap.class);
    }
}
