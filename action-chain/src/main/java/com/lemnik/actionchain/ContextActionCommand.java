package com.lemnik.actionchain;

import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * Implementations of {@code ActionCommand} that need a {@code Context} to function can use this
 * class to help stop them leaking references. This class holds a {@code Context} in a
 * {@code WeakReference}, and provides some utility methods to access it.
 * <p>
 * Created by jason on 2017/07/30.
 */
public abstract class ContextActionCommand<P, R> extends ActionCommand<P, R> {

    private final WeakReference<Context> contextRef;

    /**
     * Create a new {@coded ContextActionCommand} with a {@code Context} to reference. The
     * given {@code Context} may not be {@literal null}.
     *
     * @param context the {@code Context} to reference
     * @throws NullPointerException if {@code context} is {@literal null}
     */
    protected ContextActionCommand(final Context context) throws NullPointerException {
        if (context == null) {
            throw new NullPointerException("context may not be null");
        }

        contextRef = new WeakReference<Context>(context);
    }

    /**
     * Returns the referenced {@code Context}, or {@literal null} if it's been garbage collected.
     * The value returned by this method may change between a valid {@code Context} and {@literal null}
     * during the call to {@link #onBackground(Object) onBackground}, so it's a good idea to only
     * call it once, and to only call it when you actually need the {@code Context}.
     *
     * @return the {@code Context} held by this {@code ContextActionCommand} or {@literal null}
     */
    protected Context context() {
        return contextRef.get();
    }

    /**
     * Same as {@link #context()} but will never return {@literal null}, instead this method
     * will throw a {@code NullPointerException} if the {@code Context} has been garbage collected.
     *
     * @return the {@code Context} held by this {@code ContextActionCommand}
     * @throws NullPointerException if the {@code Context} has been garbage collected
     */
    protected Context getContext() throws NullPointerException {
        final Context context = context();

        if (context == null) {
            throw new NullPointerException("Context has been garbage collected");
        }

        return context;
    }

}
