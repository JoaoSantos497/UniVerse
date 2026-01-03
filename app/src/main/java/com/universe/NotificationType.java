package com.universe;

public enum NotificationType {
    FOLLOW("começou a seguir-te!"),
    LIKE("gostou da tua publicação" ),
    COMMENT("comentou a tua publicação"),
    POST("publicou uma nova publicação");

    private final String message;

    NotificationType (String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
