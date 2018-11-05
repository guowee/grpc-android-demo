package com.missile.service.aidl;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.annotation.Nullable;

import com.missile.service.IAidlServiceApi;


public class AidlServiceApi extends Service {


    IAidlServiceApi.Stub mBinder = new IAidlServiceApi.Stub() {
        @Override
        public void reboot(boolean confirm, String reason, boolean wait) throws RemoteException {
            IBinder b = ServiceManager.getService(Context.POWER_SERVICE);
            IPowerManager mPowerManager = IPowerManager.Stub.asInterface(b);
            mPowerManager.reboot(confirm, reason, wait);
        }

        @Override
        public String getVersion() throws RemoteException {
            String str = "";
            PackageManager pm = getPackageManager();

            PackageInfo info = null;
            try {
                info = pm.getPackageInfo(getPackageName(), 0);
                str = info.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            return str;
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
