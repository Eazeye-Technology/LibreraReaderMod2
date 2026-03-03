package com.upgradetool.upgrade;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;

public class UriUtil {
    //replace of Uri.fromFile
    public static Uri fromFile(Context context, File mediaFile) {
        return FileProvider.getUriForFile(context.getApplicationContext(), context.getPackageName()+".fileprovider", mediaFile);
    }

    //如果使用了上面的方法，必须确保后面知道用这个prepare方法
    public static void prepare(Intent intent) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }
}
