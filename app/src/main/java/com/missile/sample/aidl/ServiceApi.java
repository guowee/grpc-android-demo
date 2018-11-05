package com.missile.sample.aidl;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.missile.service.IAidlServiceApi;

public class ServiceApi {

    private IAidlServiceApi mIAidlServiceApi = null;
    private Context mContext = null;

    public ServiceApi(Context context) {
        //启动服务的方式：隐式
        mContext = context;
        Intent intent = new Intent(IAidlServiceApi.class.getName());
        intent.setAction(IAidlServiceApi.class.getName());
        intent.setPackage("com.missile.service");
        mContext.bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }


    public boolean getApiReady() {
        if (mIAidlServiceApi == null) {
            return false;
        }
        return true;
    }

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mIAidlServiceApi = IAidlServiceApi.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mIAidlServiceApi = null;
        }
    };

    public void reboot(boolean confirm, String reason) {
        try {
            if (mIAidlServiceApi != null) {
                mIAidlServiceApi.reboot(confirm, reason, false);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getVersion() {
        try {
            if (mIAidlServiceApi != null) {
                return mIAidlServiceApi.getVersion();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }


}
