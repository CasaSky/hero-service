package de.haw.heroservice.component.entities;

import org.springframework.stereotype.Component;

@Component
public class Mutexstate {

    private State state = State.RELEASED;
    private int time;

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int incrementTime() {
        this.time++;
        return time;
    }
}
