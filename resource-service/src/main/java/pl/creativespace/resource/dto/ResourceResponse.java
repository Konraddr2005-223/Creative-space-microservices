package pl.creativespace.resource.dto;

import pl.creativespace.resource.domain.Resource;
import pl.creativespace.resource.domain.ResourceType;

import java.time.Instant;
import java.util.UUID;

public record ResourceResponse(
        UUID id,
        String name,
        ResourceType type,
        String description,
        String location,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getDescription(),
                resource.getLocation(),
                resource.isActive(),
                resource.getCreatedAt(),
                resource.getUpdatedAt(),
                resource.getVersion()
        );
    }
}
