package de.haw.heroservice.controller;

import de.haw.heroservice.BlackboardService;
import de.haw.heroservice.component.entities.Assignment;
import de.haw.heroservice.component.entities.Callback;
import de.haw.heroservice.component.HeroDto;
import de.haw.heroservice.component.entities.Hiring;
import de.haw.heroservice.component.TavernaService;
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

    @RequestMapping(value = "/hero", method = RequestMethod.GET)
    public ResponseEntity<HeroDto> info() {

        return new ResponseEntity<>(heroDto, HttpStatus.OK);
    }

    @RequestMapping(value = "/hero/hirings", method = RequestMethod.POST)
    public ResponseEntity<?> addHiring(@RequestBody Hiring hiring) {

        return tavernaService.joinGroup(hiring.getGroup());
    }

    @RequestMapping(value="/hero/assignments", method = RequestMethod.POST)
    public ResponseEntity<?> addAssignment(@RequestBody Assignment assignment) {

        return blackboardService.solveTask(assignment);
    }

    @RequestMapping(value="/hero/callback", method = RequestMethod.POST)
    public ResponseEntity<?> callback(@RequestBody Callback callback) {

        //TODO finish quest: dont forget required members
        return null;
    }

}
