package com.iconloop.score.bfs;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.math.BigInteger;
import java.util.Map;


public class PinInfo {
    private final String cid;
    private final JsonObject jsonObject;

    public PinInfo(String cid) {
        this.cid = cid;
        this.jsonObject = new JsonObject();
    }

    public String getString(String keyName) {
        return this.jsonObject.getString(keyName, "");
    }

    public int getInt(String keyName) {
        return this.jsonObject.getInt(keyName, 0);
    }

    public JsonObject getJsonObject() { return this.jsonObject; }

    public void fromParams(String tracker,
                           String name,
                           String comment,
                           String created,
                           String owner,
                           BigInteger replication_min,
                           BigInteger replication_max,
                           String[] user_allocations,
                           BigInteger shard_size,
                           String expire_at,
                           String expire_in,
                           BigInteger state) {
        String userAllocations = Helper.StringListToJsonString(user_allocations);
        this.jsonObject.set("cid", this.cid);
        this.jsonObject.set("tracker", (tracker == null) ? this.jsonObject.getString("tracker", "") : tracker);
        this.jsonObject.set("name", (name == null) ? this.jsonObject.getString("name", "") : name);
        this.jsonObject.set("comment", (comment == null) ? this.jsonObject.getString("comment", "") : comment);
        this.jsonObject.set("created", (created.isEmpty()) ? this.jsonObject.getString("created", "") : created);
        this.jsonObject.set("owner", (owner == null) ? this.jsonObject.getString("owner", "") : owner);
        this.jsonObject.set("replication_min", (replication_min.intValue() == 0) ? this.jsonObject.getInt("replication_min", 0) : replication_min.intValue());
        this.jsonObject.set("replication_max", (replication_max.intValue() == 0) ? this.jsonObject.getInt("replication_max", 0) : replication_max.intValue());
        this.jsonObject.set("user_allocations", (user_allocations == null) ? this.jsonObject.getString("user_allocations", "") : userAllocations);
        this.jsonObject.set("shard_size", (shard_size == null) ? this.jsonObject.getInt("shard_size", 0) : shard_size.intValue());
        this.jsonObject.set("expire_at", (expire_at == null) ? this.jsonObject.getString("expire_at", "") : expire_at);
        this.jsonObject.set("expire_in", (expire_in == null) ? this.jsonObject.getString("expire_in", "") : expire_in);
        this.jsonObject.set("state", (state.intValue() == 0) ? this.jsonObject.getInt("state", 1) : state.intValue());
    }

    public static PinInfo fromString(String pin_info) {
        JsonValue jsonValue = Json.parse(pin_info);
        JsonObject json = jsonValue.asObject();
        String cid = json.getString("cid", "");

        PinInfo pinInfo = new PinInfo(cid);
        JsonObject jsonObject = pinInfo.getJsonObject();

        jsonObject.set("cid", cid);
        jsonObject.set("tracker", json.getString("tracker", jsonObject.getString("tracker", "")));
        jsonObject.set("name", json.getString("name", jsonObject.getString("name", "")));
        jsonObject.set("comment", json.getString("comment", jsonObject.getString("comment", "")));
        jsonObject.set("created", json.getString("created", jsonObject.getString("created", "")));
        jsonObject.set("owner", json.getString("owner", jsonObject.getString("owner", "")));
        jsonObject.set("replication_min", json.getInt("replication_min", jsonObject.getInt("replication_min", 0)));
        jsonObject.set("replication_max", json.getInt("replication_max", jsonObject.getInt("replication_max", 0)));
        jsonObject.set("user_allocations", json.getString("user_allocations", jsonObject.getString("user_allocations", "")));
        jsonObject.set("shard_size", json.getInt("shard_size", jsonObject.getInt("shard_size", 0)));
        jsonObject.set("expire_at", json.getString("expire_at", jsonObject.getString("expire_at", "")));
        jsonObject.set("expire_in", json.getString("expire_in", jsonObject.getString("expire_in", "")));
        jsonObject.set("state", json.getInt("state", jsonObject.getInt("state", 1)));

        return pinInfo;
    }

    public void unpin() {
        this.jsonObject.set("state", 0);
    }

    public String toString() {
        return this.jsonObject.toString();
    }

    public String[] userAllocations() {
        String userAllocations = this.jsonObject.getString("user_allocations", "");
        return Helper.JsonStringToStringList("user_allocations", userAllocations);
    }

    public void reallocation(String[] allocations) {
        String userAllocations = Helper.StringListToJsonString(allocations);
        this.jsonObject.set("user_allocations", userAllocations);
    }

    public Map<String, Object> toMap() {
        return Map.ofEntries(
                Map.entry("cid", this.jsonObject.getString("cid", "")),
                Map.entry("tracker", this.jsonObject.getString("tracker", "")),
                Map.entry("name", this.jsonObject.getString("name", "")),
                Map.entry("comment", this.jsonObject.getString("comment", "")),
                Map.entry("created", this.jsonObject.getString("created", "")),
                Map.entry("owner", this.jsonObject.getString("owner", "")),
                Map.entry("replication_min", this.jsonObject.getInt("replication_min", 0)),
                Map.entry("replication_max", this.jsonObject.getInt("replication_max", 0)),
                Map.entry("user_allocations", userAllocations()),
                Map.entry("shard_size", this.jsonObject.getInt("shard_size", 0)),
                Map.entry("expire_at", this.jsonObject.getString("expire_at", "")),
                Map.entry("expire_in", this.jsonObject.getString("expire_in", "")),
                Map.entry("state", this.jsonObject.getInt("state", 1))
        );
    }
}
