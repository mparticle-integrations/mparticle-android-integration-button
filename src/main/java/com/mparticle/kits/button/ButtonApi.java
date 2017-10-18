package com.mparticle.kits.button;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import static com.mparticle.kits.button.JsonBuilder.toArray;

/**
 * Button DLC API client.
 *
 * @see <a href="https://github.com/usebutton/dlcfe/tree/master/docs/protocol.md">Protocol spec</a>
 */
public class ButtonApi {

    private static final String TAG = "ButtonApi";

    private final String mBaseUrl;
    private final String mApplicationId;
    private final IdentifierForAdvertiserProvider mIdentifierForAdvertiserProvider;
    private final Http mHttp;
    private String mSessionId;
    private JSONObject mCapabilities;

    public ButtonApi(final HostInformation hostInformation, final IdentifierForAdvertiserProvider ifaProvider) {
        this(hostInformation, ifaProvider, new Http(hostInformation.getUserAgent()));
    }

    public ButtonApi(final HostInformation hostInformation, final IdentifierForAdvertiserProvider ifaProvider, final Http http) {
        mApplicationId = hostInformation.getApplicationId();
        mBaseUrl = hostInformation.getBaseUrl().replaceAll("/$", "");
        mIdentifierForAdvertiserProvider = ifaProvider;
        mHttp = http;
        mCapabilities = buildCapabilities(hostInformation);
    }

    /**
     * Sets or updates the current session id.
     */
    public void setSessionId(final String sessionId) {
        if (sessionId == mSessionId) return; // In case of both being null, quick test
        if (sessionId != null && sessionId.equals(mSessionId)) return;
        ButtonLog.info(TAG, String.format("Changed session id from '%s' to '%s'", mSessionId, sessionId));
        mSessionId = sessionId;
    }

    public String getApplicationId() {
        return mApplicationId;
    }

    public Http getHttp() {
        return mHttp;
    }

    public IdentifierForAdvertiserProvider getIdentifierForAdvertiserProvider() {
        return mIdentifierForAdvertiserProvider;
    }

    private String urlFor(final String path) {
        return mBaseUrl + "/" + path.replaceAll("^/", "");
    }

    /**
     * Will add key=value to jsonObject if it doesn't have any value for key
     * @param jsonObject target json object
     * @param key        key to insert if it doesn't exist
     * @param value      value to insert if key doesn't exist in jsonObject
     */
    private void putSafely(final JSONObject jsonObject, final String key, final String value) throws JSONException {
        if (jsonObject.has(key)) {
            return;
        }
        jsonObject.put(key, value);
    }

    private JSONObject buildCapabilities(final HostInformation hostInformation) {
        final JSONObject capabilities = new JSONObject();
        try {
            capabilities.put("screen_scale", hostInformation.getScreenDensity());
            capabilities.put("supported_display_types", toArray("standard_button_v1"));
        } catch (JSONException e) {
            ButtonLog.warn(TAG, "Couldn't form capabilities object.", e);
        }
        return capabilities;
    }

    private JSONObject executeSessionRequest(final Request.Get get) throws ButtonNetworkException {
        get.withParameterIfNotNull("session_id", mSessionId);
        final String ifa = getIfa();
        get.withParameterIfNotNull("ifa", ifa);
        if (ifa == null) {
            get.withParameterIfNotNull("android_id", mIdentifierForAdvertiserProvider.getSecondaryIdentifier());
        }
        return mHttp.executeRequest(get);
    }

    private JSONObject getBaseSessionPayload() throws JSONException {
        final JSONObject payload = new JSONObject();
        payload.put("session_id", mSessionId);
        payload.putOpt("ifa", getIfa());
        if (!payload.has("ifa")) {
            payload.putOpt("android_id", mIdentifierForAdvertiserProvider.getSecondaryIdentifier());
        }
        return payload;
    }

    private String getIfa() {
        return mIdentifierForAdvertiserProvider.getPrimaryIdentifier();
    }

    private static void append(final JSONObject object, final String arrayKey, final Object value) throws JSONException {
        JSONArray array = object.optJSONArray(arrayKey);
        if (array == null) {
            array = new JSONArray();
            object.put(arrayKey, array);
        }
        array.put(value);
    }

    private boolean isNotEmpty(final List items) {
        return items != null && !items.isEmpty();
    }

    /**
     * Will associate (non-null) value or de-associate (null) your ID (username, email, profile id)
     * with our profile data.
     * @param thirdpartyId
     */
    public void setThirdPartyId(final String thirdpartyId) throws ButtonNetworkException {
        try {
            final JSONObject parameters = getBaseSessionPayload();
            parameters.putOpt("thirdparty_id", valueOrNull(thirdpartyId));
            final Request.Put request = new Request.Put(urlFor("/v1/session/customer"));
            request.withBody(parameters);
            mHttp.executeRequest(request);
        } catch (JSONException e) {
            throw new ButtonNetworkException("Couldn't create update customer request.", e);
        }
    }

    private Object valueOrNull(final Object value) {
        return value != null ? value : JSONObject.NULL;
    }

    public DeferredAttributionDTO getPendingLink(final JSONObject signals) throws ButtonNetworkException {
        final JSONObject parameters = new JSONObject();
        try {
            parameters.put("application_id", getApplicationId());
            parameters.put("signals", signals);
            parameters.putOpt("ifa", mIdentifierForAdvertiserProvider.getPrimaryIdentifier());
            parameters.putOpt("android_id", mIdentifierForAdvertiserProvider.getSecondaryIdentifier());
            final Request.Post request = new Request.Post(urlFor("/v1/web/deferred-deeplink"));
            request.withBody(parameters);
            return DeferredAttributionDTO.fromJson(mHttp.executeRequest(request).optJSONObject("object"));
        } catch (JSONException e) {
            throw new ButtonNetworkException("Couldn't get pending deep link.", e);
        }
    }
}
