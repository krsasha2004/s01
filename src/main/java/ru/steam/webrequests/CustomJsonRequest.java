package ru.steam.webrequests;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import ru.steam.volley.NetworkResponse;
import ru.steam.volley.Response;
import ru.steam.volley.VolleyError;
import ru.steam.volley.toolbox.JsonObjectRequest;

/* compiled from: JsonRequestBuilder */
@Slf4j
abstract class CustomJsonRequest extends JsonObjectRequest {
    private final ResponseListener responseListener;

    public CustomJsonRequest(int method, String url, JSONObject jsonRequest, ResponseListener responseListener) {
        super(method, url, jsonRequest, null, null);
        this.responseListener = responseListener;
        setShouldCache(false);
    }

    protected void deliverResponse(JSONObject response) {
        if (this.responseListener != null) {
            JSONObject responseField = response.optJSONObject("response");
            ResponseListener responseListener = this.responseListener;
            if (responseField == null) {
                responseField = response;
            }
            responseListener.onSuccess(responseField);
        }
    }

    public void deliverError(VolleyError error) {
        if (this.responseListener != null) {
            RequestErrorInfo errorInfo;
            if (error.networkResponse == null) {
                errorInfo = new RequestErrorInfo(-1, null, null, error.getMessage());
            } else {
                errorInfo = new RequestErrorInfo(error.networkResponse.statusCode, error.networkResponse.headers, error.networkResponse.data, error.getMessage());
            }
            this.responseListener.onError(errorInfo);
        }
    }

    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            return super.parseNetworkResponse(response);
        } catch (Exception e) {
            log.error("error", e);
            return null;
        }
    }
}
