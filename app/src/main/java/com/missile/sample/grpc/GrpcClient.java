package com.missile.sample.grpc;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.missile.sample.utils.Utils;
import com.missile.service.grpc.BondReply;
import com.missile.service.grpc.BondRequest;
import com.missile.service.grpc.CtrlReply;
import com.missile.service.grpc.CtrlRequest;
import com.missile.service.grpc.GreeterGrpc;
import com.missile.service.grpc.HelloReply;
import com.missile.service.grpc.HelloRequest;
import com.missile.service.grpc.ScanReply;
import com.missile.service.grpc.ScanRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public class GrpcClient {

    public static Map<String, String> getHeader() {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("sn", Utils.getSN());
        headerMap.put("ip", Utils.getIP());
        return headerMap;
    }

    public static String sayHello(String name) throws InterruptedException {
        Map<String, String> headerMap = getHeader();
        ClientInterceptor interceptor = new HeaderClientInterceptor(headerMap);
        ManagedChannel managedChannel = getManagedChannel();
        Channel channel = ClientInterceptors.intercept(managedChannel, interceptor);
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
        HelloRequest message = HelloRequest.newBuilder()
                .setName(name)
                .build();
        HelloReply reply = stub.sayHello(message);
        //server headerMap
        managedChannel.shutdown()
                .awaitTermination(1, TimeUnit.SECONDS);
        return reply.getMessage();
    }

    public static String bluetoothBond(String mac, boolean insecure)
            throws Exception {
        Map<String, String> headerMap = getHeader();
        ClientInterceptor interceptor = new HeaderClientInterceptor(headerMap);
        ManagedChannel managedChannel = getManagedChannel();
        Channel channel = ClientInterceptors.intercept(managedChannel, interceptor);
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
        BondRequest message = BondRequest.newBuilder()
                .setMac(mac)
                .setInsecure(insecure)
                .build();
        BondReply reply = stub.bluetoothBond(message);
        //server headerMap
        managedChannel.shutdown()
                .awaitTermination(1, TimeUnit.SECONDS);
        return reply.getRet();
    }


    public static String ctrlBluetooth(String mac, int ctrl)
            throws Exception {
        Map<String, String> headerMap = getHeader();
        ClientInterceptor interceptor = new HeaderClientInterceptor(headerMap);
        ManagedChannel managedChannel = getManagedChannel();
        Channel channel = ClientInterceptors.intercept(managedChannel, interceptor);
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
        CtrlRequest message = CtrlRequest.newBuilder()
                .setMac(mac)
                .setCtrl(ctrl)
                .build();
        CtrlReply reply = stub.ctrlBluetooth(message);
        //server headerMap
        managedChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        return reply.getRet();
    }


    public static String showScanPic(String pic) throws Exception {
        Map<String, String> headerMap = getHeader();
        ClientInterceptor interceptor = new HeaderClientInterceptor(headerMap);
        ManagedChannel managedChannel = getManagedChannel();
        Channel channel = ClientInterceptors.intercept(managedChannel, interceptor);
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
        //发送网络请求信息，显示相应图片
        ScanRequest message = ScanRequest.newBuilder().setPic(pic).build();
        //返回网络请求的响应
        ScanReply reply = stub.showScanPic(message);
        //server headerMap
        managedChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        //获取响应信息
        return reply.getRet();
    }


    static class HeaderClientInterceptor
            implements ClientInterceptor {

        private static final String TAG = "HeaderClientInterceptor";

        private Map<String, String> mHeaderMap;

        public HeaderClientInterceptor(Map<String, String> headerMap) {
            mHeaderMap = headerMap;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                                   CallOptions callOptions,
                                                                   Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    /* put custom header */
                    if (mHeaderMap != null) {
                        for (String key : mHeaderMap.keySet()) {
                            Metadata.Key<String> customHeadKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                            headers.put(customHeadKey, mHeaderMap.get(key));
                        }
                    }
                    Log.i(TAG, "header send to server:" + headers);
                    super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                            responseListener) {
                        @Override
                        public void onHeaders(Metadata headers) {
                            /**
                             * if you don't need receive header from server,
                             * you can use {@link io.grpc.stub.MetadataUtils attachHeaders}
                             * directly to send header
                             */
                            Log.i(TAG, "header received from server:" + headers);
                            super.onHeaders(headers);
                        }
                    }, headers);
                }
            };
        }
    }

    private static ManagedChannel getManagedChannel() {
        return ManagedChannelBuilder.forAddress(Utils.getServiceIP(), Utils.getServicePort()).usePlaintext(true).build();
    }


}
