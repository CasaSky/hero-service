package de.haw.heroservice.component;

import java.util.ArrayList;
import java.util.List;

public class Hero {

    private String user;
    private boolean idle;
    private String group;
    private List<Hiring> hirings = new ArrayList<>();
    private List<Assignment> assignments = new ArrayList<>();
    private List<Message> messages;

    public Hero() {}

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isIdle() {
        return idle;
    }

    public void setIdle(boolean idle) {
        this.idle = idle;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void addHiring(Hiring hiring) {
        this.addHiring(hiring);
    }

    public List<Hiring> getHirings() {
        return hirings;
    }

    public void setHirings(List<Hiring> hirings) {
        this.hirings = hirings;
    }

    public List<Assignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Assignment> assignments) {
        this.assignments = assignments;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
