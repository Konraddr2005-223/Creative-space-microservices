package pl.creativespace.resource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.creativespace.resource.domain.Resource;
import pl.creativespace.resource.domain.ResourceType;

import java.util.List;
import java.util.UUID;

public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    List<Resource> findByTypeAndActive(ResourceType type, boolean active);

    List<Resource> findByType(ResourceType type);

    List<Resource> findByActive(boolean active);
}
