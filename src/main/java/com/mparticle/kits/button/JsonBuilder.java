package com.mparticle.kits.button;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class JsonBuilder {
    private static final String TAG = "JsonBuilder";

    public static JSONObject toJson(final Object ... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-values are unmatched: " + keyValuePairs);
        }
        final JSONObject object = new JSONObject();
        try {
            for (int i = 0; i < keyValuePairs.length; i += 2) {
                final String name = (String) keyValuePairs[i];
                final Object value = keyValuePairs[i + 1];
                if (value instanceof String) {
                    object.put(name, toString(value));
                } else if (value instanceof Integer) {
                    object.put(name, (Integer) value);
                } else if (value instanceof Double) {
                    object.put(name, toString(value));
                } else if (value instanceof Float) {
                    object.put(name, toString(value));
                } else if (value instanceof Boolean) {
                    object.put(name, toString(value));
                } else if (value instanceof JSONObject) {
                    object.put(name, (JSONObject) value);
                } else if (value instanceof JSONArray) {
                    object.put(name, (JSONArray) value);
                } else if (value == null) {
                    object.put(name, null);
                } else
                    throw new IllegalArgumentException("Unhandled value class for " + name + ": " + value);
            }
        } catch (JSONException e) {
            ButtonLog.warn(TAG, "Shouldn't happen, but some object could not be added to our json structure: " + keyValuePairs, e);
        }
        return object;
    }

    public static String toString(final Object value) {
        if (value instanceof String) {
            return (String) value;
        }
        else if (value instanceof Integer) {
            return String.format(Locale.US, "%d", value);
        }
        else if (value instanceof Double) {
            return String.format(Locale.US, "%f", value);
        }
        else if (value instanceof Float) {
            return String.format(Locale.US, "%f", value);
        }
        else if (value instanceof Boolean) {
            return ((Boolean) value) ? "true" : "false";
        }
        else if (value instanceof JSONObject) {
            return value.toString();
        }
        else throw new IllegalArgumentException("Unsupported value class type: " + value);
    }

    public static JSONArray toArray(final Object... values) {
        final JSONArray array = new JSONArray();
        for (int i = 0; i < values.length; i++) {
            array.put(values[i]);
        }
        return array;
    }
}
