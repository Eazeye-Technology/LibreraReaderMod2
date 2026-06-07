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
    public String getSite(Context context) {
        String SITE_PATH = UpgradeUtil.USE_UPGRADE_URL1;
        return SITE_PATH;
    }
    public String getBaseUrl(Context context) {
        return getSite(context);
    }

    private static final String CONTENT_ENCODING = "utf-8";
    private final static String VERSION_LIST_URL = UpgradeUtil.USE_UPGRADE_URL2;

    private static PaidanRestClient mInstance = null;
    private SyncHttpClient client;
    private CookieStore mCookieStore;
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
}

