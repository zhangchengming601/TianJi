package com.tianji.promotion;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Test {


    @org.junit.jupiter.api.Test
    public static void main(String[] args) throws ExecutionException, InterruptedException{
        CompletableFuture.supplyAsync(() -> {
            return 1;
        }).thenApply(f -> {
            return f + 2;
        }).thenApply(f -> {
            return f + 3;
        }).thenApply(f -> {
            return f + 4;
        }).thenAccept(r -> System.out.println(r));
    }
}
