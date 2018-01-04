package de.haw.heroservice.entities;

public enum Msg {
    REQUEST("request"),
    REPLYOK("reply-ok");

    private String id;

    Msg(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
