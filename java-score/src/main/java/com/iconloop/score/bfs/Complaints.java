package com.iconloop.score.bfs;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;


public class Complaints {
    private final JsonObject json_obj;

    public Complaints(String complaints) {
        if (complaints.isEmpty()) {
            complaints = "{}";
        }
        JsonValue jsonValue = Json.parse(complaints);
        this.json_obj = jsonValue.asObject();
    }

    public void addComplain(String complainFrom, long timestamp) {
        this.json_obj.set(complainFrom, timestamp);
    }

    public BigInteger isComplained(int nodeCount) {
        // TODO Complained 기준을 설정으로 변경할 것, 현재값은 2로 임시 고정
        // (complainedQuorumFactor * complaints > nodeCount) is complained
        int complainedQuorumFactor = 2;
        int complaintCount = this.json_obj.size();
        return BigInteger.valueOf((complainedQuorumFactor * complaintCount > nodeCount) ? 1 : 0);
    }

    public String toString() {
        return this.json_obj.toString();
    }

    public Map<String, Object> toMap() {
        int nodeCount = this.json_obj.names().size();
        Map<String, Object> map = new HashMap<String, Object>(nodeCount);

        for (String key : this.json_obj.names()) {
            map.put(key, this.json_obj.get(key).toString());
        }

        return map;
    }
}
