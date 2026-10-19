package pl.creativespace.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.creativespace.resource.domain.ResourceType;

public record ResourceRequest(
        @NotBlank @Size(max = 160) String name,
        @NotNull ResourceType type,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 160) String location,
        @NotNull Boolean active
) {
}
