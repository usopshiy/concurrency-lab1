package org.labs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MainTest {
    @Test
    void usesDefaultsWhenNoArgumentsAreGiven() {
        Dinner.Config config = Main.parseArguments(new String[0]);

        assertEquals(new Dinner.Config(7, 1_000_000, 2), config);
    }

    @Test
    void readsProgrammersPortionsAndWaitersInOrder() {
        Dinner.Config config = Main.parseArguments(new String[]{"4", "123", "3"});

        assertEquals(new Dinner.Config(4, 123, 3), config);
    }

    @Test
    void rejectsMissingArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.parseArguments(new String[]{"4"}));
    }

    @Test
    void rejectsNonNumericArguments() {
        assertThrows(IllegalArgumentException.class,
                () -> Main.parseArguments(new String[]{"four", "123", "3"}));
    }
}
