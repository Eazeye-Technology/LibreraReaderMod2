//===========================================================================
// Summary:
//		File Util Class
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

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

public class FileUtil {
	
	public static boolean isFileExist(String filePath) {
		if (StringUtil.getInstance().isInvalidStr(filePath)) {
			return false;
		}
		
		File f = new File(filePath);
		return f.exists();
	}
	
	public static boolean isFileParentExist(String filePath) {
		if (StringUtil.getInstance().isInvalidStr(filePath)) {
			return false;
		}
		
		File f = new File(filePath);
		File fParent = f.getParentFile();
		if (fParent == null) {
			return false;
		} else {
			return fParent.exists();
		}
	}
	
	public static boolean isDirectory(String filePath) {
		if (!isFileExist(filePath)) {
			return false;
		}
		
		File f = new File(filePath);
		return f.isDirectory();
	}
	
	public static byte[] readFile(String filePath) {
		try {
			RandomAccessFile raf = new RandomAccessFile(filePath, "r");
			int len = (int) raf.length();
			if (len == 0) {
				return null;
			}
			
			byte[] ret = new byte[len];
			int rc = raf.read(ret);
			if (rc != -1 && rc != len) {
				Log.e("readFile", "IO, program error.");
				return null;
			}
			return ret;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Log.e("readFile", "IO, program error.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String readFile2(String filePath) {
		return convertString4UTF8(readFile(filePath));
	}
	
	public static String convertString(byte[] data, String encoding) {
		if (data == null || data.length <= 0) {
			Log.e("convertString", "IO, program error.");
			return null;
		}
		
		try {
			return new String(data, encoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Log.e("convertString", "UnsupportedEncodingException");
		}
		
		return null;
	}
	
	public static byte[] convertByteArray(String data, String encoding) {
		if (data == null || data.length() <= 0 || encoding == null || encoding.length() <= 0) {
			Log.e("convertByteArray", "IO, program error.");
			return null;
		}
		
		try {
			return data.getBytes(encoding);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Log.e("convertByteArray", "UnsupportedEncodingException");
		}
		
		return null;
	}
	
	public static byte[] convertByteArray4UTF16LE(String data) {
		return convertByteArray(data, "UTF16-LE");
	}
	
	public static byte[] convertByteArray4UTF8(String data) {
		return convertByteArray(data, "UTF8");
	}
	
	public static String convertString4UTF16LE(byte[] data) {
		return convertString(data, "UTF16-LE");
	}
	
	public static String convertString4UTF8(byte[] data) {
		return convertString(data, "UTF8");
	}
	
	public static boolean writeFile(byte[] data, String filePath) {
		if (data == null || data.length <= 0 || filePath == null || filePath.length() < 0) {
			Log.e("writeFile", "IO, program error.");
			return false;
		}
		File f = new File(filePath); 
		try {
			RandomAccessFile raf = new RandomAccessFile(f, "rw");
			raf.write(data);
			raf.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Log.e("writeFile", "FileNotFoundException");
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("writeFile", "IOException");
			return false;
		}
		return true;
	}
	
	public static boolean writeFile2(String data, String filePath) {
		return writeFile(convertByteArray4UTF8(data), filePath);
	}

	public static boolean deleteDirectory(String sPath) {
	    if (!sPath.endsWith(File.separator)) {  
	        sPath = sPath + File.separator;  
	    }  
	    File dirFile = new File(sPath);
	    if (!dirFile.exists() || !dirFile.isDirectory()) {  
	        return false;  
	    }  
	    boolean flag = true;
	    File[] files = dirFile.listFiles();  
	    for (int i = 0; i < files.length; i++) {
	        if (files[i].isFile()) {  
	            flag = deleteFile(files[i].getAbsolutePath());  
	            if (!flag) break;  
	        }
	        else {  
	            flag = deleteDirectory(files[i].getAbsolutePath());  
	            if (!flag) break;  
	        }  
	    }  
	    if (!flag) return false;
	    return true;
	}

	public static boolean deleteFile(String sPath) {  
	    boolean flag = false;  
	    File file = new File(sPath);  
	    if (file.isFile() && file.exists()) {
	        file.delete();  
	        flag = true;  
	    }  
	    return flag;  
	}  

	public static boolean deleteFolder(String sPath) {  
	    boolean flag = false;  
	    File file = new File(sPath);  
	    if (!file.exists()) {
	        return flag;  
	    } else {
	        if (file.isFile()) {
	            return deleteFile(sPath);  
	        } else {
	            return deleteDirectory(sPath);  
	        }  
	    }  
	}
	
	public static String[] listFile(String dirPath) {
		if (FileUtil.isFileExist(dirPath) && FileUtil.isDirectory(dirPath)) {
			File dir = new File(dirPath);
			return dir.list();
		} else {
			return null;
		}
	}
}
