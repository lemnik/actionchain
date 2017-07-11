package com.lemnik.actionchain;

import android.os.Looper;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

class ParseIntCommand extends ActionCommand<String, Integer> {

    protected final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public Integer onBackground(final String value) throws Exception {
        assertNotSame(Looper.getMainLooper(), Looper.myLooper());
        return Integer.parseInt(value);
    }

    @Override
    public void onForeground(final Integer value) {
        assertNotNull(value);

        // make sure we are actually on the main thread
        assertSame(Looper.getMainLooper(), Looper.myLooper());

        latch.countDown();
    }

    @Override
    public void onError(Exception error) throws Exception {
        fail();
    }

    void await() throws InterruptedException {
        latch.await();
    }

}
