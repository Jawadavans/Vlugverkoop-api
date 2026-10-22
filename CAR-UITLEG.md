# Auto – uitleg van wat we hebben gebouwd

Dit document legt in gewone taal uit wat er voor de **auto** in de VlugVerkoop-API is gebouwd, hoe het werkt en hoe je het test.

---

## In het kort

- Een auto heeft naast de gewone voertuiggegevens een **aantal deuren** (2 tot en met 7).
- De server controleert de gegevens van een auto met `validate()` voordat hij wordt opgeslagen.
- De app kan auto's **ophalen, zoeken/filteren, plaatsen, wijzigen en verwijderen** via **`/cars`**.
- Er zijn **5 automatische tests** voor de auto: 3 unittests en 2 integratietesten. Ze slagen allemaal.

---

## Hoe werkt het? (een vergelijking)

Dezelfde opbouw als bij motoren en scooters:

| Onderdeel | Vergelijking | Wat het doet |
|---|---|---|
| `Car.kt` | Het **invulformulier** | Beschrijft wat een auto is en controleert de regels (`validate()`) |
| `Validatable.kt` | De **afspraak** | Elk voertuig dat `Validatable` is, heeft een `validate()` |
| `CarRepository.kt` | Het **magazijn** | Slaat de auto's op en kan erin zoeken |
| `CarResponse.kt` | De **verpakking** | Welke gegevens naar de app gaan |
| `CreateCarRequest.kt` | Het **inleverformulier** | Wat de app stuurt bij een nieuwe of gewijzigde advertentie (zonder id) |
| `ValidationErrorResponse.kt` | Het **afwijsbriefje** | De lijst met fouten als de gegevens niet kloppen |
| `CarRoutes.kt` | Het **loket** | De adressen onder `/cars` |
| `Routing.kt` | De **lijst met loketten** | Zet het autoloket open: `carRoutes(CarRepository())` |

Zo loopt een nieuwe advertentie door de server:

```
App stuurt POST /cars met JSON
   → loket (CarRoutes) leest de JSON in als CreateCarRequest
   → toCar(id) maakt er een Car van met een nieuw nummer
   → car.validate() controleert de regels
        → fouten?  → 400 met {"errors": [...]}, er wordt niets opgeslagen
        → goed?    → het magazijn slaat de auto op → 201 met de nieuwe auto
```

---

## De regels van een auto

`validate()` in `Car.kt` geeft een lijst met fouten terug. Een lege lijst betekent: de auto is goed.

| Regel | Foutmelding |
|---|---|
| Merk en model zijn verplicht | `brand is required` / `model is required` |
| Prijs groter dan 0 | `price must be greater than 0` |
| Kilometerstand niet negatief | `mileage cannot be negative` |
| Bouwjaar tussen 1950 en nu | `year must be between 1950 and …` |
| Aantal deuren tussen 2 en 7 | `doors must be between 2 and 7` |

De foutteksten zijn hetzelfde als bij motor en scooter, zodat de app ze overal op dezelfde manier kan tonen.

Het huidige jaar wordt opgehaald met `Year.now()`, zodat de code volgend jaar niet aangepast hoeft te worden.

---

## De adressen (endpoints)

| Verzoek | Wat het doet | Antwoord |
|---|---|---|
| `GET /cars` | Alle auto's (6 voorbeeldauto's) | 200 |
| `GET /cars?search=…&maxPrice=…&maxMileage=…&doors=…` | Zoeken en filteren; elk filter is optioneel | 200, of 400 als een filter geen getal is |
| `GET /cars/{id}` | Eén auto | 200, of 404 als hij niet bestaat |
| `POST /cars` | Nieuwe advertentie plaatsen | 201, of 400 met `{"errors":[...]}` |
| `PUT /cars/{id}` | Advertentie bijwerken, bijvoorbeeld na een prijswijziging | 200, 400 met `{"errors":[...]}`, of 404 als hij niet bestaat |
| `DELETE /cars/{id}` | Advertentie verwijderen | 204, of 404 als hij niet bestaat |

**Foutafhandeling, gelijk aan motor en scooter:**
- **404** betekent: de auto bestaat niet.
- **400** betekent: de gegevens kloppen niet (foute waarden, kapotte JSON, of een id of filter dat geen getal is).

`search` zoekt in merk **en** model, en hoofdletters maken niet uit. Voorbeeld: `/cars?search=volkswagen&maxPrice=15000` geeft alleen de Golf.

### Voorbeeld: nieuwe auto plaatsen

```json
{
  "brand": "Audi",
  "model": "A3",
  "year": 2019,
  "price": 17500.0,
  "mileage": 60000,
  "doors": 5
}
```

Met `"price": -1.0` en `"doors": 9` krijg je:

```json
{ "errors": ["price must be greater than 0", "doors must be between 2 and 7"] }
```

---

## De voorbeeldauto's

| Id | Merk | Model | Bouwjaar | Prijs | Km-stand | Deuren |
|---|---|---|---|---|---|---|
| 1 | Volkswagen | Golf | 2018 | € 14500 | 85000 | 5 |
| 2 | Toyota | Yaris | 2020 | € 16950 | 42000 | 5 |
| 3 | BMW | 3 Series | 2016 | € 18750 | 120000 | 4 |
| 4 | Volkswagen | Polo | 2021 | € 17250 | 31000 | 5 |
| 5 | Peugeot | 208 | 2015 | € 6950 | 135000 | 3 |
| 6 | Tesla | Model 3 | 2022 | € 34900 | 48000 | 4 |

Twee Volkswagens, zodat zoeken op merk meer dan één resultaat geeft.

---

## De tests

**`src/test/kotlin/CarTest.kt`** – unittests (3). Ze testen alleen de klasse `Car`, zonder server:

| # | Test | Wat wordt gecontroleerd |
|---|---|---|
| 1 | `valid car has no validation errors` | Een goede auto geeft een lege foutenlijst (happy flow). |
| 2 | `car with a price of zero is rejected` | Prijs 0 geeft `price must be greater than 0`. |
| 3 | `car with too few or too many doors is rejected` | 1 en 8 deuren geven allebei `doors must be between 2 and 7`. |

**`src/test/kotlin/CarRoutesTest.kt`** – integratietesten (2), via de echte API. Samen dekken ze alle vier de CRUD-soorten:

| # | Test | CRUD | Wat wordt gecontroleerd |
|---|---|---|---|
| 1 | `GET returns and filters cars, and POST only stores valid ones` | Read + Create | `/cars` geeft 200, JSON en 6 auto's; het filter geeft alleen de Golf; een goede auto krijgt 201 en id 7; een foute auto krijgt 400 met beide foutmeldingen en wordt niet opgeslagen. |
| 2 | `PUT replaces and DELETE removes a car, both giving 404 for an unknown id` | Update + Delete | PUT vervangt auto 1 zonder een extra auto toe te voegen; PUT op 99 geeft 404; DELETE geeft 204 en de auto is weg; DELETE op 99 geeft 404. |

Uitvoeren:

```
sh ./gradlew test --tests "nl.vlugverkoop.Car*"
```

In IntelliJ: rechtsklik op `src/test/kotlin` → **Run 'Tests in ...'**.

---

## Zelf proberen

1. Start de server (groene ▶ bij `EngineMain`).
2. Open `requests.http`.
3. Klik op de groene ▶ naast een auto-verzoek, bijvoorbeeld *"Create a car with wrong data (400 with errors)"*.
4. Onderaan zie je het antwoord van de server.

Gebruik `http://127.0.0.1:8080` in plaats van `localhost`.

---

## Problemen die we tegenkwamen

| Probleem | Oorzaak | Oplossing |
|---|---|---|
| Merge-conflicten na het ophalen van de code van het team | Er was lokaal gewerkt zonder eerst te pullen | Voor de scooterbestanden en `Routing.kt` de versie van GitHub gekozen (**Accept Right**), daarna het eigen werk opnieuw toegevoegd. Volgende keer: eerst **Git → Pull**, dan pas beginnen. |
| Kapotte JSON gaf een 500 (serverfout) | `StatusPages.kt` maakt van elke niet-opgevangen fout een 500 | `receiveCarRequestOrNull()` vangt de fout op en geeft 400 terug. |

---

## Nog te doen / overleggen met het team

- **Opslag.** Auto's staan nu in het geheugen. Na een herstart zijn nieuwe advertenties weg.
- **Eén adres voor alle voertuigen.** Volgens het plan van aanpak wordt het uiteindelijk `GET /vehicles?type=CAR`.
- **Dezelfde nummers.** Auto 1, motor 1 en scooter 1 hebben hetzelfde nummer. Met een echte database moet elk voertuig een eigen nummer krijgen.