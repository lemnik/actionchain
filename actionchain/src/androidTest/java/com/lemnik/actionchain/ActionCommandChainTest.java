package com.lemnik.actionchain;

import android.os.Looper;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.lemnik.actionchain.ActionCommand.onBackground;
import static com.lemnik.actionchain.ActionCommand.onError;
import static com.lemnik.actionchain.ActionCommand.onForeground;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

@RunWith(AndroidJUnit4.class)
public class ActionCommandChainTest {

    static final ActionCommand.Function<Integer, Integer> ADD_ONE = new ActionCommand.Function<Integer, Integer>() {
        @Override
        public Integer apply(final Integer integer) {
            assertNotSame(Looper.getMainLooper(), Looper.myLooper());
            return integer + 1;
        }
    };

    static final ActionCommand<Integer, Integer> ERROR = new ActionCommand<Integer, Integer>() {
        @Override
        public Integer onBackground(Integer integer) throws Exception {
            throw new Exception("deliberate failure");
        }
    };

    @Test
    public void testSimpleChain() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        onBackground(ADD_ONE)
                .then(onBackground(ADD_ONE))
                .then(onBackground(ADD_ONE))
                .then(onForeground(new ActionCommand.Consumer<Integer>() {
                    @Override
                    public void accept(final Integer integer) {
                        assertSame(Looper.getMainLooper(), Looper.myLooper());

                        assertEquals(4, integer.intValue());
                        latch.countDown();
                    }
                }))
                .exec(1);

        latch.await();
    }

    @Test
    public void testMixedChain() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(2);

        new ParseIntCommand()
                .then(onBackground(ADD_ONE))
                .then(onForeground(new ActionCommand.Consumer<Integer>() {
                    @Override
                    public void accept(final Integer integer) {
                        assertSame(Looper.getMainLooper(), Looper.myLooper());
                        assertNotNull(integer);
                        latch.countDown();
                    }
                }))
                .then(onBackground(ADD_ONE))
                .then(onForeground(new ActionCommand.Consumer<Integer>() {
                    @Override
                    public void accept(final Integer integer) {
                        assertSame(Looper.getMainLooper(), Looper.myLooper());

                        assertEquals(12, integer.intValue());
                        latch.countDown();
                    }
                }))
                .exec("10");

        latch.await();
    }

    @Test
    public void testSimpleChainError() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        new ParseIntCommand()
                .then(onBackground(ADD_ONE))
                .then(onBackground(ADD_ONE))
                .then(ERROR)
                .then(onBackground(ADD_ONE))
                .then(ActionCommand.<Integer>onError(new ActionCommand.Consumer<Exception>() {
                    @Override
                    public void accept(Exception e) {
                        assertSame(Looper.getMainLooper(), Looper.myLooper());
                        assertNotNull(e);
                        assertEquals("deliberate failure", e.getMessage());

                        latch.countDown();
                    }
                }))
                .exec("1234");

        latch.await();
    }

}
