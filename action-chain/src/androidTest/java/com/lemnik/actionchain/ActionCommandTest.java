package com.lemnik.actionchain;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.AssertionFailedError;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ActionCommandTest {

    @Test
    public void testSimpleCommand() throws Exception {
        final ParseIntCommand command = new ParseIntCommand();
        command.exec("1234");
        command.await();
    }

    @Test
    public void testSimpleError() throws Exception {
        final ExpectFailureCommand command = new ExpectFailureCommand();
        command.exec("hello world");
        command.await();

        final Exception error = command.capturedError;
        assertTrue(
                "Not a NumberFormatException: " + error.getClass().getName(),
                error instanceof NumberFormatException
        );
    }

}
