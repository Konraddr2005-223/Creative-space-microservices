package pl.creativespace.resource.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.creativespace.resource.domain.PriceTier;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceTierRequest(
        @NotNull PriceTier.PricingUnit unit,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        Instant validFrom,
        Instant validTo
) {
}
