/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.model;

import java.util.HashMap;
import java.util.Map;

import cloud.katta.client.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class JWEPayload {

    private static final ObjectMapper objectMapper = new JSON().getMapper();

    public Map<String, Object> toJSONObject() throws JsonProcessingException {
        return objectMapper.readValue(objectMapper.writeValueAsString(this), HashMap.class);
    }
}
