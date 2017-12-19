package de.haw.heroservice.component.entities;

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
