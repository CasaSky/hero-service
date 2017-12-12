package de.haw.heroservice.controller;

import de.haw.heroservice.BlackboardService;
import de.haw.heroservice.component.entities.Assignment;
import de.haw.heroservice.component.entities.Callback;
import de.haw.heroservice.component.dtos.HeroDto;
import de.haw.heroservice.component.entities.Election;
import de.haw.heroservice.component.entities.Hiring;
import de.haw.heroservice.component.TavernaService;
import de.haw.heroservice.component.utils.BullyAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

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

    @RequestMapping(value = "/hero", method = RequestMethod.GET)
    public ResponseEntity<HeroDto> info() {

        return new ResponseEntity<>(heroDto, HttpStatus.OK);
    }

    @RequestMapping(value = "/hero/hirings", method = RequestMethod.POST)
    public ResponseEntity<?> addHiring(@RequestBody Hiring hiring) {

        ResponseEntity<?> response = tavernaService.joinGroup(hiring.getGroup());

        // Refresh new group
        if (response.getStatusCode().is2xxSuccessful()) {
            heroDto.setGroup(hiring.getGroup());
        }
        return response;
    }

    @RequestMapping(value="/hero/assignments", method = RequestMethod.POST)
    public ResponseEntity<?> addAssignment(@RequestBody Assignment assignment) {
        heroDto.setIdle(true);
        ResponseEntity<?> response = blackboardService.solveTask(assignment);
        heroDto.setIdle(!response.getStatusCode().is2xxSuccessful());
        return response;
    }

    @RequestMapping(value="/hero/callback", method = RequestMethod.POST)
    public ResponseEntity<?> callback(@RequestBody Callback callback) { //TODO need only string for data?
        //TODO post tokens in resource, and post result token in quest deliveries.
        return null;
    }

    @RequestMapping(value="/hero/callback", method = RequestMethod.GET)
    public ResponseEntity<?> callback() {
        return null;
    }

    @RequestMapping(value="/hero/election", method = RequestMethod.POST)
    public ResponseEntity<?> election(@RequestBody Election election) {
        if (!bullyAlgorithm.electHero(heroDto, election)) {
            addAssignment(election.getJob());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
