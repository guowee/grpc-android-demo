package com.missile.service.fragment;


import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.missile.service.R;
import com.missile.service.grpc.GreeterGrpcService;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class GrpcFragment extends Fragment {
    private final static String TAG = "GRPCServer";
    private TextView mTextView;
    private TextView mTextViewStat;
    private Button mStartBtn;
    private TextView mIpTextView;

    public static GrpcFragment newInstance() {
        GrpcFragment fragment = new GrpcFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public GrpcFragment() {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_grpc, container,
                false);
        mTextView = (TextView) rootView.findViewById(R.id.log_cat);
        mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        mTextView.setGravity(Gravity.BOTTOM);
        mTextViewStat = (TextView) rootView.findViewById(R.id.state);
        mStartBtn = (Button) rootView.findViewById(R.id.btn_start);
        mIpTextView = (TextView) rootView.findViewById(R.id.addr);
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (GreeterGrpcService.getRunning()) {
                    Intent service = new Intent(getActivity(), GreeterGrpcService.class);
                    getActivity().stopService(service);
                    mStartBtn.setText("START");
                    mTextViewStat.setText("STOP");
                } else {
                    Intent service = new Intent(getActivity(), GreeterGrpcService.class);
                    getActivity().startService(service);
                    mStartBtn.setText("STOP");
                    mTextViewStat.setText("Running");
                    mStartBtn.setVisibility(View.GONE);
                }
            }
        });

        String strType = getNetworkType();
        String strip = getLocalIpAddress();
        if (!strType.equals("wifi") && !strType.equals("ethernet")) {
            appendLog("请打开WIFI或者以太网并正常连接\n");
        }

        mIpTextView.setText(strip);


        if (GreeterGrpcService.getRunning()) {
            mStartBtn.setText("STOP");
            mTextViewStat.setText("Running");
            mStartBtn.setVisibility(View.GONE);
        } else {
            mStartBtn.setText("START");
            mTextViewStat.setText("STOP");
        }

        return rootView;
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                            && !inetAddress.isLinkLocalAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {

        }

        return "";
    }

    /**
     * 获取当前网络类型
     *
     * @return 返回当前网络类型 为 null mobile wifi ethernet
     */
    public String getNetworkType() {
        String netType = "null";
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return netType;
        }
        int nType = networkInfo.getType();
        if (nType == ConnectivityManager.TYPE_MOBILE) {
            netType = "mobile";
        } else if (nType == ConnectivityManager.TYPE_WIFI) {
            netType = "wifi";
        } else if (nType == ConnectivityManager.TYPE_ETHERNET) {
            netType = "ethernet";
        }
        return netType;
    }

}
