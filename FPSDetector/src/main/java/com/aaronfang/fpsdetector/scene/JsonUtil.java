package com.aaronfang.fpsdetector.scene;

import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by AaFaa
 * on 2021/7/13
 * in package com.aaronfang.fpsdetector
 * with project FpsDetector
 */
public class JsonUtil {
    public static JSONObject copyJson(JSONObject param) {
        if (param == null) {
            return null;
        }
        List <String> keyList = new LinkedList <>();
        Iterator <String> keysIterable = param.keys();
        if (keysIterable == null) {
            return null;
        }
        while (keysIterable.hasNext()) {
            keyList.add(keysIterable.next());
        }
        try {
            return new JSONObject(param, keyList.toArray(new String[]{}));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return param;
    }
}
