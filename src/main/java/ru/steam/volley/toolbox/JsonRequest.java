package ru.steam.volley.toolbox;


import lombok.extern.slf4j.Slf4j;
import ru.steam.volley.NetworkResponse;
import ru.steam.volley.Request;
import ru.steam.volley.Response;

import java.io.UnsupportedEncodingException;

@Slf4j
public abstract class JsonRequest<T> extends Request<T> {
    private static final String PROTOCOL_CONTENT_TYPE;
    private final Response.Listener<T> mListener;
    private final String mRequestBody;

    protected abstract Response<T> parseNetworkResponse(NetworkResponse networkResponse);

    static {
        PROTOCOL_CONTENT_TYPE = String.format("application/json; charset=%s", new Object[]{"utf-8"});
    }

    public JsonRequest(int method, String url, String requestBody, Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        this.mListener = listener;
        this.mRequestBody = requestBody;
    }

    protected void deliverResponse(T response) {
        this.mListener.onResponse(response);
    }

    public String getPostBodyContentType() {
        return getBodyContentType();
    }

    public byte[] getPostBody() {
        return getBody();
    }

    public String getBodyContentType() {
        return PROTOCOL_CONTENT_TYPE;
    }

    public byte[] getBody() {
        byte[] bArr = null;
        try {
            if (this.mRequestBody != null) {
                bArr = this.mRequestBody.getBytes("utf-8");
            }
        } catch (UnsupportedEncodingException e) {
            log.warn("Unsupported Encoding while trying to get the bytes of %s using %s", this.mRequestBody, "utf-8");
        }
        return bArr;
    }
}
