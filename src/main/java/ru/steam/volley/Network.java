package ru.steam.volley;

public interface Network {
    NetworkResponse performRequest(Request<?> request) throws VolleyError;
}
