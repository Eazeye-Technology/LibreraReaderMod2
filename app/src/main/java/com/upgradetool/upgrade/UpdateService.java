package com.upgradetool.upgrade;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.foobnix.ui2.MainTabs2;
import com.txkj.readingapp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * see https://github.com/shunliz/AndroidCommonClient/blob/master/XUE8/src/com/zsl/services/UpdateService.java
 * see https://github.com/sonyi/androidClass/blob/master/MusicPlayer/MyMusicPlayer/src/com/mymusicplay/notification/MyNotification.java
 * @author Administrator
 *
 */
public class UpdateService extends Service {
	public static final int UPDATE_SERVICE_ID = 10001;

	//FIXME:
	public static final String DOWNLOAD_URL = 
			"http://101.101.106.18:8080/mt/upload/201508/1438735415672.apk";
			//"http://101.101.106.18/test.apk";
	
	public static final String RENAME_SUFFIX = ".rename";
	public static final String EXTRA_APP_NAME = "EXTRA_APP_NAME"; //下载标题名
	public static final String EXTRA_DOWNLOAD_URL = "EXTRA_DOWNLOAD_URL";
	public static final String EXTRA_DOWNLOAD_PATH = "EXTRA_DOWNLOAD_PATH";
	private static final boolean KEEP_NOTIFICATION = false;
	
	private static final AtomicInteger id = new AtomicInteger(0);
	private static final Map<Integer, String> idMap = new HashMap<Integer, String>();
	private static final Object idMapLock = new Object();
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			createNotification(intent, flags, startId);
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	public void createNotification(Intent intent, int flags, int startId) {
		String url = intent.getStringExtra(EXTRA_DOWNLOAD_URL);
		if (url == null || url.length() == 0) {
			toast("The link is empty");
			return;
		}
		synchronized (idMapLock) {
			if (idMap.containsValue(url)) {
				toast("Download not completed");
			}
			if (idMap.size() > 1) {
				toast("Only one file can be downloaded simultaneously");
			}
		}
		new DownloadTask(intent, flags, startId).execute();
	}
	
	private class DownloadTask extends AsyncTask<String, String, String> {
	    private File mUpdateFile, mRenameFile;
	    private RemoteViews mRemoteViews;
	    private int mNotificationId;
	    private String mAppName;
		private NotificationManager mNotificationManager;
		private Notification mNotification;
		private Intent mUpdateIntent;
		private PendingIntent mPendingIntent;
		private String mDownUrl;
		private String mDownPath;
		
		private static final int TIMEOUT = 10 * 1000;
		private static final String DOWN_OK = "DOWN_OK";
		private static final String DOWN_ERROR = "DOWN_ERROR";
		private static final String DOWN_ING = "DOWN_ING";
		
	    public DownloadTask(Intent intent, int flags, int startId) {
			mAppName = intent.getStringExtra(EXTRA_APP_NAME);
			mDownUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL);
			mDownPath = intent.getStringExtra(EXTRA_DOWNLOAD_PATH);
			if (mDownPath == null) {
				mDownPath = "" + RENAME_SUFFIX;
			} else {
				mDownPath = mDownPath + RENAME_SUFFIX;
			}
			mUpdateFile = new File(mDownPath);
			mRenameFile = new File(mDownPath.replace(RENAME_SUFFIX, ""));
			
			mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotification = new Notification();

			mRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_item);
			mRemoteViews.setTextViewText(R.id.notificationTitle, "Upgrading...");
			mRemoteViews.setTextViewText(R.id.notificationPercent, "0%");
			mRemoteViews.setProgressBar(R.id.notificationProgress, 100, 0, false);
			
			mNotification.icon = android.R.drawable.stat_sys_download;//R.drawable.ic_launcher;
			mNotification.when = System.currentTimeMillis();
			mNotification.contentView = mRemoteViews;

			mUpdateIntent = new Intent(UpdateService.this, MainTabs2.class/*LoginActivity.class*/);
			mUpdateIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				mPendingIntent = PendingIntent.getActivity(UpdateService.this, 0, mUpdateIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
			} else {
				mPendingIntent = PendingIntent.getActivity(UpdateService.this, 0, mUpdateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			}
			mNotification.contentIntent = mPendingIntent;

			mNotificationId = id.getAndIncrement();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//				if (isForground) {
				//				return;
				//			}
				//			isForground = true;
							/*
							首先需要一个NotificationManager来对通知进行管理
							调用Context的getSystemService()方法获取到。
							getSystemService()方法接受的一个字符串参数用于确定系统的的哪一个服务。
							 */
				NotificationManager notificationManager =
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
							/*
							调用NotificationChannel创建通知渠道实例
							并为它设置属性
							 */
				//通知渠道的ID
				String id = "channel_01222";
				try {
					notificationManager.deleteNotificationChannel(id);
				} catch (Exception e){
					//https://stackoverflow.com/questions/68398557/android-10-java-lang-securityexception-not-allowed-to-delete-channel-mynotifica
					//20220317: java.lang.SecurityException: Not allowed to delete channel channel_01222 with a foreground service
					//not here
					e.printStackTrace();
				}
				//用户可以看到的通知渠道的名字
				CharSequence name = getString(R.string.app_name);
				//用户可看到的通知描述
				String description = getString(R.string.app_name);
				//构建NotificationChannel实例
				NotificationChannel notificationChannel =
						new NotificationChannel(id,name,NotificationManager.IMPORTANCE_MIN);//NotificationManager.IMPORTANCE_HIGH);
				//配置通知渠道的属性
				notificationChannel.setDescription(description);
				//设置通知出现时的闪光灯
				notificationChannel.enableLights(false);
				//notificationChannel.enableLights(true);
				//notificationChannel.setLightColor(Color.RED);
				//设置通知出现时的震动
				notificationChannel.enableVibration(true);
				//notificationChannel.enableVibration(true);
				//notificationChannel.setVibrationPattern(new long[]{100,200,300,400,500,400,300,200,100});
				notificationChannel.setVibrationPattern(new long[]{0});
				notificationChannel.setSound(null, null);
				// 是否绕过请勿打扰模式
				notificationChannel.canBypassDnd();
				// 设置绕过请勿打扰模式
				notificationChannel.setBypassDnd(true);
				//https://blog.csdn.net/OxuanO/article/details/86165497
				//在notificationManager中创建通知渠道
				notificationManager.createNotificationChannel(notificationChannel);

				Notification notification = new NotificationCompat.Builder(UpdateService.this, id)
						.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
						//指定通知的标题内容
						.setContentTitle(getText(R.string.app_name))
						//设置通知的内容
						.setContentText("Upgrading...")
						//指定通知被创建的时间
						.setWhen(System.currentTimeMillis())
						//设置通知的小图标
						.setSmallIcon(R.mipmap.ic_launcher_bookmanager2)
						//设置通知的大图标
						//.setLargeIcon(BitmapFactory.decodeResource(getResources(),
						//		R.drawable.ic_launcher_background))
						//添加点击跳转通知跳转
						//.setContentIntent(pendingIntent)
						//实现点击跳转后关闭通知
						.setAutoCancel(true)
						.setSound(null)
						.setVibrate(new long[]{0})
						.setOnlyAlertOnce(true)
						.build();
				//https://blog.csdn.net/qq_36607515/article/details/81393794
				//https://blog.csdn.net/a2241076850/article/details/75668457

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(UPDATE_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
                } else {
                    startForeground(UPDATE_SERVICE_ID, notification);
                }

			} else {
				mNotificationManager.notify(mNotificationId, mNotification);
			}
	    }

	    
	    
	    @Override
		protected void onPreExecute() {
			super.onPreExecute();
			synchronized (idMapLock) {
				idMap.put(Integer.valueOf(mNotificationId), mDownUrl);
			}
		}

		@Override
	    protected String doInBackground(String... params) {
			long downloadSize;
			try {
				refreshUpgrade(0, 100, UpgradeUtil.UPGRADE_STATUS_OK);
				
				downloadSize = downloadUpdateFile(mDownUrl, mUpdateFile.toString());
				
				//重命名
		        boolean success = mUpdateFile.renameTo(mRenameFile);
		        if (success != true) {
		        	toast("重命名失败：" + mDownPath);
		        } else {
			        if (downloadSize > 0) {
						this.publishProgress(DOWN_OK);
						refreshUpgrade(100, 100, UpgradeUtil.UPGRADE_STATUS_OK);
					} else {
						refreshUpgrade(100, 100, UpgradeUtil.UPGRADE_STATUS_ERROR);
					}
		        }
			} catch (Exception e) {
				e.printStackTrace();
				this.publishProgress(DOWN_ERROR);
				refreshUpgrade(100, 100, UpgradeUtil.UPGRADE_STATUS_ERROR);
			}
			return null;
	    }

		@Override
		protected void onProgressUpdate(String... values) {
	    	if (values.length > 0) {
	    		String what = values[0];
	    		if (what != null) {
	    			if (DOWN_OK.equals(what)) {
						Uri uri = UriUtil.fromFile(UpdateService.this, mRenameFile);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							UpdateService.this.stopForeground(true);
						} else {
							if (KEEP_NOTIFICATION) {
								Intent intent = new Intent(Intent.ACTION_VIEW);
								intent.setDataAndType(uri, "application/vnd.android.package-archive");
								UriUtil.prepare(intent);
								mPendingIntent = PendingIntent.getActivity(UpdateService.this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

								//							mNotification.icon = android.R.drawable.stat_sys_download_done;
								//							mNotification.flags = Notification.FLAG_AUTO_CANCEL;
								//							mNotification.setLatestEventInfo(UpdateService.this, mAppName, "下载成功", mPendingIntent);
								Notification.Builder builder = new Notification.Builder(UpdateService.this)
										.setAutoCancel(true)
										.setContentTitle(mAppName)
										.setContentText("Download successful")
										.setContentIntent(mPendingIntent)
										.setSmallIcon(android.R.drawable.stat_sys_download_done)
										.setWhen(System.currentTimeMillis())
										.setOngoing(true);
								mNotification = builder.getNotification();

								mNotificationManager.notify(mNotificationId, mNotification);
							} else {
								mNotificationManager.cancel(mNotificationId);
							}
						}
	    				stopService(mUpdateIntent);

	    				if (false) {
							Intent intent2 = new Intent(Intent.ACTION_VIEW);
							intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							intent2.setDataAndType(uri, "application/vnd.android.package-archive");
							startActivity(intent2);
						} else {
							sendBeginInstall(mRenameFile.getAbsolutePath());
						}
	    			} else if (DOWN_ERROR.equals(what)) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							UpdateService.this.stopForeground(true);
							toast("Download failed");
						} else {
							if (KEEP_NOTIFICATION) {
								//		    				mNotification.icon = android.R.drawable.stat_sys_download_done;
								//		    				mNotification.flags = Notification.FLAG_AUTO_CANCEL;
								//		    				mNotification.setLatestEventInfo(UpdateService.this, mAppName, "下载失败", mPendingIntent);
								Notification.Builder builder = new Notification.Builder(UpdateService.this)
										.setAutoCancel(true)
										.setContentTitle(mAppName)
										.setContentText("Download failed")
										.setContentIntent(mPendingIntent)
										.setSmallIcon(android.R.drawable.stat_sys_download_done)
										.setWhen(System.currentTimeMillis())
										.setOngoing(true);
								mNotification = builder.getNotification();

								mNotificationManager.notify(mNotificationId, mNotification);
							} else {
								mNotificationManager.cancel(mNotificationId);
								toast("Download failed");
							}
						}
	    				stopService(mUpdateIntent);
	    			} else if (DOWN_ING.equals(what)) {
	    				int updateCount = 0;
	    				try {
	    					updateCount = Integer.parseInt(values[1]);
	    				} catch (NumberFormatException e) {
	    					updateCount = 0;
	    				}
	    				refreshUpgrade(updateCount, 100, UpgradeUtil.UPGRADE_STATUS_OK);
						this.mRemoteViews.setTextViewText(R.id.notificationPercent, updateCount + "%");
						this.mRemoteViews.setProgressBar(R.id.notificationProgress, 100, updateCount, false);
						mNotificationManager.notify(mNotificationId, mNotification);
	    			}
	    		}
	    	}
	    }
	    
	    protected void onPostExecute(String result) {
	    	synchronized (idMapLock) {
	    		idMap.remove(Integer.valueOf(mNotificationId));
	    	}
	    }
	    
		public long downloadUpdateFile(String down_url, String file) throws Exception {
			int down_step = 5;
			int totalSize;
			int downloadCount = 0;
			int updateCount = 0;
			InputStream inputStream;
			OutputStream outputStream;

			URL url = new URL(down_url);
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			httpURLConnection.setConnectTimeout(TIMEOUT);
			httpURLConnection.setReadTimeout(TIMEOUT);

			totalSize = httpURLConnection.getContentLength();
			if (httpURLConnection.getResponseCode() != 200) {
				throw new Exception("fail!");
			}
			inputStream = httpURLConnection.getInputStream();
			outputStream = new FileOutputStream(file, false);
			byte buffer[] = new byte[1024 * 8];
			int readsize = 0;
			while ((readsize = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, readsize);
				downloadCount += readsize;
				if (updateCount == 0 || (downloadCount * 100 / totalSize - down_step) >= updateCount) {
					updateCount += down_step;
					this.publishProgress(DOWN_ING, Integer.toString(updateCount));
				}
			}
			if (httpURLConnection != null) {
				httpURLConnection.disconnect();
			}
			inputStream.close();
			outputStream.close();
			return downloadCount;
		}
	}
	
	
	private void toast(String info) {
		if (info != null) {
			//Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
			if (false) {
				//在2.3系统下崩溃
				new ToastMessageTask().execute(info);
			} else {
				handler.sendMessage(handler.obtainMessage(0, info));
			}
		}
	}
	
	private class ToastMessageTask extends AsyncTask<String, String, String> {
	    String toastMessage;

	    @Override
	    protected String doInBackground(String... params) {
	        toastMessage = params[0];
	        return toastMessage;
	    }

	    protected void OnProgressUpdate(String... values) { 
	        super.onProgressUpdate(values);
	    }
	    // This is executed in the context of the main GUI thread
	    protected void onPostExecute(String result){
	           Toast toast = Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT);
	           toast.show();
	    }
	}
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Toast.makeText(getApplicationContext(), (String)msg.obj, Toast.LENGTH_SHORT).show();
		}	
	};
	
	public void refreshUpgrade(int progress, int total, int status) {
		Intent intent = null;
		intent = new Intent(UpgradeUtil.ACTION_UPGRADE)
		.putExtra(UpgradeUtil.EXTRA_UPGRADE_PROGRESS, progress)
		.putExtra(UpgradeUtil.EXTRA_UPGRADE_TOTAL, total)
		.putExtra(UpgradeUtil.EXTRA_UPGRADE_STATUS, status)
		;
		sendBroadcast(intent);
	}

	public void sendBeginInstall(String path) {
		Intent intent = null;
		intent = new Intent(UpgradeUtil.ACTION_UPGRADE_INSTALL)
				.putExtra(UpgradeUtil.EXTRA_UPGRADE_PATH, path)
		;
		sendBroadcast(intent);
	}
}
