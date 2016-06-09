package com.mparticle.kits.button;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class Command<T> {

    public final Set<FailableReceiver<T>> mReceivers = new LinkedHashSet<>();
    private boolean mCancelled = false;

    public Command() {

    }

    public Command(final FailableReceiver<T> receiver) {
        mReceivers.add(receiver);
    }

    /**
     * Implement to perform the heavy work. Return result or throw exception. Will not run on the main thread
     * @return result if successful
     * @throws Exception if anything failed.
     */
    public abstract T execute() throws Exception;

    /**
     * Cancel this command, check periodically {@code isCancelled() and return immediately if true}
     */
    public synchronized void cancel() {
        mReceivers.clear();
        mCancelled = true;
    }

    public synchronized boolean isCancelled() {
        return mCancelled;
    }

    /**
     * A stable key for this command. Used to avoid duplicate commands executing
     * @return
     */
    public abstract String key();

    public void deliverSuccess(final T result) {
        for (FailableReceiver receiver : mReceivers) {
            receiver.onResult(result);
        }
    }

    public void deliverFailure() {
        for (FailableReceiver receiver : mReceivers) {
            receiver.onError();
        }
    }

    /**
     * If two operations of same key are queued, join callbacks instead of executing multiple times for efficiency.
     * Callbacks are held in a set, so if key & callback are equal, only one callback will be made.
     * @param command
     */
    public void join(final Command command) {
        if (!command.key().equals(key())) return;
        mReceivers.addAll(command.mReceivers);
    }

    @Override
    public String toString() {
        return "Command{" +
                "key=" + key() +
                ", mCancelled=" + mCancelled +
                '}';
    }

    @Override
    public int hashCode() {
        return key().hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Command command = (Command) o;

        return key().equals(command.key());
    }
}
