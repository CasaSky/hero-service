package de.haw.heroservice.controller;

import de.haw.heroservice.component.Hero;
import de.haw.heroservice.component.HeroDto;
import de.haw.heroservice.component.Hiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HeroController {

    @Autowired
    private HeroDto heroDto;

    @RequestMapping(value = "/hero", method = RequestMethod.GET)
    public ResponseEntity<HeroDto> info() {

        return new ResponseEntity<>(heroDto, HttpStatus.OK);
    }

    @RequestMapping(value = "/hero/hirings", method = RequestMethod.POST)
    public ResponseEntity<?> addHiring(@RequestBody Hiring hiring) {


        new Hero().addHiring(hiring); // TODO
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
