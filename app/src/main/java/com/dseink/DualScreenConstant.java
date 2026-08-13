package com.dseink;

import android.app.Activity;
import android.widget.RelativeLayout;

import com.foobnix.pdf.SlidingTabLayout;
import com.txkj.readingapp.R;

public class DualScreenConstant {
    /**
     *	20211012,add for dual-screen.whitch screen we want to launcher
     *	the activity.
     */
    public static final String EXTRA_LAUNCH_SCREEN =
            "android.intent.extra.LAUNCH_SCREEN";

    public static final int EXTRA_LAUNCH_SCREEN_PANEL_NONE	= 0;
    public static final int EXTRA_LAUNCH_SCREEN_PANEL_A 	= 1;
    public static final int EXTRA_LAUNCH_SCREEN_PANEL_B		= 2;
    public static final int EXTRA_LAUNCH_SCREEN_PANEL_BOTH 	= 3;

    //FIXME:added
    public static void setupPadding(Activity act) {
        if (true) {
            //root_layout
            //LinearLayout rootLayout = findViewById(R.id.parentParent);
            SlidingTabLayout rootLayout = act.findViewById(R.id.slidingTabs1);
            rootLayout.setPadding(0, 50, 0, 0);
        }
    }

    public static void setupPadding2(Activity act) {
        if (true) {
            //root_layout
            //LinearLayout rootLayout = findViewById(R.id.parentParent);
            RelativeLayout rootLayout = act.findViewById(R.id.parentParent);
            rootLayout.setPadding(0, 50, 0, 0);
        }
    }
}
