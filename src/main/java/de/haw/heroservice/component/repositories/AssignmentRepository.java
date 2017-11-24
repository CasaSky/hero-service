package de.haw.heroservice.component.repositories;

import de.haw.heroservice.component.entities.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository  extends JpaRepository<Assignment, Integer> {
}
