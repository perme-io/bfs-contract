package com.iconloop.score.bfs;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import score.ArrayDB;

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
}
