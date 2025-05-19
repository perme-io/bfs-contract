package com.iconloop.score.bfs;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
    @Test
    void parseJson() {
        String json_str = "{\"user_allocations\":[\"1.1.1.1\",\"2.2.9.2\",\"3.4.4.4\"]}";
        JsonValue jsonValue = Json.parse(json_str);
        JsonObject json = jsonValue.asObject();
        System.out.println(json.get("user_allocations").toString());

        JsonArray json_array = json.get("user_allocations").asArray();
        System.out.println(json.toString());
        System.out.println(json_array.toString());

        int index = 0;
        String[] list = new String[json_array.size()];
        for(JsonValue value:json_array){
            list[index++] = value.toString().replace("\"", "");
        }

        System.out.println(list[0]);
    }

    @Test
    void icxInBigInteger() {
        BigInteger ONE_ICX = new BigInteger("1000000000000000000");
        System.out.println("0x" + ONE_ICX.toString(16));
        assertEquals("0xde0b6b3a7640000", "0x" + ONE_ICX.toString(16));
    }

    @Test
    void loopBreak() {
        int length = 5;
        int breakLine = 3;
        int index = 0;
        int count = 1;

        while (index < length) {
            System.out.println(index);
            index++;
            if (count++ >= breakLine) {
                break;
            }
        }

        assertEquals(index, breakLine);
    }
}
