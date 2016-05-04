package ru.steam.volley;

public class NoConnectionError extends NetworkError {
    public NoConnectionError(Throwable reason) {
        super(reason);
    }
}
