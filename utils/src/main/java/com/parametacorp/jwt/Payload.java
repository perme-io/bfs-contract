package com.parametacorp.jwt;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.math.BigInteger;

public class Payload {
    static final String KEY_METHOD = "method";
    static final String KEY_PARAM = "param";

    private final JsonObject payload;

    public Payload(JsonObject payload) {
        this.payload = payload;
    }

    public boolean validate(JsonObject actual, long currentHeight) {
        if (actual == null) {
            return false;
        }
        if (actual.get(KEY_METHOD).asString().equals(payload.get(KEY_METHOD).asString())) {
            JsonObject expectedParams = payload.get(KEY_PARAM).asObject();
            JsonObject actualParams = actual.get(KEY_PARAM).asObject();
            if (expectedParams.size() != actualParams.size()) {
                return false;
            }
            for (String key : expectedParams.names()) {
                if (!actualParams.contains(key)) {
                    return false;
                }
                JsonValue expected = expectedParams.get(key);
                JsonValue actualValue = actualParams.get(key);
                if (expected.isString()) {
                    if (!actualValue.isString() || !expected.asString().equals(actualValue.asString())) {
                        return false;
                    }
                } else if (expected.isNumber()) {
                    if (!actualValue.isNumber()) {
                        return false;
                    }
                    if (key.equals("base_height")) {
                        if (actualValue.asLong() < expected.asLong() || currentHeight <= actualValue.asLong()) {
                            return false;
                        }
                    } else if (actualValue.asLong() !=  expected.asLong()) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return payload.toString();
    }

    public static class Builder {
        private final String method;
        private String cid;
        private String group;
        private BigInteger size;
        private BigInteger expire_at;
        private long baseHeight;

        public Builder(String method) {
            this.method = method;
        }

        public Builder cid(String cid) {
            this.cid = cid;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder size(BigInteger size) {
            this.size = size;
            return this;
        }

        public Builder expire_at(BigInteger expire_at) {
            this.expire_at = expire_at;
            return this;
        }

        public Builder baseHeight(long height) {
            this.baseHeight = height;
            return this;
        }

        public Payload build() {
            JsonObject params = Json.object();
            if (cid != null) {
                params.add("cid", Json.value(cid));
            }

            if (method.equals("update_group") && group != null){
                params.add("group", Json.value(group));
            }

            if (size != null) {
                params.add("size", Json.value(size.toString(16)));
            }

            if (expire_at != null) {
                params.add("expire_at", Json.value(expire_at.toString(16)));
            }

            if (baseHeight > 0) {
                params.add("base_height", Json.value(baseHeight));
            }

            JsonObject payload = Json.object()
                    .add(KEY_METHOD, method)
                    .add(KEY_PARAM, params);
            return new Payload(payload);
        }

        private void addIfNotNull(JsonObject params, String name, JsonValue value) {
            if (value != Json.NULL) {
                params.add(name, value);
            }
        }
    }
}
