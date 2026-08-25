package com.dseink;

import android.app.Activity;
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

    //Activity
    //public int getKeyEventStatus() {
    //        return this.mKeyEventStatus;
    //}
    public static int getKeyEventStatus(Activity act) {
        if (act == null) {
            return 0;
        }
        try {
            Integer result = (Integer) ReflectUtils.reflect(act).method("getKeyEventStatus").get();
            if (result != null) {
                return result;
            } else {
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
