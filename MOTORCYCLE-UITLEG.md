# Motor – uitleg van wat we hebben gebouwd

Dit document legt in gewone taal uit wat er voor de **motor** in de VlugVerkoop-API is gebouwd, hoe het werkt en hoe je het test.

---

## In het kort

- Een motor heeft een **cilinderinhoud** (cc), een **vermogen** (kW), een **rijstijl** (naked, touring, sport, enduro, chopper) en een **rijbewijscategorie** (A1, A2 of A).
- De server controleert of het rijbewijs past bij de motor: **A1** tot 125 cc en 11 kW, **A2** tot 35 kW, daarboven **A**.
- De app kan motoren **ophalen, zoeken/filteren, plaatsen en verwijderen** via **`/motorcycles`**.
- Er zijn **5 automatische tests** voor de motor: 3 unittests en 2 integratietests. Ze slagen allemaal.

---

## Hoe werkt het? (een vergelijking)

Dezelfde opbouw als bij auto's en scooters:

| Onderdeel | Vergelijking | Wat het doet |
|---|---|---|
| `Motorcycle.kt` | Het **invulformulier** | Beschrijft wat een motor is en controleert de regels (`validate()`) |
| `LicenseCategory.kt` | Keuzelijst | Rijbewijs A1, A2 of A |
| `MotorcycleStyle.kt` | Keuzelijst | De rijstijlen voor het uitklapmenu in de app |
| `MotorcycleRepository.kt` | Het **magazijn** | Slaat de motoren op en kan erin zoeken |
| `MotorcycleResponse.kt` | De **verpakking** | Welke gegevens naar de app gaan |
| `CreateMotorcycleRequest.kt` | Het **inleverformulier** | Wat de app stuurt bij een nieuwe advertentie (zonder id) |
| `ValidationErrorResponse.kt` | Het **afwijsbriefje** | De lijst met fouten als de gegevens niet kloppen |
| `MotorcycleRoutes.kt` | Het **loket** | De adressen onder `/motorcycles` |
| `Routing.kt` | De **lijst met loketten** | Zet het motorloket open: `motorcycleRoutes(MotorcycleRepository())` |

---

## De regels van een motor

`validate()` in `Motorcycle.kt` geeft een lijst met fouten terug. Een lege lijst betekent: de motor is goed.

| Regel | Foutmelding |
|---|---|
| Merk en model zijn verplicht | `brand is required` / `model is required` |
| Prijs groter dan 0 | `price must be greater than 0` |
| Kilometerstand niet negatief | `mileage cannot be negative` |
| Bouwjaar tussen 1950 en nu | `year must be between 1950 and …` |
| Cilinderinhoud tussen 50 en 2500 cc | `engineCc must be between 50 and 2500` |
| Vermogen tussen 1 en 300 kW | `powerKw must be between 1 and 300` |
| Rijbewijs moet passen bij cc en vermogen | `licenseCategory A1 is not allowed for 373 cc and 32 kW` |

Welk rijbewijs is minimaal nodig (`LicenseCategory.minimumFor`):

| Motor | Minimaal rijbewijs |
|---|---|
| tot en met 125 cc **én** tot en met 11 kW | A1 |
| tot en met 35 kW | A2 |
| meer dan 35 kW | A |

Voorbeelden: een 125 cc met 15 kW heeft A2 nodig. Een 689 cc begrensd op 35 kW mag met A2. Dezelfde motor met 54 kW heeft A nodig.

De gewichtsregel van A2 (maximaal 0,2 kW per kg) controleren we niet, omdat we het gewicht niet opslaan.

Omdat de keuzelijst op volgorde staat (A1 < A2 < A), kun je ze vergelijken. Iemand met **A2** mag dus ook alle **A1**-motoren rijden.

---

## De adressen (endpoints)

| Verzoek | Wat het doet | Antwoord |
|---|---|---|
| `GET /motorcycles` | Alle motoren (6 voorbeeldmotoren) | 200 |
| `GET /motorcycles?search=…&style=…&license=…&minCc=…&maxCc=…&maxPrice=…` | Zoeken en filteren; elk filter is optioneel | 200 |
| `GET /motorcycles/styles` | Alle rijstijlen, voor het uitklapmenu | 200 |
| `GET /motorcycles/{id}` | Eén motor | 200, of 404 als hij niet bestaat |
| `POST /motorcycles` | Nieuwe advertentie plaatsen | 201, of 400 met `{"errors":[...]}` |
| `PUT /motorcycles/{id}` | Advertentie bijwerken, bijvoorbeeld na een prijswijziging | 200, 400 met `{"errors":[...]}`, of 404 als hij niet bestaat |
| `DELETE /motorcycles/{id}` | Advertentie verwijderen | 204, of 404 als hij niet bestaat |

Let op bij `license`: dit is het rijbewijs van de **koper**. `license=A2` geeft alle motoren die je met A2 mag rijden (A1 en A2).

Voorbeeld: `/motorcycles?search=honda&style=NAKED&maxPrice=5000` geeft alleen de Honda CB125R.

### Voorbeeld: nieuwe motor plaatsen

```json
{
  "brand": "KTM",
  "model": "390 Duke",
  "year": 2022,
  "price": 4750.0,
  "mileage": 5100,
  "engineCc": 373,
  "powerKw": 32,
  "style": "NAKED",
  "licenseCategory": "A2"
}
```

Met `"licenseCategory": "A1"` krijg je:

```json
{ "errors": ["licenseCategory A1 is not allowed for 373 cc and 32 kW"] }
```

---

## De voorbeeldmotoren

| Id | Merk | Model | Bouwjaar | Prijs | Km-stand | cc | kW | Stijl | Rijbewijs |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Yamaha | MT-07 | 2019 | € 6450 | 18200 | 689 | 35 | NAKED | A2 |
| 2 | Honda | CB125R | 2021 | € 3950 | 6400 | 125 | 11 | NAKED | A1 |
| 3 | BMW | R 1250 GS | 2020 | € 15900 | 32500 | 1254 | 100 | TOURING | A |
| 4 | Kawasaki | Ninja 650 | 2018 | € 5250 | 24100 | 649 | 35 | SPORT | A2 |
| 5 | KTM | 690 Enduro R | 2019 | € 8750 | 15300 | 693 | 55 | ENDURO | A |
| 6 | Harley-Davidson | Street Bob | 2018 | € 12450 | 21000 | 1746 | 64 | CHOPPER | A |

De MT-07 en de Ninja 650 zijn de A2-versies, die zijn begrensd op 35 kW.

---

## De tests

**`src/test/kotlin/MotorcycleTest.kt`** – unittests (3). Ze testen alleen de klasse `Motorcycle` en de rijbewijsregels, zonder server:

| # | Test | Wat wordt gecontroleerd |
|---|---|---|
| 1 | `valid motorcycle has no validation errors` | Een goede motor geeft een lege foutenlijst (happy flow). |
| 2 | `minimum licence is A1 up to 125 cc and 11 kW, A2 up to 35 kW and A above that` | De grenzen van `minimumFor`: 125 cc/11 kW is A1, 126 cc of 12 kW is A2, 35 kW is A2, 36 kW is A. |
| 3 | `motorcycle with a licence that is too low is rejected` | A2 bij 689 cc en 54 kW geeft `licenseCategory A2 is not allowed for 689 cc and 54 kW`. |

**`src/test/kotlin/MotorcycleRoutesTest.kt`** – integratietests (2), via de echte API. Samen dekken ze alle vier de CRUD-soorten:

| # | Test | CRUD | Wat wordt gecontroleerd |
|---|---|---|---|
| 1 | `GET returns and filters motorcycles, and POST only stores valid ones` | Read + Create | `/motorcycles` geeft 200, JSON en 6 motoren; `/motorcycles/3` geeft de BMW en id 99 geeft 404; het filter `license=A2&maxPrice=6000` geeft de Honda en de Kawasaki; een goede motor krijgt 201 en id 7; A1 bij 373 cc wordt geweigerd met 400 en niet opgeslagen. |
| 2 | `PUT replaces and DELETE removes a motorcycle, both giving 404 for an unknown id` | Update + Delete | PUT vervangt motor 1 zonder een extra motor toe te voegen; A2 bij 54 kW wordt geweigerd met 400 en niet opgeslagen; PUT op 99 geeft 404; DELETE geeft 204 en de motor is weg; DELETE op 99 geeft 404. |

Uitvoeren:

```
sh ./gradlew test --tests "nl.vlugverkoop.Motorcycle*"
```

---

## Nog te doen / overleggen met het team

- **Gewicht.** Voor A2 geldt ook maximaal 0,2 kW per kg. Daarvoor moet het gewicht van de motor worden opgeslagen.
- **Opslag.** Motoren staan nu in het geheugen. Na een herstart zijn nieuwe advertenties weg.
