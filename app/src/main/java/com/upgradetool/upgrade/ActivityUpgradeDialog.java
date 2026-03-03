package com.upgradetool.upgrade;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.txkj.readingapp.R;

public class ActivityUpgradeDialog {
    public static int getCenteredTitleThemeOverlay() {
        //return com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered;
        return R.style.MyThemeOverlayAlertDialog;
    }

    private final static int WIN_WIDTH = 312 + 24 * 2;//312;

    private Activity mContext;
    private Runnable mRunnable;

    public ActivityUpgradeDialog(Activity ctx, Runnable runnable) {
        this.mContext = ctx;
        this.mRunnable = runnable;
    }

    public AlertDialog create() {
        AlertDialog dialog = new MaterialAlertDialogBuilder(this.mContext, getCenteredTitleThemeOverlay())
                //.setTitle(title)
                .setView(R.layout.activity_dialog_upgrade)
                .setCancelable(true)
//                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        TextView input = ((androidx.appcompat.app.AlertDialog) dialog).findViewById(R.id.textState);
//                        //Toast.makeText(BookListActivity.this, input.getText(), Toast.LENGTH_LONG).show();
//                        renameBook(dialog, input.getText().toString());
//                    }
//                })
//                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                AlertDialog dialog = (AlertDialog) dialogInterface;
                Button btnSave = (Button) dialog.findViewById(R.id.btnSave);
//                Button btnCancel = (Button) dialog.findViewById(R.id.btnCancel);
                btnSave.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        if (mRunnable != null) {
                            mRunnable.run();
                        }
                    }
                });
//                btnCancel.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View view) {
//                        if (dialog != null) {
//                            dialog.dismiss();
//                        }
//                    }
//                });
            }
        });
        try {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(WIN_WIDTH, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        } catch (Throwable eee) {
            eee.printStackTrace();
        }
        return dialog;
    }
}
