package com.lemnik.actionchain;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * A much simpler solution to task processing than {@link AsyncTask}. An {@code ActionCommand} is called
 * strictly in sequence {@link #onBackground(Object) onBackground}, followed by either
 * {@link #onForeground(Object) onForeground} or {@link #onError(Exception) onError}.
 */
public abstract class ActionCommand<P, R> {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * Convenience method for {@code ActionCommand} implementations that don't require any input
     * parameters. It's strongly encouraged that you override the
     * {@link #onBackground(Object) onBackground(P)} method rather than take constructor parameters,
     * as this allows for better use of chaining.
     *
     * @return {@literal null} by default
     */
    public R onBackground() throws Exception {
        return null;
    }

    /**
     * Process a value on a background thread and then return a new value to be handled on the
     * main thread. Implementations of this method should (as far as possible) behave as functions
     * and have no side-effects. This allows {@code ActionCommand} objects to be chained together.
     *
     * @param value the value to process on the background thread
     * @return a value to pass along to {@link #onForeground(Object) onForeground}, and then
     * onto the next {@code ActionCommand} (if there is another one)
     * @throws Exception if something goes wrong, this will be delivered to {@link #onError(Exception) onError}
     */
    public R onBackground(final P value) throws Exception {
        return onBackground();
    }

    /**
     * <p>
     * Consume the value return by {@link #onBackground(Object) onBackground} on the main thread.
     * This method won't be called if {@code onBackground} threw an exception, instead
     * {@link #onError(Exception) onError} will be called. This method should run as quickly as
     * possible to free up the main thread for the user interface to keep working.
     * </p>
     * <p>
     * <b>Important Note:</b> leaving {@link #onBackground(Object) onBackground} as it's default
     * implementation will mean that this method will always be invoked with {@literal null}.
     * </p>
     *
     * @param value the value returned by {@link #onBackground(Object)}
     * @see #onBackground(Object)
     * @see #onError(Exception)
     * @see IdentityActionCommand
     */
    public void onForeground(final R value) {
    }

    /**
     * <p>
     * Handle errors that occurred on the background or main thread. By default this implementation
     * re-throws the {@code error}, which causes the {@link Chain} class to pass it onto the next
     * {@code ActionCommand} in the chain. {@code onError} is always invoked on the main thread.
     * </p>
     * <p>
     * A good pattern for error handling is to define one or more generic {@code ActionCommand} classes
     * that handle your errors, and then simply place them as the last step in the {@code Chain}. Any
     * error that happens in the {@code Chain} should then make it's way to your generic handler.
     * </p>
     *
     * @param error the {@code Exception} to handle or report
     * @throws Exception by default {@code error}, but any {@code Exception} thrown will cause a
     *                   chain to pass it on to the next {@code ActionCommand}
     */
    public void onError(final Exception error) throws Exception {
        throw error;
    }

    public <T> Chain<P, T> then(final ActionCommand<R, T> next) {
        return new Chain<>(this).then(next);
    }

    public static <P, R> ActionCommand<P, R> onBackground(final Function<P, R> function) {
        return ComposableActionCommand.forBackground(function);
    }

    public static <R> ActionCommand<R, R> onForeground(final Consumer<R> consumer) {
        return ComposableActionCommand.forForeground(consumer);
    }

    public static <R> ActionCommand<R, R> onError(final Consumer<Exception> consumer) {
        return ComposableActionCommand.forError(consumer);
    }

    public void exec() {
        exec(null, AsyncTask.SERIAL_EXECUTOR);
    }

    public void exec(final P parameter) {
        exec(parameter, AsyncTask.SERIAL_EXECUTOR);
    }

    public void exec(final P parameter, final Executor background) {
        background.execute(new ActionCommandRunner(background, parameter, this));
    }

    public interface Consumer<T> {
        void accept(T t);
    }

    public interface Function<T, R> {
        R apply(T t) throws Exception;
    }

    private static class ActionCommandRunner implements Runnable {

        private static final int STATE_BACKGROUND = 1;
        private static final int STATE_FOREGROUND = 2;
        private static final int STATE_ERROR = 3;

        private final Executor background;

        private final Iterator<ActionCommand> chain;

        private ActionCommand command;

        private int state = STATE_BACKGROUND;

        private Object value;

        ActionCommandRunner(
                final Executor background,
                final Object value,
                final ActionCommand command) {

            this.background = background;
            this.value = value;
            this.command = command;
            this.chain = Collections.<ActionCommand>emptyList().iterator();
        }

        ActionCommandRunner(
                final Executor background,
                final Object value,
                final Iterator<ActionCommand> chain) {

            this.background = background;
            this.value = value;
            this.chain = chain;
            this.command = chain.next();
        }

        @SuppressWarnings("unchecked")
        void onBackground() {
            try {
                // our current "value" is the commands parameter
                this.value = command.onBackground(value);
                this.state = STATE_FOREGROUND;
            } catch (final Exception error) {
                this.value = error;
                this.state = STATE_ERROR;
            } finally {
                MAIN_HANDLER.post(this);
            }
        }

        @SuppressWarnings("unchecked")
        void onForeground() {
            try {
                command.onForeground(value);

                // we chain internally by resetting our state
                if (chain.hasNext()) {
                    this.command = chain.next();
                    this.state = STATE_BACKGROUND;
                    background.execute(this);
                }
            } catch (final Exception error) {
                this.value = error;
                this.state = STATE_ERROR;

                // we go into an error state, and return to foreground to deliver it
                MAIN_HANDLER.post(this);
            }

            // in the case of an Error, we remain on the foreground thread, and let it through
        }

        void onError() {
            try {
                // onError breaks the chain, and happens on the foreground
                command.onError((Exception) value);
            } catch (final Exception error) {
                // we remain in an error state, and continue if we can
                if (chain.hasNext()) {
                    this.command = chain.next();
                    MAIN_HANDLER.post(this);
                }
            }
        }

        @Override
        public void run() {
            switch (state) {
                case STATE_BACKGROUND:
                    onBackground();
                    break;
                case STATE_FOREGROUND:
                    onForeground();
                    break;
                case STATE_ERROR:
                    onError();
                    break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static class ComposableActionCommand extends ActionCommand {

        Function background;

        Consumer foreground;

        Consumer error;

        @Override
        public Object onBackground(Object value) throws Exception {
            if (this.background != null) {
                return this.background.apply(value);
            } else {
                // identity
                return value;
            }
        }

        @Override
        public void onForeground(Object value) {
            if (this.foreground != null) {
                this.foreground.accept(value);
            }
        }

        @Override
        public void onError(Exception err) throws Exception {
            if (this.error != null) {
                this.error.accept(err);
            } else {
                // rethrow
                throw err;
            }
        }

        private void chainBackground(final Function next) {
            final Function previous = this.background;
            this.background = new Function() {
                @Override
                public Object apply(Object o) throws Exception {
                    return next.apply(previous.apply(o));
                }
            };
        }

        private void chainForeground(final Consumer next) {
            final Consumer previous = this.foreground;
            this.foreground = new Consumer() {
                @Override
                public void accept(Object o) {
                    previous.accept(o);
                    next.accept(o);
                }
            };
        }

        boolean isBackground() {
            return background != null && foreground == null && error == null;
        }

        boolean isForeground() {
            return background == null && foreground != null && error == null;
        }

        boolean isError() {
            return background == null && foreground == null && error != null;
        }

        boolean compose(final ComposableActionCommand then) {
            if (isBackground() && then.isBackground()) {
                chainBackground(then.background);
                return true;
            } else if (isForeground() && then.isForeground()) {
                chainForeground(then.foreground);
                return true;
            } else if (isBackground() && then.isForeground()) {
                this.foreground = then.foreground;
                return true;
            } else if (this.error != null && then.isError()) {
                this.error = then.error;
                return true;
            }

            return false;
        }

        static <R, P> ActionCommand<R, P> forBackground(final Function background) {
            final ComposableActionCommand command = new ComposableActionCommand();
            command.background = background;
            return command;
        }

        static <R, P> ActionCommand<R, P> forForeground(final Consumer foreground) {
            final ComposableActionCommand command = new ComposableActionCommand();
            command.foreground = foreground;
            return command;
        }

        static <R, P> ActionCommand<R, P> forError(final Consumer error) {
            final ComposableActionCommand command = new ComposableActionCommand();
            command.error = error;
            return command;
        }
    }

    public static class Chain<P, V> {
        private final List<ActionCommand> chain = new ArrayList<>();

        Chain(final ActionCommand<P, V> first) {
            if (first != null) {
                chain.add(first);
            }
        }

        @SuppressWarnings("unchecked")
        private <R> Chain<P, R> this0() {
            return (Chain<P, R>) this;
        }

        public <R> Chain<P, R> then(final ActionCommand<V, R> next) {
            if (next == null) {
                return this0();
            }

            if (!chain.isEmpty() && next instanceof ComposableActionCommand) {
                final ActionCommand lastCommand = chain.get(chain.size() - 1);
                if (lastCommand instanceof ComposableActionCommand
                        && ((ComposableActionCommand) lastCommand).compose((ComposableActionCommand) next)) {

                    // we composed the next command into the existing tail of the chain
                    return this0();
                }
            }

            chain.add(next);
            return this0();
        }

        public void exec() {
            exec(null, AsyncTask.SERIAL_EXECUTOR);
        }

        public void exec(final P parameter) {
            exec(parameter, AsyncTask.SERIAL_EXECUTOR);
        }

        public void exec(final P parameter, final Executor background) {
            background.execute(new ActionCommandRunner(background, parameter, chain.iterator()));
        }

    }

}