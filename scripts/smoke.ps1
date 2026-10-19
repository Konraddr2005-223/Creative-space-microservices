# Smoke: JWT → list resources → create booking → checkout (via Gateway :8080).
# Prerequisites: infra + apps up (docker compose --profile apps up -d --build, or local mvnw runs).
$ErrorActionPreference = "Stop"

$Keycloak = if ($env:KEYCLOAK_URL) { $env:KEYCLOAK_URL } else { "http://localhost:8180" }
$Gateway = if ($env:GATEWAY_URL) { $env:GATEWAY_URL } else { "http://localhost:8080" }
$ResourceId = "11111111-1111-1111-1111-111111111111"

Write-Host "==> Token (demo/demo)"
$body = @{
  grant_type = "password"
  client_id  = "creative-space-public"
  username   = "demo"
  password   = "demo"
}
$tokenResponse = Invoke-RestMethod -Method Post `
  -Uri "$Keycloak/realms/creative-space/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" -Body $body
$TOKEN = $tokenResponse.access_token
if (-not $TOKEN) { throw "No access_token from Keycloak" }

$headers = @{
  Authorization  = "Bearer $TOKEN"
  "Content-Type" = "application/json"
}

Write-Host "==> List resources"
$resources = Invoke-RestMethod -Uri "$Gateway/resource-service/api/resources" -Headers @{ Authorization = "Bearer $TOKEN" }
Write-Host ("    count={0}" -f @($resources).Count)

$hour = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() % 20
$startAt = (Get-Date).ToUniversalTime().Date.AddDays(40).AddHours($hour).ToString("yyyy-MM-dd'T'HH:mm:ss'Z'")
$endAt = ([DateTime]::Parse($startAt)).ToUniversalTime().AddHours(2).ToString("yyyy-MM-dd'T'HH:mm:ss'Z'")

Write-Host "==> Create booking $startAt → $endAt"
$bookingBody = @{
  resourceId = $ResourceId
  userId     = "demo"
  startAt    = $startAt
  endAt      = $endAt
} | ConvertTo-Json
$booking = Invoke-RestMethod -Method Post `
  -Uri "$Gateway/booking-service/api/bookings" `
  -Headers $headers `
  -Body $bookingBody
$bookingId = $booking.id
Write-Host "    bookingId=$bookingId status=$($booking.status)"

Write-Host "==> Checkout"
$checkout = Invoke-RestMethod -Method Post `
  -Uri "$Gateway/booking-service/api/bookings/$bookingId/checkout" `
  -Headers $headers `
  -Body '{"amount": 180.00, "currency": "PLN"}'
Write-Host "    status=$($checkout.status)"

if ($checkout.status -ne "CONFIRMED") {
  throw "Expected CONFIRMED, got $($checkout.status)"
}
Write-Host "SMOKE OK"
