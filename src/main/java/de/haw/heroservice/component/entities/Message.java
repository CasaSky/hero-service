package de.haw.heroservice.component.entities;


public class Message {

    private String message;
    private int statusValue;

    public Message(String message, int statusValue) {
        this.message = message;
        this.statusValue = statusValue;
    }


    public String getMessage() {
        return message;
    }

    public int getStatusValue() {
        return statusValue;
    }
}
