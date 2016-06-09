package com.mparticle.kits.button;

import java.net.URL;

public class ButtonHttpStatusException extends ButtonNetworkException{
    private final int mStatusCode;

    public ButtonHttpStatusException(final int statusCode) {
        this(statusCode, null);
    }

    public ButtonHttpStatusException(final int statusCode, final URL url) {
        this(statusCode, url, null);
    }

    public ButtonHttpStatusException(final int statusCode, final URL url, final String requestId) {
        super("HTTP error code: " + statusCode +  url != null ? (" for " + url) : "", requestId, null);
        mStatusCode = statusCode;
    }

    /**
     * @return true if status code falls in the range indicating bad requests. (400-499)
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
     */
    public boolean wasBadRequest() {
        return mStatusCode >= 400 && mStatusCode < 500;
    }

    /**
     * @return if the exception was due to the server responding with a 401 (Unauthorized).
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
     */
    public boolean wasUnauthorized() {
        return mStatusCode == 401;
    }

    /**
     * @return true if status code falls in the range indicating server errors. (500-599)
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html
     */
    public boolean wasServerError() {
        return mStatusCode >= 500 && mStatusCode < 600;
    }
}
