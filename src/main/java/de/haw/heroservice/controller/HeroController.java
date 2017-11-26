package de.haw.heroservice.controller;

import de.haw.heroservice.component.entities.Hero;
import de.haw.heroservice.component.HeroDto;
import de.haw.heroservice.component.entities.Hiring;
import de.haw.heroservice.component.TavernaService;
import de.haw.heroservice.component.repositories.HeroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.security.Principal;

@RestController
public class HeroController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private HeroDto heroDto;

    @Autowired
    private TavernaService tavernaService;

    @Autowired
    private HeroRepository heroRepository;

    @RequestMapping(value = "/hero", method = RequestMethod.GET)
    public ResponseEntity<HeroDto> info() {

        return new ResponseEntity<>(heroDto, HttpStatus.OK);
    }

    @RequestMapping(value = "/hero/hirings", method = RequestMethod.POST)
    public ResponseEntity<?> addHiring(@RequestBody Hiring hiring) {

       // Hero hero = new Hero();//heroRepository.findOne(1);

        /*if (hero.getGroup()!=null) {
            return new ResponseEntity<>("Can't join the group.", HttpStatus.CONFLICT);
        }

        hero.setUser(heroDto.getUser());
        hero.addHiring(hiring);*/

        return tavernaService.joinGroup(hiring.getGroup());
    }

}
