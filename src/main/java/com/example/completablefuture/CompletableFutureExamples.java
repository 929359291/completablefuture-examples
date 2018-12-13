package com.example.completablefuture;

import org.junit.Test;
import org.junit.experimental.theories.suppliers.TestedOn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class CompletableFutureExamples {

    static ExecutorService executor = Executors.newFixedThreadPool(6, new ThreadFactory() {
        int count = 1;

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "custom-executor-" + count++);
        }
    });

    static Random random = new Random();

    // 使用一个预定义的结果创建一个完成的CompletableFuture,通常我们会在计算的开始阶段使用它。
    @Test
    public void completedFutureExample() {
        CompletableFuture<String> cf = CompletableFuture.completedFuture("message");
        assertTrue(cf.isDone());
        assertEquals("message", cf.getNow(null));
    }

    // 处理要抛出的异常，可做分支处理，正确与错误
    // 如果尚未完成，则会调用get（）和相关方法来抛出给定的异常。
    @Test
    public void completeExceptionallyExample() {
        CompletableFuture<String> cf = CompletableFuture.completedFuture("message").thenApplyAsync(String::toUpperCase, CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));
        CompletableFuture<String> exceptionHandler = cf.handle((s, th) -> (th != null) ? "message upon cancel" : "");
        cf.completeExceptionally(new RuntimeException("completed exceptionally"));
        assertTrue("Was not completed exceptionally", cf.isCompletedExceptionally());
        try {
            cf.join();
            fail("Should have thrown an exception");
        } catch (CompletionException ex) { // just for testing
            assertEquals("completed exceptionally", ex.getCause().getMessage());
            System.out.println(ex.getCause().getMessage());
        }

        assertEquals("message upon cancel", exceptionHandler.join());
    }

    //CompletableFuture的方法如果以Async结尾，它会异步的执行(没有指定executor的情况下)， 异步执行通过ForkJoinPool实现， 它使用守护线程去执行任务。注意这是CompletableFuture的特性， 其它CompletionStage可以override这个默认的行为。
    @Test
    public void runAsyncExample() {
        CompletableFuture<Void> cf = null;
        for (int i = 0; i < 100; i++) {
            cf = CompletableFuture.runAsync(okHttpClientTest::requestOkHttp);
        }
        System.out.println(cf.join());
        System.out.println(ForkJoinPool.commonPool().getPoolSize());

    }

    /**
     * then意味着这个阶段的动作发生当前的阶段正常完成之后。本例中，当前节点完成，返回字符串message。
     * <p>
     * Apply意味着返回的阶段将会对结果前一阶段的结果应用一个函数。
     * <p>
     * 函数的执行会被阻塞，这意味着getNow()只有大写操作被完成后才返回
     */
    @Test
    public void thenApplyExample() {
        CompletableFuture<String> cf = CompletableFuture.completedFuture("message").thenApply(s -> {
            assertFalse(Thread.currentThread().isDaemon());
            return s.toUpperCase();
        });
        assertEquals("MESSAGE", cf.getNow(null));
    }

    /**
     * 在前一个阶段上异步应用函数
     * <p>
     * 通过调用异步方法(方法后边加Async后缀)，串联起来的CompletableFuture可以异步地执行（使用ForkJoinPool.commonPool()）。
     */
    @Test
    public void thenApplyAsyncExample() {
        CompletableFuture<String> cf = CompletableFuture.completedFuture("message").thenApplyAsync(s -> {
            assertTrue(Thread.currentThread().isDaemon());
            return s.toUpperCase();
        });
        assertNull(cf.getNow(null));
        System.out.println(cf.getNow(null));
        assertEquals("MESSAGE", cf.join());
        System.out.println(cf.join());
    }

    //异步方法一个非常有用的特性就是能够提供一个Executor来异步地执行CompletableFuture。这个例子演示了如何使用一个固定大小的线程池来应用大写函数。
    @Test
    public void thenApplyAsyncWithExecutorExample() {
        CompletableFuture<String> cf = CompletableFuture.completedFuture("message").thenApplyAsync(s -> {
            assertTrue(Thread.currentThread().getName().startsWith("custom-executor-"));
            assertFalse(Thread.currentThread().isDaemon());
            return s.toUpperCase();
        }, executor);

        assertNull(cf.getNow(null));
        assertEquals("MESSAGE", cf.join());
    }

    //如果下一阶段接收了当前阶段的结果，但是在计算的时候不需要返回值(它的返回类型是void)，
    // 那么它可以不应用一个函数，而是一个消费者， 调用方法也变成了thenAccept:
    @Test
    public void thenAcceptExample() {
        StringBuilder result = new StringBuilder();
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            list.add(i + 1);
        }
        CompletableFuture.completedFuture(list).thenAccept(result::append);
        assertTrue("Result was empty", result.length() > 0);
        System.out.println(result.toString());
    }

    // 异步消费
    @Test
    public void thenAcceptAsyncExample() {
        StringBuilder result = new StringBuilder();
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            list.add(i + 1);
        }
        List<String> list2 = new ArrayList<>() {{
            add("a");
            add("b");
        }};
        CompletableFuture<Void> cf = CompletableFuture.completedFuture(list).thenAcceptAsync(result::append, executor);
        cf.join();
        assertTrue("Result was empty", result.length() > 0);
        System.out.println(result.toString());
    }

    //和完成异常类似，我们可以调用cancel(boolean mayInterruptIfRunning)取消计算。对于CompletableFuture类，布尔参数并没有被使用，这是因为它并没有使用中
    // 断去取消操作，相反，cancel等价于completeExceptionally(new CancellationException())。
    @Test
    public void cancelExample() {
        CompletableFuture<String> cf = CompletableFuture.completedFuture("message").thenApplyAsync(String::toUpperCase, CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS));
        CompletableFuture<String> cf2 = cf.exceptionally(throwable -> "canceled message");
        assertTrue("Was not canceled", cf.cancel(true));
        assertTrue("Was not completed exceptionally", cf.isCompletedExceptionally());
        assertEquals("canceled message", cf2.join());
    }

    private List<Object> testList = new ArrayList<>();

    /**
     * 返回一个与此阶段具有相同结果或异常的新CompletionStage，当此阶段完成时，
     * 执行给定操作使用此阶段的默认异步执行工具执行给定操作，结果（如果没有则为null）和异常（或 如果没有，则将此阶段作为参数。
     */
    @Test
    public void whenCompleteExample() {
        for (int i = 0; i < 20; i++) {
            testList.add(i);
        }

        List<Object> values = new ArrayList<>();
        CompletableFuture.completedFuture(testList).whenCompleteAsync((list, throwable) -> {
            if (throwable == null) {
                for (int i = 20; i < 40; i++) {
                    list.add(i);
                    if (i == 21){
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }, executor).thenApplyAsync(this::changeResultType).thenAcceptAsync(values::addAll).join();

        System.out.println(values);
    }

    private List<Object> changeResultType(List<Object> list) {
        ArrayList<Object> objects = new ArrayList<>();
        for (Object object : list) {
            objects.add(String.valueOf(object) + "-x");
        }
        return objects;
    }

    /**
     * 在两个完成的阶段其中之一上应用函数
     *  在其中之一上应用函数(保证哪一个被执行)。 本例中的两个阶段一个是应用大写转换在原始的字符串上， 另一个阶段是应用小些转换。
     */
    @Test
    public void applyToEitherExample() throws ExecutionException, InterruptedException {
        String original = "Message";
        CompletableFuture<String> cf = CompletableFuture.completedFuture(original)
                                        .thenApplyAsync(s -> delayedUpperCase(s))
                                        .applyToEither(CompletableFuture.completedFuture(original)
                                        .thenApplyAsync(s -> delayedLowerCase(s)), s -> s + " from applyToEither");

//        CompletableFuture<String> cf1 = CompletableFuture.completedFuture(original)
//                                        .thenApplyAsync(s -> delayedUpperCase(s));
//        CompletableFuture<String> cf = cf1.applyToEither(
//                CompletableFuture.completedFuture(original).thenApplyAsync(s -> delayedLowerCase(s)),
//                s -> s + " from applyToEither");
        assertTrue(cf.join().endsWith(" from applyToEither"));
        System.out.println(cf.get());
    }

    /**
     * 在两个完成的阶段其中之一上调用消费函数
     */
    @Test
    public void acceptEitherExample() {
        String original = "Message";
        StringBuilder result = new StringBuilder();
        CompletableFuture<Void> cf = CompletableFuture.completedFuture(original)
                                            .thenApplyAsync(s -> delayedUpperCase(s))
                                            .acceptEither(CompletableFuture.completedFuture(original)
                                            .thenApplyAsync(s -> delayedLowerCase(s)), s -> result.append(s).append("acceptEither"));
        cf.join();
        assertTrue("Result was empty", result.toString().endsWith("acceptEither"));
        System.out.println(result.toString());
    }

    /**
     * 在两个阶段都执行完后运行一个 Runnable
     * 注意下面所有的阶段都是同步执行的，第一个阶段执行大写转换，第二个阶段执行小写转换。
     */
    @Test
    public void runAfterBothExample() {
        String original = "Message";
        StringBuilder result = new StringBuilder();
        CompletableFuture<Void> done = CompletableFuture.completedFuture(original)
                                        .thenApply(String::toUpperCase)
                                        .runAfterBoth(CompletableFuture.completedFuture(original)
                                        .thenApply(String::toLowerCase), () -> result.append("done"));
        assertTrue("Result was empty", result.length() > 0);
        System.out.println(result);
    }

    /**
     * 使用BiConsumer处理两个阶段的结果
     */
    @Test
    public void thenAcceptBothExample() {
        String original = "Message";
        StringBuilder result = new StringBuilder();
        CompletableFuture.completedFuture(original)
                            .thenApply(String::toUpperCase)
                            .thenAcceptBoth(CompletableFuture.completedFuture(original)
                                      .thenApply(String::toLowerCase), (s1, s2) -> result.append(s1).append(s2));
        assertEquals("MESSAGEmessage", result.toString());
        System.out.println(result);
    }

    /**
     * 使用BiFunction处理两个阶段的结果
     *
     * 如果CompletableFuture依赖两个前面阶段的结果， 它复合两个阶段的结果再返回一个结果，
     * 我们就可以使用thenCombine()函数。整个流水线是同步的，所以getNow()会得到最终的结果，它把大写和小写字符串连接起来。
     */
    @Test
    public void thenCombineExample() {
        String original = "Message";
        CompletableFuture<String> cf = CompletableFuture.completedFuture(original)
                                        .thenApply(CompletableFutureExamples::delayedUpperCase)
                                        .thenCombine(CompletableFuture.completedFuture(original)
                                                .thenApply(CompletableFutureExamples::delayedLowerCase), (s1, s2) -> s1 + s2);
        assertEquals("MESSAGEmessage", cf.getNow(null));
    }

    /**
     * 异步使用BiFunction处理两个阶段的结果
     *
     * 类似上面的例子，但是有一点不同： 依赖的前两个阶段异步地执行，所以thenCombine()也异步地执行，即时它没有Async后缀。
     */
    @Test
    public void thenCombineAsyncExample() {
        String original = "Message";
        CompletableFuture<String> cf = CompletableFuture.completedFuture(original)
                                        .thenApplyAsync(s -> delayedUpperCase(s))
                                        .thenCombine(CompletableFuture.completedFuture(original)
                                                .thenApplyAsync(s -> delayedLowerCase(s)), (s1, s2) -> s1 + s2);
        assertEquals("MESSAGEmessage", cf.join());
    }

    /**
     * 组合 CompletableFuture
     *
     * 我们可以使用thenCompose()完成上面两个例子。这个方法等待第一个阶段的完成(大写转换)，
     * 它的结果传给一个指定的返回CompletableFuture函数，它的结果就是返回的CompletableFuture的结果。
     */
    @Test
    public void thenComposeExample() {
        String original = "Message";
        CompletableFuture<String> cf = CompletableFuture.completedFuture(original)
                                                        .thenApply(s -> delayedUpperCase(s))
                                                        .thenCompose(upper -> CompletableFuture.completedFuture(original)
                                                                .thenApply(s -> delayedLowerCase(s))
                                                                .thenApply(s -> upper + s));
        assertEquals("MESSAGEmessage", cf.join());
    }

    @Test
    public void thenComposeAsyncExample() {
        String original = "Message";
        CompletableFuture<String> cf = CompletableFuture.completedFuture(original)
                                                        .thenApplyAsync(s -> delayedUpperCase(s))
                                                        .thenCompose(upper -> CompletableFuture.completedFuture(original)
                                                                .thenApplyAsync(s -> delayedLowerCase(s))
                                                                .thenApplyAsync(s -> upper + s));
        assertEquals("MESSAGEmessage", cf.join());
    }

    /**
     * 当几个阶段中的一个完成，创建一个完成的阶段
     *
     * 当任意一个CompletableFuture完成后， 创建一个完成的CompletableFuture.
     *
     * 待处理的阶段首先创建， 每个阶段都是转换一个字符串为大写。因为本例中这些阶段都是同步地执行(thenApply),
     * 从anyOf中创建的CompletableFuture会立即完成，这样所有的阶段都已完成，我们使用whenComplete(BiConsumer<? super Object, ? super Throwable> action)处理完成的结果。
     */
    @Test
    public void anyOfExample() {
        StringBuilder result = new StringBuilder();
        List<String> messages = Arrays.asList("a", "b", "c");

        CompletableFuture.anyOf(messages.stream()
                            .map(msg -> CompletableFuture.completedFuture(msg)
                                    .thenApply(CompletableFutureExamples::delayedUpperCase))
                                    .toArray(CompletableFuture[]::new)).whenComplete((res, th) -> {

            if (th == null) {
                assertTrue(isUpperCase((String) res));
                result.append(res);
            }
        });
        assertTrue("Result was empty", result.length() > 0);
        System.out.println(result.toString());
    }

    /**
     * 当所有的阶段都完成后创建一个阶段
     *
     * 上一个例子是当任意一个阶段完成后接着处理，
     * 接下来的两个例子演示当所有的阶段完成后才继续处理, 同步地方式和异步地方式两种。
     */
    @Test
    public void allOfExample() {
        StringBuilder result = new StringBuilder();
        List<String> messages = Arrays.asList("a", "b", "c");
        List<CompletableFuture<String>> futures = messages.stream()
                                                .map(msg -> CompletableFuture.completedFuture(msg)
                                                        .thenApply(s -> delayedUpperCase(s)))
                                                        .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((v, th) -> {
            futures.forEach(cf -> assertTrue(isUpperCase(cf.getNow(null))));
            result.append("done");
            result.append(futures.stream().map(list -> list.getNow(null)).collect(Collectors.toList()));
        });
        assertTrue("Result was empty", result.length() > 0);
        System.out.println(result.toString());
    }

    /**
     * 当所有的阶段都完成后异步地创建一个阶段
     * 使用thenApplyAsync()替换那些单个的CompletableFutures的方法，
     * allOf()会在通用池中的线程中异步地执行。所以我们需要调用join方法等待它完成。
     */
    @Test
    public void allOfAsyncExample() {
        StringBuilder result = new StringBuilder();
        List<String> messages = Arrays.asList("a", "b", "c");
        List<CompletableFuture<String>> futures = messages.stream()
                                                          .map(msg -> CompletableFuture.completedFuture(msg)
                                                                                       .thenApplyAsync(s -> delayedUpperCase(s)))
                                                                                       .collect(Collectors.toList());

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete((v, th) -> {
            futures.forEach(cf -> assertTrue(isUpperCase(cf.getNow(null))));
            result.append("done");
            result.append(futures.stream().map(list -> list.getNow(null)).collect(Collectors.toList()));
        });
        allOf.join();
        assertTrue("Result was empty", result.length() > 0);
        System.out.println(result.toString());
    }

    private static boolean isUpperCase(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLowerCase(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String delayedUpperCase(String s) {
        randomSleep();
        return s.toUpperCase();
    }

    private static String delayedLowerCase(String s) {
        randomSleep();
        return s.toLowerCase();
    }

    private static void randomSleep() {
        try {
            Thread.sleep(random.nextInt(5000));
            System.out.println("sleep 1000 seconds");
        } catch (InterruptedException e) {
            // ...
        }
    }

    private static void sleepEnough() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // ...
        }
    }

}
