package com.universe;

public enum NotificationType {


    FOLLOW("follow", "começou a seguir-te!"),

    LIKE("like", "gostou da tua publicação" ),
    COMMENT("comment", "comentou a tua publicação"),

    POST("post", "publicou uma nova publicação");







    private final String type;
    private final String message;


    NotificationType (String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }
    public String getMessage() {
        return message;
    }


}
