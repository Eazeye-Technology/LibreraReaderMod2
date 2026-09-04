package com.dseink;

import android.app.Activity;
import android.app.Dialog;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

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

    //DualScreenConstant.EXTRA_LAUNCH_SCREEN_PANEL_BOTH
    public static int getCurrentScreenPos(Activity activity, int defVal) {
        try {
            Activity act = (Activity) activity;
            //return act.getCurrentScreenPanel();
            Integer result = null;
            try {
                result = ReflectUtils.reflect(act).method("getCurrentScreenPanel").get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result != null ? result.intValue() : defVal;
        } catch (Throwable e) {
            e.printStackTrace();
            return defVal;
        }
    }

    //if dual screen, move to left screen center
    public static void centerToLeftScreen(Activity act, Dialog dialog) {
        if (act == null) {
            return;
        }
        if (dialog == null) {
            return;
        }
        if (EinkUtils.getCurrentScreenPos(act, 0) ==
                DualScreenConstant.EXTRA_LAUNCH_SCREEN_PANEL_BOTH) {
            Window window = dialog.getWindow();
            if (window != null) {
//                            window.setGravity(Gravity.BOTTOM);
//                            WindowManager.LayoutParams params = window.getAttributes();
//                            params.x = 0;
//                            params.y = 0;
//                            window.setAttributes(params);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                act.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                int screenWidth = displayMetrics.widthPixels;
//                            window.setGravity(Gravity.CENTER_VERTICAL);
//                            window.setLayout((int) (screenWidth * 0.5), WindowManager.LayoutParams.WRAP_CONTENT);

                //WindowManager.LayoutParams params = window.getAttributes();
                WindowManager.LayoutParams params = new WindowManager.LayoutParams();
                params.copyFrom(window.getAttributes());
                params.x = -(int)(screenWidth*0.25);//-310;
//                            params.y = 200;
                params.width = (int)(screenWidth*0.25);//500;
//                            params.height = 200;
                window.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                window.setAttributes(params);
            }
        }
    }
}
