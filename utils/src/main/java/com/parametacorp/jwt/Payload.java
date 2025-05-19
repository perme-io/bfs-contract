package com.parametacorp.jwt;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class Payload {
    private final JsonObject payload;

    public Payload(JsonObject payload) {
        this.payload = payload;
    }

    public boolean validate(JsonObject actual, long currentHeight) {
        if (actual == null) {
            return false;
        }

        //DEBUG
        System.out.println("currentHeight: " + currentHeight);
        System.out.println("expected: " + payload);
        System.out.println("actual: " + actual);

        if (actual.get("method").asString().equals(payload.get("method").asString())) {
            JsonObject expectedParams = payload.get("params").asObject();
            JsonObject actualParams = actual.get("params").asObject();
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
                    if (key.equals("base_height") &&
                            (actualValue.asLong() < expected.asLong() || currentHeight <= actualValue.asLong())) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return payload.toString();
    }

    public static class Builder {
        private final String method;
        private String cid;
        private String group;
        private int size;
        private String expire_at;

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

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder expire_at(String expire_at) {
            this.expire_at = expire_at;
            return this;
        }

        public Payload build() {
            JsonObject params = Json.object();
            if (cid != null) {
                params.add("cid", Json.value(cid));
            }

            if (group != null) {
                params.add("group", Json.value(group));
            }

            if (size > 0) {
                params.add("size", Json.value(size));
            }

            if (expire_at != null) {
                params.add("expire_at", Json.value(expire_at));
            }

            JsonObject payload = Json.object()
                    .add("method", method)
                    .add("params", params);
            return new Payload(payload);
        }

        private void addIfNotNull(JsonObject params, String name, JsonValue value) {
            if (value != Json.NULL) {
                params.add(name, value);
            }
        }
    }
}
