package com.missile.sample;

import android.app.Application;

import com.missile.sample.aidl.ServiceApi;


public class AppApplication extends Application {

    private static ServiceApi mServiceApi;

    @Override
    public void onCreate() {
        super.onCreate();

        //通过有参构造函数启动Service服务
        mServiceApi = new ServiceApi(getApplicationContext());
    }


    public static ServiceApi getServiceApi() {
        return mServiceApi;
    }
}
