# Auto – leerdocument

Dit document bevat **alles** wat we voor de auto in de VlugVerkoop-API hebben gemaakt:
- wat we hebben gebouwd en waarom
- hoe alles samenwerkt
- **alle bestanden met de volledige code**
- uitleg van elk stukje code in gewone taal
- hoe we het hebben gebouwd (stap voor stap, ook Git)
- een spiekbriefje met Kotlin-begrippen
- overhoorvragen met antwoorden

> **Tip voor een andere chat:** plak dit hele document en schrijf erbij:
> *"Ik ben beginner in Kotlin. Hieronder staat alle code en uitleg van mijn auto-onderdeel. Leg het stap voor stap uit, bestand voor bestand, en overhoor me daarna met vragen."*

> Dit document is om zelf te leren. Het hoort **niet** op GitHub. Voor GitHub is er `CAR-UITLEG.md`.

---

## Inhoud

1. Het grote plaatje
2. De eisen: waarom de auto zo werkt
3. Hoe alles samenwerkt
4. Overzicht van de bestanden
5. Alle bestanden met code en uitleg
6. Statuscodes
7. Ontwerpkeuzes: waarom we het zo hebben gedaan
8. Hoe we het hebben gebouwd (stap voor stap)
9. Git: wat we hebben gedaan
10. Nog te doen
11. Spiekbriefje Kotlin
12. Overhoorvragen

---

## 1. Het grote plaatje

**VlugVerkoop** is een app (zoals Marktplaats, maar alleen voor voertuigen) waarmee mensen hun **auto, motor of scooter** te koop zetten. Het project bestaat uit twee delen:

| Deel | Wat | Techniek |
|---|---|---|
| Deel 1 | De **server** (backend / API) | Kotlin + **Ktor** |
| Deel 2 | De **Android-app** | Kotlin + Jetpack Compose |

Dit document gaat over **deel 1, de server**, en alleen over de **auto**.

**Taakverdeling in het team:**

| Voertuig | Wie |
|---|---|
| Auto (`Car`) | Jawad |
| Motor (`Motorcycle`) | Younes |
| Scooter (`Scooter`) | Abram |

### Wat is een API?

Een **API** is een manier waarop twee programma's met elkaar praten. De app stuurt een **verzoek** (request) naar een **adres** (endpoint) op de server. De server stuurt een **antwoord** (response) terug in **JSON**.

Voorbeeld:
- De app vraagt: `GET http://127.0.0.1:8080/cars`
- De server antwoordt met een lijst auto's in JSON.

### Wat is JSON?

Tekst in een vaste vorm, die zowel mensen als computers kunnen lezen:

```json
{
  "id": 1,
  "brand": "Volkswagen",
  "model": "Golf",
  "doors": 5,
  "listingFee": 9.95
}
```

---

## 2. De eisen: waarom de auto zo werkt

### Uit de opdracht van de docent

| Eis | Hoe de auto eraan voldoet |
|---|---|
| Minstens 3 soortgelijke klassen met gedeelde functionaliteit | `Car`, `Motorcycle` en `Scooter` erven van `Vehicle` en hebben allemaal `calculateListingFee()` en `validate()` |
| Aanmaken | `POST /cars` |
| Meerdere tegelijk bekijken | `GET /cars` |
| Zoeken of filteren | `GET /cars?search=…&maxPrice=…&maxMileage=…&doors=…` |
| Verwijderen | `DELETE /cars/{id}` |
| Zinnig gebruik van OO-principes | overerving, abstracte klasse, interface, encapsulatie (`private`) |
| Code en documentatie in het Engels | alle code en KDoc in het Engels |
| Minstens 3 unittests en 3 integratietesten (deel 1) | per persoon afgesproken: 3 unit + 2 integratie |

### Afspraken in het team

- Iedereen gebruikt **dezelfde opbouw** (model, dto, repository, routes).
- Iedereen gebruikt **dezelfde foutafhandeling**: **404** = bestaat niet, **400** = gegevens kloppen niet, met `{"errors": [...]}`.
- Iedereen gebruikt **dezelfde foutteksten**, zoals `price must be greater than 0` en `mileage cannot be negative`.
- De naam schrijf je als **VlugVerkoop** (brandbook).

---

## 3. Hoe alles samenwerkt

Zie de server als een **winkel**:

| Onderdeel | Vergelijking | Bestand |
|---|---|---|
| Model | Het **invulformulier** voor een auto + de regels | `Car.kt` |
| Interface | De **afspraak** dat een voertuig zichzelf kan controleren | `Validatable.kt` |
| Repository | Het **magazijn** waar auto's liggen | `CarRepository.kt` |
| Response (DTO) | De **verpakking** waarin de auto naar de app gaat | `CarResponse.kt` |
| Request (DTO) | Het **plaatsformulier** dat de app invult | `CreateCarRequest.kt` |
| Foutbriefje (DTO) | Het **afwijsbriefje** met alle fouten | `ValidationErrorResponse.kt` |
| Routes | Het **loket** waar de app aanklopt | `CarRoutes.kt` |
| Routing | De **lijst met loketten** in de winkel | `Routing.kt` |

### Auto's ophalen en filteren (GET)

```
App:  GET /cars?search=volkswagen&maxPrice=15000
  → CarRoutes leest de filters uit het adres
       → is een filter geen getal (bijv. maxPrice=goedkoop)? → 400
  → CarRepository.find(...) geeft alleen de auto's die bij alle filters passen
  → elke Car wordt een CarResponse (verpakking)
  → server stuurt de lijst als JSON terug, met 200 OK
```

### Een auto plaatsen (POST)

```
App:  POST /cars  + JSON {brand, model, year, price, mileage, doors}
  → CarRoutes leest de JSON in als CreateCarRequest
       → kapotte JSON of een veld ontbreekt? → 400
  → toCar(nextId) maakt er een Car van met een nieuw nummer
  → car.validate() controleert alle regels
       → fouten? → 400 met {"errors": [...]}  (er wordt niets opgeslagen)
       → goed?   → opslaan in het magazijn → 201 Created met de nieuwe auto
```

### Een auto wijzigen (PUT)

```
App:  PUT /cars/2  + JSON
  → id geen getal? → 400
  → auto 2 bestaat niet? → 404
  → JSON inlezen → Car maken met id 2 → validate()
       → fouten? → 400 met {"errors": [...]}
       → goed?   → vervangen in het magazijn → 200 OK
```

### Een auto verwijderen (DELETE)

```
App:  DELETE /cars/1
  → id geen getal? → 400
  → verwijderen gelukt? → 204 No Content
  → bestaat niet?       → 404 Not Found
```

---

## 4. Overzicht van de bestanden

```
Vlugverkoop/
├── requests.http                          ← voorbeeldverzoeken om met de hand te testen
├── CAR-UITLEG.md                          ← korte uitleg (op GitHub)
└── src/
    ├── main/kotlin/
    │   ├── Routing.kt                     ← lijst met loketten
    │   └── nl/vlugverkoop/
    │       ├── model/
    │       │   ├── Vehicle.kt             ← basisklasse (gedeeld)
    │       │   ├── Validatable.kt         ← interface (gedeeld)
    │       │   └── Car.kt                 ← de auto + regels
    │       ├── dto/
    │       │   ├── CarResponse.kt         ← verpakking naar de app
    │       │   ├── CreateCarRequest.kt    ← plaatsformulier van de app
    │       │   └── ValidationErrorResponse.kt ← foutbriefje (gedeeld)
    │       ├── repository/
    │       │   └── CarRepository.kt       ← magazijn
    │       └── routes/
    │           └── CarRoutes.kt           ← loket: GET, POST, PUT, DELETE
    └── test/kotlin/
        ├── CarTest.kt                     ← 3 unittests
        └── CarRoutesTest.kt               ← 2 integratietesten
```

### Auto naast motor en scooter

| Wat | Auto | Motor | Scooter |
|---|---|---|---|
| Eigen veld | `doors` | `engineCc`, `powerKw`, `style`, `licenseCategory` | `maxSpeedKmh`, `plateColor` |
| Advertentiekosten | 9,95 | 4,95 | 2,95 |
| Eigen filter | `doors` | `style`, `license`, cc | `plateColor` |
| Adres | `/cars` | `/motorcycles` | `/scooters` |

---

## 5. Alle bestanden met code en uitleg

### 5.1 `model/Vehicle.kt` – de basisklasse

**Pad:** `src/main/kotlin/nl/vlugverkoop/model/Vehicle.kt`

```kotlin
package nl.vlugverkoop.model

/**
 * Base class for every vehicle that can be listed on Vlugverkoop.
 */
abstract class Vehicle(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int
) {
    /** Fee the seller pays to place a listing. Differs per vehicle type. */
    abstract fun calculateListingFee(): Double
}
```

**Uitleg:**

- `package nl.vlugverkoop.model` → in welk pakket (map) deze code hoort.
- `/** ... */` → **KDoc**: uitleg voor mensen. De computer slaat dit over.
- `abstract class` → een **abstracte klasse**. Je kunt er **zelf geen object van maken**. Je kunt geen "Vehicle" verkopen, wel een auto, motor of scooter. Vehicle is alleen het sjabloon.
- `val id: Int` enzovoort → dit heeft **elk** voertuig:

  | Veld | Type | Betekenis |
    |---|---|---|
  | `id` | `Int` (heel getal) | Nummer van de advertentie |
  | `brand` | `String` (tekst) | Merk |
  | `model` | `String` | Model |
  | `year` | `Int` | Bouwjaar |
  | `price` | `Double` (kommagetal) | Prijs |
  | `mileage` | `Int` | Kilometerstand |

- `val` → een waarde die **niet meer verandert** na het aanmaken.
- `abstract fun calculateListingFee(): Double` → een **verplichte functie zonder inhoud**. Elke subklasse moet zelf invullen wat de advertentiekosten zijn. Dat heet **polymorfisme**.

---

### 5.2 `model/Validatable.kt` – de afspraak

**Pad:** `src/main/kotlin/nl/vlugverkoop/model/Validatable.kt`

```kotlin
package nl.vlugverkoop.model

interface Validatable {
    fun validate(): List<String>
}
```

> De echte versie in het project heeft nog KDoc erboven. De werking is hetzelfde.

**Uitleg:**

- `interface` → een **afspraak** zonder eigen gegevens. Elke klasse die `Validatable` is, **belooft** een functie `validate()` te hebben.
- `List<String>` → de functie geeft een **lijst met teksten** terug: de foutmeldingen.
- **Lege lijst** = alles goed. **Niet leeg** = er is iets fout.

**Verschil tussen abstracte klasse en interface:**

| | `abstract class Vehicle` | `interface Validatable` |
|---|---|---|
| Kan gegevens bewaren (`val id`) | ja | nee |
| Hoeveel kan een klasse er hebben? | maar **één** | **meerdere** |
| Betekenis | "een auto **is een** voertuig" | "een auto **kan** zichzelf controleren" |

---

### 5.3 `model/Car.kt` – de auto en zijn regels

**Pad:** `src/main/kotlin/nl/vlugverkoop/model/Car.kt`

```kotlin
package nl.vlugverkoop.model

import java.time.Year

/**
 * A car listed for sale on VlugVerkoop.
 *
 * @property doors number of doors of the car (2 to 7).
 */
class Car(
    id: Int,
    brand: String,
    model: String,
    year: Int,
    price: Double,
    mileage: Int,
    val doors: Int
) : Vehicle(id, brand, model, year, price, mileage), Validatable {

    override fun calculateListingFee(): Double = LISTING_FEE

    /**
     * Checks whether this car may be listed.
     * Returns a list of error messages; an empty list means the car is valid.
     */
    override fun validate(): List<String> {
        val errors = mutableListOf<String>()
        val currentYear = Year.now().value

        if (brand.isBlank()) {
            errors.add("brand is required")
        }
        if (model.isBlank()) {
            errors.add("model is required")
        }
        if (price <= 0) {
            errors.add("price must be greater than 0")
        }
        if (mileage < 0) {
            errors.add("mileage cannot be negative")
        }
        if (year !in MIN_YEAR..currentYear) {
            errors.add("year must be between $MIN_YEAR and $currentYear")
        }
        if (doors !in MIN_DOORS..MAX_DOORS) {
            errors.add("doors must be between $MIN_DOORS and $MAX_DOORS")
        }
        return errors
    }

    companion object {
        private const val LISTING_FEE = 9.95
        private const val MIN_YEAR = 1950
        private const val MIN_DOORS = 2
        private const val MAX_DOORS = 7
    }
}
```

#### Deel 1: de kop van de klasse

```kotlin
class Car(
    id: Int,
    ...
    val doors: Int
) : Vehicle(id, brand, model, year, price, mileage), Validatable {
```

- `class Car(...)` → het formulier voor een auto. Wat tussen `( )` staat heet de **constructor**.
- `id: Int` **zonder** `val` → wordt alleen **doorgegeven** aan `Vehicle`, die het bewaart.
- `val doors: Int` **met** `val` → een **eigen veld** van de auto. Dat zit niet in Vehicle.
- `: Vehicle(...)` → **overerving**: "een Car **is een** Vehicle".
- `, Validatable` → de auto houdt zich **ook** aan de afspraak van `Validatable`.

Een auto maken:

```kotlin
val car = Car(1, "Volkswagen", "Golf", 2018, 14500.0, 85000, 5)
```

#### Deel 2: de advertentiekosten

```kotlin
override fun calculateListingFee(): Double = LISTING_FEE
```

- `override` → "ik vul hier de verplichte functie van `Vehicle` in".
- `= LISTING_FEE` → een functie van één regel. Hij geeft altijd 9,95 terug.

#### Deel 3: de regels (`validate`)

```kotlin
override fun validate(): List<String> {
    val errors = mutableListOf<String>()
    ...
    return errors
}
```

1. `mutableListOf<String>()` → maak een **lege lijst** die je kunt aanvullen.
2. Elke `if` controleert één regel. Klopt die regel niet, dan komt er een melding bij met `errors.add(...)`.
3. `return errors` → geef de lijst terug.

Waarom een **lijst** en niet stoppen bij de eerste fout? Zo krijgt de gebruiker **alle** fouten in één keer te zien en hoeft hij niet steeds opnieuw te proberen.

| Code | Betekenis |
|---|---|
| `brand.isBlank()` | De tekst is leeg of bestaat alleen uit spaties |
| `price <= 0` | Prijs is 0 of minder |
| `mileage < 0` | Kilometerstand is negatief |
| `Year.now().value` | Het huidige jaar, bijv. 2026 |
| `MIN_YEAR..currentYear` | Een **range**: alle getallen van 1950 tot en met nu |
| `year !in ...` | Het bouwjaar valt **niet** in die range |
| `"... $MIN_YEAR and $currentYear"` | **String template**: zet de waarden in de tekst |

Voorbeelden:

| Auto | `validate()` geeft |
|---|---|
| Toyota Yaris, 2020, € 16950, 42000 km, 5 deuren | `[]` (leeg = goed) |
| prijs 0 | `["price must be greater than 0"]` |
| 1 deur | `["doors must be between 2 and 7"]` |
| prijs -1 **en** 9 deuren | `["price must be greater than 0", "doors must be between 2 and 7"]` |

#### Deel 4: `companion object`

```kotlin
companion object {
    private const val LISTING_FEE = 9.95
    private const val MIN_YEAR = 1950
    ...
}
```

- `companion object` → een plek voor dingen die bij de **klasse** horen, niet bij één auto. Elke auto heeft dezelfde regels.
- `const val` → een **vaste waarde** die al bekend is als de code wordt gecompileerd.
- `private` → alleen `Car` kan erbij.
- Waarom niet gewoon `2` en `7` in de code typen? Een naam als `MAX_DOORS` zegt **wat** het getal betekent. Zo'n los getal zonder naam heet een **magic number**, en dat wil je vermijden.

---

### 5.4 `dto/CarResponse.kt` – de verpakking naar de app

**Pad:** `src/main/kotlin/nl/vlugverkoop/dto/CarResponse.kt`

```kotlin
package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable
import nl.vlugverkoop.model.Car

/**
 * Car data as it is sent to the Android app in JSON.
 */
@Serializable
data class CarResponse(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int,
    val doors: Int,
    val listingFee: Double
)

/** Converts a Car into a CarResponse. */
fun Car.toResponse(): CarResponse = CarResponse(
    id = id,
    brand = brand,
    model = model,
    year = year,
    price = price,
    mileage = mileage,
    doors = doors,
    listingFee = calculateListingFee()
)
```

**Waarom een aparte verpakking?** `Car` is voor de server zelf. `CarResponse` bepaalt precies **wat de app te zien krijgt**. Zo'n verpakking heet een **DTO** (Data Transfer Object). Bovendien kan `Car` niet direct naar JSON, omdat hij van een abstracte klasse erft.

**Uitleg:**

- `@Serializable` → een **annotatie**. Hiermee kan **kotlinx.serialization** dit object automatisch omzetten naar JSON en terug.
- `data class` → een klasse alleen voor **gegevens**. Kotlin maakt er automatisch `equals`, `toString` en `copy` bij.
- `listingFee` → staat niet in `Car`. Het wordt uitgerekend met `calculateListingFee()`.
- `fun Car.toResponse()` → een **extension function**: je "plakt" een functie aan `Car` vast zonder `Car.kt` aan te passen. Daarna kun je overal `car.toResponse()` schrijven.
- `id = id` → **named arguments**: links de naam in `CarResponse`, rechts de waarde van de auto.

---

### 5.5 `dto/CreateCarRequest.kt` – het plaatsformulier van de app

**Pad:** `src/main/kotlin/nl/vlugverkoop/dto/CreateCarRequest.kt`

```kotlin
package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable
import nl.vlugverkoop.model.Car

/**
 * Data the Android app sends to create or update a car listing.
 * It has no id: the server decides the id.
 */
@Serializable
data class CreateCarRequest(
    val brand: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int,
    val doors: Int
)

/** Builds a car with the given [id] from this request. */
fun CreateCarRequest.toCar(id: Int): Car = Car(
    id = id,
    brand = brand.trim(),
    model = model.trim(),
    year = year,
    price = price,
    mileage = mileage,
    doors = doors
)
```

**Uitleg:**

Dit is wat de app naar de server stuurt. Er ontbreken bewust twee dingen:

| Niet in het formulier | Waarom niet |
|---|---|
| `id` | Het nummer bepaalt de **server** |
| `listingFee` | Die rekent de **server** uit |

- `fun CreateCarRequest.toCar(id: Int)` → weer een extension function. Hij maakt van het formulier een echte `Car`, met het id dat de server kiest.
- `.trim()` → haalt spaties aan het begin en eind weg. `"  Audi "` wordt `"Audi"`.
- Dezelfde opzet als `toScooter(id)` en `toMotorcycle(id)` van de anderen.

Wat de app stuurt:

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

---

### 5.6 `dto/ValidationErrorResponse.kt` – het afwijsbriefje

**Pad:** `src/main/kotlin/nl/vlugverkoop/dto/ValidationErrorResponse.kt` (gemaakt door Younes, gedeeld door iedereen)

```kotlin
package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable

/**
 * Sent to the app with status 400 when a request contains invalid data.
 * The app shows each message under the matching input field.
 */
@Serializable
data class ValidationErrorResponse(
    val errors: List<String>
)
```

**Uitleg:** de verpakking voor fouten. In JSON ziet het er zo uit:

```json
{ "errors": ["price must be greater than 0", "doors must be between 2 and 7"] }
```

Omdat auto, motor en scooter hetzelfde briefje gebruiken, kan de app fouten overal op dezelfde manier tonen.

---

### 5.7 `repository/CarRepository.kt` – het magazijn

**Pad:** `src/main/kotlin/nl/vlugverkoop/repository/CarRepository.kt`

```kotlin
package nl.vlugverkoop.repository

import nl.vlugverkoop.model.Car

/**
 * Stores cars in memory. Will be replaced by a real database later.
 */
class CarRepository {

    private val cars = mutableListOf(
        Car(1, "Volkswagen", "Golf", 2018, 14500.0, 85000, 5),
        Car(2, "Toyota", "Yaris", 2020, 16950.0, 42000, 5),
        Car(3, "BMW", "3 Series", 2016, 18750.0, 120000, 4),
        Car(4, "Volkswagen", "Polo", 2021, 17250.0, 31000, 5),
        Car(5, "Peugeot", "208", 2015, 6950.0, 135000, 3),
        Car(6, "Tesla", "Model 3", 2022, 34900.0, 48000, 4)
    )

    /** Returns all cars. */
    fun getAll(): List<Car> = cars.toList()

    fun find(
        search: String? = null,
        maxPrice: Double? = null,
        maxMileage: Int? = null,
        doors: Int? = null
    ): List<Car> = cars.filter { car ->
        (search == null || car.brand.contains(search, ignoreCase = true) || car.model.contains(search, ignoreCase = true)) &&
            (maxPrice == null || car.price <= maxPrice) &&
            (maxMileage == null || car.mileage <= maxMileage) &&
            (doors == null || car.doors == doors)
    }

    fun getById(id: Int): Car? = cars.find { it.id == id }

    fun nextId(): Int = (cars.maxOfOrNull { it.id } ?: 0) + 1

    fun add(car: Car): Car {
        cars.add(car)
        return car
    }

    fun update(id: Int, car: Car): Boolean {
        val index = cars.indexOfFirst { it.id == id }
        if (index == -1) {
            return false
        }
        cars[index] = car
        return true
    }

    fun delete(id: Int): Boolean = cars.removeIf { it.id == id }
}
```

> In het project staat boven elke functie nog KDoc. Die is hier weggelaten om het kort te houden.

**Wat is een repository?** De plek waar gegevens worden **bewaard en opgehaald**. Nu is dat een lijst in het **geheugen**, en later een **database**. De rest van de code praat alleen met de repository, dus die hoeft dan niet te veranderen. Dit heet het **Repository pattern**.

**Uitleg per stuk:**

- `private val cars = mutableListOf(...)`
    - `private` → alleen deze klasse kan bij de lijst. Dit heet **encapsulatie**.
    - `mutableListOf` → een lijst die je **kunt veranderen**.
    - De 6 auto's heten **seed data**. Er zijn twee Volkswagens, zodat zoeken op merk meer dan één resultaat geeft.

- `getAll()` → `cars.toList()` geeft een **kopie** terug, zodat niemand van buitenaf het magazijn kan aanpassen.

- `find(...)`
    - `String?`, `Double?`, `Int?` → het **vraagteken** betekent dat het ook `null` (niets) mag zijn.
    - `= null` → een **standaardwaarde**. Geef je een filter niet mee, dan is het `null`.
    - `cars.filter { car -> ... }` → houd alleen de auto's waarvoor de voorwaarde `true` is. Dit is een **lambda** (functioneel programmeren).
    - `search == null || ...` → "geen filter opgegeven? Dan is het goed. Anders moet het kloppen."
    - `&&` → **en**: alle filters moeten kloppen.
    - `contains(search, ignoreCase = true)` → zit de tekst erin? Hoofdletters maken niet uit.

- `getById(id)` → `find { it.id == id }` geeft de eerste auto met dat nummer, of `null`. Daarom is het returntype `Car?`.

- `nextId()`
    1. `maxOfOrNull { it.id }` → het hoogste nummer, of `null` bij een lege lijst
    2. `?: 0` → de **Elvis-operator**: "is het `null`, gebruik dan 0"
    3. `+ 1` → met auto 1 t/m 6 wordt het volgende nummer 7

- `add(car)` → zet een auto die **al gecontroleerd** is in de lijst.

- `update(id, car)`
    - `indexOfFirst { it.id == id }` → op welke plek in de lijst staat die auto? Geeft `-1` als hij er niet is.
    - `cars[index] = car` → vervang de auto op die plek.
    - Geeft `true` of `false` terug, zodat de route weet of het gelukt is.

- `delete(id)` → `removeIf { it.id == id }` verwijdert de auto. Geeft `true` als er iets verwijderd is.

> **Let op:** de lijst staat in het geheugen. Herstart je de server, dan staan alleen de 6 voorbeeldauto's er weer.

---

### 5.8 `routes/CarRoutes.kt` – het loket (de endpoints)

**Pad:** `src/main/kotlin/nl/vlugverkoop/routes/CarRoutes.kt`

```kotlin
package nl.vlugverkoop.routes

import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import nl.vlugverkoop.dto.CreateCarRequest
import nl.vlugverkoop.dto.ValidationErrorResponse
import nl.vlugverkoop.dto.toCar
import nl.vlugverkoop.dto.toResponse
import nl.vlugverkoop.repository.CarRepository

private const val INVALID_ID = "id must be a whole number"
private const val INVALID_BODY = "request body must be valid car JSON"
private const val CAR_NOT_FOUND = "Car not found"

fun Route.carRoutes(repository: CarRepository) {
    route("/cars") {

        // GET /cars?search=&maxPrice=&maxMileage=&doors=
        get {
            val params = call.request.queryParameters
            val errors = mutableListOf<String>()
            val maxPrice = params.doubleOrError("maxPrice", errors)
            val maxMileage = params.intOrError("maxMileage", errors)
            val doors = params.intOrError("doors", errors)

            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@get
            }

            val cars = repository.find(
                search = params["search"]?.takeIf { it.isNotBlank() },
                maxPrice = maxPrice,
                maxMileage = maxMileage,
                doors = doors
            ).map { it.toResponse() }
            call.respond(cars)
        }

        // GET /cars/{id}
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@get
            }

            val car = repository.getById(id)
            if (car == null) {
                call.respond(HttpStatusCode.NotFound, CAR_NOT_FOUND)
                return@get
            }
            call.respond(car.toResponse())
        }

        // POST /cars
        post {
            val request = call.receiveCarRequestOrNull()
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_BODY)))
                return@post
            }

            val car = request.toCar(repository.nextId())
            val errors = car.validate()
            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@post
            }

            repository.add(car)
            call.respond(HttpStatusCode.Created, car.toResponse())
        }

        // PUT /cars/{id}
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@put
            }

            if (repository.getById(id) == null) {
                call.respond(HttpStatusCode.NotFound, CAR_NOT_FOUND)
                return@put
            }

            val request = call.receiveCarRequestOrNull()
            if (request == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_BODY)))
                return@put
            }

            val car = request.toCar(id)
            val errors = car.validate()
            if (errors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(errors))
                return@put
            }

            repository.update(id, car)
            call.respond(car.toResponse())
        }

        // DELETE /cars/{id}
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ValidationErrorResponse(listOf(INVALID_ID)))
                return@delete
            }

            if (repository.delete(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, CAR_NOT_FOUND)
            }
        }
    }
}

private suspend fun ApplicationCall.receiveCarRequestOrNull(): CreateCarRequest? =
    try {
        receive<CreateCarRequest>()
    } catch (e: BadRequestException) {
        null
    }

private fun Parameters.intOrError(name: String, errors: MutableList<String>): Int? {
    val text = this[name] ?: return null
    val number = text.toIntOrNull()
    if (number == null) {
        errors.add("$name must be a whole number")
    }
    return number
}

private fun Parameters.doubleOrError(name: String, errors: MutableList<String>): Double? {
    val text = this[name] ?: return null
    val number = text.toDoubleOrNull()
    if (number == null) {
        errors.add("$name must be a number")
    }
    return number
}
```

**Uitleg van de opbouw:**

- `private const val INVALID_ID = ...` → vaste teksten bovenaan, zodat je ze niet steeds opnieuw hoeft te typen.
- `fun Route.carRoutes(repository: CarRepository)` → een functie die de auto-adressen aan de server toevoegt. Ze krijgt het magazijn **van buitenaf** mee, in plaats van het zelf te maken. Dat heet **Dependency Injection**, en het maakt testen en vervangen makkelijker.
- `route("/cars") { ... }` → alles hierbinnen begint met `/cars`.
- `call` → het huidige verzoek en het antwoord.
- `return@get` / `return@post` → **stop dit blok hier**. Het woord na `@` zegt welk blok.

#### Endpoint 1: `GET /cars` – ophalen en filteren

1. `call.request.queryParameters` → alles na het `?` in het adres. Bij `/cars?doors=5` is `params["doors"]` gelijk aan `"5"` (tekst).
2. `doubleOrError` en `intOrError` → maken van de tekst een getal. Lukt dat niet (bijv. `maxPrice=goedkoop`), dan komt er een fout in de lijst.
3. Fouten? → **400** met `ValidationErrorResponse` en stoppen.
4. `params["search"]?.takeIf { it.isNotBlank() }` → neem de zoektekst alleen als hij niet leeg is. Anders wordt het `null`, en dan filtert `find` er niet op.
5. `repository.find(...)` → filteren in het magazijn.
6. `.map { it.toResponse() }` → elke `Car` wordt een `CarResponse`.
7. `call.respond(cars)` → als JSON terugsturen, met **200 OK**.

#### Endpoint 2: `GET /cars/{id}` – één auto

1. `{id}` → een **plekhouder** in het adres. Bij `/cars/2` is `id` gelijk aan `"2"`.
2. `?.toIntOrNull()` → maak er een getal van. Bij `/cars/abc` wordt het `null`.
    - `?.` is de **safe call**: "doe dit alleen als het niet `null` is".
3. Geen getal? → **400**.
4. `repository.getById(id)` → bestaat hij niet? → **404**.
5. Wel? → **200** met de auto.

#### Endpoint 3: `POST /cars` – plaatsen

1. `receiveCarRequestOrNull()` → lees de JSON in. Kapot of onvolledig? → **400**.
2. `request.toCar(repository.nextId())` → maak de auto met een nieuw nummer.
3. `car.validate()` → controleer de regels. Fouten? → **400** met alle fouten, en **niets opslaan**.
4. Alles goed? → `repository.add(car)` en **201 Created** met de nieuwe auto.

> **Belangrijk:** eerst controleren, dan pas opslaan. Een foute auto komt dus **nooit** in het magazijn.

#### Endpoint 4: `PUT /cars/{id}` – wijzigen

Dezelfde stappen als POST, met twee verschillen:
- Eerst wordt gecontroleerd of de auto **bestaat**. Zo niet → **404**.
- De auto houdt **hetzelfde id** (`request.toCar(id)`), en `repository.update` vervangt hem.
- Gelukt → **200 OK**.

#### Endpoint 5: `DELETE /cars/{id}` – verwijderen

1. Id geen getal? → **400**.
2. `repository.delete(id)` gelukt → **204 No Content**.
3. Bestond niet → **404 Not Found**.

#### De hulpfuncties onderaan

- `private` → alleen dit bestand kan ze gebruiken.
- `suspend fun` → een functie die even kan **wachten** (hier op het inlezen van de JSON) zonder de hele server te blokkeren. Dat hoort bij **coroutines** in Kotlin.
- `try { ... } catch (e: BadRequestException) { null }` → **probeer** de JSON in te lezen. Gaat het fout met een `BadRequestException`, geef dan `null` terug.
- **Waarom zelf opvangen?** In `StatusPages.kt` staat dat elke fout die niemand opvangt een **500** wordt ("de server is kapot"). Kapotte JSON is een fout van de **gebruiker**, en dat hoort een **400** te zijn.
- `this[name] ?: return null` → is het filter niet meegegeven, stop dan meteen en geef `null` terug.

---

### 5.9 `Routing.kt` – de lijst met loketten

**Pad:** `src/main/kotlin/Routing.kt`

```kotlin
fun Application.configureRouting() {
    routing {
        carRoutes(CarRepository())
        scooterRoutes(ScooterRepository())
        motorcycleRoutes(MotorcycleRepository())
        get("/") {
            call.respondText("Hello, World!")
        }
        ...
    }
}
```

- `carRoutes(CarRepository())` → maak **één** magazijn en open het autoloket. Zolang de server draait, gebruikt het loket steeds hetzelfde magazijn.
- De server roept `configureRouting()` aan bij het opstarten. Dat staat in `application.yaml`.

---

### 5.10 `CarTest.kt` – de 3 unittests

**Pad:** `src/test/kotlin/CarTest.kt`

```kotlin
package nl.vlugverkoop

import nl.vlugverkoop.model.Car
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CarTest {

    /** Creates a valid car. Change one value to test one rule. */
    private fun car(
        brand: String = "Toyota",
        model: String = "Yaris",
        year: Int = 2020,
        price: Double = 16950.0,
        mileage: Int = 42000,
        doors: Int = 5
    ) = Car(1, brand, model, year, price, mileage, doors)

    @Test
    fun `valid car has no validation errors`() {
        val errors = car().validate()

        assertTrue(errors.isEmpty(), "Expected no errors but got $errors")
    }

    @Test
    fun `car with a price of zero is rejected`() {
        val errors = car(price = 0.0).validate()

        assertEquals(listOf("price must be greater than 0"), errors)
    }

    @Test
    fun `car with too few or too many doors is rejected`() {
        assertEquals(listOf("doors must be between 2 and 7"), car(doors = 1).validate())
        assertEquals(listOf("doors must be between 2 and 7"), car(doors = 8).validate())
    }
}
```

**Wat is een unittest?** Een test van **één klein stukje code**, apart van de rest. Hier alleen de klasse `Car`, zonder server.

**Uitleg:**

- `private fun car(...)` → een **hulpfunctie** die een **goede** auto maakt. Alle parameters hebben een **standaardwaarde**. Met `car(price = 0.0)` verander je alleen de prijs, en de rest blijft goed. Zo test je steeds **één regel tegelijk**.
- `@Test` → label: "dit is een test".
- `` `valid car has no validation errors` `` → met backticks mag de naam spaties bevatten, zodat hij leest als een zin.
- `assertTrue(x)` → slaagt als `x` waar is.
- `assertEquals(verwacht, echt)` → slaagt als de twee gelijk zijn.

| # | Test | Soort | Wat het controleert |
|---|---|---|---|
| 1 | `valid car has no validation errors` | **happy flow** | Een goede auto geeft een lege lijst |
| 2 | `car with a price of zero is rejected` | foutgeval | Prijs 0 → precies één fout |
| 3 | `car with too few or too many doors is rejected` | foutgeval + **grenswaarden** | 1 deur (net te weinig) en 8 deuren (net te veel) worden allebei geweigerd |

---

### 5.11 `CarRoutesTest.kt` – de 2 integratietesten

**Pad:** `src/test/kotlin/CarRoutesTest.kt`

```kotlin
class CarRoutesTest {

    private companion object {
        const val SEEDED = 6

        const val VALID_BODY = """
            {"brand":"Audi","model":"A3","year":2019,"price":17500.0,"mileage":60000,"doors":5}
        """

        const val INVALID_BODY = """
            {"brand":"Audi","model":"A3","year":2019,"price":-1.0,"mileage":60000,"doors":9}
        """
    }

    @Test
    fun `GET returns and filters cars, and POST only stores valid ones`() = testApplication {
        configure()
        // GET alles, GET met filter, POST goed, POST fout
        ...
    }

    @Test
    fun `PUT replaces and DELETE removes a car, both giving 404 for an unknown id`() = testApplication {
        configure()
        // PUT goed, PUT onbekend, DELETE goed, DELETE onbekend
        ...
    }
}
```

> De volledige code staat in het project. Hieronder staat wat er gebeurt.

**Wat is een integratietest?** Een test waarin **alle onderdelen samen** worden getest: loket, magazijn, model en verpakking. De test doet de app na: hij stuurt echte verzoeken en controleert de antwoorden.

**Uitleg:**

| Code | Betekenis |
|---|---|
| `testApplication { ... }` | Start een **testserver in het geheugen**. De echte server hoeft niet aan te staan. |
| `configure()` | Laad `application.yaml`, zodat de testserver precies zo is ingesteld als de echte |
| `client.get("/cars")` | Een nep-app stuurt een verzoek |
| `client.post("/cars") { contentType(...); setBody(...) }` | Stuur JSON mee |
| `Json.decodeFromString<List<CarResponse>>(...)` | Zet de JSON-tekst om naar Kotlin-objecten |
| `"""..."""` | **Raw string**: tekst waarin `"` gewoon mag |
| `SEEDED = 6` | Het aantal startauto's, met een naam in plaats van een los getal |

**Test 1 – Read + Create:**

1. `GET /cars` → 200, JSON, 6 auto's, en auto 1 is een Volkswagen van 14500
2. `GET /cars?search=volkswagen&maxPrice=15000` → alleen auto 1 (de Golf; de Polo is te duur)
3. `POST` met de Audi → 201 en id 7
4. `POST` met prijs -1 en 9 deuren → 400 met **beide** foutmeldingen
5. Daarna zijn er **7** auto's, dus de foute is niet opgeslagen

**Test 2 – Update + Delete:**

1. `PUT /cars/1` met de Audi → 200, id blijft 1, merk is nu Audi
2. Daarna zijn er nog steeds **6** auto's, dus PUT voegt er geen toe
3. `PUT /cars/99` → 404
4. `DELETE /cars/1` → 204, en auto 1 is echt weg (5 auto's over)
5. `DELETE /cars/99` → 404

> Elke test start een **nieuwe** testserver met een **vers magazijn**. Daarom begint test 2 weer met 6 auto's, ook al heeft test 1 er een toegevoegd.

**Waarom 2 grote tests en niet 1 per endpoint?** Dezelfde opzet als Abram: samen raken ze alle vier de CRUD-soorten (Create, Read, Update, Delete), en per persoon zijn er 2 integratietesten afgesproken.

---

### 5.12 `requests.http` – met de hand testen

**Pad:** `requests.http` (in de hoofdmap)

```http
### Search cars on brand or model
GET http://127.0.0.1:8080/cars?search=volkswagen

### Get one car
GET http://127.0.0.1:8080/cars/2

### Create a car with wrong data (400 with errors)
POST http://127.0.0.1:8080/cars
Content-Type: application/json

{
  "brand": "",
  "model": "A3",
  "year": 1900,
  "price": -1.0,
  "mileage": -5,
  "doors": 9
}
```

- Een bestand voor de **HTTP-client van IntelliJ**. Hiermee stuur je verzoeken naar je **draaiende** server.
- `###` → begin van een nieuw verzoek, met een titel.
- Na een lege regel volgt de JSON.

**Gebruiken:** start de server, open `requests.http` en klik op de groene ▶ naast een verzoek. Onderaan zie je het antwoord.

> Een browser kan alleen **GET**. Voor POST, PUT en DELETE heb je dit bestand nodig (of curl in de Opdrachtprompt).

---

## 6. Statuscodes

| Code | Naam | Betekenis | Wanneer bij auto's |
|---|---|---|---|
| **200** | OK | Gelukt, hier zijn de gegevens | GET, PUT |
| **201** | Created | Gelukt, iets nieuws aangemaakt | POST met goede gegevens |
| **204** | No Content | Gelukt, niets terug te sturen | DELETE |
| **400** | Bad Request | Foute invoer van de gebruiker | foute regels, kapotte JSON, id of filter geen getal |
| **404** | Not Found | Bestaat niet | GET, PUT of DELETE op een onbekend id |
| **500** | Internal Server Error | Er ging iets mis in de server | als een fout door niemand wordt opgevangen |

Ezelsbruggetje: **2xx = gelukt**, **4xx = fout van de gebruiker**, **5xx = fout van de server**.

---

## 7. Ontwerpkeuzes: waarom we het zo hebben gedaan

| Keuze | Waarom |
|---|---|
| **`Car` erft van `Vehicle`** | Gemeenschappelijke velden staan op één plek. Dat is **overerving**. |
| **`Validatable` als interface** | Elk voertuig kan zichzelf controleren, en de routes behandelen ze allemaal op dezelfde manier. |
| **`validate()` geeft een lijst, geen exception** | De gebruiker ziet **alle** fouten tegelijk. |
| **Eerst valideren, dan opslaan** | Een foute auto komt nooit in het magazijn. |
| **Vaste getallen in een `companion object`** | Geen magic numbers: `MAX_DOORS` zegt wat 7 betekent. |
| **Aparte Request- en Response-DTO's** | De app krijgt alleen wat hij nodig heeft, en kan zelf geen id of listingFee opgeven. |
| **Repository pattern** | Later komt er een database voor in de plaats, en dan hoeft alleen de repository te veranderen. |
| **Repository wordt meegegeven aan de routes** | **Dependency Injection**: makkelijker te testen en te vervangen. |
| **Fout zelf opvangen (400 in plaats van 500)** | Een fout van de gebruiker is geen serverfout. |
| **Dezelfde foutteksten en statuscodes als motor en scooter** | De app kan alle voertuigen op dezelfde manier behandelen. |
| **`Year.now()` in plaats van een vast jaartal** | De code klopt volgend jaar nog steeds. |

---

## 8. Hoe we het hebben gebouwd (stap voor stap)

1. **Ktor-project gemaakt** met Gradle en 6 plugins (Status Pages, Content Negotiation, kotlinx.serialization, Exposed, Swagger, Call Logging). Een eerste poging met Amper werkte niet.
2. **`Vehicle`** gemaakt als abstracte klasse, met **`Car`**, **`Motorcycle`** en **`Scooter`** erbij.
3. **Project op GitHub gezet** en de groepsgenoten uitgenodigd.
4. **`.gitignore`** gemaakt, zodat `.gradle`, `build` en `*.db` niet op GitHub komen.
5. **`CarRepository`** en **`CarResponse`** gemaakt, en het eerste endpoint **`GET /cars`**. Getest in de browser.
6. **`DELETE /cars/{id}`** toegevoegd. Getest met curl in de Opdrachtprompt.
7. **`POST /cars`** toegevoegd, met **`CreateCarRequest`**.
8. **Werk van het team binnengehaald.** Er waren merge-conflicten, omdat er niet eerst was gepulld (zie hoofdstuk 9).
9. **Car op hetzelfde niveau gebracht als de scooter:** `Validatable` + `validate()`, filters, `GET /{id}`, `PUT`, 400 met `ValidationErrorResponse`, en 6 auto's.
10. **Tests geschreven:** 3 unittests (`CarTest`) en 2 integratietesten (`CarRoutesTest`). Alle 16 tests van het project slagen.
11. **`CAR-UITLEG.md`** en dit leerdocument geschreven.

---

## 9. Git: wat we hebben gedaan

**Git** houdt alle versies van de code bij. **GitHub** is de online plek waar het team de code deelt.

| Actie in IntelliJ | Commando | Betekenis |
|---|---|---|
| **Ctrl + T** (Update Project) | `git pull` | Haal de nieuwe code van GitHub binnen |
| **Ctrl + K** (Commit) | `git add` + `git commit` | Sla je wijzigingen op, met een beschrijving |
| **Ctrl + Shift + K** (Push) | `git push` | Stuur je commits naar GitHub |
| Accept Left / Accept Right | – | Bij een conflict: kies jouw versie (links) of die van GitHub (rechts) |

**Het probleem dat we hadden:**
- Er was aan Car gewerkt zonder eerst te **pullen**.
- Bij het ophalen kwamen er **6 conflicten**: de scooterbestanden en `Routing.kt`.
- **Oplossing:** voor al die bestanden de versie van GitHub gekozen (**Accept Right**). Daarna bleken de eigen Car-wijzigingen weg te zijn (ze stonden in de **stash**), en zijn ze opnieuw toegevoegd.
- **Les:** altijd eerst **Ctrl + T** voordat je begint met werken.

**Andere problemen:**

| Probleem | Oplossing |
|---|---|
| Commit-venster blijft op "updating..." staan | IntelliJ herstarten, of rechtsklik op de map → **Git → Commit Directory...** |
| Bestand `.idea/AndroidProjectSystem.xml` blokkeert een pull | Bestand verwijderen, het komt vanzelf terug |
| `localhost` gaf andere resultaten dan `127.0.0.1` | Altijd `http://127.0.0.1:8080` gebruiken |

**Bestanden die NIET naar GitHub gaan:** `.gradle/`, `build/`, `.kotlin/`, `*.db` (staan in `.gitignore`).

---

## 10. Nog te doen

| Punt | Uitleg |
|---|---|
| **Database** | Nu staat alles in het geheugen. Later komt er H2 met Exposed (zit al in het project). |
| **Eigen nummers** | Auto 1, motor 1 en scooter 1 hebben nu hetzelfde nummer. Met een database krijgt elk voertuig een eigen nummer. |
| **Eén adres voor alle voertuigen** | Volgens het plan wordt het `GET /vehicles?type=CAR`. |
| **Advertentiekosten** | Met het team bespreken of `calculateListingFee` blijft, omdat het brandbook "geen verborgen kosten" belooft. |
| **Spelling** | In `Vehicle.kt` staat nog "Vlugverkoop" in plaats van "VlugVerkoop". |

---

## 11. Spiekbriefje Kotlin

| Begrip | Voorbeeld | Betekenis |
|---|---|---|
| `package` | `package nl.vlugverkoop.model` | In welk pakket (map) de code hoort |
| `import` | `import nl.vlugverkoop.model.Car` | Code uit een ander bestand gebruiken |
| `class` | `class Car(...)` | Een bouwtekening |
| constructor | `Car(1, "Volkswagen", ...)` | Wat je invult om een object te maken |
| `abstract class` | `abstract class Vehicle` | Klasse waarvan je zelf geen object kunt maken |
| `interface` | `interface Validatable` | Een afspraak: welke functies een klasse moet hebben |
| overerving | `class Car(...) : Vehicle(...)` | "Car **is een** Vehicle" |
| `override` | `override fun validate()` | Een verplichte functie invullen |
| `val` / `var` | `val doors: Int` | Vaste waarde / waarde die kan veranderen |
| `Int` / `Double` / `String` / `Boolean` | `5`, `14500.0`, `"Golf"`, `true` | Heel getal / kommagetal / tekst / waar-niet waar |
| `List` / `mutableListOf` | `mutableListOf<String>()` | Lijst die je niet / wel kunt veranderen |
| `data class` | `data class CarResponse(...)` | Klasse alleen voor gegevens |
| `@Serializable` | boven een data class | Kan automatisch naar en van JSON |
| `companion object` | `companion object { ... }` | Dingen die bij de klasse horen, niet bij één object |
| `const val` | `const val MAX_DOORS = 7` | Vaste waarde met een naam |
| `private` | `private val cars` | Alleen deze klasse/dit bestand kan erbij (encapsulatie) |
| `fun` | `fun getById(id: Int): Car?` | Een functie |
| `= ...` functie | `fun x(): Double = 9.95` | Functie van één regel |
| extension function | `fun Car.toResponse()` | Een functie aan een bestaande klasse vastplakken |
| named arguments | `brand = request.brand` | Bij het aanroepen zeggen welke waarde bij welke naam hoort |
| standaardwaarde | `doors: Int? = null` | Waarde die gebruikt wordt als je niks meegeeft |
| lambda `{ }` | `filter { it.price <= 15000 }` | Een stukje code dat je meegeeft aan een functie |
| `it` | `{ it.id == id }` | Het element dat nu wordt bekeken |
| `null` / `Type?` | `Car?` | "Niets" / dit mag ook niets zijn |
| `?.` | `x?.toIntOrNull()` | Safe call: alleen als `x` niet `null` is |
| `?:` | `x ?: 0` | Elvis: is `x` `null`, gebruik dan `0` |
| range `..` | `2..7` | Alle getallen van 2 tot en met 7 |
| `in` / `!in` | `doors !in 2..7` | Zit (niet) in de range |
| `$naam` / `${...}` | `"was $doors"` | Een waarde in een tekst zetten |
| `"""..."""` | `"""{"a":1}"""` | Raw string |
| `try { } catch (e: ...) { }` | | Probeer iets; gaat het fout, doe dan dit |
| `return@post` | | Stop het `post { }`-blok |
| `suspend fun` | | Functie die kan wachten zonder de server te blokkeren |
| `.map { }` | `cars.map { it.toResponse() }` | Maak van elk element iets nieuws |
| `.filter { }` | | Houd alleen de elementen die kloppen |
| `.find { }` | | Eerste element dat klopt, anders `null` |
| `.first { }` | | Eerste element dat klopt, anders een fout |
| `.none { }` | | `true` als **geen enkel** element klopt |
| `.removeIf { }` | | Verwijder wat klopt; `true` als er iets verwijderd is |
| `.indexOfFirst { }` | | Plek van het eerste element dat klopt, of `-1` |
| `.maxOfOrNull { }` | | Hoogste waarde, of `null` bij een lege lijst |
| `.isBlank()` / `.trim()` | | Leeg of alleen spaties / spaties aan de randen weghalen |

**Ktor-begrippen:**

| Begrip | Betekenis |
|---|---|
| **Ktor** | Kotlin-bibliotheek om een server te bouwen |
| `routing { }` | Hierbinnen staan alle adressen |
| `route("/cars") { }` | Groep adressen die met `/cars` beginnen |
| `get { }` / `post { }` / `put("/{id}") { }` / `delete("/{id}") { }` | Wat er gebeurt bij dat soort verzoek |
| `call` | Het huidige verzoek en antwoord |
| `call.receive<T>()` | Lees de JSON in als object van type T |
| `call.respond(x)` | Stuur een antwoord (automatisch als JSON) |
| `call.respond(HttpStatusCode.X, y)` | Antwoord met een bepaalde statuscode |
| `call.parameters["id"]` | Lees `{id}` uit het adres |
| `call.request.queryParameters` | Lees de filters na het `?` |
| `testApplication { }` | Testserver in het geheugen |

**Architectuur- en OO-begrippen:**

| Begrip | Betekenis | Voorbeeld in Car |
|---|---|---|
| **Overerving** | Een klasse neemt alles over van een andere | `Car : Vehicle` |
| **Abstractie** | Alleen het belangrijke vastleggen, de details later | `abstract class Vehicle` |
| **Polymorfisme** | Dezelfde functie werkt per klasse anders | `calculateListingFee()` |
| **Encapsulatie** | Gegevens afschermen | `private val cars` |
| **Interface** | Een afspraak over welke functies er zijn | `Validatable` |
| **DTO** | Verpakking tussen server en app | `CarResponse`, `CreateCarRequest` |
| **Repository pattern** | Opslag in één klasse | `CarRepository` |
| **Dependency Injection** | Een onderdeel krijgt wat het nodig heeft van buitenaf | `carRoutes(CarRepository())` |
| **Unittest** | Test van één klein stukje | `CarTest` |
| **Integratietest** | Test van alle onderdelen samen | `CarRoutesTest` |

---

## 12. Overhoorvragen

Probeer eerst zelf te antwoorden. De antwoorden staan eronder.

1. Wat is het verschil tussen `Vehicle` en `Car`?
2. Waarom kun je geen `Vehicle` maken?
3. Wat is het verschil tussen een abstracte klasse en een interface?
4. Waarom staat er bij `id` in `Car` geen `val`, en bij `doors` wel?
5. Wat geeft `validate()` terug voor een goede auto, en voor een auto met prijs -1 en 9 deuren?
6. Waarom geeft `validate()` een lijst terug in plaats van te stoppen bij de eerste fout?
7. Wat is een magic number, en hoe voorkom je het in `Car`?
8. Wat is het verschil tussen `Car` en `CarResponse`?
9. Waarom staan `id` en `listingFee` niet in `CreateCarRequest`?
10. Wat doet `.trim()` in `toCar`?
11. Hoe bepaalt het magazijn het nummer van een nieuwe auto?
12. Wat betekent `maxPrice: Double? = null` in `find`?
13. Waarom geeft `getAll()` `cars.toList()` terug in plaats van `cars`?
14. Wat gebeurt er bij `GET /cars?maxPrice=goedkoop`?
15. Waarom wordt een foute auto nooit opgeslagen bij POST?
16. Welke statuscode krijg je bij `PUT /cars/99`, en waarom?
17. Waarom vangen we kapotte JSON zelf op met `try/catch`?
18. Wat betekent `?.` en wat betekent `?:`?
19. Wat is het verschil tussen je unittests en je integratietesten?
20. Waarom start test 2 in `CarRoutesTest` weer met 6 auto's?
21. Wat is Dependency Injection, en waar zie je het in de auto-code?
22. Wat moet je altijd eerst doen voordat je begint met werken in Git?

### Antwoorden

1. `Vehicle` is de **algemene basisklasse** met wat elk voertuig heeft. `Car` **erft** daarvan en voegt `doors` en eigen regels toe.
2. Omdat `Vehicle` **abstract** is. Het is alleen een sjabloon voor Car, Motorcycle en Scooter.
3. Een abstracte klasse kan **gegevens** bewaren, en je kunt er maar van **één** erven. Een interface is alleen een **afspraak** over functies, en een klasse kan er **meerdere** hebben.
4. `id` wordt alleen **doorgegeven** aan `Vehicle`, die het al bewaart. `doors` is een **eigen veld** van de auto.
5. Een goede auto: een **lege lijst**. Prijs -1 en 9 deuren: `["price must be greater than 0", "doors must be between 2 and 7"]`.
6. Zo ziet de gebruiker **alle fouten tegelijk** en hoeft hij niet steeds opnieuw te proberen.
7. Een los getal in de code zonder uitleg. Je voorkomt het met een **naam**: `const val MAX_DOORS = 7` in een `companion object`.
8. `Car` is het **model** voor de server. `CarResponse` is de **verpakking** (DTO) naar de app, met `@Serializable` en `listingFee`.
9. Het `id` en de `listingFee` bepaalt de **server**, niet de app.
10. Het haalt **spaties** aan het begin en eind weg: `"  Audi "` wordt `"Audi"`.
11. **Hoogste nummer + 1** met `nextId()`. Met auto 1 t/m 6 wordt het 7.
12. Het filter mag **niets** (`null`) zijn, en als je het niet meegeeft, is het standaard `null`. Dan filtert `find` er niet op.
13. Dan krijg je een **kopie**, en kan niemand van buitenaf het magazijn aanpassen (encapsulatie).
14. `doubleOrError` kan er geen getal van maken. De server antwoordt **400** met `{"errors": ["maxPrice must be a number"]}`.
15. Omdat eerst `validate()` wordt aangeroepen. Bij fouten stopt de route met `return@post`, **voordat** `repository.add` wordt bereikt.
16. **404**, omdat auto 99 niet bestaat. Dat wordt gecontroleerd **voordat** de gegevens worden bekeken.
17. Anders maakt `StatusPages` er een **500** (serverfout) van, terwijl het een fout van de **gebruiker** is. Dat hoort een **400** te zijn.
18. `?.` = **safe call**: alleen uitvoeren als het niet `null` is. `?:` = **Elvis**: is het `null`, gebruik dan de waarde rechts.
19. Een **unittest** test alleen de klasse `Car`, zonder server. Een **integratietest** start een testserver en test loket, magazijn, model en verpakking **samen**.
20. Elke test start een **nieuwe testserver** met een **vers magazijn**.
21. Een onderdeel krijgt wat het nodig heeft **van buitenaf mee**, in plaats van het zelf te maken. In de auto-code: `carRoutes(repository: CarRepository)` krijgt het magazijn mee vanuit `Routing.kt`.
22. **Pullen** (Ctrl + T in IntelliJ), zodat je de nieuwste code van het team hebt en conflicten voorkomt.