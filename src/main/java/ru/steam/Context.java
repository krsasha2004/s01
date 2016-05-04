package ru.steam;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sasha on 5/3/16.
 */
public class Context {

    Map<String, SharedPreferences> sharedPreferences = new HashMap<String, SharedPreferences>();

    public SharedPreferences getSharedPreferences(String key, int mode) {
        SharedPreferences result = sharedPreferences.get(key);
        if (result == null) {
            result = new SharedPreferences();
            sharedPreferences.put(key, result);
        }
        return result;
    }

}
