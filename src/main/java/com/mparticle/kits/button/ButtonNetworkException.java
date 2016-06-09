package com.mparticle.kits.button;

public class ButtonNetworkException extends ButtonException {
    private String mRequestId;

    public ButtonNetworkException(final Throwable e) {
        super(e);
    }

    public ButtonNetworkException(final String message) {
        super(message);
    }

    public ButtonNetworkException(final String message, final Throwable e) {
        super(message, e);
    }
    public ButtonNetworkException(final String message, final String requestId, final Throwable e) {
        super(message, e);
        mRequestId = requestId;
    }

    public String getRequestId() {
        return mRequestId;
    }
}
