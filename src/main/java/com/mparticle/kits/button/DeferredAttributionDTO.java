package com.mparticle.kits.button;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

public class DeferredAttributionDTO {
    public final String id;
    public final boolean match;
    // Null if no match
    public final Uri action;
    // Null if no match
    public final AttributionDTO attribution;

    public DeferredAttributionDTO(final String id, final boolean match, final Uri action,
                                  final AttributionDTO attrDto) {
        this.id = id;
        this.match = match;
        this.action = action;
        attribution = attrDto;
    }

    public static DeferredAttributionDTO fromJson(final JSONObject object) throws JSONException {
        if (object == null) {
            return null;
        }
        final JSONObject attribution = object.optJSONObject("attribution");
        AttributionDTO attrDto = null;
        if (attribution != null) {
            attrDto = new AttributionDTO(attribution.optString("btn_ref"), attribution.optString("utm_source"));
        }
        return new DeferredAttributionDTO(object.getString("id"), object.optBoolean("match", false), uriOrNull(object, "action"), attrDto);
    }

    private static Uri uriOrNull(final JSONObject object, final String name) throws JSONException {
        if (object.has(name)) {
            return Uri.parse(object.getString(name));
        }
        return null;
    }

    public static class AttributionDTO {
        public final String btnRef;
        public final String utmSource;

        public AttributionDTO(final String btnRef, final String utmSource) {
            this.btnRef = btnRef;
            this.utmSource = utmSource;
        }

        @Override
        public String toString() {
            return "AttributionDTO{" +
                    "btnRef='" + btnRef + '\'' +
                    ", utmSource='" + utmSource + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "DeferredAttributionDTO{" +
                "id='" + id + '\'' +
                ", match=" + match +
                ", action=" + action +
                ", attribution=" + attribution +
                '}';
    }
}
