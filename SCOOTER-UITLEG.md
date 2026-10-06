# Scooter – uitleg van wat we hebben gebouwd

Dit document legt in gewone taal uit wat er voor de **scooter** in de VlugVerkoop-API is gebouwd, hoe het werkt en hoe je het test. Je hoeft geen Kotlin te kennen om dit te volgen.

---

## In het kort

- Een scooter heeft nu een **snelheid** (25 of 45 km/u) en een **kentekenkleur** (blauw of geel), zoals het plan van aanpak vraagt.
- De app kan alle scooters ophalen via het adres **`/scooters`**.
- Er zijn **3 automatische tests** die controleren of `/scooters` goed werkt. Ze slagen alle drie.

---

## Hoe werkt het? (een vergelijking)

Zie de server als een winkel:

| Onderdeel | Vergelijking | Wat het doet |
|---|---|---|
| `Scooter.kt` | Het **invulformulier** | Beschrijft wat een scooter is en welke regels gelden |
| `ScooterRepository.kt` | Het **magazijn** | Hier liggen de scooters opgeslagen |
| `ScooterResponse.kt` | De **verpakking** | Bepaalt welke gegevens naar de app gaan |
| `ScooterRoutes.kt` | Het **loket** | Het adres (`/scooters`) waar de app aanklopt |
| `Routing.kt` | De **lijst met loketten** | Zet het scooterloket open in de server |

Zo loopt een verzoek van de app:

```
App vraagt /scooters
   → loket (ScooterRoutes) haalt de scooters uit het magazijn (ScooterRepository)
   → elke scooter gaat in de verpakking (ScooterResponse)
   → de server stuurt alles als JSON terug naar de app
```

We hebben dezelfde opbouw gebruikt als Jawad voor auto's (`/cars`), zodat alles in het project op dezelfde manier werkt.

---

## De bestanden één voor één

### 1. `model/Scooter.kt` – het formulier

Elke scooter heeft wat elk voertuig heeft (id, merk, model, bouwjaar, prijs, kilometerstand). Daarnaast heeft een scooter:

- **`maxSpeedKmh`**: de maximale snelheid, 25 of 45 km/u.
- **`plateColor`**: de kentekenkleur. **Die vul je niet zelf in.** De scooter rekent hem uit op basis van de snelheid.

| Snelheid | Soort | Kenteken |
|---|---|---|
| 25 km/u | Snorfiets | Blauw (`BLUE`) |
| 45 km/u | Bromfiets | Geel (`YELLOW`) |

**Waarom rekent de scooter de kleur zelf uit?** Dan kan niemand per ongeluk "45 km/u met een blauw kenteken" invullen, want dat bestaat niet.

**Wat als je een andere snelheid invult, bijvoorbeeld 80?** Dan weigert het programma de scooter met de melding:
`Speed class must be 25 or 45 km/h, was 80`

In dit bestand staat ook de keuzelijst met kentekenkleuren:

```kotlin
enum class PlateColor(val maxSpeedKmh: Int) {
    BLUE(25),
    YELLOW(45)
}
```

Een `enum` is een **vaste keuzelijst**, net als een uitklapmenu met maar twee opties.

### 2. `repository/ScooterRepository.kt` – het magazijn

Hier staan voorlopig 3 voorbeeldscooters. Later komt er een echte database voor in de plaats.

| Id | Merk | Model | Bouwjaar | Prijs | Km-stand | Snelheid | Kenteken |
|---|---|---|---|---|---|---|---|
| 1 | Vespa | Primavera | 2021 | € 2650 | 4200 | 45 km/u | Geel |
| 2 | Piaggio | Zip | 2019 | € 1150 | 9800 | 25 km/u | Blauw |
| 3 | Kymco | Agility | 2020 | € 1395 | 6100 | 25 km/u | Blauw |

### 3. `dto/ScooterResponse.kt` – de verpakking

Dit bepaalt welke gegevens de app krijgt:

| Veld | Betekenis |
|---|---|
| `id` | Nummer van de advertentie |
| `brand` | Merk |
| `model` | Model |
| `year` | Bouwjaar |
| `price` | Prijs in euro |
| `mileage` | Kilometerstand |
| `maxSpeedKmh` | Maximale snelheid (25 of 45) |
| `plateColor` | Kentekenkleur (`BLUE` of `YELLOW`) |
| `listingFee` | Advertentiekosten (nu € 2,95) |

### 4. `routes/ScooterRoutes.kt` – het loket

Maakt het adres **`GET /scooters`**. Als de app dit opvraagt, krijgt hij alle scooters terug.

### 5. `Routing.kt` – de lijst met loketten

Hier is één regel bijgekomen, `scooterRoutes(ScooterRepository())`, zodat het scooterloket echt openstaat.

---

## Wat krijgt de app terug?

Zo ziet het antwoord van `/scooters` eruit (eerste scooter):

```json
{
  "id": 1,
  "brand": "Vespa",
  "model": "Primavera",
  "year": 2021,
  "price": 2650.0,
  "mileage": 4200,
  "maxSpeedKmh": 45,
  "plateColor": "YELLOW",
  "listingFee": 2.95
}
```

---

## De tests

De tests staan in **`src/test/kotlin/ScooterRoutesTest.kt`**.

Een test is een stukje code dat de app nadoet: hij vraagt `/scooters` op en controleert of het antwoord klopt. De server hoeft daarvoor niet aan te staan. In het plan van aanpak heten dit **integratietesten met Ktor testApplication**.

| # | Naam van de test | Wat wordt gecontroleerd |
|---|---|---|
| 1 | `GET scooters returns 200 with JSON` | De server antwoordt met "OK" (code 200) en stuurt JSON terug. |
| 2 | `GET scooters returns all scooters with their details` | Er komen 3 scooters terug, en de Vespa heeft het juiste merk, model, de juiste prijs en snelheid. |
| 3 | `GET scooters gives blue plate for 25 kmh and yellow plate for 45 kmh` | 25 km/u geeft een blauw kenteken en 45 km/u een geel kenteken. |

**Resultaat:** 3 van 3 geslaagd, 0 fouten.

Er bestaat ook nog een oudere test, `ServerTest.kt`. Die controleert alleen of de server opstart (adres `/`).

---

## Zelf proberen

### De server starten

- **In de terminal:** `sh ./gradlew run`
- **In IntelliJ:** klik op de groene ▶ naast `main` in `main.kt`.

De voortgangsbalk blijft op **"83% EXECUTING"** staan. Dat is normaal: de server draait. Stoppen doe je met **Ctrl+C**.

Open daarna in je browser: **http://127.0.0.1:8080/scooters**

### De tests uitvoeren

- **In IntelliJ:** open `ScooterRoutesTest.kt` en klik op de groene ▶ naast `class ScooterRoutesTest`.
- **In de terminal:**
  ```
  sh ./gradlew test --tests "nl.vlugverkoop.ScooterRoutesTest"
  ```
- **Alle tests van het project tegelijk:** `sh ./gradlew test`

---

## Problemen die we tegenkwamen

| Probleem | Oorzaak | Oplossing |
|---|---|---|
| `git pull` geeft *"cannot pull with rebase: You have unstaged changes"* | Je had nog wijzigingen die niet gecommit waren. Git wil dan niet binnenhalen. | Gebruik `git pull --autostash`. Git zet je wijzigingen dan even opzij en daarna weer terug. |
| Browser toont *"HTTP header is larger than 8192 bytes"* | Je browser stuurt te veel cookies mee voor `localhost` (van andere projecten). | Gebruik **http://127.0.0.1:8080** in plaats van `localhost`, of een incognitovenster. |
| `./gradlew` geeft *"permission denied"* | Het bestand mag niet direct worden uitgevoerd. | Zet `sh` ervoor: `sh ./gradlew ...` |

---

## Nog te doen / overleggen met het team

- **Eén adres voor alle voertuigen.** Volgens het plan van aanpak wordt het uiteindelijk `GET /vehicles?type=SCOOTER`. Nu hebben auto's `/cars` en scooters `/scooters`.
- **`getType()` en `VehicleType`.** Volgens het plan moet elk voertuig zijn soort kunnen noemen (auto, motor of scooter). Dat moet in `Vehicle.kt` gebeuren, dus dat raakt alle voertuigen.
- **Advertentiekosten (`listingFee`).** Betalingen vallen volgens het plan buiten de scope, en het brandbook belooft "geen verborgen kosten". Bespreek of deze kosten eruit moeten.
- **Dezelfde nummers.** Auto's en scooters hebben allebei nummer 1, 2 en 3. Met een echte database moet elk voertuig een eigen nummer krijgen.
- **Unit-tests.** Volgens het plan horen bij deel 1 ook 3 unit-tests, bijvoorbeeld "een negatieve prijs wordt geweigerd".
- **Spelling van de merknaam.** In `Vehicle.kt`, `Car.kt` en `Motorcycle.kt` staat nog "Vlugverkoop" in plaats van "VlugVerkoop".

---

## Woordenlijst

| Woord | Betekenis |
|---|---|
| **class** | Een bouwtekening of formulier, bijvoorbeeld voor een scooter |
| **val** | Een vakje dat na het invullen niet meer verandert |
| **enum** | Een vaste keuzelijst (zoals blauw/geel) |
| **Int / Double** | Een heel getal (45) / een getal met komma (2650.0) |
| **override** | "Ik vul hier zelf in wat het algemene formulier verplicht stelt" |
| **endpoint** | Een adres op de server, zoals `/scooters` |
| **GET** | "Geef me gegevens" (alleen ophalen, niets veranderen) |
| **JSON** | De tekstvorm waarin de server gegevens naar de app stuurt |
| **Repository** | De plek waar gegevens worden opgeslagen en opgehaald |
| **DTO / Response** | De "verpakking" van gegevens die naar de app gaan |
| **Compileren** | De computer vertaalt de code en controleert of er fouten in zitten |
| **Test** | Code die automatisch controleert of iets goed werkt |
| **Commit** | Je wijzigingen opslaan in Git, met een korte beschrijving |
