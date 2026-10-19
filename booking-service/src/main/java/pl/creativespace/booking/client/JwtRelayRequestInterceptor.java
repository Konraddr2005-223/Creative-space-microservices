package pl.creativespace.booking.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * WHY relay the inbound Authorization header to payment-service:
 * payment is also a JWT resource server. Without this, Feign calls would get 401
 * even when the user already authenticated at Gateway/booking.
 * Trade-off: ties the saga hop to the user token lifetime (fine for sync checkout).
 */
@Component
public class JwtRelayRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        String authorization = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && !authorization.isBlank()) {
            template.header(HttpHeaders.AUTHORIZATION, authorization);
        }
    }
}
