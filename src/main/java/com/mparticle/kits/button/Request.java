package com.mparticle.kits.button;

import android.net.Uri;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class Request {
    private Uri mUrl;
    private List<Pair<String, String>> mHeaders = new ArrayList<>();
    private String mRequestId;

    public Request(final String url) {
        mUrl = Uri.parse(url);
    }

    public Request(final Uri url) {
        mUrl = url;
    }

    void addHeader(final String key, final String value) {
        mHeaders.add(new Pair<>(key, value));
    }

    public Uri url() {
        return mUrl;
    }

    public abstract String body();

    public abstract String method();

    public Iterable<? extends Pair<String, String>> headers() {
        return mHeaders;
    }

    public void setRequestId(final String requestId) {
        mRequestId = requestId;
    }

    public String getRequestId() {
        return mRequestId;
    }

    public static class Get extends Request {

        public Get(final String url) {
            super(url);
        }

        public Get(final Uri url) {
            super(url);
        }

        @Override
        public String body() {
            return null;
        }

        @Override
        public String method() {
            return "GET";
        }

        public Get withParameter(final String key, final String value) {
            super.appendQueryParameter(key, value);
            return this;
        }

        public Get withParameterIfNotNull(final String key, final String value) {
            if (value == null) return this;
            return withParameter(key, value);
        }

        public Get withParameter(final String key, final int value) {
            return withParameter(key, String.format(Locale.US, "%d", value));
        }

        public Get withParameter(final String key, final float value) {
            return withParameter(key, stripTrailingZeros(String.format(Locale.US, "%f", value)));
        }

        /**
         * Java is stupid and provides no good formatting for just 1 zero if immediately after comma and strip if we're further back into the decimals.
         * @param input
         * @return This will produce: "4.0" -> "4.0" and "4.320000" -> "4.32"
         */
        private String stripTrailingZeros(final String input) {
            int lastSignificantDigit = input.length() - 1;
            for (int i = input.length() - 1; i > 0; i--) {
                if (input.charAt(i - 1) == '.') {
                    break;
                } else if (input.charAt(i) == '0') {
                    lastSignificantDigit = i;
                }
                else {
                    break;
                }
            }
            return input.substring(0, lastSignificantDigit);
        }

        public Get withParameter(final String key, final double value) {
            return withParameter(key, stripTrailingZeros(String.format(Locale.US, "%f", value)));
        }

        public Get withParameter(final String key, final JSONObject jsonObject) {
            return withParameter(key, jsonObject.toString());
        }
    }

    private void appendQueryParameter(final String key, final String value) {
        mUrl = mUrl.buildUpon().appendQueryParameter(key, value).build();
    }

    public static class Post extends Request {

        private String mBody;

        public Post(final String url) {
            super(url);
        }

        @Override
        public String body() {
            return mBody;
        }

        @Override
        public String method() {
            return "POST";
        }

        public Post withBody(final JSONObject jsonObject) {
            addHeader("Content-Type", "application/json");
            mBody = jsonObject.toString();
            return this;
        }

        public Post withBody(final String rawBody) {
            mBody = rawBody;
            return this;
        }
    }

    public static class Put extends Post {
        public Put(final String url) {
            super(url);
        }

        @Override
        public String method() {
            return "PUT";
        }
    }

    @Override
    public String toString() {
        return "Request{" +
                "mUrl=" + mUrl +
                ", mHeaders=" + mHeaders +
                ", method=" + method() +
                ", body=" + body() +
                '}';
    }

}
