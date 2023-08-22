package com.iconloop.score.bfs;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import score.ArrayDB;
import scorex.util.StringTokenizer;

import java.math.BigInteger;

public class Helper {
    public static String StringListToJsonString(String[] list) {
        if(list == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for(String s : list) {
            if(builder.length() > 0) {
                builder.append(",");
            }
            builder.append("\"").append(s).append("\"");
        }

        return builder.toString();
    }

    public static String[] JsonStringToStringList(String keyName, String jsonString) {
        String json_str = "{\"" + keyName + "\":[" + jsonString + "]}";
        JsonValue jsonValue = Json.parse(json_str);
        JsonObject json = jsonValue.asObject();
        JsonArray jsonArray = json.get(keyName).asArray();

        int index = 0;
        String[] stringList = new String[jsonArray.size()];
        for(JsonValue value:jsonArray){
            stringList[index++] = value.toString().replace("\"", "");
        }

        return stringList;
    }

    public static boolean ArraysEqual(String[] arr1, String[] arr2) {
        // Check if the arrays have the same length
        if (arr1.length != arr2.length) {
            return false;
        }

        // Iterate through the elements of the arrays and check if they are equal
        for (int i = 0; i < arr1.length; i++) {
            if (!arr1[i].equals(arr2[i])) {
                return false;
            }
        }

        return true;
    }

    public static String[] ArrayDBToArray(ArrayDB<String> arrayDB) {
        String[] array = new String[(int) arrayDB.size()];

        for(int i = 0; i < arrayDB.size(); i++) {
            array[i] = arrayDB.get(i);
        }

        return array;
    }

    public static DidMessage DidMessageParser(String message) {
        String[] did_info = new String[4];
        StringTokenizer st = new StringTokenizer(message, "#");
        int countTokens = st.countTokens();

        int index = 0;
        while (st.hasMoreTokens()) {
            did_info[index++] = st.nextToken();
        }

        String did = message;
        String kid = "publicKey";
        String target = "";
        String nonce = "0";

        if (countTokens >= 2) {
            did = did_info[0];
            kid = did_info[1];
        }

        if (countTokens == 4) {
            target = did_info[2];
            nonce = did_info[3];
        }

        return new DidMessage(did, kid, target, new BigInteger(nonce, 10));
    }
}
