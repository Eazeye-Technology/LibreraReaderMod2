package com.upgradetool.upgrade;

public abstract class RecvDataListener {
	public abstract void onRecvData(boolean successStatus, int statusCode, 
		String responseString, Throwable throwable);
	
	public void onProgress(int bytesWritten, int totalSize) {
		
	}
}
