package pl.creativespace.resource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import pl.creativespace.resource.domain.Resource;
import pl.creativespace.resource.domain.ResourceType;
import pl.creativespace.resource.dto.ResourceRequest;
import pl.creativespace.resource.dto.ResourceResponse;
import pl.creativespace.resource.repository.PriceTierRepository;
import pl.creativespace.resource.repository.ResourceRepository;
import pl.creativespace.resource.service.ResourceCatalogService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceCatalogServiceTest {

    @Mock
    ResourceRepository resourceRepository;

    @Mock
    PriceTierRepository priceTierRepository;

    @InjectMocks
    ResourceCatalogService catalogService;

    @Test
    void createPersistsResource() {
        ResourceRequest request = new ResourceRequest(
                "Studio X", ResourceType.STUDIO, "desc", "Warsaw", true
        );
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> {
            Resource resource = invocation.getArgument(0);
            // mimic JPA id assignment for mapping assertions
            var idField = Resource.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(resource, UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
            var createdField = Resource.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(resource, java.time.Instant.parse("2026-01-01T00:00:00Z"));
            var updatedField = Resource.class.getDeclaredField("updatedAt");
            updatedField.setAccessible(true);
            updatedField.set(resource, java.time.Instant.parse("2026-01-01T00:00:00Z"));
            return resource;
        });

        ResourceResponse response = catalogService.create(request);

        assertThat(response.name()).isEqualTo("Studio X");
        assertThat(response.type()).isEqualTo(ResourceType.STUDIO);
        verify(resourceRepository).save(any(Resource.class));
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(resourceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.get(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Resource not found");
    }

    @Test
    void listFiltersByTypeAndActive() {
        Resource resource = new Resource("Cam", ResourceType.EQUIPMENT, null, "KRK", true);
        when(resourceRepository.findByTypeAndActive(ResourceType.EQUIPMENT, true))
                .thenReturn(List.of(resource));

        List<ResourceResponse> result = catalogService.list(ResourceType.EQUIPMENT, true);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Cam");
    }
}
