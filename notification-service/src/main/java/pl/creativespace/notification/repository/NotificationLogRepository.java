package pl.creativespace.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.creativespace.notification.domain.NotificationLog;

import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
}
