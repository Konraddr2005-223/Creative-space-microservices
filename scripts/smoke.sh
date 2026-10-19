#!/usr/bin/env bash
# Smoke: JWT → list resources → create booking → checkout (via Gateway :8080).
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
RESOURCE_ID="11111111-1111-1111-1111-111111111111"

echo "==> Token (demo/demo)"
TOKEN="$(curl -s -X POST "${KEYCLOAK_URL}/realms/creative-space/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=creative-space-public" \
  -d "username=demo" \
  -d "password=demo" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')"
test -n "$TOKEN" || { echo "No access_token"; exit 1; }

echo "==> List resources"
curl -sf "${GATEWAY_URL}/resource-service/api/resources" \
  -H "Authorization: Bearer ${TOKEN}" >/dev/null
echo "    ok"

# Unique slot avoids EXCLUDE collisions across runs
HOUR=$(( $(date +%s) % 20 ))
START_AT="$(date -u -d "+40 days +${HOUR} hours" +%Y-%m-%dT%H:00:00Z 2>/dev/null \
  || date -u -v+40d -v+${HOUR}H +%Y-%m-%dT%H:00:00Z)"
END_AT="$(date -u -d "${START_AT} +2 hours" +%Y-%m-%dT%H:%M:%SZ 2>/dev/null \
  || date -u -j -f %Y-%m-%dT%H:%M:%SZ -v+2H "${START_AT}" +%Y-%m-%dT%H:%M:%SZ)"

echo "==> Create booking ${START_AT} → ${END_AT}"
BOOKING_JSON="$(curl -sf -X POST "${GATEWAY_URL}/booking-service/api/bookings" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"resourceId\":\"${RESOURCE_ID}\",\"userId\":\"demo\",\"startAt\":\"${START_AT}\",\"endAt\":\"${END_AT}\"}")"
BOOKING_ID="$(printf '%s' "$BOOKING_JSON" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')"
echo "    bookingId=${BOOKING_ID}"

echo "==> Checkout"
CHECKOUT_JSON="$(curl -sf -X POST "${GATEWAY_URL}/booking-service/api/bookings/${BOOKING_ID}/checkout" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"amount": 180.00, "currency": "PLN"}')"
STATUS="$(printf '%s' "$CHECKOUT_JSON" | sed -n 's/.*"status":"\([^"]*\)".*/\1/p')"
echo "    status=${STATUS}"
test "$STATUS" = "CONFIRMED" || { echo "Expected CONFIRMED"; exit 1; }
echo "SMOKE OK"
