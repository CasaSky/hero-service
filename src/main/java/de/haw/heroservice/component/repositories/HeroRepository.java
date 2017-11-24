package de.haw.heroservice.component.repositories;

import de.haw.heroservice.component.entities.Hero;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeroRepository extends JpaRepository<Hero, Integer> {
}
