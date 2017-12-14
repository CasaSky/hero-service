package de.haw.heroservice.controller;

import de.haw.heroservice.BlackboardService;
import de.haw.heroservice.component.entities.Assignment;
import de.haw.heroservice.component.entities.Callback;
import de.haw.heroservice.component.dtos.HeroDto;
import de.haw.heroservice.component.entities.Election;
import de.haw.heroservice.component.entities.Hiring;
import de.haw.heroservice.component.TavernaService;
import de.haw.heroservice.component.utils.BullyAlgorithm;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@RestController
public class HeroController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HeroDto heroDto;

    @Autowired
    private TavernaService tavernaService;

    @Autowired
    private BlackboardService blackboardService;

    @Autowired
    private BullyAlgorithm bullyAlgorithm;

    private Assignment assignment;

    private Hiring hiring;

    private Integer requiredPlayers = 0;

    private List<Callback> callbacks = new ArrayList<>();

    private Logger logger = Logger.getLogger(HeroController.class);

    @RequestMapping(value = "/hero", method = RequestMethod.GET)
    public ResponseEntity<HeroDto> info() {

        logger.info("Hero request received!");
        return new ResponseEntity<>(heroDto, HttpStatus.OK);
    }

    @RequestMapping(value = "/hero/hirings", method = RequestMethod.POST)
    public ResponseEntity<?> addHiring(@RequestBody Hiring hiring) {
        logger.info("Hiring request received!");

        this.hiring = hiring;

        ResponseEntity<?> response;
        try {
            response = tavernaService.joinGroup(hiring.getGroup());

            // Refresh new group
            if (response.getStatusCode().is2xxSuccessful()) {
                heroDto.setGroup(hiring.getGroup());
            }
        } catch (HttpStatusCodeException e){
            logger.error("Error on Hiring request!", e);
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
        return response;
    }

    @RequestMapping(value="/hero/assignments", method = RequestMethod.POST)
    public ResponseEntity<?> addAssignment(@RequestBody Assignment assignment) {
        logger.info("Assignment request received!");
            this.assignment = assignment;
            heroDto.setIdle(true);
            ResponseEntity<?> response;
            try {
                response = blackboardService.solveTask(assignment);
                heroDto.setIdle(!response.getStatusCode().is2xxSuccessful());
            } catch (HttpStatusCodeException e) {
                logger.error("Error on Assignment request!", e);
                return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
            }
           return response;
    }

    @RequestMapping(value="/hero/callback", method = RequestMethod.POST)
    public ResponseEntity<?> callback(@RequestBody Callback callback) { //TODO need only string for data?
        //TODO post tokens in resource, and post result token in quest deliveries.
        callbacks.add(callback);
        logger.info("Post on callback received!");
        requiredPlayers++;

        if (requiredPlayers.equals(Integer.parseInt(blackboardService.getRequiredPlayers(callback.getTask())))) {
            return blackboardService.solveQuest(callbacks);
        }
        logger.info("Callback ok!");
        return new ResponseEntity<>(HttpStatus.OK);

    }

    @RequestMapping(value="/hero/callback", method = RequestMethod.GET)
    public ResponseEntity<?> callback() {
        return null;
    }

    @RequestMapping(value="/hero/election", method = RequestMethod.POST)
    public ResponseEntity<?> election(@RequestBody Election election) {
        logger.info("Election request received!");
        try {
            if (bullyAlgorithm.electHero(heroDto, election)) {
                addAssignment(election.getJob());
            }
        } catch (HttpStatusCodeException e) {
            logger.error("Error on election request!", e);
            return new ResponseEntity<>(e.getMessage(), e.getStatusCode());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
