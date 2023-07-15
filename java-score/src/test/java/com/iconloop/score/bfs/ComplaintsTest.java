package com.iconloop.score.bfs;

import com.eclipsesource.json.JsonObject;
import org.junit.jupiter.api.Test;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ComplaintsTest {
    @Test
    void parseComplaintsDefault() {
        Complaints complaints = new Complaints("");
        System.out.println(complaints.toString());
        assertEquals(complaints.toString(), "{}");
    }

    @Test
    void parseComplaintsFromString() {
        final JsonObject complaintsJson = new JsonObject();
        complaintsJson.set("peer-1", 1657660001);
        complaintsJson.set("peer-2", 1657660002);
        complaintsJson.set("peer-3", 1657660003);

        System.out.println(complaintsJson.toString());
        Complaints complaints = new Complaints(complaintsJson.toString());

        assertEquals(complaintsJson.toString(), complaints.toString());

        complaints.addComplain("peer-4", 1657660004);
        System.out.println(complaints.toString());

        assertNotEquals(complaintsJson.toString(), complaints.toString());
    }

    @Test
    void isComplained() {
        final JsonObject complaintsJson = new JsonObject();
        complaintsJson.set("peer-1", 1657660001);
        complaintsJson.set("peer-2", 1657660002);
        complaintsJson.set("peer-3", 1657660003);
        Complaints complaints = new Complaints(complaintsJson.toString());

        assertEquals(complaints.isComplained(10), BigInteger.ZERO);
        assertEquals(complaints.isComplained(5), BigInteger.ONE);
    }

    @Test
    void hashMap() {
        final JsonObject complaintsJson = new JsonObject();
        complaintsJson.set("peer-1", 1657660001);
        complaintsJson.set("peer-2", 1657660002);
        complaintsJson.set("peer-3", 1657660003);

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("peer-1", 1657660001);
        map.put("peer-2", 1657660002);
        map.put("peer-3", 1657660003);
        System.out.println(map);

        assertEquals(map.get("peer-1"), 1657660001);
    }

    @Test
    void complaintsToMap() {
        final JsonObject complaintsJson = new JsonObject();
        complaintsJson.set("peer-1", 1657660001);
        complaintsJson.set("peer-2", 1657660002);
        complaintsJson.set("peer-3", 1657660003);

        Complaints complaints = new Complaints(complaintsJson.toString());
        System.out.println(complaints.toMap(complaintsJson.size()));

        assertEquals(complaints.toMap(complaintsJson.size()).get("peer-1"), "1657660001");
    }
}
