package com.missile.service.grpc;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.missile.service.MainActivity;
import com.missile.service.dialog.ShowQRCodeDialog;
import com.missile.service.utils.Utils;

import java.util.HashMap;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;


public class GreeterGrpcService extends IntentService {

    private static final String TAG = "GreeterGrpcService";
    private Server server;
    private static boolean start = false;
    private static Context mContext;
    private static HeaderServerInterceptor mHeaderServerInterceptor;
    private static ShowQRCodeDialog mScanPicDialog = null;

    public GreeterGrpcService() {
        super("GreeterGrpcService");
    }

    public GreeterGrpcService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.e(TAG, "=====GreeterGrpcService Start=====");
        Utils.setContext(mContext);
        mScanPicDialog = new ShowQRCodeDialog(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stop();
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            mHeaderServerInterceptor = new HeaderServerInterceptor();
            Thread.currentThread().setContextClassLoader(getClass().getC‌​lassLoader());
            server = ServerBuilder.forPort(50051)
                    .addService(ServerInterceptors.intercept(new GreeterImpl(), mHeaderServerInterceptor))
                    .build()
                    .start();
            start = true;
            blockUntilShutdown();
            start = false;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static boolean getRunning() {
        return start;
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
            start = false;
        }
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
            start = false;
        }
    }

    private static String getRunningActivityName() {

        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);

        String runningActivity = activityManager.getRunningTasks(1).get(0).topActivity.getClassName();

        return runningActivity;

    }

    public class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + request.getName()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void bluetoothBond(BondRequest request, StreamObserver<BondReply> responseObserver) {

            if (!getRunningActivityName().equals("com.missile.service.MainActivity")) {
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                SystemClock.sleep(500);
            }
            MainActivity.setBluetoothFragment();
            String ret = MainActivity.bluetoothBond(request.getMac(), request.getInsecure());
            BondReply reply = BondReply.newBuilder().setRet(ret).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();

        }

        @Override
        public void ctrlBluetooth(CtrlRequest request, StreamObserver<CtrlReply> responseObserver) {
            if (!getRunningActivityName().equals("com.missile.service.MainActivity")) {
                Intent intent = new Intent(mContext, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                SystemClock.sleep(500);
            }
            MainActivity.setBluetoothFragment();
            String ret = MainActivity.ctrlBluetooth(request.getMac(), request.getCtrl());
            CtrlReply reply = CtrlReply.newBuilder().setRet(ret).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void showScanPic(ScanRequest request, StreamObserver<ScanReply> responseObserver) {
            String ret = mScanPicDialog.setImageShowPic(request.getPic());
            ScanReply reply = ScanReply.newBuilder().setRet(ret).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

    public class HeaderServerInterceptor implements ServerInterceptor {

        Metadata mRequestHeaders;
        Metadata mSendRequestHeaders;

        private Metadata.Key<String> customSNKey =
                Metadata.Key.of("sn", Metadata.ASCII_STRING_MARSHALLER);

        private Metadata.Key<String> customIPKey =
                Metadata.Key.of("ip", Metadata.ASCII_STRING_MARSHALLER);


        public HashMap<String, String> getRequestHeaders() {
            HashMap<String, String> map = new HashMap<String, String>();
            if (mRequestHeaders != null) {
                for (String key : mRequestHeaders.keys()) {
                    try {
                        String value = mRequestHeaders.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
                        if (value == null) value = "";
                        map.put(key, value);
                    } catch (Exception e) {
                    }
                }
            }
            return map;
        }

        public Metadata setRequestHeaders(HashMap<String, String> headerMap) {
            mSendRequestHeaders = new Metadata();
            if (headerMap != null) {
                for (String key : headerMap.keySet()) {
                    Metadata.Key<String> customHeadKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                    mSendRequestHeaders.put(customHeadKey, headerMap.get(key));
                }
            }
            return mSendRequestHeaders;
        }

        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
            mRequestHeaders = headers;
            HashMap<String, String> map = getRequestHeaders();
            return next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                @Override
                public void sendHeaders(Metadata responseHeaders) {
                    responseHeaders.put(customSNKey, Utils.getSN());
                    responseHeaders.put(customIPKey, Utils.getIP());
                    super.sendHeaders(responseHeaders);
                }
            }, headers);
        }
    }


}
