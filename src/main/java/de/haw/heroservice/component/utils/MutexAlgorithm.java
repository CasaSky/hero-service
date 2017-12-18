package de.haw.heroservice.component.utils;

import org.springframework.stereotype.Component;

@Component
public class MutexAlgorithm {

    public int getTime(int ownTime, int givenTime) {
        return givenTime > ownTime ? givenTime + 1 : ownTime + 1;
    }
}
