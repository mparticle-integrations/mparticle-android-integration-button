package com.mparticle.kits.button;

public abstract class ButtonException extends Exception {
    public ButtonException(final Throwable e) {
        super(e);
    }

    public ButtonException(final String message, final Throwable e) {
        super(message, e);
    }

    public ButtonException(final String message) {
        super(message);
    }
}
