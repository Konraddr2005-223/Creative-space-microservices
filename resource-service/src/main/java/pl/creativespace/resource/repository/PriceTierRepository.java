package pl.creativespace.resource.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.creativespace.resource.domain.PriceTier;

import java.util.List;
import java.util.UUID;

public interface PriceTierRepository extends JpaRepository<PriceTier, UUID> {

    List<PriceTier> findByResourceIdOrderByCreatedAtAsc(UUID resourceId);
}
