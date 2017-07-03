package com.lemnik.actionchain;

/**
 * A convenience class that can be extended when only {@link #onForeground(Object) onForeground} or
 * {@link #onError(Exception) onError} implementations are required.
 */
public abstract class IdentityActionCommand<P> extends ActionCommand<P, P> {

    /**
     * Returns the parameter that was passed as {@code value}.
     *
     * @param value the value to return
     * @return {@code value}
     */
    @Override
    public P onBackground(final P value) {
        return value;
    }
}
