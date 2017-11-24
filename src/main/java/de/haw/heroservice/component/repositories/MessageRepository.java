package de.haw.heroservice.component.repositories;

import de.haw.heroservice.component.entities.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Integer> {
}
