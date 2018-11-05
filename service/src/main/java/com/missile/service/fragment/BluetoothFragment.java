package com.missile.service.fragment;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.missile.service.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;


public class BluetoothFragment extends Fragment {
    private static final String TAG = "BluetoothFragment";
    static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private TextView mTextView;
    private TextView mTextViewStat;
    private Button mStartBtn;
    private TextView mMacTextView;
    private static boolean bl_start = false;
    private static boolean bl_ready = false;
    private static boolean bl_timeout = false;
    private static boolean bl_Insecure = false;
    private static boolean bl_bond = false;
    private BluetoothAdapter btAdapt;
    public static BluetoothSocket btSocket;
    private BluetoothServerSocket serverSocket = null;
    static final int dataLen = 1024;
    public Activity mActivity;
    private int rLen = 0;
    private Handler handler = new Handler();
    private static ArrayList<String> foundList = new ArrayList<>();


    private BroadcastReceiver searchDevices = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle b = intent.getExtras();
            Object[] lstName = b.keySet().toArray();

            // 显示所有收到的消息及其细节
            for (int i = 0; i < lstName.length; i++) {
                String keyName = lstName[i].toString();
                Log.e(keyName, String.valueOf(b.get(keyName)));
            }
            BluetoothDevice device = null;
            // 搜索设备时，取得设备的MAC地址
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                foundList.add(device.getAddress());
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    String str = "未配对|" + device.getName() + "|"
                            + device.getAddress();
                    appendLog(str + "\n");
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:
                        appendLog("正在配对......" + device.getName() + "|"
                                + device.getAddress() + "\n");
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        appendLog("完成配对......" + device.getName() + "|"
                                + device.getAddress() + "\n");
                        bl_bond = true;
                        break;
                    case BluetoothDevice.BOND_NONE:
                        appendLog("取消配对......" + device.getName() + "|"
                                + device.getAddress() + "\n");
                    default:
                        break;
                }
            } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                appendLog("自动pin......\n");
                BluetoothDevice tmp = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String passkey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY, BluetoothDevice.ERROR) + "";
                Log.d(TAG, "pass " + passkey);
                try {
                    Method setPairingConfirmationMethod = BluetoothDevice.class.getMethod("setPairingConfirmation", new Class[]{boolean.class});
                    setPairingConfirmationMethod.invoke(tmp, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                abortBroadcast();

                try {
                    Method setPinMethod = BluetoothDevice.class.getDeclaredMethod("setPin", new Class[]{byte[].class});
                    Boolean returnValue = (Boolean) setPinMethod.invoke(tmp,
                            new Object[]
                                    {passkey.getBytes()});
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    };

    public static BluetoothFragment newInstance() {
        BluetoothFragment fragment = new BluetoothFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(searchDevices);
    }

    public BluetoothFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    public synchronized void appendLog(String str) {
        if (mTextView != null) {
            if (mTextView.getLineCount() > 100)
                mTextView.setText("");
            mTextView.append(str);
        }
    }

    public synchronized void setLog(String str) {
        if (mTextView != null) {
            mTextView.setText(str);
        }
    }

    public synchronized String getLog() {
        if (mTextView != null) {
            return mTextView.getText().toString();
        }
        return "";
    }

    private void connect(final BluetoothDevice btDev) {
        bl_start = true;
        Log.d(TAG, "connect");

        new Thread() {
            @Override
            public void run() {
                super.run();
                SystemClock.sleep(100);
                InputStream in = null;
                OutputStream out = null;
                rLen = 0;
                byte[] data = new byte[dataLen];
                UUID uuid = UUID.fromString(SPP_UUID);

                try {

                    if (bl_Insecure) {
                        serverSocket = btAdapt.listenUsingInsecureRfcommWithServiceRecord("local1", uuid);
                    } else {
                        serverSocket = btAdapt.listenUsingRfcommWithServiceRecord("local1", uuid);
                    }

                    bl_ready = true;
                    if (btAdapt.isDiscovering())
                        btAdapt.cancelDiscovery();
                    SystemClock.sleep(100);
                    btSocket = serverSocket.accept();

                    if (btSocket != null) {

                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                appendLog("通讯..." + btSocket.getRemoteDevice().getAddress() + "\n");
                                mTextViewStat.setText("Ready");
                            }
                        });

                        in = btSocket.getInputStream();
                        out = btSocket.getOutputStream();

                        while (in != null && out != null && bl_start) {
                            rLen = in.read(data);

                            if (rLen > 0) {
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        appendLog("receive data " + rLen + "\n");
                                    }
                                });
                                out.write(data, 0, rLen);
                                out.flush();
                            }
                        }
                    } else {
                        Log.d(TAG, "btSocket null");
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            appendLog("STOP...\n");
                            mTextViewStat.setText("Running");
                        }
                    });

                    try {
                        if (in != null)
                            in.close();
                        if (out != null)
                            out.close();
                        if (btSocket != null)
                            btSocket.close();
                        if (serverSocket != null)
                            serverSocket.close();

                        btSocket = null;
                        serverSocket = null;
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    bl_ready = false;


                }
            }
        }.start();

    }

    //绑定mac地址
    private boolean foundBond(String mac) {
        Log.d(TAG, "foundBond");
        boolean bl_found = false;

        bl_timeout = false;
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "timeout");
                bl_timeout = true;
            }
        };
        handler.postDelayed(runnable, 30000);
        while (!foundList.contains(mac) && !bl_timeout) ;
        handler.removeCallbacks(runnable);
        if (bl_timeout) return false;
        return true;
    }

    public String ctrlBluetooth(String mac, int ctrl) {
        BluetoothDevice btDev = btAdapt.getRemoteDevice(mac);

        Method setScanModeMethod = null;
        try {
            //setDiscoverableTimeoutMethod = BluetoothAdapter.class.getMethod("setDiscoverableTimeout",int.class);
            setScanModeMethod = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            Boolean res = (Boolean) setScanModeMethod.invoke(btAdapt, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (!btAdapt.isDiscovering()) {
            foundList.clear();
            btAdapt.startDiscovery();
        }
        if (ctrl == 1) {
            try {

                if (btDev.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "createBond");
                    if (!foundBond(mac)) {
                        return "null";
                    }
                    //利用反射方法调用BluetoothDevice.createBond(BluetoothDevice remoteDevice);
                    Method createBondMethod = BluetoothDevice.class
                            .getMethod("createBond");
                    int cnt = 0;
                    while (!(boolean) createBondMethod.invoke(btDev)) {
                        if (cnt++ > 3) {
                            return "null";
                        }
                        SystemClock.sleep(500);
                    }
                    return btAdapt.getAddress();
                } else if (btDev.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BOND_BONDED");
                    return btAdapt.getAddress();

                }
            } catch (Exception e) {
                return "null";
            }
        } else if (ctrl == 2) {
            if (btDev.getBondState() == BluetoothDevice.BOND_BONDED) {
                try {
                    bl_start = false;
                    if (btSocket != null) {
                        btSocket.close();
                        btSocket = null;
                    }

                    if (serverSocket != null) {
                        serverSocket.close();
                        serverSocket = null;
                    }
                } catch (Exception e) {

                }

                try {
                    Method m = BluetoothDevice.class
                            .getMethod("removeBond");
                    //m.invoke(device);
                    int cnt = 0;
                    while (!(boolean) m.invoke(btDev)) {
                        if (cnt++ > 3) {
                            return "null";
                        }
                        SystemClock.sleep(500);
                    }
                    return btAdapt.getAddress();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                    return "null";
                }
            }
            return btAdapt.getAddress();
        } else {
            return btAdapt.getAddress();
        }
        return "null";
    }

    public String bluetoothBond(String mac, boolean insecure) {
        Log.d(TAG, "bluetoothBond");
        bl_Insecure = insecure;
        bl_ready = false;
        bl_start = false;

        Method setScanModeMethod = null;
        try {
            //setDiscoverableTimeoutMethod = BluetoothAdapter.class.getMethod("setDiscoverableTimeout",int.class);
            setScanModeMethod = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            Boolean res = (Boolean) setScanModeMethod.invoke(btAdapt, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!btAdapt.isDiscovering()) {
            foundList.clear();
            btAdapt.startDiscovery();
        }

        BluetoothDevice btDev = btAdapt.getRemoteDevice(mac);
        try {
            if (btSocket != null) {
                btSocket.close();
                btSocket = null;
            }

            if (serverSocket != null) {
                serverSocket.close();
                serverSocket = null;
            }
            Thread.sleep(500);
            if (btDev.getBondState() == BluetoothDevice.BOND_NONE) {
                Log.d(TAG, "createBond");

                if (!foundBond(mac)) {
                    return "null";
                }

                bl_bond = false;
                //利用反射方法调用BluetoothDevice.createBond(BluetoothDevice remoteDevice);
                Method createBondMethod = BluetoothDevice.class
                        .getMethod("createBond");
                int cnt = 0;
                while (!(boolean) createBondMethod.invoke(btDev)) {
                    if (cnt++ > 3) {
                        return "null";
                    }
                    SystemClock.sleep(500);
                }

            } else if (btDev.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.d(TAG, "BOND_BONDED");
                connect(btDev);
            }
            if (bl_ready) {
                return btAdapt.getAddress();
            } else {
                bl_timeout = false;
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "timeout");
                        bl_timeout = true;
                    }
                };
                handler.postDelayed(runnable, 30000);
                while (!bl_ready && !bl_timeout) {
                    if (bl_bond && !bl_ready) {
                        bl_bond = false;
                        connect(btAdapt.getRemoteDevice(mac));//连接设备
                    }
                }
                handler.removeCallbacks(runnable);
                if (bl_timeout) return "";
                return btAdapt.getAddress();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "null";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bluetooth, container,
                false);
        mTextView = (TextView) rootView.findViewById(R.id.log_cat);
        mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mTextView.setGravity(Gravity.BOTTOM);
        mTextViewStat = (TextView) rootView.findViewById(R.id.state);
        mStartBtn = (Button) rootView.findViewById(R.id.btn_start);
        mMacTextView = (TextView) rootView.findViewById(R.id.mac);
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bl_start) {
                    mStartBtn.setText("START");
                    mTextViewStat.setText("STOP");
                    bl_start = false;
                    btAdapt.disable();
                    mMacTextView.setText("null");
                } else {
                    mStartBtn.setText("STOP");
                    mTextViewStat.setText("Running");
                    btAdapt = BluetoothAdapter.getDefaultAdapter();
                    if (!btAdapt.isEnabled()) {
                        btAdapt.enable();
                    }

                    mMacTextView.setText(btAdapt.getAddress());
                    bl_start = true;
                }
            }
        });

        mActivity = getActivity();

        btAdapt = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapt.isEnabled()) {
            btAdapt.enable();
        }

        Method setScanModeMethod = null;
        try {
            //setDiscoverableTimeoutMethod = BluetoothAdapter.class.getMethod("setDiscoverableTimeout",int.class);
            setScanModeMethod = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            Boolean res = (Boolean) setScanModeMethod.invoke(btAdapt, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMacTextView.setText(btAdapt.getAddress());
        bl_start = true;

        mStartBtn.setText("STOP");
        mStartBtn.setVisibility(View.GONE);
        mTextViewStat.setText("Running");

        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(searchDevices, intent);

        if (btAdapt.isDiscovering())
            btAdapt.cancelDiscovery();
        Object[] lstDevice = btAdapt.getBondedDevices().toArray();
        for (int i = 0; i < lstDevice.length; i++) {
            BluetoothDevice device = (BluetoothDevice) lstDevice[i];
            String str = "已配对|" + device.getName() + "|"
                    + device.getAddress();
            appendLog(str + "\n");
        }
        foundList.clear();
        appendLog("本机蓝牙地址：" + btAdapt.getAddress() + "\n");
        btAdapt.startDiscovery();

        return rootView;
    }
}
