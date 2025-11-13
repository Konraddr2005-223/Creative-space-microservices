# Creative-space-microservices
Mikroserwisowa platforma (Java 17, Spring Boot 3) do rezerwacji zasobów kreatywnych. Implementuje Spring Cloud, Kafkę i Docker.
"Creative-Space" to w pełni funkcjonalna platforma oparta na architekturze mikroserwisowej, napisana w Javie 17 i Spring Boot 3. Projekt ten symuluje działanie komercyjnego systemu C2C/B2C do rezerwacji niszowych zasobów kreatywnych, takich jak studia nagraniowe, profesjonalny sprzęt fotograficzny czy specjalistyczne warsztaty.

Głównym celem jest zademonstrowanie budowy nowoczesnego, rozproszonego systemu, który radzi sobie z realnymi wyzwaniami inżynieryjnymi. Kładzie nacisk nie tylko na logikę biznesową, ale przede wszystkim na wzorce architektoniczne używane w komercyjnych projektach.

System rozwiązuje kluczowe problemy, takie jak:

Współbieżność: Zarządzanie wyścigami o zasoby (tzw. "race conditions"), gdy wielu użytkowników próbuje zarezerwować ten sam slot w tym samym czasie.

Odporność na awarie (Fault Tolerance): Zapewnienie, że awaria jednego komponentu (np. serwisu płatności) nie powoduje zatrzymania całego procesu rezerwacji.

Komunikacja między serwisami: Implementacja zarówno synchronicznej (REST API przez OpenFeign) jak i asynchronicznej (event-driven przy użyciu Apache Kafka) wymiany danych.

Architektura składa się z kilku niezależnych usług, w tym:

Serwisy infrastrukturalne: API Gateway (Spring Cloud Gateway) jako pojedynczy punkt wejścia oraz Service Discovery (Eureka) jako rejestr usług.

Serwisy biznesowe: User Service (do uwierzytelniania przez JWT), Resource Service (katalog zasobów i kalendarze), Booking Service (rdzeń logiki rezerwacji), Payment Service oraz Notification Service.

Cały system jest w pełni skonteneryzowany przy użyciu Docker i Docker Compose, co pozwala na łatwe uruchomienie kompletnego środowiska deweloperskiego jednym poleceniem. Projekt ten służy jako praktyczny przykład implementacji zaawansowanych koncepcji backendowych.
