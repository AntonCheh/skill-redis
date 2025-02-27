package com.skillbox.redisdemo;

import org.redisson.Redisson;
import org.redisson.api.RKeys;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisConnectionException;
import org.redisson.codec.KryoCodec;
import org.redisson.config.Config;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.lang.System.out;

public class RedisStorage {

    // Объект для работы с Redis
    private RedissonClient redisson;

    // Объект для работы с ключами
    private RKeys rKeys;

    // Объект для работы с Sorted Set'ом
    private RScoredSortedSet<String> onlineUsers;

    private final static String KEY = "ONLINE_USERS";

    private double getTs() {

        return new Date().getTime() / 1000;
    }

    // Пример вывода всех ключей
    public void listKeys() {
        Iterable<String> keys = rKeys.getKeys();
        for(String key: keys) {
            out.println("KEY: " + key + ", type:" + rKeys.getType(key));
        }
    }

    void init() {
        Config config = new Config();
        config.setCodec(new KryoCodec());
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        try {
            redisson = Redisson.create(config);
        } catch (RedisConnectionException Exc) {
            out.println("Не удалось подключиться к Redis");
            out.println(Exc.getMessage());
        }
        rKeys = redisson.getKeys();
        onlineUsers = redisson.getScoredSortedSet(KEY);
        rKeys.delete(KEY);

        //инициализация пользователей
        for (int i = 1; i <= 20; i++){
            onlineUsers.add(getTs(), String.valueOf(i));
        }
    }

    void shutdown() {
        redisson.shutdown();
    }

    // Фиксирует посещение пользователем страницы
    void logPageVisit(int user_id)
    {
        //ZADD ONLINE_USERS
        onlineUsers.add(getTs(), String.valueOf(user_id));
    }

    // Удаляет
    void deleteOldEntries(int secondsAgo)
    {
        //ZREVRANGEBYSCORE ONLINE_USERS 0 <time_5_seconds_ago>
        onlineUsers.removeRangeByScore(0, true, getTs() - secondsAgo, true);
    }

    int calculateUsersNumber()
    {
        //ZCOUNT ONLINE_USERS
        return onlineUsers.count(Double.NEGATIVE_INFINITY, true, Double.POSITIVE_INFINITY, true);
    }

    // Эмуляция работы сайта знакомств
    public void simulate() {
        Random random = new Random();

        while (true) {
            if (onlineUsers.isEmpty()) {
                out.println("Очередь пуста.");
                break;
            }

            // Показать первого пользователя
            String userId = onlineUsers.first();
            if (userId != null) {
                out.println("— На главной странице показываем пользователя " + userId);
                // Переместить пользователя в конец очереди
                onlineUsers.add(getTs(), userId);
            }

            // С вероятностью 1/10 переместить случайного пользователя в начало очереди
            if (random.nextInt(10) == 0) {
                int paidUserId = random.nextInt(20) + 1;
                if (onlineUsers.contains(String.valueOf(paidUserId))) {
                    out.println("> Пользователь " + paidUserId + " оплатил платную услугу");
                    onlineUsers.add(getTs() - 1, String.valueOf(paidUserId)); // Переместить в начало очереди
                }
            }

            // Пауза на 1 секунду
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
