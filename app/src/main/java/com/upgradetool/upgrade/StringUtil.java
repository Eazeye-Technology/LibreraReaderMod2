//===========================================================================
// Summary:
//		字符串工具类。
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

	/**
	 * 是否是无效的字符
	 * @return
	 */
	public boolean isInvalidStr(String str) {
		return str == null || str.trim().length() == 0;
	}

	/**
	   * 顺序的拼接字符串，使用StringBuffer，线程安全
	   * @param args
	   * @return
	   */
	public String appendBuffer(String...args) {
	     StringBuffer buffer = new StringBuffer();
	     for (String s: args) {
	    	 if (s != null)
	    		 buffer.append(s);
	     }
	     return buffer.toString();
	}

	   /**
	   * 顺序的拼接字符串，使用StringBuilder，非线程安全
	   * @param args
	   * @return
	   */
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
	
	 /**
	   * 顺序的拼接字符串，使用StringBuilder，非线程安全
	   * @param args
	   * @return
	   */
	public String toValid(String str) {
	     if (isInvalidStr(str)) {
	    	 return "";
	     } else {
	    	 return str;
	     }  
	}
	
	/**
	 * 对非法字符进行转义，目前只处理了单引号
	 * @param str
	 * @return
	 */
	public String removeIllegalCharacters(String str) {
		if (str != null && str.length() != 0) {
			str = str.replace("'", "''");
		}
		return str;
	}

}
