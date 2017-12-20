package de.haw.heroservice.component.utils;

import de.haw.heroservice.BlackboardService;
import de.haw.heroservice.component.TavernaService;
import de.haw.heroservice.component.dtos.HeroDto;
import de.haw.heroservice.component.entities.Election;
import de.haw.heroservice.component.entities.Message;
import de.haw.heroservice.component.entities.Payload;
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

    public synchronized void start(HeroDto heroDto, Election election) {

        String groupUrl = heroDto.getGroup();

        //---> When an election message is received:
        //---> 1 Send ok message to the sender
        //---> 2 Post election message to the heroes in our group with higher usernames.
        postElectionOk(election.getUser(), election);
        postElectionToHigherHeroes(groupUrl, election);
        try {
            wait(2000);
        } catch (InterruptedException e) {

        }
    }

    public void postElectionCoordinatorToAll(String groupUrl, Election election) {
        election.setPayload(Payload.coordinator);
        List<String> membersUsernames = tavernaService.getMembersUsernames(groupUrl);
        logger.info(membersUsernames);
        List<String> heroUrls = tavernaService.getHeroUrls();
        logger.info(heroUrls);

        for (String heroUrl : heroUrls) {
            try {
                blackboardService.postElection(heroUrl, election);
            } catch (ResourceAccessException ex) {

            }
        }
    }

    private void postElectionOk(String user, Election election) {
        election.setPayload(Payload.ok);
        blackboardService.postElection(user, election);
    }

    private List<String> getHigherUsernames(List<String> memberUsernames) {
       return memberUsernames.stream().filter(u -> u.compareTo(username) == 1).collect(Collectors.toList());
    }

    private void postElectionToHigherHeroes(String groupUrl, Election election) {
        election.setPayload(Payload.election);
        List<String> membersUsernames = getHigherUsernames(tavernaService.getMembersUsernames(groupUrl));// higher usernames
        logger.info(membersUsernames);
        List<String> heroUrls = tavernaService.getHeroUrls();
        logger.info(heroUrls);

        for (String heroUrl : heroUrls) {
            try {
                blackboardService.postElection(heroUrl, election);
            } catch (ResourceAccessException ex) {

            }
        }
    }
}
