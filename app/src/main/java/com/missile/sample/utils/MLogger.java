package com.missile.sample.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.missile.sample.MainActivity;


public class MLogger {
    private static MainActivity mActivity;

    private final static int MSG_LOG_MSG = 0;

    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOG_MSG:
                    if (mActivity.logcatView != null) {
                        if (!TextUtils.isEmpty((String) msg.obj)) {
                            mActivity.logcatView.append((String) msg.obj + "\n");
                        }
                    }
                    break;
                default:
                    break;

            }
        }
    };

    public MLogger() {

    }

    public static void setContext(Context context) {
        mActivity = (MainActivity) context;
    }

    public synchronized static void msg(String str) {
        Message message = mHandler.obtainMessage(MSG_LOG_MSG);
        message.obj = str;
        mHandler.sendMessage(message);
    }

}
