package ru.steam.volley;

import java.util.Map;

public class NetworkResponse {
    public final byte[] data;
    public final Map<String, String> headers;
    public final long networkTimeMs;
    public final boolean notModified;
    public final int statusCode;

    public NetworkResponse(int statusCode, byte[] data, Map<String, String> headers, boolean notModified, long networkTimeMs) {
        this.statusCode = statusCode;
        this.data = data;
        this.headers = headers;
        this.notModified = notModified;
        this.networkTimeMs = networkTimeMs;
    }

    public NetworkResponse(byte[] data, Map<String, String> headers) {
        this(200, data, headers, false, 0);
    }
}
