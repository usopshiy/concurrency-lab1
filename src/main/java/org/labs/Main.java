package org.labs;

public class Main {
    public static void main(String[] args) {
        if (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))) {
            printUsage();
            return;
        }
        try {
            Dinner.Config config = parseArguments(args);
            long started = System.nanoTime();
            Dinner.Result result = new Dinner(config).run();
            double elapsedSeconds = (System.nanoTime() - started) / 1_000_000_000.0;
            printResult(config, result, elapsedSeconds);
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка: " + e.getMessage());
            printUsage();
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Обед прерван; рабочие потоки остановлены.");
        }
    }

    static Dinner.Config parseArguments(String[] args) {
        if (args.length == 0) {
            return Dinner.Config.defaults();
        }
        if (args.length != 3) {
            throw new IllegalArgumentException("укажите три числа или запустите без аргументов");
        }
        int programmers = Integer.parseInt(args[0]);
        long portions = Long.parseLong(args[1]);
        int waiters = Integer.parseInt(args[2]);
        return new Dinner.Config(programmers, portions, waiters);
    }

    private static void printResult(Dinner.Config config, Dinner.Result result, double elapsedSeconds) {
        System.out.printf("Программистов: %d, порций: %d, официантов: %d%n",
                config.programmers(), config.portions(), config.waiters());

        for (int i = 0; i < result.eaten().size(); i++) {
            System.out.printf("Программист %d съел: %d%n", i + 1, result.eaten().get(i));
        }
        for (int i = 0; i < result.served().size(); i++) {
            System.out.printf("Официант %d принёс: %d%n", i + 1, result.served().get(i));
        }

        System.out.printf("Всего съедено: %d, осталось: %d, время: %.3f с%n",
                result.totalEaten(), result.remaining(), elapsedSeconds);
    }

    private static void printUsage() {
        System.out.println("Запуск: ./gradlew run --args=\"<программисты> <порции> <официанты>\"");
        System.out.println("По умолчанию: 7 1000000 2. Программистов >= 2, порций >= 0, официантов >= 1.");
    }
}
