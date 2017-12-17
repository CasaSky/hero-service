package de.haw.heroservice.component.entities;

public class Mutexstate {

    private State state;
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
}
