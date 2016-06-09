package com.mparticle.kits.button;

public interface FailableReceiver<T> {
    void onResult(final T result);
    void onError();
}
