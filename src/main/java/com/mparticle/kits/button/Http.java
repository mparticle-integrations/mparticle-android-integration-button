package com.mparticle.kits.button;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * A simpler, Button-flavored HTTP client interface.
 */
public class Http {

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CONTENT_TYPE_PNG = "image/png";
    private static final String CONTENT_TYPE_JPEG = "image/jpeg";
    private static final String TAG = "Http";
    private static final String ENCODING_UTF8 = "UTF-8";
    private static final int SO_CONNECT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(5);
    private static final int SO_READ_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(15);

    private static final int HTTP_STATUS_BAD_REQUEST = 400;

    private String mUserAgent;

    public Http(final String userAgent) {
        mUserAgent = userAgent;
    }

    /**
     * Performs an HTTP requestJson in a non-annoying way.
     *
     * @param request
     * @return
     * @throws IOException
     */
    public JSONObject requestJson(final Request request) throws ButtonNetworkException {
        HttpURLConnection urlConnection = null;
        final String requestId = requestId(urlConnection);
        try {
            urlConnection = connect(request, CONTENT_TYPE_JSON);
            final InputStream responseStream = streamForConnection(urlConnection);
            final String responseBody = ButtonUtil.streamToString(responseStream);
            ButtonLog.infoFormat(TAG, "Response (id=%s) for: %s\n%s", requestId, urlConnection.getURL(), responseBody);
            final JSONObject response;
            try {
                response = new JSONObject(responseBody);
            } catch (JSONException e) {
                throw new ButtonNetworkException("Couldn't parse response body to JSON", requestId, e);
            }
            return response;
        } catch (IOException e) {
            ButtonLog.visibleFormat("Network request failed (Request ID: %s)", requestId);
            throw new ButtonNetworkException("Exception while requesting: " + request.toString(), requestId, e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private static String requestId(final HttpURLConnection urlConnection) {
        if (urlConnection == null) return null;
        return urlConnection.getHeaderField("X-Button-Request");
    }

    private InputStream streamForConnection(final HttpURLConnection urlConnection) throws IOException, ButtonNetworkException {
        final int responseCode = urlConnection.getResponseCode();
        ButtonLog.infoFormat(TAG, "%d response for %s", responseCode, urlConnection.getURL());
        if (responseCode >= HTTP_STATUS_BAD_REQUEST) {
            throw new ButtonHttpStatusException(responseCode, urlConnection.getURL(), requestId(urlConnection));
        }
        return urlConnection.getInputStream();
    }

    private HttpURLConnection connect(final Request request, final String contentType) throws IOException, ButtonNetworkException {
        ButtonLog.infoFormat(TAG, "Will request: %s", request.toString());

        final URL url = new URL(request.url().toString());
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setConnectTimeout(SO_CONNECT_TIMEOUT);
        urlConnection.setReadTimeout(SO_READ_TIMEOUT);
        urlConnection.setRequestProperty("User-Agent", mUserAgent);
        urlConnection.setRequestProperty("Accept", contentType);

        for (Pair<String, String> propertyPair : request.headers()) {
            urlConnection.setRequestProperty(propertyPair.first(), propertyPair.second());
        }

        urlConnection.setRequestMethod(request.method());

        final String body = request.body();
        if (!TextUtils.isEmpty(body)) {
            final OutputStream os = urlConnection.getOutputStream();
            ButtonUtil.writeStringToStream(os, body);
            ButtonLog.infoFormat(TAG, "POST'ed: %s", body);
        }

        return urlConnection;
    }

    /**
     * Executes a Request synchronously
     *
     * @param request
     * @return
     * @throws com.usebutton.sdk.internal.api.ButtonNetworkException on network or parsing errors
     */
    public JSONObject executeRequest(final Request request) throws ButtonNetworkException {
        return requestJson(request);
    }

    /**
     * Executes Request synchronously, expecting a PNG bitmap as response object.
     * Will ask for Content-type: application/png
     *
     * @param request
     * @return bitmap or null if failed.
     * @throws com.usebutton.sdk.internal.api.ButtonNetworkException on network or parsing errors
     */
    public byte[] requestBitmapData(final Request request) throws ButtonNetworkException {
        HttpURLConnection urlConnection = null;

        try {
            urlConnection = connect(request, CONTENT_TYPE_PNG);
            if (!isSupportedImage(urlConnection.getContentType())) {
                return null;
            }
            final InputStream inputStream = streamForConnection(urlConnection);

            byte[] data = ButtonUtil.streamToByteArray(inputStream, urlConnection.getContentLength());
            if (inputStream != null) {
                inputStream.close();
            }
            return data;
        } catch (IOException e) {
            throw new ButtonNetworkException("Exception while GET'ing bitmap", requestId(urlConnection), e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    private boolean isSupportedImage(final String contentType) {
        return CONTENT_TYPE_PNG.equals(contentType) || CONTENT_TYPE_JPEG.equals(contentType);
    }

    public void setUserAgent(final String userAgent) {
        mUserAgent = userAgent;
    }

    public String getUserAgent() {
        return mUserAgent;
    }

    private static class Data {
        private final byte[] mData;

        public Data(final byte[] data) {
            mData = data;
        }
    }
}
