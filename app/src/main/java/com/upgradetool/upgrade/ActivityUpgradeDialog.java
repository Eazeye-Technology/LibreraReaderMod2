package com.upgradetool.upgrade;

import android.app.Activity;
import android.content.DialogInterface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.txkj.readingapp.R;

import org.w3c.dom.Text;

public class ActivityUpgradeDialog {
    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }
    public void setMessage(String text) {
        if (dialog != null) {
            TextView tvUpgradeMessage2 = (TextView) dialog.findViewById(R.id.tvUpgradeMessage2);
            if (tvUpgradeMessage2 != null && text != null) {
                tvUpgradeMessage2.setText(text);
            }
        }
    }

    private final static boolean ENABLE_DOWNLOAD = true;
    public static int getCenteredTitleThemeOverlay() {
        //return com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered;
        return R.style.MyThemeOverlayAlertDialogDark;
    }

    //private final static int WIN_WIDTH = 312 + 24 * 2;//312;

    private Activity mContext;
    private Runnable mRunnable;
    private Runnable mRunnable2;
    private String mFromVersion, mToVersion;
    private boolean mActive = true;

    public ActivityUpgradeDialog(Activity ctx,
                                 String fromVersion, String toVersion,
                                 Runnable runnable, Runnable runnable2) {
        this.mContext = ctx;
        this.mRunnable = runnable;
        this.mRunnable2 = runnable2;
        this.mFromVersion = fromVersion;
        this.mToVersion = toVersion;
    }

    private AlertDialog dialog = null;
    public AlertDialog create() {
         dialog = new MaterialAlertDialogBuilder(this.mContext, getCenteredTitleThemeOverlay())
                //.setTitle(title)
                .setView(R.layout.activity_dialog_upgrade2)
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

                TextView tvVersionFrom = (TextView) dialog.findViewById(R.id.tvVersionFrom);
                TextView tvVersionTo = (TextView) dialog.findViewById(R.id.tvVersionTo);
                tvVersionFrom.setText(mFromVersion != null ? mFromVersion : "");
                tvVersionTo.setText(mToVersion != null ? mToVersion : "");

                CardView btnSave = (CardView) dialog.findViewById(R.id.btnSave);
                TextView tvSave = (TextView) dialog.findViewById(R.id.tvSave);
//                Button btnCancel = (Button) dialog.findViewById(R.id.btnCancel);
                btnSave.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (ENABLE_DOWNLOAD) {
                            //don't close the dialog
                            if (mActive) {
                                mActive = false;
                                btnSave.setCardBackgroundColor(0xFF000000);
                                tvSave.setTextColor(0xFFC6C6C6);
                                if (mRunnable2 != null) {
                                    mRunnable2.run();
                                }
                            }
                        } else {
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                            if (mRunnable != null) {
                                mRunnable.run();
                            }
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
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
//                if (mContext instanceof MainActivity2) {
//                    ((MainActivity2) mContext).onDialogDismiss();
//                }
            }
        });
        try {
            Window window = dialog.getWindow();
            if (window != null) {
                int WIN_WIDTH = mContext.getResources().getDimensionPixelSize(R.dimen.activity_dialog_upgrade_min_width);
                window.setLayout(WIN_WIDTH, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        } catch (Throwable eee) {
            eee.printStackTrace();
        }
        return dialog;
    }


    public AlertDialog createOld() {
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
                RelativeLayout btnSave = (RelativeLayout) dialog.findViewById(R.id.btnSave);
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
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
//                if (mContext instanceof MainActivity2) {
//                    ((MainActivity2) mContext).onDialogDismiss();
//                }
            }
        });
        try {
            Window window = dialog.getWindow();
            if (window != null) {
                int WIN_WIDTH = mContext.getResources().getDimensionPixelSize(R.dimen.activity_dialog_upgrade_min_width);
                window.setLayout(WIN_WIDTH, WindowManager.LayoutParams.WRAP_CONTENT);
            }
        } catch (Throwable eee) {
            eee.printStackTrace();
        }
        return dialog;
    }
}
