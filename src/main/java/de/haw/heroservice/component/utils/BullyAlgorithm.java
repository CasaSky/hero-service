package de.haw.heroservice.component.utils;

import de.haw.heroservice.BlackboardService;
import de.haw.heroservice.component.TavernaService;
import de.haw.heroservice.component.dtos.HeroDto;
import de.haw.heroservice.component.entities.Election;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BullyAlgorithm {

    @Value("${user.username}")
    private String username;

    @Autowired
    private TavernaService tavernaService;

    @Autowired
    private BlackboardService blackboardService;

    public String electHero(HeroDto heroDto, Election election) {
        String groupUrl = heroDto.getGroup();
        List<String> membersUsernames = getHigherUsernames(tavernaService.getMembersUsernames());// higher usernames
        List<String> heroUrls = tavernaService.getHeroUrls();
        if (!areSomeoneAvailable(heroUrls)) { // CheckAvailability
            // i am the new coordinator till the end of the quest/task
            // solve quests? and then after send answer to the origin owners callback. // temporar
            //if not do nothing?
        }

        return null;
    }

    private List<String> getHigherUsernames(List<String> memberUsernames) {
       return memberUsernames.stream().filter(u -> u.compareTo(username) == 1).collect(Collectors.toList());
    }

    private boolean areSomeoneAvailable(List<String> heroUrls) {
        for (String heroUrl : heroUrls) {
            try {
                ResponseEntity<?> response = blackboardService.getHero(heroUrl);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return true;
                }
            } catch (ResourceAccessException ex) {
                return false;
            }
        }
        return false;
    }
}
