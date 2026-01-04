package com.universe;

public interface DataListener<T> {
    void onData(T data);
    void onError(Exception e);
}
