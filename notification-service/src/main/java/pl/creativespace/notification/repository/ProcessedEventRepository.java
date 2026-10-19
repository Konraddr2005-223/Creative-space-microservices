package pl.creativespace.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.creativespace.notification.domain.ProcessedEvent;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
}
