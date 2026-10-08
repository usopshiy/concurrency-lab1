package org.labs;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class Dinner {
    private final Config config;

    public Dinner(Config config) {
        this.config = Objects.requireNonNull(config);
    }

    public Result run() throws InterruptedException {
        Table table = new Table(config);
        int participantCount = config.programmers() + config.waiters();
        ExecutorService executor = Executors.newFixedThreadPool(participantCount);
        ExecutorCompletionService<Void> completedTasks = new ExecutorCompletionService<>(executor);

        try {
            for (int waiter = 0; waiter < config.waiters(); waiter++) {
                int waiterId = waiter;
                completedTasks.submit(() -> {
                    table.serve(waiterId);
                    return null;
                });
            }

            for (int programmer = 0; programmer < config.programmers(); programmer++) {
                int programmerId = programmer;
                completedTasks.submit(() -> {
                    table.dine(programmerId);
                    return null;
                });
            }

            // Официанты сами не завершаются: ждём всех программистов.
            // Общая очередь завершений позволяет заметить и ошибку официанта.
            for (int programmer = 0; programmer < config.programmers(); programmer++) {
                completedTasks.take().get();
            }
        } catch (ExecutionException e) {
            throw new IllegalStateException("Ошибка участника обеда", e.getCause());
        } finally {
            executor.shutdownNow();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Не удалось остановить участников обеда");
            }
        }

        return table.result();
    }

    public record Config(int programmers, long portions, int waiters) {
        public Config {
            if (programmers < 2) {
                throw new IllegalArgumentException("нужны хотя бы два программиста и две ложки");
            }
            if (portions < 0) {
                throw new IllegalArgumentException("число порций не может быть отрицательным");
            }
            if (waiters < 1) {
                throw new IllegalArgumentException("нужен хотя бы один официант");
            }
            if ((long) programmers + waiters > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("слишком много участников");
            }
        }

        public static Config defaults() {
            return new Config(7, 1_000_000, 2);
        }
    }

    public record Result(List<Long> eaten, List<Long> served, long remaining) {
        public Result {
            eaten = List.copyOf(eaten);
            served = List.copyOf(served);
        }

        public long totalEaten() {
            long total = 0;
            for (long portions : eaten) {
                total += portions;
            }
            return total;
        }
    }
}
