package com.example.completablefuture;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author zengxc
 * @Date 2018/10/29
 */
public class CompletableFutureTest {

    public static void main(String[] args) throws Exception {
        Map hbaseData = getHbaseData();
        System.out.println(hbaseData);
    }

    public static Map getHbaseData(){
        Map<String, String> data = new HashMap<>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        data.put("key3", "value3");
        data.put("key4", "value4");

        CompletableFuture<Map<String, String>> completableFuture = new CompletableFuture<>();
        completableFuture.complete(data);

        final Map[] map = new Map[]{new HashMap<String, String>()};
        completableFuture.whenComplete((res, error) -> map[0] = res);
        return map[0];
    }

    public static void test1() throws Exception{
        CompletableFuture<String> completableFuture=new CompletableFuture();
        new Thread(() -> {
            //模拟执行耗时任务
            System.out.println("task doing...");
            try {
                Thread.sleep(3000);
                System.out.println("task sleep end or complete");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //告诉completableFuture任务已经完成
            completableFuture.complete("result");
        }).start();
        //获取任务结果，如果没有完成会一直阻塞等待
        String result=completableFuture.getNow(null);
//        String result=completableFuture.get();
        System.out.println("计算结果:"+result);

    }
}
