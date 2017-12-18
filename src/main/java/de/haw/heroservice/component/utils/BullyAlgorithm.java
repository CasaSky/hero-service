package de.haw.heroservice.component.utils;

import de.haw.heroservice.BlackboardService;
import de.haw.heroservice.component.TavernaService;
import de.haw.heroservice.component.dtos.HeroDto;
import de.haw.heroservice.component.entities.Election;
import org.apache.log4j.Logger;
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

    private Logger logger = Logger.getLogger(BullyAlgorithm.class);

    // If no one answers, then solve task -> return false
    public boolean electHero(HeroDto heroDto, Election election) {
        String groupUrl = heroDto.getGroup();
logger.info(groupUrl);
        List<String> membersUsernames = getHigherUsernames(tavernaService.getMembersUsernames());// higher usernames
logger.info(membersUsernames);
        List<String> heroUrls = tavernaService.getHeroUrls();
logger.info(heroUrls);
        return !postElection(heroUrls, election);
    }

    private List<String> getHigherUsernames(List<String> memberUsernames) {
       return memberUsernames.stream().filter(u -> u.compareTo(username) == 1).collect(Collectors.toList());
    }

    private boolean postElection(List<String> heroUrls, Election election) {
        for (String heroUrl : heroUrls) {
            try {
                ResponseEntity<?> response = blackboardService.postElection(heroUrl, election);
logger.info(response);
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
