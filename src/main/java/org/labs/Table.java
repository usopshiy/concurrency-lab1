package org.labs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

// Состояние одного обеда. Каждый запуск Dinner создаёт новый стол.
final class Table {
    private final Dinner.Config config;
    private final ReentrantLock[] spoons;
    private final BlockingQueue<CompletableFuture<Void>> orders;
    private final long[] eaten;
    private final long[] served;
    private long remainingPortions;

    Table(Dinner.Config config) {
        this.config = config;
        remainingPortions = config.portions();
        eaten = new long[config.programmers()];
        served = new long[config.waiters()];
        orders = new ArrayBlockingQueue<>(config.programmers(), true);

        spoons = new ReentrantLock[config.programmers()];
        for (int i = 0; i < spoons.length; i++) {
            spoons[i] = new ReentrantLock(true);
        }
    }

    void dine(int programmerId) throws InterruptedException, ExecutionException {
        long portions = portionLimit(programmerId);
        for (long meal = 0; meal < portions; meal++) {
            // Обсуждаем преподавателей, не удерживая ложки.
            Thread.yield();
            requestSoup();
            eat(programmerId);
        }
    }

    void serve(int waiterId) throws InterruptedException {
        while (true) {
            CompletableFuture<Void> order = orders.take();
            takePortion();
            served[waiterId]++;
            order.complete(null);
        }
    }

    private long portionLimit(int programmerId) {
        long portions = config.portions() / config.programmers();
        long extraPortions = config.portions() % config.programmers();
        if (programmerId < extraPortions) {
            portions++;
        }
        return portions;
    }

    private void requestSoup() throws InterruptedException, ExecutionException {
        CompletableFuture<Void> order = new CompletableFuture<>();
        orders.put(order);
        // Официант наполнит тарелку одной порцией. Ложки пока не занимаем.
        order.get();
    }

    private void eat(int programmerId) throws InterruptedException {
        int left = programmerId;
        int right = (programmerId + 1) % spoons.length;

        // Все берут ложки по возрастанию номера, чтобы не было цикла ожидания.
        ReentrantLock first = spoons[Math.min(left, right)];
        ReentrantLock second = spoons[Math.max(left, right)];
        first.lockInterruptibly();
        try {
            second.lockInterruptibly();
            try {
                eaten[programmerId]++;
            } finally {
                second.unlock();
            }
        } finally {
            first.unlock();
        }
    }

    private synchronized void takePortion() {
        if (remainingPortions == 0) {
            throw new IllegalStateException("Запас еды исчерпан раньше выполнения заказов");
        }
        remainingPortions--;
    }

    // Вызывается после остановки всех участников, когда счётчики уже не меняются.
    Dinner.Result result() {
        return new Dinner.Result(toList(eaten), toList(served), remainingPortions);
    }

    private static List<Long> toList(long[] counts) {
        List<Long> result = new ArrayList<>(counts.length);
        for (long count : counts) {
            result.add(count);
        }
        return result;
    }
}
