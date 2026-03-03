//===========================================================================
// Summary:
//		文件工具类。
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
	
//	public static void saveHttpJson(Context context, String dest, String response) {
//		String jsonDir = FilePathManager.getFilePathManager(context).getJsonDirPath();
//		String destPath = jsonDir + File.separator + dest + ".txt";
//		writeFile2(response, destPath);
//	}
//	
//	public static String getHttpJson(Context context, String dest) {
//		String jsonDir = FilePathManager.getFilePathManager(context).getJsonDirPath();
//		String destPath = jsonDir + File.separator + dest + ".txt";
//		return readFile2(destPath);
//	}
	
	/** 
	 * 删除目录（文件夹）以及目录下的文件 
	 * @param   sPath 被删除目录的文件路径 
	 * @return  目录删除成功返回true，否则返回false 
	 */  
	public static boolean deleteDirectory(String sPath) {  
	    //如果sPath不以文件分隔符结尾，自动添加文件分隔符  
	    if (!sPath.endsWith(File.separator)) {  
	        sPath = sPath + File.separator;  
	    }  
	    File dirFile = new File(sPath);  
	    //如果dir对应的文件不存在，或者不是一个目录，则退出  
	    if (!dirFile.exists() || !dirFile.isDirectory()) {  
	        return false;  
	    }  
	    boolean flag = true;  
	    //删除文件夹下的所有文件(包括子目录)  
	    File[] files = dirFile.listFiles();  
	    for (int i = 0; i < files.length; i++) {  
	        //删除子文件  
	        if (files[i].isFile()) {  
	            flag = deleteFile(files[i].getAbsolutePath());  
	            if (!flag) break;  
	        } //删除子目录  
	        else {  
	            flag = deleteDirectory(files[i].getAbsolutePath());  
	            if (!flag) break;  
	        }  
	    }  
	    if (!flag) return false;  
	    //删除当前目录
	    //FIXME:不删除目录本身，只删除文件
	    /*
	    if (dirFile.delete()) {  
	        return true;  
	    } else {  
	        return false;  
	    } 
	    */
	    return true;
	}
	
	/** 
	 * 删除单个文件 
	 * @param   sPath    被删除文件的文件名 
	 * @return 单个文件删除成功返回true，否则返回false 
	 */  
	public static boolean deleteFile(String sPath) {  
	    boolean flag = false;  
	    File file = new File(sPath);  
	    // 路径为文件且不为空则进行删除  
	    if (file.isFile() && file.exists()) {  
	        file.delete();  
	        flag = true;  
	    }  
	    return flag;  
	}  
	
	/** 
	 *  根据路径删除指定的目录或文件，无论存在与否 
	 *@param sPath  要删除的目录或文件 
	 *@return 删除成功返回 true，否则返回 false。 
	 */  
	public static boolean deleteFolder(String sPath) {  
	    boolean flag = false;  
	    File file = new File(sPath);  
	    // 判断目录或文件是否存在  
	    if (!file.exists()) {  // 不存在返回 false  
	        return flag;  
	    } else {  
	        // 判断是否为文件  
	        if (file.isFile()) {  // 为文件时调用删除文件方法  
	            return deleteFile(sPath);  
	        } else {  // 为目录时调用删除目录方法  
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
	
//	public static List<Document_Info> list(String dirPath) {
//		String[] files = listFile(dirPath);
//		String tmp = "";
//		int index = -1;
//		final String ext = ".cebx";
//		if (files == null) {
//			return null;
//		} else {
//			ArrayList<Document_Info> list = new ArrayList<Document_Info>();
//			for (String e : files) {
//				if (StringUtil.getInstance().isInvalidStr(e) || !e.contains(ext)) {
//					continue;
//				} else {
//					index = e.indexOf(ext);
//					tmp = e.substring(0, index);
//					Document_Info d = new Document_Info();
//					d.doucmentName = tmp;
//					d.doucmentPath = dirPath + File.separator + e;
//					list.add(d);
//				}
//			}
//			return list;
//		}
//	}

}
