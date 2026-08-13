package com.dseink;

import android.view.View;

public class EinkUtils {
    public static void forceEinkFullUpdateWithView(View view) {
        if (view == null) {
            return;
        }
        try {
            ReflectUtils.reflect(view).method("forceEinkFullUpdate");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
