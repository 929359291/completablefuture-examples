package com.example.completablefuture;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.*;

/**
 * @author zengxc
 * @Date 2018/11/26
 */
public class okHttpClientTest {

    private static final OkHttpClient client;

    static {
        try {
            OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
            builder.connectTimeout(10,TimeUnit.SECONDS); // connect timeout
            builder.writeTimeout(10,TimeUnit.SECONDS); // socket timeout
            builder.readTimeout(30,TimeUnit.SECONDS); // socket timeout
            client = builder.build();
        } catch (Exception e) {
            System.out.println(String.format("config client failed.cause:%s",e));
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {

        ExecutorService executorService = Executors.newFixedThreadPool(50);

//        requestOkHttp();

        for (int i = 0; i < 10000; i++) {
            executorService.execute(okHttpClientTest::requestOkHttp);
        }


    }

    public static void requestOkHttp() {
        try {
            Request.Builder builder = new Request.Builder();
            Response response = client.newCall(builder.url("http://localhost:8082/tomcat/timeout").build()).execute();
            assert response.body() != null;
            System.out.println(response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
