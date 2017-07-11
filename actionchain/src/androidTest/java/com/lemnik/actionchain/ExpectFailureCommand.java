package com.lemnik.actionchain;

import android.os.Looper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

class ExpectFailureCommand extends ParseIntCommand {

    Exception capturedError;

    @Override
    public void onForeground(final Integer value) {
        fail("onForeground should never be called");
    }

    @Override
    public void onError(final Exception error) throws Exception {
        assertNotNull(error);

        // make sure we are actually on the main thread
        assertSame(Looper.getMainLooper(), Looper.myLooper());

        capturedError = error;

        latch.countDown();
    }
}
