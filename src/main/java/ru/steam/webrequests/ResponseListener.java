package ru.steam.webrequests;

import org.json.JSONObject;

public abstract class ResponseListener {
    public abstract void onError(RequestErrorInfo requestErrorInfo);

    public abstract void onSuccess(JSONObject jSONObject);
}
