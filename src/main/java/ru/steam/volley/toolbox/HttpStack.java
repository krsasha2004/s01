package ru.steam.volley.toolbox;

import org.apache.http.HttpResponse;
import ru.steam.volley.AuthFailureError;
import ru.steam.volley.Request;

import java.io.IOException;
import java.util.Map;

public interface HttpStack {
    HttpResponse performRequest(Request<?> request, Map<String, String> map) throws IOException, AuthFailureError;
}
