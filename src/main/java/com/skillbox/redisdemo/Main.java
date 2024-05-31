package com.skillbox.redisdemo;

public class Main {
    public static void main(String[] args) {
        RedisStorage storage = new RedisStorage();
        storage.init();
        storage.simulate();
        storage.shutdown();
    }
}

