package pl.creativespace.resource.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.creativespace.resource.domain.ResourceType;
import pl.creativespace.resource.dto.PriceTierRequest;
import pl.creativespace.resource.dto.PriceTierResponse;
import pl.creativespace.resource.dto.ResourceRequest;
import pl.creativespace.resource.dto.ResourceResponse;
import pl.creativespace.resource.service.ResourceCatalogService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceCatalogService catalogService;

    public ResourceController(ResourceCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponse create(@Valid @RequestBody ResourceRequest request) {
        return catalogService.create(request);
    }

    @GetMapping
    public List<ResourceResponse> list(
            @RequestParam(required = false) ResourceType type,
            @RequestParam(required = false) Boolean active
    ) {
        return catalogService.list(type, active);
    }

    @GetMapping("/{id}")
    public ResourceResponse get(@PathVariable UUID id) {
        return catalogService.get(id);
    }

    @PutMapping("/{id}")
    public ResourceResponse update(@PathVariable UUID id, @Valid @RequestBody ResourceRequest request) {
        return catalogService.update(id, request);
    }

    @PostMapping("/{id}/price-tiers")
    @ResponseStatus(HttpStatus.CREATED)
    public PriceTierResponse addPriceTier(
            @PathVariable UUID id,
            @Valid @RequestBody PriceTierRequest request
    ) {
        return catalogService.addPriceTier(id, request);
    }

    @GetMapping("/{id}/price-tiers")
    public List<PriceTierResponse> listPriceTiers(@PathVariable UUID id) {
        return catalogService.listPriceTiers(id);
    }
}
