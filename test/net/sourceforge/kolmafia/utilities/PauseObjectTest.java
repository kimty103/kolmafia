package net.sourceforge.kolmafia.utilities;

import static org.junit.jupiter.api.Assertions.*;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PauseObjectTest {

    private PauseObject pauseObject;

    @BeforeEach
    public void setUp() {
        pauseObject = new PauseObject();
    }
    /**
     * purpose: test unpause method
     * Input: pause -> unpause
     * Expected:
     *     return true
     */
    @Test
    public void testUnpause() throws Exception {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> future = service.submit(() -> {
            pauseObject.pause();
        });

        long start = System.currentTimeMillis();
        Thread.sleep(500);


        pauseObject.unpause();

        future.get();

        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed > 300 && elapsed < 700);
    }

    /**
     * purpose: test pause method with zero input
     * Input: pause 0
     * Expected:
     *     return true
     */
    @Test
    public void testPauseWithZero() throws Exception {
        long start = System.currentTimeMillis();

        // This should not pause at all
        pauseObject.pause(0);

        long elapsed = System.currentTimeMillis() - start;

        // The elapsed time should be almost nothing
        assertTrue(elapsed < 100);
    }


    /**
     * purpose: test pause method with negative input
     * Input: pause -1
     * Expected:
     *     return true
     */
    @Test
    public void testPauseWithNegative() throws Exception {
        long start = System.currentTimeMillis();

        // This should not pause at all
        pauseObject.pause(-1);

        long elapsed = System.currentTimeMillis() - start;

        // The elapsed time should be almost nothing
        assertTrue(elapsed < 100);
    }

    /**
     * purpose: test pause method with 5000 input
     * Input: pause 5000
     * Expected:
     *     return true
     */
    @Test
    public void testPauseWith5000() throws Exception {
        ExecutorService service = Executors.newSingleThreadExecutor();
        Future<?> future = service.submit(() -> {
            pauseObject.pause(5000);
        });

        long start = System.currentTimeMillis();


        // Wait for the future to complete
        future.get();

        long elapsed = System.currentTimeMillis() - start;

        // assert true when elapsed time is similar to 5000
        assertTrue(elapsed > 4800 && elapsed < 5200);
    }
}