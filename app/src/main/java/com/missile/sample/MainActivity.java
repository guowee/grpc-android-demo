package com.missile.sample;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.missile.sample.grpc.GrpcClient;
import com.missile.sample.settings.SettingsActivity;
import com.missile.sample.utils.Utils;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Context mContext;
    private AppCompatButton sayHelloButton;
    private TextView logcatView;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0x01:
                    appendLog((CharSequence) msg.obj);
                    break;
                default:
                    break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        Utils.setContext(mContext);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        sayHelloButton = findViewById(R.id.btn_say_hello);
        sayHelloButton.setOnClickListener(this);

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
                String result = sayHello("James");
                Message.obtain(mHandler, 0x01, result).sendToTarget();
                break;
            default:
                break;
        }
    }

    private String sayHello(String name) {
        try {
            return GrpcClient.sayHello(name);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void appendLog(CharSequence message) {
        if (logcatView != null) {
            if (!TextUtils.isEmpty(message)) {
                logcatView.append(message + "\n");
            }
        }
    }

}
