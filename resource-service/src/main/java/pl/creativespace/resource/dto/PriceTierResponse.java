package pl.creativespace.resource.dto;

import pl.creativespace.resource.domain.PriceTier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceTierResponse(
        UUID id,
        UUID resourceId,
        PriceTier.PricingUnit unit,
        BigDecimal amount,
        String currency,
        Instant validFrom,
        Instant validTo,
        Instant createdAt
) {
    public static PriceTierResponse from(PriceTier tier) {
        return new PriceTierResponse(
                tier.getId(),
                tier.getResource().getId(),
                tier.getUnit(),
                tier.getAmount(),
                tier.getCurrency(),
                tier.getValidFrom(),
                tier.getValidTo(),
                tier.getCreatedAt()
        );
    }
}
