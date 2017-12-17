package de.haw.heroservice.component.utils;

public class MutexAlgorithm {

    public int getTime(int ownTime, int givenTime) {
        return givenTime > ownTime ? givenTime + 1 : ownTime + 1;
    }
}
