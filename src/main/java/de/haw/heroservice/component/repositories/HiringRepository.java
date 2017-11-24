package de.haw.heroservice.component.repositories;

import de.haw.heroservice.component.entities.Hiring;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HiringRepository  extends JpaRepository<Hiring, Integer> {
}
