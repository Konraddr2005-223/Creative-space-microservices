package pl.creativespace.resource.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pl.creativespace.resource.domain.PriceTier;
import pl.creativespace.resource.domain.Resource;
import pl.creativespace.resource.domain.ResourceType;
import pl.creativespace.resource.dto.PriceTierRequest;
import pl.creativespace.resource.dto.PriceTierResponse;
import pl.creativespace.resource.dto.ResourceRequest;
import pl.creativespace.resource.dto.ResourceResponse;
import pl.creativespace.resource.repository.PriceTierRepository;
import pl.creativespace.resource.repository.ResourceRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ResourceCatalogService {

    private final ResourceRepository resourceRepository;
    private final PriceTierRepository priceTierRepository;

    public ResourceCatalogService(ResourceRepository resourceRepository, PriceTierRepository priceTierRepository) {
        this.resourceRepository = resourceRepository;
        this.priceTierRepository = priceTierRepository;
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = new Resource(
                request.name(),
                request.type(),
                request.description(),
                request.location(),
                request.active()
        );
        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional(readOnly = true)
    public ResourceResponse get(UUID id) {
        return ResourceResponse.from(requireResource(id));
    }

    @Transactional(readOnly = true)
    public List<ResourceResponse> list(ResourceType type, Boolean active) {
        List<Resource> resources;
        if (type != null && active != null) {
            resources = resourceRepository.findByTypeAndActive(type, active);
        } else if (type != null) {
            resources = resourceRepository.findByType(type);
        } else if (active != null) {
            resources = resourceRepository.findByActive(active);
        } else {
            resources = resourceRepository.findAll();
        }
        return resources.stream().map(ResourceResponse::from).toList();
    }

    @Transactional
    public ResourceResponse update(UUID id, ResourceRequest request) {
        Resource resource = requireResource(id);
        resource.setName(request.name());
        resource.setType(request.type());
        resource.setDescription(request.description());
        resource.setLocation(request.location());
        resource.setActive(request.active());
        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public PriceTierResponse addPriceTier(UUID resourceId, PriceTierRequest request) {
        validateValidityWindow(request);
        Resource resource = requireResource(resourceId);
        PriceTier tier = new PriceTier(
                resource,
                request.unit(),
                request.amount(),
                request.currency().toUpperCase(),
                request.validFrom(),
                request.validTo()
        );
        return PriceTierResponse.from(priceTierRepository.save(tier));
    }

    @Transactional(readOnly = true)
    public List<PriceTierResponse> listPriceTiers(UUID resourceId) {
        requireResource(resourceId);
        return priceTierRepository.findByResourceIdOrderByCreatedAtAsc(resourceId).stream()
                .map(PriceTierResponse::from)
                .toList();
    }

    private Resource requireResource(UUID id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found: " + id));
    }

    private static void validateValidityWindow(PriceTierRequest request) {
        if (request.validFrom() != null && request.validTo() != null
                && !request.validTo().isAfter(request.validFrom())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "validTo must be after validFrom");
        }
    }
}
