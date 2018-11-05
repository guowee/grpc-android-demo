package com.missile.service;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;
import android.widget.Toast;

import com.missile.service.fragment.BluetoothFragment;
import com.missile.service.fragment.GrpcFragment;
import com.missile.service.fragment.ScanFragment;
import com.missile.service.utils.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private static ViewPager mViewPager;
    private TabLayout mTabLayout;
    private static BluetoothFragment bluetoothFragment;
    private static GrpcFragment grpcFragment;
    private static ScanFragment scanFragment;
    private static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    mViewPager.setCurrentItem(1);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EasyPermissions.requestPermissions(this,
                "Request permissions",
                0,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        /**
         * 启动类型为WindowManager.LayoutParams.TYPE_SYSTEM_ALERT的AlertDialog的时候：
         * 如果是在Android 4.x的情况，只用在AndroidManifest.xml里面声明<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
         * 如果是在Android 6(API 23)的情况下，则可以添加以下代码进行请求权限，让用户同意后才可以弹出AlertDialog
         */

        if (Build.VERSION.SDK_INT >= 23) {

            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    Class clazz = Class.forName("android.provider.Settings");
                    Method method = clazz.getMethod("canDrawOverlays", Class.forName("android.content.Context"));
                    if (!(boolean) method.invoke(null, this)) {
                        Intent intent = new Intent("android.settings.action.MANAGE_OVERLAY_PERMISSION",
                                Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, 10);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Utils.setContext(this);
        bluetoothFragment = BluetoothFragment.newInstance();
        grpcFragment = GrpcFragment.newInstance();
        scanFragment = ScanFragment.newInstance();

        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);


    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Service");
        builder.setMessage("退出服务？");
        builder.setPositiveButton("EXIT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 10) {
            if (Build.VERSION.SDK_INT >= 23) {
                try {
                    Class clazz = Class.forName("android.provider.Settings");
                    Method method = clazz.getMethod("canDrawOverlays", Class.forName("android.content.Context"));
                    if (!(boolean) method.invoke(null, this)) {
                        Toast.makeText(this, "not granted", Toast.LENGTH_SHORT);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void setBluetoothFragment() {
        Message msg = mHandler.obtainMessage(0);
        mHandler.sendMessage(msg);
    }

    public static String bluetoothBond(String mac, boolean insecure) {
        return bluetoothFragment.bluetoothBond(mac, insecure);
    }

    public static String ctrlBluetooth(String mac, int ctrl) {
        return bluetoothFragment.ctrlBluetooth(mac, ctrl);
    }

    public class SectionsPagerAdapter
            extends FragmentPagerAdapter {
        FragmentManager fm;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            this.fm = fm;
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            if (position == 0) {
                return grpcFragment;
            } else if (position == 1) {
                return bluetoothFragment;
            } else if (position == 2) {
                return scanFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }


        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "gRPC";
                case 1:
                    return "Bluetooth";
                case 2:
                    return "SCAN";

            }
            return null;
        }
    }
}
