package de.haw.heroservice.component.utils;

import de.haw.heroservice.component.TavernaService;
import de.haw.heroservice.component.dtos.HeroDto;
import de.haw.heroservice.component.entities.Election;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BullyAlgorithm {

    @Value("${user.username}")
    private String username;

    @Autowired
    private TavernaService tavernaService;

    public String electHero(HeroDto heroDto, Election election) {
        String groupUrl = heroDto.getGroup();
        List<String> membersUsernames = tavernaService.getMembersUsernames();
        //TODO filter only those higher than my username in new list;
        // Get there hero urls.
        //TODO send get hero to all with timeout
        // If no answer, i am the new coordinator tilll the end of the quest/task
        // solve quests? and then after send answer to the origin owners callback. // temporar
        //if not do nothing?
        return null;
    }
}
