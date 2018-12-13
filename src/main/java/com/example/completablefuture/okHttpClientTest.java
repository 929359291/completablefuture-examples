package com.example.completablefuture;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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

    @Test
    public void test1(){
        List<Person> list =  new ArrayList();
//        for (int i = 0; i < 10; i++) {
//            Person person = new Person();
//            person.setKey("key" + i);
//            person.setValue("value" + i);
//            list.add(person);
//        }
//        Person valueNull = new Person();
//        valueNull.setKey("key+");
//        list.add(valueNull);
//        System.out.println(list);

        Map<String, Object> collect = list.stream().map(arr -> {
            Map<String, Object> resp = new HashMap();
//            resp.put(arr.getKey(), arr.getValue());
            return resp.entrySet().iterator().next();
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        System.out.println("==========================");

        System.out.println(collect);
    }

    @Test
    public void test01(){
        Map<String, Object> map = new HashMap<>();
        map.entrySet().iterator().next();
    }

    public static  class Person{
        private String key;
        private String value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Person person = (Person) o;
            return Objects.equals(key, person.key) && Objects.equals(value, person.value);
        }

        @Override
        public int hashCode() {

            return Objects.hash(key, value);
        }

        @Override
        public String toString() {
            return "Person{" + "key='" + key + '\'' + ", value='" + value + '\'' + '}';
        }


    }
}
