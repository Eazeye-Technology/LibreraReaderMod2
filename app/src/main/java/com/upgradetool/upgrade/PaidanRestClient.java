package com.upgradetool.upgrade;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.Context;

import com.upgradetool.loopj.android.http.AsyncHttpResponseHandler;
import com.upgradetool.loopj.android.http.PersistentCookieStore;
import com.upgradetool.loopj.android.http.RequestParams;
import com.upgradetool.loopj.android.http.SyncHttpClient;
import com.upgradetool.loopj.android.http.TextHttpResponseHandler;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.client.CookieStore;

public class PaidanRestClient {
    public static String getPackName(Context context) {
        if (true) {
            return context.getPackageName(); //正式环境
        } else {
            return "com.klpro.weboa_test"; //测试环境
        }
    }

    public String getSite(Context context) {
//        String SITE_PATH = "http://192.168.0.114:8007";
//        String SITE_PATH = "http://42.192.233.179:8007";
//        String SITE_PATH = "http://192.168.1.6:8007";
        String SITE_PATH = UpgradeUtil.USE_UPGRADE_URL1; //"http://www.jmtxkj.cn";
//        String SITE_PATH = "http://10.0.2.2:8007";



//        String SITE_PATH = "http://19.120.112.145"; //正式服务器
//        String pack = getPackName(context);
//        if (pack.equals("com.klpro.weboa_test")) { //测试环境
//            //see upper, if (true) or if (false)
//            SITE_PATH = "http://101.101.106.18:8080"; //"http://192.168.1.19:8080";//
//        }
        return SITE_PATH;
    }
    public String getBaseUrl(Context context) {
        return getSite(context);
    }

    private final static boolean D = false;
    private final static String TAG = "PaidanRestClient";

    //private static final int TIMEOUT_MONTH = 1; //文件过期时间（单位月）

    private static final String CONTENT_ENCODING = "utf-8";

    //private final static String UPLOAD_FILE_URL = "http://101.101.106.18:8080/vuesvr/upload"; //上传文件

    public final static boolean globalLoadLocal = true; //可以全局屏蔽SETTING_PREF_KEY_LOAD_LOCAL，不需要缓存
    private final static int TIME_SLEEP = 500;
    private long lastLoginTime = 0; //测试用，不需要缓存
    private boolean isCheckedNoSpace = false; //检查是否够空间，不需要缓存

    //https://cbcx-sj.jmtxkj.cn/dri/getApkVersion
    //private final static String VERSION_LIST_URL = "/dri/getApkVersion"; //最新版本
    private final static String VERSION_LIST_URL = UpgradeUtil.USE_UPGRADE_URL2; //最新版本
    private final static String UPDATE_LOCATION = "/xxxx/updateLocation"; //更新坐标信息
    private final static String GET_WS_TOKEN_BY_PHONE = "/xxxx/getWsTokenByPhone"; //获取令牌
    private final static String SECOND_PUSH = "/xxxx/secondPush"; //第二推送方案

    private static PaidanRestClient mInstance = null;
    private SyncHttpClient client;
    private CookieStore mCookieStore;
    //	public void setCookieStore(CookieStore cookieStore) {
//		mCookieStore = cookieStore;
//		this.client.setCookieStore(cookieStore);
//    }
//	public CookieStore getCookieStore() {
//		return this.mCookieStore;
//    }
    public static PaidanRestClient getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new PaidanRestClient();
            mInstance.client = new SyncHttpClient();
            mInstance.client.setEnableRedirects(true);
            mInstance.client.setTimeout(2500); //3000
            mInstance.client.setMaxRetriesAndTimeout(1, 2500); //3000
            mInstance.client.setCookieStore(new PersistentCookieStore(context));
        }
        return mInstance;
    }

    private void post(Context context, int flag, String url, RequestParams params,
                      AsyncHttpResponseHandler responseHandler) {
        String fullurl = url;
        client.post(fullurl, params, responseHandler);
    }

    public void upload(Context context, String url, File file, String oid,
                       final RecvDataListener responseHandler) {
        RequestParams params = new RequestParams();
        params.setContentEncoding(CONTENT_ENCODING);
        try {
            params.put("file", file);
            params.put("name", file.getName());
            //params.put("handleType", "5");
            //params.put("upload", "开始上传");

            //handleType:5
            //oid:原始附件的id
            //uid:上传人的id
//			params.put("oid", oid);
//			int ctype2 = 1;
//			if (file != null && file.getName() != null) {
//				String filename = file.getName();
//				if (filename != null) {
//					filename = filename.toLowerCase();
//				}
//				if (filename != null && filename.endsWith("pdf")) {
//					ctype2 = 2;
//				}
//			}
//			params.put("ctype2", ctype2);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        params.put("filename", file.getName());
        post(context, 0, url, params, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  String responseString) {
                responseHandler.onRecvData(true, statusCode, responseString, null);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  String responseString, Throwable throwable) {
                responseHandler.onRecvData(false, statusCode, responseString, throwable);
            }

            @Override
            public void onProgress(int bytesWritten, int totalSize) {
                super.onProgress(bytesWritten, totalSize);
                responseHandler.onProgress(bytesWritten, totalSize);
            }
        });
    }

    private void get(Context context, int flag, String url, RequestParams params,
                     AsyncHttpResponseHandler responseHandler) {
        String fullurl = url;
        if (client != null) {
            client.get(fullurl, params, responseHandler);
        }
    }

    public void getVersionList(Context context,
                               final RecvDataListener responseHandler) {
        get(context, 1, getSite(context) + VERSION_LIST_URL, null, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  String responseString) {
                responseHandler.onRecvData(true, statusCode, responseString, null);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  String responseString, Throwable throwable) {
                responseHandler.onRecvData(false, statusCode, responseString, throwable);
            }
        });
    }

    public void updateLocation(Context context, String userToken, String userId, String userPhone,
                               String lat, String lng,
                               String provider,
                                String altitude,
                                String accuracy,
                                String bearing,
                                String speed,
                               String locerr,
                               final RecvDataListener responseHandler) {
        RequestParams params = new RequestParams();
        params.setContentEncoding(CONTENT_ENCODING);
        params.put("userToken", userToken);
        params.put("userId", userId);
        params.put("userPhone", userPhone);
        params.put("lat", lat);
        params.put("lng", lng);
        params.put("provider", provider);
        params.put("altitude", altitude);
        params.put("accuracy", accuracy);
        params.put("bearing", bearing);
        params.put("speed", speed);
        params.put("locerr", locerr);
        get(context, 1, getSite(context) + UPDATE_LOCATION, params, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  String responseString) {
                responseHandler.onRecvData(true, statusCode, responseString, null);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  String responseString, Throwable throwable) {
                responseHandler.onRecvData(false, statusCode, responseString, throwable);
            }
        });
    }


    public void secondPush(Context context, String phone, String now, String minNow, String ver,
                           final RecvDataListener responseHandler) {
        RequestParams params = new RequestParams();
        params.setContentEncoding(CONTENT_ENCODING);
        params.put("phone", phone);
        params.put("now", now);
        params.put("minNow", minNow);
        params.put("ver", ver); //空白是只推送专车，1是推送专车和ordDriChange
        get(context, 1, getSite(context) + SECOND_PUSH, params, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  String responseString) {
                responseHandler.onRecvData(true, statusCode, responseString, null);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  String responseString, Throwable throwable) {
                responseHandler.onRecvData(false, statusCode, responseString, throwable);
            }
        });
    }

    public String getDocURLShort(String baseUrl, String url) {
        if (url == null) {
            url = "";
        }
        if (url.contains("://")) {
            return url;
        }
        if (baseUrl != null) {
            if (baseUrl.endsWith("/")) {
                if (url.startsWith("/")) {
                    return baseUrl + url.substring(1);
                } else {
                    return baseUrl + url;
                }
            } else {
                if (url.startsWith("/")) {
                    return baseUrl + url;
                } else {
                    return baseUrl + "/" + url;
                }
            }
        } else {
            return url;
        }
    }


    public void getWsTokenByPhone(Context context, String userPhone,
                               final RecvDataListener responseHandler) {
        RequestParams params = new RequestParams();
        params.setContentEncoding(CONTENT_ENCODING);
        params.put("userPhone", userPhone);
        get(context, 1, getSite(context) + GET_WS_TOKEN_BY_PHONE, params, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  String responseString) {
                responseHandler.onRecvData(true, statusCode, responseString, null);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  String responseString, Throwable throwable) {
                responseHandler.onRecvData(false, statusCode, responseString, throwable);
            }
        });
    }
}

