package com.foobnix;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.multidex.MultiDexApplication;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.hypen.HypenUtils;
import com.foobnix.pdf.info.ADS;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.Prefs;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.tts.TTSNotification;

import org.acra.ACRA;
import org.acra.BuildConfig;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.sender.HttpSender;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
//import com.google.android.gms.ads.MobileAds;
//import com.google.android.gms.ads.RequestConfiguration;
//import com.google.android.gms.ads.initialization.InitializationStatus;
//import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;


public class LibreraApp extends MultiDexApplication {


    public static Context context;


    @Override
    public void onCreate() {
        if (false) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        super.onCreate();
        onCreate_EspeakApp();

        //AppsConfig.loadEngine(this);


        context = getApplicationContext();
        AppsConfig.init(this);
        Dips.init(this);
        Prefs.get().init(this);

        try {
            if (!AppsConfig.checkIsProInstalled(this)) {
//                MobileAds.initialize(this, new OnInitializationCompleteListener() {
//                    @Override
//                    public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
//                        LOG.d("ads-complete");
//
//                    }
//                });
            }
        } catch (Exception e) {
            LOG.e(e);
        }

        LOG.d("AppsConfig.IS_TEST_DEVICE",AppsConfig.IS_TEST_DEVICE);
        if (AppsConfig.IS_TEST_DEVICE) {
//            RequestConfiguration configuration = new RequestConfiguration.Builder().setTestDeviceIds(AppsConfig.testDevices).build();
//            MobileAds.setRequestConfiguration(configuration);
        }


        Log.d("Build", "Build.TestDeviceID :" + ADS.getByTestID(this));
        Log.d("Build", "Build.MODEL :" + Build.MODEL);
        Log.d("Build", "Build.DEVICE:" + Build.DEVICE);

        TTSNotification.initChannels(this);


        CacheZipUtils.init(this);

        IMG.init(this);

        LOG.d("Build", "Build.MANUFACTURER", Build.MANUFACTURER);
        LOG.d("Build", "Build.PRODUCT", Build.PRODUCT);
        LOG.d("Build", "Build.DEVICE", Build.DEVICE);
        LOG.d("Build", "Build.BRAND", Build.BRAND);
        LOG.d("Build", "Build.MODEL", Build.MODEL);
        LOG.d("Build", "Build.VERSION.SDK_INT", Build.VERSION.SDK_INT);

        LOG.d("Build", "Build.screenWidth", Dips.screenWidthDP(), Dips.screenWidth());

        LOG.d("Build.Context", "Context.getFilesDir()", getFilesDir());
        LOG.d("Build.Context", "Context.getCacheDir()", getCacheDir());
        LOG.d("Build.Context", "Context.getExternalCacheDir", getExternalCacheDir());
        LOG.d("Build.Context", "Context.getExternalFilesDir(null)", getExternalFilesDir(null));
        LOG.d("Build.Context", "Environment.getExternalStorageDirectory()", Environment.getExternalStorageDirectory());
        LOG.d("Build.Height", Dips.screenHeight());


        if (TxtUtils.isEmpty(AppsConfig.FLAVOR)) {
            throw new RuntimeException("Application not configured correctly!");
        }

        if (AppsConfig.IS_WRITE_LOGS) {
            LOG.writeCrashTofile = true;
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, final Throwable e) {
                    LOG.uncaughtException(e);

                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    System.exit(0);

                }
            });
        }


    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LOG.d("AppState save onLowMemory");
        IMG.clearMemoryCache();
        TintUtil.clean();
        HypenUtils.cache.clear();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        LOG.d("onTrimMemory", level);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
//        CoreConfigurationBuilder builder = new CoreConfigurationBuilder(this);
//        builder.setBuildConfigClass(BuildConfig.class).setReportFormat(StringFormat.JSON);
//        builder.getPluginConfigurationBuilder(HttpSenderConfigurationBuilder.class)
//                .setUri("http://192.168.1.118:8080/AcraServiceDemo/CrashApiAction")
//                .setHttpMethod(HttpSender.Method.POST)
//                .setEnabled(true);
//        ACRA.init(this, builder);

        //need android:usesCleartextTraffic="true"
        String URL = "http://43.106.83.57:3456/report";
        if (getAcraEnableFile(APPNAME_NEW)) {
            ACRA.init(this, new CoreConfigurationBuilder()
                            //core configuration:
                            .withBuildConfigClass(BuildConfig.class)
                            .withReportFormat(StringFormat.JSON)
                            .withPluginConfigurations(
                                    //each plugin you chose above can be configured with its builder like this:
//                        new ToastConfigurationBuilder()
//                                .withText(getString(R.string.acra_toast_text))
//                                .build()
                                    new HttpSenderConfigurationBuilder()
                                            .withUri(URL)
                                            .withHttpMethod(HttpSender.Method.POST)
                                            .withEnabled(true)
                                            .build(),
                                    new DialogConfigurationBuilder()
                                            .withText("It looks like the application has crashed. Tap OK to send a report.")// to " + URL + " .")
                                            .build()
                            )
            );
        }
    }



    public final static String APPNAME_NEW = "txkjreader";
    private final static String PREFNAME = "acrapref.json";
    public boolean getAcraEnableFile(String appName) {
        String prefName = PREFNAME;
        String recentFiles = "";
        try {
            String rootPath = null;
            rootPath = new File(Environment.getExternalStorageDirectory(), appName).toString();
            boolean kkk = new File(rootPath).mkdirs();
            if (new File(rootPath, "" + prefName).exists()) {
                InputStream fis = new FileInputStream(new File(rootPath, "" + prefName));
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                StringBuffer recentFilesBuffer = new StringBuffer();
                while (true) {
                    String line = reader.readLine();
                    if (line != null) {
                        recentFilesBuffer.append(line);
                        recentFilesBuffer.append("\n");
                    } else {
                        break;
                    }
                }
                recentFiles = recentFilesBuffer.toString();
                reader.close();
                isr.close();
                fis.close();
            }
        } catch (Throwable eee) {
            eee.printStackTrace();
        }
        //Log.e(TAG, "recentFiles: " + recentFiles);
        JSONObject item = new JSONObject();
        try {
            item = new JSONObject(recentFiles);
            return item.optBoolean("acraEnable", false);
        } catch (Throwable eee) {
            eee.printStackTrace();
        }
        return false;
    }

    public void setAcraEnableFile(String appName, boolean acraEnable) {
        String prefName = PREFNAME;
        String recentFiles = "";
        try {
            String rootPath = null;
            rootPath = new File(Environment.getExternalStorageDirectory(), appName).toString();
            boolean kkk = new File(rootPath).mkdirs();
            if (new File(rootPath, "" + prefName).exists()) {
                InputStream fis = new FileInputStream(new File(rootPath, "" + prefName));
                InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
                BufferedReader reader = new BufferedReader(isr);
                StringBuffer recentFilesBuffer = new StringBuffer();
                while (true) {
                    String line = reader.readLine();
                    if (line != null) {
                        recentFilesBuffer.append(line);
                        recentFilesBuffer.append("\n");
                    } else {
                        break;
                    }
                }
                recentFiles = recentFilesBuffer.toString();
                reader.close();
                isr.close();
                fis.close();
            }
        } catch (Throwable eee) {
            eee.printStackTrace();
        }
        //Log.e(TAG, "recentFiles: " + recentFiles);
        JSONObject item = new JSONObject();
        try {
            item = new JSONObject(recentFiles);
            item.put("acraEnable", acraEnable);
        } catch (Throwable eee) {
            eee.printStackTrace();
            if (item != null) {
                try {
                    item.put("acraEnable", acraEnable);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        try {
            String rootPath = null;
            rootPath = new File(Environment.getExternalStorageDirectory(), appName).toString();
            FileOutputStream fout = new FileOutputStream(new File(rootPath, "" + prefName));
            OutputStreamWriter osw = new OutputStreamWriter(fout, "UTF-8");
            BufferedWriter writer = new BufferedWriter(osw);
            writer.write(item.toString());
            writer.flush();
            writer.close();
            osw.close();
            fout.close();
        } catch (Throwable eee) {
            eee.printStackTrace();
        }
    }







    private static Context storageContext;

    public void onCreate_EspeakApp() {
        //super.onCreate();
        Context appContext = getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LibreraApp.storageContext = appContext.createDeviceProtectedStorageContext();
        }
        else {
            LibreraApp.storageContext = appContext;
        }
    }

    public static Context getStorageContext() {
        return LibreraApp.storageContext;
    }
}
