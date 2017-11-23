package de.haw.heroservice.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HeroDto {

    @Value("${url}")
    private String url;
    @Value("${url.user}")
    private String user;
    private boolean idle;
    private String group;
    @Value("${uri.hirings}")
    private String hirings;
    @Value("${uri.assignments}")
    private String assignments;
    @Value("${uri.messages}")
    private String messages;

    public HeroDto() {}

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

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

    public String getHirings() {
        return hirings;
    }

    public void setHirings(String hirings) {
        this.hirings = hirings;
    }

    public String getAssignments() {
        return assignments;
    }

    public void setAssignments(String assignments) {
        this.assignments = assignments;
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
    }
}
