package org.labs;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class DinnerTest {
    @ParameterizedTest(name = "{0} программистов, {1} порций, {2} официантов")
    @CsvSource(textBlock = """
             7,       0, 2
             7,       1, 2
             7,       3, 2
             7,      70, 2
             7,     101, 2
             2,   10001, 1
             3,   10000, 5
            20,   10003, 4
             7, 1000000, 2
            """)
    void allFoodIsServedAndEatenFairly(int programmers, long portions, int waiters)
            throws InterruptedException {
        Dinner.Config config = new Dinner.Config(programmers, portions, waiters);
        Dinner.Result result = new Dinner(config).run();

        assertEquals(programmers, result.eaten().size());
        assertEquals(waiters, result.served().size());
        assertEquals(portions, result.totalEaten());
        assertEquals(0, result.remaining());

        long totalServed = 0;
        for (long served : result.served()) {
            assertTrue(served >= 0);
            totalServed += served;
        }
        assertEquals(portions, totalServed);

        long minEaten = Collections.min(result.eaten());
        long maxEaten = Collections.max(result.eaten());
        assertTrue(maxEaten - minEaten <= 1, "Разница должна быть не больше одной порции");
    }

    @RepeatedTest(20)
    void dinnerFinishesWithoutDeadlock() throws InterruptedException {
        Dinner dinner = new Dinner(new Dinner.Config(7, 1000, 2));

        Dinner.Result result = dinner.run();

        assertEquals(1000, result.totalEaten());
    }

    @Test
    void sameDinnerCanBeRunAgain() throws InterruptedException {
        Dinner dinner = new Dinner(new Dinner.Config(7, 100, 2));

        Dinner.Result firstRun = dinner.run();
        Dinner.Result secondRun = dinner.run();

        assertEquals(firstRun.eaten(), secondRun.eaten());
    }

    @Test
    void interruptionStopsTheSimulation() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread simulation = new Thread(() -> {
            started.countDown();
            try {
                new Dinner(new Dinner.Config(7, Long.MAX_VALUE, 2)).run();
                outcome.set(new AssertionError("Обед не должен успеть завершиться"));
            } catch (Throwable e) {
                outcome.set(e);
            }
        });
        simulation.start();
        try {
            assertTrue(started.await(5, TimeUnit.SECONDS));
            simulation.interrupt();
            simulation.join(5000);
            assertFalse(simulation.isAlive(), "Обед должен завершиться после прерывания");
            assertInstanceOf(InterruptedException.class, outcome.get());
        } finally {
            simulation.interrupt();
            simulation.join(5000);
        }
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
                     0, 10,  2
                     1, 10,  2
                    -1, 10,  2
                     7, -1,  2
                     7, 10,  0
                     7, 10, -1
            2147483647, 10,  1
            """)
    void invalidConfigurationIsRejected(int programmers, long portions, int waiters) {
        assertThrows(IllegalArgumentException.class,
                () -> new Dinner.Config(programmers, portions, waiters));
    }
}
