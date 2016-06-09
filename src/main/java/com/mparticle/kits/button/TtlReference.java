package com.mparticle.kits.button;

import android.os.SystemClock;

/**
 * @hide
 * This class is a convenience class for objects that we want to expire after a certain amount of time.
 * When the ttl has elapsed against the time provider {@link SystemClock#elapsedRealtime()}, {@link #get()} will return null.
 * @param <T> type of object we're holding on to
 */
public class TtlReference<T> {


    public static final TimeProvider REALTIME_MILLIS_PROVIDER = new TimeProvider() {
        @Override
        public long getTime() {
            return SystemClock.elapsedRealtime();
        }
    };

    public interface TimeProvider {
        long getTime();
    }

    private final T mObject;
    private final long mTimeOfDeath;
    private final TimeProvider mTimeProvider;

    /**
     * @param object
     * @param ttl postive number of ms object should be accessible
     */
    public TtlReference(final TimeProvider timeProvider, final T object, long ttl) {
        mObject = object;
        mTimeOfDeath = timeProvider.getTime() + ttl;
        mTimeProvider = timeProvider;
    }

    /**
     * @return object or null if ttl have elapsed
     */
    public T get() {
        if (isDead()) {
            return null;
        }
        return mObject;
    }

    /**
     * @return true when ttl have elapsed meaning {@link #get()} will return null
     */
    public boolean isDead() {
        return mTimeProvider.getTime() > mTimeOfDeath;
    }
}
