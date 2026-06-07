//===========================================================================
// Summary:
//		String Util Class
// Usage:
//		Null
// Remarks:
//		Null
// Date:
//		2014-08-14
// Author:
//		Liu Xin (liuxin@wafa.com.cn)
//===========================================================================
package com.upgradetool.upgrade;

public class StringUtil {

	private static StringUtil instance;
	
	private StringUtil() {
		
	}
	
	public static StringUtil getInstance() {
		if (instance == null) {
			instance = new StringUtil();
		}
		return instance;
	}

	public boolean isInvalidStr(String str) {
		return str == null || str.trim().length() == 0;
	}

	public String appendBuffer(String...args) {
	     StringBuffer buffer = new StringBuffer();
	     for (String s: args) {
	    	 if (s != null)
	    		 buffer.append(s);
	     }
	     return buffer.toString();
	}

	public String appendBuild(String... args) {
	     StringBuilder builder = new StringBuilder();
	     for (String s: args) {
	    	 if (s != null)
	    		 builder.append(s);
	     }
	     return builder.toString();  
	}
	
	public String toDataBaseValid(String str) {
	     if (isInvalidStr(str)) {
	    	 return "";
	     } else {
	    	 return removeIllegalCharacters(str);
	     }  
	}

	public String toValid(String str) {
	     if (isInvalidStr(str)) {
	    	 return "";
	     } else {
	    	 return str;
	     }  
	}

	public String removeIllegalCharacters(String str) {
		if (str != null && str.length() != 0) {
			str = str.replace("'", "''");
		}
		return str;
	}

}
