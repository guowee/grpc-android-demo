

package com.missile.sample.utils;

import android.app.Activity;
import android.os.Message;

import com.missile.sample.MainActivity;


public class Display {
    private static MainActivity mActivity;
    private static Message message = null;

    private static final String maohao = ":";

    public static void setContext(Activity act) {
        mActivity = (MainActivity) act;
    }

    public static void appendInfo(String str) {
        MLogger.msg(str);
    }


}
