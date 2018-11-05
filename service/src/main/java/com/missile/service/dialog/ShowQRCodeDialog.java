package com.missile.service.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import com.missile.service.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;


public class ShowQRCodeDialog {
    private static final String TAG = "ShowQRCodeDialog";
    private static final boolean AUTO_HIDE = false;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    private static final int UI_ANIMATION_DELAY = 300;
    Dialog mDialog;
    private Spinner mSpinnerTest;
    private Spinner mSpinnerPic;

    private ArrayAdapter<String> mSpinnerTestAdapter;
    private ArrayAdapter<String> mSpinnerPicAdapter;

    private final Handler mHideHandler = new Handler();
    private Bitmap bitmap;
    private ImageView mContentView;
    String[] images = null;
    private AssetManager assets = null;

    private View mControlsView;
    private View mControlsViewSpinner;

    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    private HashMap<String, ArrayList<String>> mPicMap = new HashMap<>();
    private HashMap<String, String> mPicFileMap = new HashMap<>();
    private ArrayList<String> mSpinnerTestItemsList = new ArrayList<>();
    private ArrayList<String> mSpinnerPicItemsList = new ArrayList<>();

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    hide();
                    mDialog.show();
                    Bundle bundle = (Bundle) msg.obj;
                    retShow = setImageShow(bundle.getString("dir"), bundle.getString("pic"));
                    ready = true;
                    break;
                case 1:
                    hide();
                    mDialog.show();
                    retShow = setImageShow((String) msg.obj);
                    ready = true;
                    break;
            }
        }
    };

    private Handler mTimeoutHandler = new Handler();
    private boolean timeout = false;
    private boolean ready = false;
    private String retShow = "null";
    private static Context mContext;

    public ShowQRCodeDialog(Context context) {
        mContext = context;
        mDialog = new Dialog(context, R.style.Dialog_Fullscreen);
        View view = LayoutInflater.from(context).inflate(R.layout.scan_dialog, null);
        mDialog.setContentView(view);
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        try {
            mPicMap.clear();
            mPicFileMap.clear();
            mSpinnerTestItemsList.clear();
            assets = context.getAssets();
            //获取/assets/目录下所有文件
            images = assets.list("scan");
            for (int i = 0; i < images.length; i++) {
                String[] img = assets.list("scan/" + images[i]);
                mSpinnerTestItemsList.add(images[i]);
                ArrayList<String> list = new ArrayList<>();
                for (int j = 0; j < img.length; j++) {
                    list.add(img[j]);
                    mPicFileMap.put(img[j], "scan/" + images[i] + "/" + img[j]);
                }
                mPicMap.put(images[i], list);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mVisible = false;
        mControlsView = view.findViewById(R.id.fullscreen_content_controls);
        mControlsViewSpinner = view.findViewById(R.id.fullscreen_content_controls_spinner);

        mContentView = (ImageView) view.findViewById(R.id.fullscreen_content);

        mSpinnerTest = (Spinner) view.findViewById(R.id.spinner_test);
        mSpinnerTestAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, mSpinnerTestItemsList);
        mSpinnerTestAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerTest.setAdapter(mSpinnerTestAdapter);
        mSpinnerTest.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSpinnerPicItemsList.clear();
                ArrayList<String> list = mPicMap.get(mSpinnerTestItemsList.get(position));
                for (int i = 0; i < list.size(); i++) {
                    mSpinnerPicItemsList.add(list.get(i));
                }
                setImageShow(mSpinnerTestItemsList.get(position), mSpinnerPicItemsList.get(0));
                mSpinnerPic.setSelection(0);
                mSpinnerPicAdapter.notifyDataSetChanged();
                if (AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
            }
        });

        mSpinnerPicItemsList.clear();
        ArrayList<String> flist = mPicMap.get(mSpinnerTestItemsList.get(0));
        for (int i = 0; i < flist.size(); i++) {
            mSpinnerPicItemsList.add(flist.get(i));
        }
        mSpinnerPic = (Spinner) view.findViewById(R.id.spinner_pic);
        mSpinnerPicAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, mSpinnerPicItemsList);
        mSpinnerPicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerPic.setAdapter(mSpinnerPicAdapter);
        mSpinnerPic.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setImageShow(mSpinnerTestItemsList.get(mSpinnerTest.getSelectedItemPosition()), mSpinnerPicItemsList.get(position));
                if (AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
            }
        });

        setImageShow(mSpinnerTestItemsList.get(0), mSpinnerPicItemsList.get(0));
        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggle();
                return false;
            }
        });

        view.findViewById(R.id.rotate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }

                float rotation = 0;
                if (mContentView.getRotation() == 0)
                    rotation = -90;

                mContentView.setPivotX(mContentView.getWidth() / 2);
                mContentView.setPivotY(mContentView.getHeight() / 2);
                mContentView.setRotation(rotation);

            }
        });

        view.findViewById(R.id.next_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AUTO_HIDE) {
                    delayedHide(AUTO_HIDE_DELAY_MILLIS);
                }
            }
        });
        hide();
    }

    public void showDialog() {
        mDialog.show();
    }

    public void dismissDialog() {
        mDialog.dismiss();
    }

    public String setImageShowPic(String pic) {
        timeout = false;
        ready = false;
        Message msg = mHandler.obtainMessage(1, pic);
        mHandler.sendMessage(msg);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                timeout = true;
            }
        };
        mTimeoutHandler.postDelayed(runnable, 1000);
        while (!timeout && !ready) ;
        return retShow;
    }

    public String setImageShowEx(String test, String pic) {
        timeout = false;
        ready = false;
        Bundle bundle = new Bundle();
        bundle.putString("dir", test);
        bundle.putString("pic", pic);
        Message msg = mHandler.obtainMessage(0, bundle);
        mHandler.sendMessage(msg);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                timeout = true;
            }
        };
        mTimeoutHandler.postDelayed(runnable, 1000);
        while (!timeout && !ready) ;
        return retShow;
    }

    public String setImageShow(String pic) {
        InputStream assetFile = null;
        if (!mPicFileMap.containsKey(pic)) {
            return "no";
        }
        String path = mPicFileMap.get(pic);
        try {
            //打开指定资源对应的输入流
            assetFile = assets.open(path);
        } catch (IOException e) {
            //e.printStackTrace();
            return "null";
        }
        BitmapDrawable bitmapDrawable = (BitmapDrawable) mContentView
                .getDrawable();
        //如果图片还未回收，先强制回收该图片
        if (bitmapDrawable != null
                && !bitmapDrawable.getBitmap().isRecycled())             //①
        {
            bitmapDrawable.getBitmap().recycle();
        }
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            bitmap = BitmapFactory.decodeStream(assetFile);
            Matrix matrix = new Matrix();
            matrix.setRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            mContentView.setImageBitmap(bitmap);
        } else {
            //改变ImageView显示的图片
            mContentView.setImageBitmap(BitmapFactory.decodeStream(assetFile));
        }
        return "ok";
    }

    public String setImageShow(String test, String pic) {
        InputStream assetFile = null;
        try {
            //打开指定资源对应的输入流
            assetFile = assets
                    .open("scan/" + test + "/" + pic);
        } catch (IOException e) {
            //e.printStackTrace();
            return "null";
        }
        BitmapDrawable bitmapDrawable = (BitmapDrawable) mContentView
                .getDrawable();
        //如果图片还未回收，先强制回收该图片
        if (bitmapDrawable != null
                && !bitmapDrawable.getBitmap().isRecycled())             //①
        {
            bitmapDrawable.getBitmap().recycle();
        }
        if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            bitmap = BitmapFactory.decodeStream(assetFile);
            Matrix matrix = new Matrix();
            matrix.setRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            mContentView.setImageBitmap(bitmap);
        } else {
            //改变ImageView显示的图片
            mContentView.setImageBitmap(BitmapFactory.decodeStream(assetFile));
        }
        return "ok";
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {

        mControlsView.setVisibility(View.GONE);
        mControlsViewSpinner.setVisibility(View.GONE);
        mVisible = false;

        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        mControlsView.setVisibility(View.VISIBLE);
        mControlsViewSpinner.setVisibility(View.VISIBLE);

    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

}
