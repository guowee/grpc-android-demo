package com.missile.sample;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.missile.sample.action.ExtendAction;
import com.missile.sample.aidl.ServiceApi;
import com.missile.sample.grpc.GrpcClient;
import com.missile.sample.settings.SettingsActivity;
import com.missile.sample.utils.Display;
import com.missile.sample.utils.MLogger;
import com.missile.sample.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Context mContext;
    private AppCompatButton sayHelloButton;
    private AppCompatButton bindBluetoothButton;
    private AppCompatButton ctrlBluetooth0Button;
    private AppCompatButton ctrlBluetooth1Button;
    private AppCompatButton ctrlBluetooth2Button;
    private AppCompatButton showQRButton;
    private AppCompatButton getVersionButton;
    private AppCompatButton rebootButton;
    public TextView logcatView;

    private static Runnable runnable;
    private static boolean bl_timeout = false;

    private Handler mHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        Utils.setContext(mContext);
        ExtendAction.setContext(mContext);
        Display.setContext(this);
        MLogger.setContext(mContext);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sayHelloButton = findViewById(R.id.btn_say_hello);
        sayHelloButton.setOnClickListener(this);
        bindBluetoothButton = findViewById(R.id.btn_bind_bt);
        bindBluetoothButton.setOnClickListener(this);
        ctrlBluetooth0Button = findViewById(R.id.btn_ctrl_bt_0);
        ctrlBluetooth0Button.setOnClickListener(this);
        ctrlBluetooth1Button = findViewById(R.id.btn_ctrl_bt_1);
        ctrlBluetooth1Button.setOnClickListener(this);
        ctrlBluetooth2Button = findViewById(R.id.btn_ctrl_bt_2);
        ctrlBluetooth2Button.setOnClickListener(this);
        showQRButton = findViewById(R.id.btn_scan_qr);
        showQRButton.setOnClickListener(this);
        getVersionButton = findViewById(R.id.btn_get_version);
        getVersionButton.setOnClickListener(this);
        rebootButton = findViewById(R.id.btn_reboot);
        rebootButton.setOnClickListener(this);

        logcatView = findViewById(R.id.log_cat);
        logcatView.setMovementMethod(ScrollingMovementMethod.getInstance());
        logcatView.setGravity(Gravity.BOTTOM);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_say_hello:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sayHello("James");
                    }
                }).start();
                break;
            case R.id.btn_bind_bt:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        bondBluetooth();
                    }
                }).start();
                break;
            case R.id.btn_ctrl_bt_0:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ctrlBluetooth(0);
                    }
                }).start();
                break;
            case R.id.btn_ctrl_bt_1:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ctrlBluetooth(1);
                    }
                }).start();
                break;
            case R.id.btn_ctrl_bt_2:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ctrlBluetooth(2);
                    }
                }).start();
                break;
            case R.id.btn_scan_qr:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        showScanPic();
                    }
                }).start();
                break;
            case R.id.btn_get_version:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String version = getServiceVersion();
                        Display.appendInfo(version);
                    }
                }).start();
                break;
            case R.id.btn_reboot:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        reboot();
                    }
                }).start();
                break;
            default:
                break;
        }
    }

    private void sayHello(String name) {
        String ret = ExtendAction.sayHello(name);
        Display.appendInfo(ret);
    }


    private void ctrlBluetooth(int ctrl) {
        String str = null;
        if (ctrl == 0) {
            str = "ctrlBluetooth  found";
        } else if (ctrl == 1) {
            str = "ctrlBluetooth  paring";
        } else if (ctrl == 2) {
            str = "ctrlBluetooth  remove";
        }
        Display.appendInfo(str);
        String ret = ExtendAction.ctrlBluetooth(ctrl);
        Display.appendInfo("ret " + ret);
    }

    private void bondBluetooth() {
        int datalen = 1024;
        byte[] data = new byte[datalen];
        byte[] recdata = new byte[datalen];
        int CNT = 256;
        final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
        UUID uuid = UUID.fromString(SPP_UUID);
        boolean insecure = false;
        Display.appendInfo("GrpcClient bluetoothBond");
        BluetoothDevice device = ExtendAction.bluetoothBond(insecure);
        if (device == null) {
            return;
        }

        for (int i = 0; i < datalen; i++) {
            data[i] = (byte) (i % 256);
        }

        BluetoothSocket btSocket;
        InputStream in = null;
        OutputStream out = null;
        try {
            if (insecure) {
                Display.appendInfo("insecure");
                btSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            } else {
                btSocket = device.createRfcommSocketToServiceRecord(uuid);
            }
            Display.appendInfo("socket");
            Thread.sleep(100);
            try {
                btSocket.connect();
                Display.appendInfo("connect");
            } catch (IOException e) {
                Display.appendInfo("connectd");
                btSocket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
                btSocket.connect();
            }
            Thread.sleep(100);
            in = btSocket.getInputStream();
            out = btSocket.getOutputStream();
            Display.appendInfo("testing...");
            for (int i = 0; i < CNT; i++) {
                Display.appendInfo("testing cnt " + i);
                out.write(data, 0, datalen);
                out.flush();
                int readCount = 0;
                if (in.available() < datalen) {
                    bl_timeout = false;
                    runnable = new Runnable() {
                        @Override
                        public void run() {
                            Display.appendInfo("TIMEOUT");
                            bl_timeout = true;
                        }
                    };
                    if (mHandler != null)
                        mHandler.postDelayed(runnable, 3000);

                    while (readCount < datalen && !bl_timeout) {
                        readCount += in.read(recdata, readCount, datalen - readCount);
                    }
                    if (mHandler != null)
                        mHandler.removeCallbacks(runnable);
                } else {
                    readCount = in.read(recdata, 0, datalen);
                }

                if (readCount != datalen) {
                    in.close();
                    out.close();
                    btSocket.close();
                    return;
                }

                for (int j = 0; j < datalen; j++) {
                    if (recdata[j] != data[j]) {
                        in.close();
                        out.close();
                        btSocket.close();
                        return;
                    }
                }
            }
            in.close();
            out.close();
            btSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void showScanPic() {
        Display.appendInfo("Show QR Code Picture\n");
        String str = ExtendAction.showScanPic("QR Code_137101_470x642.PNG");
        if (str.length() < 1) {
            return;
        }
        Display.appendInfo("str " + str);
    }

    private String getServiceVersion() {
        ServiceApi serviceApi = AppApplication.getServiceApi();
        Log.e("TAG", "Service State: " + serviceApi.getApiReady());
        if (serviceApi.getApiReady()) {
            return serviceApi.getVersion();
        }
        return null;
    }

    private void reboot() {
        Display.appendInfo("reboot");
        SystemClock.sleep(100);
        ServiceApi serviceApi = AppApplication.getServiceApi();
        serviceApi.reboot(false, null);
        while (true) {
            ;
        }
    }

}
