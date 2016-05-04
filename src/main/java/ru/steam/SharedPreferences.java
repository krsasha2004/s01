package ru.steam;

import java.util.HashMap;

/**key
 * Created by sasha on 5/3/16.
 */
public class SharedPreferences extends HashMap {

    public String getString(String key, String defValue) {
        return get(key) != null ? get(key).toString() : defValue;
    }

    public SharedPreferences edit() {
        return this;
    }

    public void commit() {

    }

    public SharedPreferences putString(String key, String value) {
        put(key, value);
        return this;
    }
}
