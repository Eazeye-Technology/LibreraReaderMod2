package com.upgradetool.upgrade;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.txkj.readingapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpgradeUtil {
    public final static boolean USE_UPGRADE = true;
    public final static String USE_UPGRADE_URL1 = "https://software.eazeye.com";
//    public final static String USE_UPGRADE_URL2 = "/update.json";
//    public final static String USE_UPGRADE_URL1 = "http://192.168.0.110:8080";
    public final static String USE_UPGRADE_URL2 = "/update2.json";


    private final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION = 100; //FIXME:???
    //don't modify this
    public final static boolean USE_UPGRADE_DEBUG_VERSION = false;//true; //false;//reverse version compare
    private final static boolean USE_UPGRADE_NEWDIALOG = false; //true; //if without download

    private final static boolean D = false;
    private final static String TAG = "UpgradeUtil";

    private Activity mAct;
    public UpgradeUtil(Activity act) {
        this.mAct = act;
    }

    private final static String DOWNLOAD_FILE_PATH = "/txkj/download";
    private final static String UPDATE_FILE_PATH = "/txkj/update";

    private void checkPermissioin2() {
        // Check whether this app has write external storage permission or not.
        int requstInstallPacakgesPermission = ContextCompat.checkSelfPermission(mAct, Manifest.permission.REQUEST_INSTALL_PACKAGES);
        // If do not grant write external storage permission.
        if (requstInstallPacakgesPermission != PackageManager.PERMISSION_GRANTED) {
            AlertDialog dialog3 = new AlertDialog.Builder(mAct)
                    .setIcon(R.mipmap.ic_launcher_bookmanager2)
                    .setTitle("Permission request")
                    .setMessage(
                            //"Please allow to use the storage permission"
                            "Please allow to use the install packages permission"
                    )
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            checkPermission2_next();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog3.show();
        }
    }

    private void checkPermission2_next() {
        // Request user to grant write external storage permission.
        ActivityCompat.requestPermissions(mAct, new String[]{
                Manifest.permission.REQUEST_INSTALL_PACKAGES}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE_PERMISSION);
    }












    //https://blog.csdn.net/changmu175/article/details/78906829
    private File m_apk;
    private void installProcess() {
        checkPermissioin2();
        boolean haveInstallPermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            haveInstallPermission = mAct.getPackageManager().canRequestPackageInstalls();
            if (!haveInstallPermission) {
                if (true) {
                    AlertDialog dialog2 = new AlertDialog.Builder(mAct)
                            .setIcon(R.mipmap.ic_launcher_bookmanager2)
                            .setTitle("Permission request")
                            .setMessage("Installing the application requires opening unknown source permissions. Please enable permissions in the settings")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startInstallPermissionSettingActivity();
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .create();
                    dialog2.show();
                    return;
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startInstallPermissionSettingActivity();
                    }
                }
            } else {
                installApk(m_apk);
            }
        } else {
            installApk(m_apk);
        }
    }

    private final static int REQUEST_CODE_MANAGE_UNKNOWN = 10086;
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startInstallPermissionSettingActivity() {
        Uri packageURI = Uri.parse("package:" + mAct.getPackageName());
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
        mAct.startActivityForResult(intent, REQUEST_CODE_MANAGE_UNKNOWN);
    }

    //	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		super.onActivityResult(requestCode, resultCode, data);
    protected boolean onActivityResult2(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_MANAGE_UNKNOWN) {
            installProcess();
            return true;
        }
        return  false;
    }

    private void installApk(File apk) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                intent.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
            } else {
                Uri uri = UriUtil.fromFile(mAct, apk);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                UriUtil.prepare(intent);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mAct.startActivity(intent);
        } catch (Throwable e) {
            e.printStackTrace();
            Toast.makeText(mAct, "Install apk failed", Toast.LENGTH_LONG).show();
        }
    }


    public static final String ACTION_UPGRADE_INSTALL = "com.ACTION_UPGRADE_INSTALL";
    public static final String EXTRA_UPGRADE_PATH = "EXTRA_UPGRADE_PATH";
    private void prepareInstall(IntentFilter filter) {
        filter.addAction(ACTION_UPGRADE_INSTALL);
    }



    public static final String ACTION_UPGRADE = "com.ACTION_UPGRADE";
    public static final String EXTRA_UPGRADE_PROGRESS = "EXTRA_UPGRADE_PROGRESS";
    public static final String EXTRA_UPGRADE_TOTAL = "EXTRA_UPGRADE_TOTAL";
    public static final String EXTRA_UPGRADE_STATUS = "EXTRA_UPGRADE_STATUS";
    public static final int UPGRADE_STATUS_OK = 0;
    public static final int UPGRADE_STATUS_ERROR = 1;

    public String getUpdatePath() {
        try {
            //FIXME:added
            File sd = mAct.getExternalFilesDir(null);
            if (sd != null) {
                return sd.getAbsolutePath();
            }
            //--------------------------
            String sdPath = "/mnt/sdcard";
            File sdDir = null;
            boolean sdCardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            if (sdCardExist) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //Build.VERSION_CODES.Q) {
                    //https://www.jianshu.com/p/f53294992596
                    sdDir = mAct.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
                } else {
                    sdDir = Environment.getExternalStorageDirectory();
                }
                sdPath = sdDir.toString();
            }
            File storageDir = new File(sdPath + UPDATE_FILE_PATH);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }
            String path = storageDir.toString();
            return path;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private ExecutorService newFixedThreadPool;
    public void onCreate_upgrade() {
        newFixedThreadPool = Executors.newFixedThreadPool(6);
    }
    private String mUpdatePath;
    private final static int DIALOG_UPGRADE = 1201;
    private int serverVersion = 0;
    private String serverVersionStr = null;
    private String serverVersionUrl = null;
    private String serverPathPage = null;
    private String serverNote = null;
    private String serverForceUpdate = null;
    public void checkVersion() {
        if (android.os.Build.VERSION.SDK_INT < 11) {
            new CheckVersionTask().execute();
        } else {
            new CheckVersionTask().executeOnExecutor(newFixedThreadPool);
        }
    }
    public class CheckVersionTask extends AsyncTask<Void, Void, Void> {
        private boolean isSuccess = false;
        private String localPackageName = null;
        public CheckVersionTask() {
            try {
                PackageInfo packageInfo = null;
                packageInfo = mAct.getApplicationContext().getPackageManager()
                        .getPackageInfo(mAct.getPackageName(), 0);
                localPackageName = packageInfo.packageName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (D) {
                Log.e(TAG, "MainActivity.CheckVersionTask begin CheckVersionTask ");
            }
            PaidanRestClient.getInstance(mAct).getVersionList(mAct,
                    new RecvDataListener() {
                        @Override
                        public void onRecvData(boolean successStatus, int statusCode,
                                               String responseString, Throwable throwable) {
                            if (successStatus) {
                                if (true) { //if (D) {
                                    Log.e(TAG, "LoginActivity.CheckVersionTask success: " + responseString);
                                }
                                if (responseString != null) {
                                    String issuccess = null;
                                    try {
                                        JSONObject data = new JSONObject(responseString);
                                        if (data != null) {
                                            JSONArray datas = data.optJSONArray("data");
                                            for (int i = 0; i < datas.length(); i++) {
                                                JSONObject softItem = datas.getJSONObject(i);
                                                String id = JSONUtil.optString(softItem, "id");
                                                String vstr = JSONUtil.optString(softItem, "vstr");
                                                int vint = softItem.optInt("vint");
                                                int status = softItem.optInt("status");
                                                String path = JSONUtil.optString(softItem, "path");
                                                String pathPage = JSONUtil.optString(softItem, "pathPage");
                                                int idx = softItem.optInt("idx");
                                                String cdate = JSONUtil.optString(softItem, "cdate");
                                                String note = JSONUtil.optString(softItem, "note");
                                                String forceUpdate = JSONUtil.optString(softItem, "forceUpdate");
                                                String packageName = JSONUtil.optString(softItem, "packageName");

                                                if (localPackageName != null && packageName != null &&
                                                        localPackageName.equals(packageName)) {
                                                    if (status == 1) {
                                                        if (D) {
                                                            Log.e(TAG, "LoginActivity.CheckVersionTask success serverVersion == " + serverVersion);
                                                            Log.e(TAG, "LoginActivity.CheckVersionTask success vint == " + vint);
                                                            Log.e(TAG, "LoginActivity.CheckVersionTask success vstr == " + vstr);
                                                        }
                                                        if (vint > serverVersion) {
                                                            serverVersion = vint;
                                                            serverVersionStr = vstr;
                                                            serverNote = note;
                                                            serverForceUpdate = forceUpdate;
                                                            String baseUrl = PaidanRestClient
                                                                    .getInstance(mAct)
                                                                    .getBaseUrl(mAct);
                                                            String absUrl = PaidanRestClient
                                                                    .getInstance(mAct)
                                                                    .getDocURLShort(baseUrl, path);
                                                            serverVersionUrl = absUrl;
                                                            serverPathPage = pathPage;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (JSONException e) {

                                    }
                                    isSuccess = true;

                                }
                            } else {
                                if (D) {
                                    Log.e(TAG, "LoginActivity.checkUserStatus failure1: " + statusCode);
                                }
                                isSuccess = false;
                            }
                        }
                    });

            return null;
        }

        @Override
        protected void onPostExecute(Void result_) {
            super.onPostExecute(result_);
            if (isSuccess) {
                if (serverVersion > 0 &&
                        serverVersionUrl != null &&
                        serverVersionUrl.length() > 0) {
                    afterCheckVersion();
                }
            } else {

            }
        }
    }

    public void afterCheckVersion() {
        PackageInfo packageInfo = null;
        try {
            packageInfo = mAct.getApplicationContext().getPackageManager()
                    .getPackageInfo(mAct.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        int localVersion = packageInfo.versionCode;
        if (USE_UPGRADE_DEBUG_VERSION) {
            if (localVersion > serverVersion) {
                mAct.showDialog(DIALOG_UPGRADE);
            }
        } else {
            if (localVersion < serverVersion) {
                mAct.showDialog(DIALOG_UPGRADE);
            }
        }
    }

    public static String getUrlFileName(String url) {
        if (url != null && url.length() > 0) {
            int lastIndex = url.lastIndexOf("/");
            if (lastIndex != -1 && lastIndex < url.length() - 1) {
                String docName = url.substring(lastIndex + 1);
                return docName;
            }
        }
        return null;
    }

    private AlertDialog dialog3;
    private androidx.appcompat.app.AlertDialog dialog4;
    //@Override
    public Dialog onCreateDailog_upgrade(int id) {
        ProgressDialog dialog;
        Dialog dialog2;
        switch (id) {
            case DIALOG_UPGRADE:
                if (USE_UPGRADE_NEWDIALOG) {
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            String url = serverVersionUrl;
                            if (serverPathPage != null && serverPathPage.length() > 0) {
                                url = serverPathPage;
                            }
                            Uri uri = Uri.parse(url);
                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            mAct.startActivity(intent);

                            if (serverForceUpdate != null && serverForceUpdate.equals("1")) {
                                mAct.finish();
                            } else {
                                if (dialog3 != null && dialog3.isShowing()) {
                                    dialog3.dismiss();
                                }
                            }
                        }
                    };
                    dialog4 = new ActivityUpgradeDialog(mAct, runnable).create();
                    dialog4.show();
                } else {
                    dialog3 = new AlertDialog.Builder(mAct)
                            .setTitle("Software upgrade")
                            .setMessage(
                                    (serverForceUpdate != null && serverForceUpdate.equals("1")) ?
                                            "New version discovered, must be updated immediately." :
                                            "New version discovered, it is recommended to update immediately.")
                            .setPositiveButton("Update", null)
                            .setNeutralButton("Browser Update", null)
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (serverForceUpdate != null && serverForceUpdate.equals("1")) {
                                        mAct.finish();
                                    } else {
                                        dialog.dismiss();
                                    }
                                }
                            })
                            .create();
                    dialog3.setCanceledOnTouchOutside(false);
                    dialog3.setCancelable(false);
                    dialog3.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(final DialogInterface arg0) {
                            String strNote = "";
                            if (serverNote != null && serverNote.length() > 0) {
                                strNote = "\n" + "Update Notes: " + serverNote;
                            }
                            if (serverForceUpdate != null && serverForceUpdate.equals("1")) {
                                dialog3.setMessage("A new version is discovered, force an immediate update, otherwise it cannot be used.\n" +
                                        "Version: " + (serverVersionStr != null ? serverVersionStr : "") +
                                        " (" + serverVersion + ")" +
                                        strNote
                                );
                            } else {
                                dialog3.setMessage("New version discovered, it is recommended to update immediately.\n" +
                                        "Version: " + (serverVersionStr != null ? serverVersionStr : "") +
                                        " (" + serverVersion + ")" +
                                        strNote
                                );
                            }
                            final Button b2 = dialog3.getButton(AlertDialog.BUTTON_NEUTRAL);
                            b2.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View arg0) {
                                    String url = serverVersionUrl;
                                    if (serverPathPage != null && serverPathPage.length() > 0) {
                                        url = serverPathPage;
                                    }
                                    Uri uri = Uri.parse(url);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                    mAct.startActivity(intent);

                                    if (serverForceUpdate != null && serverForceUpdate.equals("1")) {
                                        mAct.finish();
                                    } else {
                                        if (dialog3 != null && dialog3.isShowing()) {
                                            dialog3.dismiss();
                                        }
                                    }
                                }
                            });

                            if (serverForceUpdate != null && serverForceUpdate.equals("1")) {
                                final Button b3 = dialog3.getButton(AlertDialog.BUTTON_POSITIVE);
                                b3.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        mAct.finish();
                                    }
                                });
                            }

                            final Button b = dialog3.getButton(AlertDialog.BUTTON_POSITIVE);
                            b.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    b.setEnabled(false);
                                    mUpdatePath = getUpdatePath();
                                    //----------------
                                    //FIXME:added
                                    FileUtil.deleteFolder(mUpdatePath);
                                    //----------------

                                    if (mUpdatePath == null || mUpdatePath.length() == 0) {
                                        Toast.makeText(mAct,
                                                "The download directory does not exist. Please restart the device and try again, or update it using a browser", Toast.LENGTH_SHORT).show();
                                    } else {
                                        File updateParent = new File(mUpdatePath);
                                        updateParent.mkdirs();
                                        if (!updateParent.isDirectory()) {
                                            Toast.makeText(mAct,
                                                    "The download directory does not exist. Please restart the device and try again, or update it using a browser", Toast.LENGTH_SHORT).show();
                                        } else {
                                            String url = serverVersionUrl;//UpdateService.DOWNLOAD_URL;
                                            if (url == null || url.length() == 0) {
                                                Toast.makeText(mAct,
                                                        "Download URL is empty", Toast.LENGTH_SHORT).show();
                                            } else {
                                                String docName = getUrlFileName(url);
                                                File updateFileObj = new File(mUpdatePath, docName);
                                                String updateFilePath = updateFileObj.getAbsolutePath();
                                                if (updateFileObj.isFile() && updateFileObj.canRead()) {
                                                    if (false) {
                                                        Uri uri = UriUtil.fromFile(mAct, updateFileObj);
                                                        Intent intent2 = new Intent(Intent.ACTION_VIEW);
                                                        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                        intent2.setDataAndType(uri, "application/vnd.android.package-archive");
                                                        UriUtil.prepare(intent2);
                                                        try {
                                                            if (arg0 != null) {
                                                                arg0.dismiss();
                                                            }
                                                        } catch (Throwable e) {
                                                            e.printStackTrace();
                                                        }
                                                        try {
                                                            mAct.startActivity(intent2);
                                                        } catch (Throwable e) {
                                                            e.printStackTrace();
                                                        }
                                                    } else {
                                                        m_apk = updateFileObj;
                                                        installProcess();
                                                    }
                                                } else {
                                                    String updatePath = getUpdatePath();
                                                    FileUtil.deleteFolder(updatePath);

                                                    Intent intent = new Intent(mAct, UpdateService.class);
                                                    intent.putExtra(UpdateService.EXTRA_APP_NAME, "");
                                                    intent.putExtra(UpdateService.EXTRA_DOWNLOAD_URL, url);
                                                    intent.putExtra(UpdateService.EXTRA_DOWNLOAD_PATH, updateFilePath);
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        mAct.startForegroundService(intent);
                                                    } else {
                                                        mAct.startService(intent);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    });
                    dialog3.show();
                }
                break;
        }
        return null;
    }



    private MyReceiver receiver;
    public void onCreateUpdateReceiver() {
        receiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPGRADE);
        prepareInstall(filter);
        ContextCompat.registerReceiver(this.mAct, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }
    public void onDestroyUpdateReceiver() {
        if (this.receiver != null) {
            this.mAct.unregisterReceiver(receiver);
        }
    }

    private class MyReceiver extends BroadcastReceiver {
        private long lastTimer = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (ACTION_UPGRADE.equals(intent.getAction())) {
                    int progress = intent.getIntExtra(EXTRA_UPGRADE_PROGRESS, 0);
                    int total = intent.getIntExtra(EXTRA_UPGRADE_TOTAL, 0);
                    int status = intent.getIntExtra(EXTRA_UPGRADE_STATUS, 0);
                    mAct.runOnUiThread(new UiUpdater(progress, total, status));
                } else if (ACTION_UPGRADE_INSTALL.equals(intent.getAction())) {
                    String path = intent.getStringExtra(EXTRA_UPGRADE_PATH);
                    installApk(new File(path));
                }
            }
        }
    }
    private class UiUpdater implements Runnable {
        private int m_Progess;
        private int m_Total;
        private int m_Status;

        public UiUpdater(int progress, int total, int status) {
            this.m_Progess = progress;
            this.m_Total = total;
            this.m_Status = status;
        }

        @Override
        public void run() {
            if (dialog3 != null) {
                if (m_Progess < 100 && m_Status == UPGRADE_STATUS_OK) {
                    dialog3.setMessage("Downloading: " + m_Progess + "%");
                } else if (m_Status == UPGRADE_STATUS_ERROR) {
                    dialog3.setMessage("Download failed, please check network and retry\n(url=" + serverVersionUrl + ")");
                } else {
                    try {
                        dialog3.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
