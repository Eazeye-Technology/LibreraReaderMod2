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

	public static final String RENAME_SUFFIX = ".rename";
	public static final String EXTRA_APP_NAME = "EXTRA_APP_NAME";
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
				NotificationManager notificationManager =
						(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				String id = "channel_01222";
				try {
					notificationManager.deleteNotificationChannel(id);
				} catch (Exception e){
					e.printStackTrace();
				}
				CharSequence name = getString(R.string.app_name);
				String description = getString(R.string.app_name);
				NotificationChannel notificationChannel =
						new NotificationChannel(id,name,NotificationManager.IMPORTANCE_MIN);
				notificationChannel.setDescription(description);
				notificationChannel.enableLights(false);
				//notificationChannel.enableLights(true);
				//notificationChannel.setLightColor(Color.RED);
				notificationChannel.enableVibration(true);
				//notificationChannel.enableVibration(true);
				//notificationChannel.setVibrationPattern(new long[]{100,200,300,400,500,400,300,200,100});
				notificationChannel.setVibrationPattern(new long[]{0});
				notificationChannel.setSound(null, null);
				notificationChannel.canBypassDnd();
				notificationChannel.setBypassDnd(true);
				notificationManager.createNotificationChannel(notificationChannel);

				Notification notification = new NotificationCompat.Builder(UpdateService.this, id)
						.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
						.setContentTitle(getText(R.string.app_name))
						.setContentText("Upgrading...")
						.setWhen(System.currentTimeMillis())
						.setSmallIcon(R.mipmap.ic_launcher_bookmanager2)
						//.setLargeIcon(BitmapFactory.decodeResource(getResources(),
						//		R.drawable.ic_launcher_background))
						//.setContentIntent(pendingIntent)
						.setAutoCancel(true)
						.setSound(null)
						.setVibrate(new long[]{0})
						.setOnlyAlertOnce(true)
						.build();
				//https://blog.csdn.net/qq_36607515/article/details/81393794
				//https://blog.csdn.net/a2241076850/article/details/75668457

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(UPDATE_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
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
				
				boolean success = mUpdateFile.renameTo(mRenameFile);
		        if (success != true) {
		        	toast("Rename failed: " + mDownPath);
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
                .setPackage(getPackageName())
		.putExtra(UpgradeUtil.EXTRA_UPGRADE_PROGRESS, progress)
		.putExtra(UpgradeUtil.EXTRA_UPGRADE_TOTAL, total)
		.putExtra(UpgradeUtil.EXTRA_UPGRADE_STATUS, status)
		;
		sendBroadcast(intent);
	}

	public void sendBeginInstall(String path) {
		Intent intent = null;
		intent = new Intent(UpgradeUtil.ACTION_UPGRADE_INSTALL)
                .setPackage(getPackageName())
				.putExtra(UpgradeUtil.EXTRA_UPGRADE_PATH, path)
		;
		sendBroadcast(intent);
	}
}
