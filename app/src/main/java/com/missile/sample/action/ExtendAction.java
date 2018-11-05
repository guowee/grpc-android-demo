package com.missile.sample.action;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.SystemClock;

import com.missile.sample.grpc.GrpcClient;
import com.missile.sample.utils.Display;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class ExtendAction {

    private static Context mContext;
    private static Handler handler = new Handler();
    private static ArrayList<String> foundList = new ArrayList<>();
    private static boolean bl_timeout = false;
    private static boolean bl_ready = false;
    private static boolean bl_remove = false;

    public static void setContext(Context context) {
        mContext = context;
    }

    private static BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            BluetoothDevice device = null;
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                foundList.add(device.getAddress());
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    String str = "配对设备|" + device.getName() + "|"
                            + device.getAddress();
                    Display.appendInfo(str);
                }
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        Display.appendInfo("正在配对......" + device.getName() + "|"
                                + device.getAddress() + "\n");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Display.appendInfo("完成配对......" + device.getName() + "|"
                                + device.getAddress() + "\n");
                        bl_ready = true;
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Display.appendInfo("取消配对......" + device.getName() + "|"
                                + device.getAddress() + "\n");
                        bl_remove = true;
                        break;
                    default:
                        break;
                }
            } else if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                Display.appendInfo("自动pin......\n");
                BluetoothDevice tmp = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String passkey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR) + "";
                try {
                    Method setPairingConfirmationMethod = BluetoothDevice.class.getMethod("setPairingConfirmation", new Class[]{boolean.class});
                    setPairingConfirmationMethod.invoke(tmp, true);
                } catch (Exception e) {
                    e.printStackTrace();

                }
                abortBroadcast();

                try {
                    Method setPinMethod = BluetoothDevice.class.getDeclaredMethod("setPin", new Class[]{byte[].class});
                    Boolean returnValue = (Boolean) setPinMethod.invoke(tmp, new Object[]{passkey.getBytes()});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }
    };

    public static String sayHello(String msg) {
        try {
            return GrpcClient.sayHello(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String ctrlBluetooth(int ctrl) {
        String retValue = null;
        BluetoothDevice device = null;
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(receiver, intent);
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            if (!btAdapter.isEnabled()) {
                btAdapter.enable();
            }

            Method setScanModeMethod = null;
            try {
                //setDiscoverableTimeoutMethod = BluetoothAdapter.class.getMethod("setDiscoverableTimeout",int.class);
                setScanModeMethod = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
                Boolean res = (Boolean) setScanModeMethod.invoke(btAdapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
                //Log.d(TAG,"setScanModeMethod FIND OK");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                //Log.d(TAG,"setScanModeMethod FIND Exception");
            }
            if (btAdapter.isDiscovering())
                btAdapter.cancelDiscovery();

            if (!btAdapter.isDiscovering()) {
                btAdapter.startDiscovery();
                foundList.clear();
            }


            if (ctrl == 0) {
                String ret = GrpcClient.ctrlBluetooth(btAdapter.getAddress(), ctrl);
                Display.appendInfo("found " + ret + "\n");
                if (ret == null || ret.equals("null"))
                    retValue = null;
                if (foundBond(ret)) {
                    retValue = ret;
                }
            } else if (ctrl == 1) {
                String ret = GrpcClient.ctrlBluetooth(btAdapter.getAddress(), ctrl);
                Display.appendInfo("paring " + ret + "\n");
                if (ret == null || ret.equals("null"))
                    retValue = null;
                if (bluetoothBond(ret)) {
                    retValue = ret;
                }
            } else if (ctrl == 2) {
                String ret = GrpcClient.ctrlBluetooth(btAdapter.getAddress(), ctrl);
                Display.appendInfo("remove " + ret + "\n");
                if (ret == null || ret.equals("null"))
                    retValue = null;
                if (removeBondDevice(ret)) {
                    retValue = ret;
                }
            }

        } catch (Exception e) {
            retValue = null;
        }
        if (btAdapter.isDiscovering())
            btAdapter.cancelDiscovery();
        mContext.unregisterReceiver(receiver);
        foundList.clear();
        return retValue;
    }

    //移除绑定设备
    private static boolean removeBondDevice(String mac) {
        bl_remove = false;
        BluetoothAdapter btAdapt = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice btDev = btAdapt.getRemoteDevice(mac);
        try {
            if (btDev.getBondState() == BluetoothDevice.BOND_BONDED) {
                Method m = btDev.getClass().getMethod("removeBond");
                int cnt = 0;
                while (!(boolean) m.invoke(btDev)) {
                    if (cnt++ > 3) {
                        return false;
                    }
                    SystemClock.sleep(500);
                }
            } else {
                bl_remove = true;
            }

            if (bl_remove) {
                return true;
            } else {
                bl_timeout = false;
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        bl_timeout = true;
                    }
                };
                handler.postDelayed(runnable, 30000);
                while (!bl_remove && !bl_timeout) ;
                handler.removeCallbacks(runnable);
                if (bl_timeout) return false;
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static BluetoothDevice bluetoothBond(boolean insecure) {

        BluetoothDevice bluetoothDevice = null;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mContext.registerReceiver(receiver, intentFilter);
        BluetoothAdapter btAdapterer = BluetoothAdapter.getDefaultAdapter();
        try {
            if (!btAdapterer.isEnabled()) {
                btAdapterer.enable();
            }

            Method setScanModeMethod = null;
            setScanModeMethod = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanModeMethod.invoke(btAdapterer, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);

            foundList.clear();
            if (btAdapterer.isDiscovering()) {
                btAdapterer.cancelDiscovery();
            }

            Object[] devices = btAdapterer.getBondedDevices().toArray();

            for (int i = 0; i < devices.length; i++) {
                BluetoothDevice device = (BluetoothDevice) devices[i];
                String str = "已配对|" + device.getName() + "|" + device.getAddress();
                Display.appendInfo(str);
            }
            Display.appendInfo("本机蓝牙地址：" + btAdapterer.getAddress());
            foundList.clear();
            btAdapterer.startDiscovery();
            Thread.sleep(100);
            String ret = GrpcClient.bluetoothBond(btAdapterer.getAddress(), insecure);
            Display.appendInfo("Service蓝牙地址：" + ret);
            if ((ret != null) && (!ret.equals("null")) && (ret.length() > 0)) {
                if (bluetoothBond(ret)) {
                    bluetoothDevice = btAdapterer.getRemoteDevice(ret);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mContext.unregisterReceiver(receiver);
        foundList.clear();
        return bluetoothDevice;
    }

    private static boolean bluetoothBond(String mac) {
        BluetoothAdapter btAdapterer = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = btAdapterer.getRemoteDevice(mac);
        try {

            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                if (!foundBond(mac)) {
                    return false;
                }
                Method createBondMethod = BluetoothDevice.class.getMethod("createBond");
                int cnt = 0;
                while (!(boolean) createBondMethod.invoke(device)) {
                    if (cnt++ > 3) {
                        return false;
                    }
                    SystemClock.sleep(500);
                }
            } else if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                bl_ready = true;
            }


            if (bl_ready) {
                return true;
            } else {
                bl_timeout = false;
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        bl_timeout = true;
                    }
                };

                handler.postDelayed(runnable, 30000);
                while (!bl_ready && !bl_timeout) ;
                handler.removeCallbacks(runnable);
                if (bl_timeout) {
                    return false;
                }
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        return false;


    }

    //绑定mac
    private static boolean foundBond(String mac) {
        bl_timeout = false;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                bl_timeout = true;
            }
        };
        handler.postDelayed(runnable, 30000);
        while (!foundList.contains(mac) && !bl_timeout) ;
        handler.removeCallbacks(runnable);
        if (bl_timeout) return false;
        return true;
    }

    /**
     * 在远程服务上面显示相应的图片，扫描测试用。
     * 竖屏机器，图片转90度显示，横屏机器正常显示
     *
     * @param pic 文件名字 如 Anker Plessey_013715_800x480.png
     * @return 空字符，代表有问题。 no 代表没图片， ok 表示已经显示。
     */
    public static String showScanPic(String pic) {
        if (pic == null) {
            return "";
        }
        try {
            String test = GrpcClient.showScanPic(pic);
            if (test.equals("null")) {
                return "";
            }
            return test;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }


}
