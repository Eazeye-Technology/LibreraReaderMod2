package com.upgradetool.upgrade;

import org.json.JSONObject;

public class JSONUtil {
	public static String optString(JSONObject obj, String name) {
		if (obj.isNull(name)) {
			return null;
		} else {
			return obj.optString(name);
		}
	}
	
	//optInt
}

