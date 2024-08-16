package com.shopwallet.ituchallenger;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * A class that tracks the current activity within the application.
 * Implements the Application.ActivityLifecycleCallbacks to monitor the activity lifecycle events.
 */
public class AppLifecycleTracker implements Application.ActivityLifecycleCallbacks {
    private Activity currentActivity;

    /**
     * Called when an activity is created.
     * Updates the current activity reference to the newly created activity.
     *
     * @param activity The activity being created.
     * @param savedInstanceState The bundle containing the activity's previous state, if any.
     */
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        currentActivity = activity;
    }

    /**
     * Called when an activity is started.
     * Updates the current activity reference to the started activity.
     *
     * @param activity The activity being started.
     */
    @Override
    public void onActivityStarted(Activity activity) {
        currentActivity = activity;
    }

    /**
     * Called when an activity is resumed.
     * Updates the current activity reference to the resumed activity.
     *
     * @param activity The activity being resumed.
     */
    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
    }

    /**
     * Called when an activity is paused.
     * Does nothing but provides a placeholder for potential future use.
     *
     * @param activity The activity being paused.
     */
    @Override
    public void onActivityPaused(Activity activity) {
        // No implementation needed for this example
    }

    /**
     * Called when an activity is stopped.
     * Does nothing but provides a placeholder for potential future use.
     *
     * @param activity The activity being stopped.
     */
    @Override
    public void onActivityStopped(Activity activity) {
        // No implementation needed for this example
    }

    /**
     * Called when an activity's instance state is being saved.
     * Does nothing but provides a placeholder for potential future use.
     *
     * @param activity The activity whose state is being saved.
     * @param outState The bundle to save the activity's state.
     */
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // No implementation needed for this example
    }

    /**
     * Called when an activity is destroyed.
     * Does nothing but provides a placeholder for potential future use.
     *
     * @param activity The activity being destroyed.
     */
    @Override
    public void onActivityDestroyed(Activity activity) {
        // No implementation needed for this example
    }

    /**
     * Returns the currently active activity.
     *
     * @return The current activity or null if no activity is currently active.
     */
    public Activity getCurrentActivity() {
        return currentActivity;
    }
}
