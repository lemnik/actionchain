package com.lemnik.actionchain;

import android.app.Activity;

/**
 * Created by jason on 2017/07/30.
 */
public class ActivityActionCommand<P, R> extends ContextActionCommand<P, R> {
    protected ActivityActionCommand(final Activity activity) {
        super(activity);
    }

    protected Activity activity() {
        return (Activity) context();
    }

    protected Activity getActivity() {
        return (Activity) getContext();
    }
}
