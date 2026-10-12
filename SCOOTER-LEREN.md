# Scooter – leerdocument

Dit document bevat **alles** wat we voor de scooter in de VlugVerkoop-API hebben gemaakt:
- wat we hebben gebouwd en waarom
- hoe alles samenwerkt
- **alle bestanden met de volledige code**
- uitleg van elk stukje code in gewone taal
- hoe we het hebben gebouwd (stap voor stap, ook Git)
- een spiekbriefje met Kotlin-begrippen
- overhoorvragen met antwoorden

> **Tip voor de andere chat:** plak dit hele document en schrijf erbij:
> *"Ik ben beginner in Kotlin. Hieronder staat alle code en uitleg van mijn scooter-onderdeel. Leg het stap voor stap uit, bestand voor bestand, en overhoor me daarna met vragen."*

---

## Inhoud

1. Het grote plaatje
2. De eisen: waarom de scooter zo werkt
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

Wij werken nu aan **deel 1, de server**. De server bewaart de advertenties. De app vraagt via internet gegevens op bij de server.

**Taakverdeling in het team:**

| Voertuig | Wie |
|---|---|
| Auto (`Car`) | Jawad |
| Motor (`Motorcycle`) | Younes |
| Scooter (`Scooter`) | Abram |

### Wat is een API?

Een **API** is een manier waarop twee programma's met elkaar praten. De app stuurt een **verzoek** (request) naar een **adres** (endpoint) op de server. De server stuurt een **antwoord** (response) terug in **JSON**.

Voorbeeld:
- De app vraagt: `GET http://127.0.0.1:8080/scooters`
- De server antwoordt met een lijst scooters in JSON.

### Wat is JSON?

JSON is tekst in een vaste vorm, die zowel mensen als computers kunnen lezen:

```json
{
  "id": 1,
  "brand": "Vespa",
  "maxSpeedKmh": 45,
  "plateColor": "YELLOW"
}
```

---

## 2. De eisen: waarom de scooter zo werkt

### Uit het plan van aanpak

| Eis | Waar het staat |
|---|---|
| Car, Motorcycle en Scooter **erven** van de abstracte basisklasse `Vehicle` | §2 en klassendiagram |
| Een scooter heeft een **snelheidsklasse (25 / 45 km/u)** en een **kenteken geel of blauw** | Tabel in §2 |
| Er is een enum **`PlateColor`** | Lijst met enums |
| Code en documentatie volgen de **Kotlin-codeconventies** | Afspraken |
| Deel 1 testen: **integratietesten met Ktor testApplication** | Teststrategie §8 |

### De Nederlandse regels voor scooters

| Snelheid | Soort | Kenteken |
|---|---|---|
| 25 km/u | Snorfiets | **Blauw** |
| 45 km/u | Bromfiets | **Geel** |

Een andere snelheid bestaat niet voor scooters. "45 km/u met een blauw kenteken" kan ook niet.

### Uit het brandbook

- De naam schrijf je als **VlugVerkoop**, met twee hoofdletters en aan elkaar.

---

## 3. Hoe alles samenwerkt

Zie de server als een **winkel**:

| Onderdeel | Vergelijking | Bestand |
|---|---|---|
| Model | Het **invulformulier** voor een scooter + de regels | `Scooter.kt` |
| Repository | Het **magazijn** waar scooters liggen | `ScooterRepository.kt` |
| Response (DTO) | De **verpakking** waarin de scooter naar de app gaat | `ScooterResponse.kt` |
| Request (DTO) | Het **plaatsformulier** dat de app invult | `CreateScooterRequest.kt` |
| Routes | Het **loket** waar de app aanklopt | `ScooterRoutes.kt` |
| Routing | De **lijst met loketten** in de winkel | `Routing.kt` |

### Alle scooters ophalen (GET)

```
App:  GET /scooters
  → ScooterRoutes (loket) vraagt het magazijn: "geef alle scooters"
  → ScooterRepository geeft de lijst
  → elke Scooter wordt een ScooterResponse (verpakking)
  → server stuurt de lijst als JSON terug, met statuscode 200 OK
```

### Een scooter plaatsen (POST)

```
App:  POST /scooters  + JSON {brand, model, year, price, mileage, maxSpeedKmh}
  → ScooterRoutes leest de JSON in als CreateScooterRequest (plaatsformulier)
  → het magazijn maakt een nieuwe Scooter met een nieuw nummer
       → snelheid 25 of 45?  ja → de scooter bepaalt zelf de kentekenkleur
                             nee → fout → server antwoordt 400 Bad Request
  → de scooter wordt opgeslagen
  → server stuurt de nieuwe scooter terug, met statuscode 201 Created
```

### Een scooter verwijderen (DELETE)

```
App:  DELETE /scooters/1
  → ScooterRoutes leest het nummer uit het adres (1)
       → geen geldig getal? → 400 Bad Request
  → het magazijn verwijdert scooter 1
       → gelukt?      → 204 No Content
       → bestaat niet? → 404 Not Found
```

---

## 4. Overzicht van de bestanden

```
Vlugverkoop-api/
├── requests.http                         ← voorbeeldverzoeken om met de hand te testen
├── SCOOTER-UITLEG.md                     ← korte uitleg (op GitHub)
└── src/
    ├── main/kotlin/
    │   ├── Routing.kt                    ← lijst met loketten (1 regel voor scooters)
    │   └── nl/vlugverkoop/
    │       ├── model/
    │       │   ├── Vehicle.kt            ← basisklasse (niet door ons gemaakt)
    │       │   └── Scooter.kt            ← scooter + PlateColor
    │       ├── dto/
    │       │   ├── ScooterResponse.kt    ← verpakking naar de app
    │       │   └── CreateScooterRequest.kt ← plaatsformulier van de app
    │       ├── repository/
    │       │   └── ScooterRepository.kt  ← magazijn
    │       └── routes/
    │           └── ScooterRoutes.kt      ← loket: GET, POST, DELETE
    └── test/kotlin/
        └── ScooterRoutesTest.kt          ← 6 tests
```

### Scooter naast auto

We hebben precies dezelfde opbouw gebruikt als Jawad voor auto's:

| Wat | Auto | Scooter |
|---|---|---|
| Model | `Car.kt` | `Scooter.kt` |
| Magazijn | `CarRepository.kt` | `ScooterRepository.kt` |
| Verpakking | `CarResponse.kt` | `ScooterResponse.kt` |
| Plaatsformulier | `CreateCarRequest.kt` | `CreateScooterRequest.kt` |
| Loket | `CarRoutes.kt` | `ScooterRoutes.kt` |
| Ophalen | `GET /cars` | `GET /scooters` |
| Plaatsen | `POST /cars` | `POST /scooters` |
| Verwijderen | `DELETE /cars/{id}` | `DELETE /scooters/{id}` |

**Verschil:** een auto heeft `doors` (aantal deuren). Een scooter heeft `maxSpeedKmh` en `plateColor`, en weigert een snelheid die niet 25 of 45 is.

---

## 5. Alle bestanden met code en uitleg

### 5.1 `model/Vehicle.kt` – de basisklasse

> Dit bestand hebben **wij niet gemaakt**, maar je moet het snappen, want de Scooter bouwt erop voort.

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

- `package nl.vlugverkoop.model` → in welke "map" (pakket) deze code hoort. Zo kunnen andere bestanden hem terugvinden.
- `/** ... */` → **KDoc**: uitleg voor mensen. De computer slaat dit over.
- `abstract class Vehicle(...)` → een **abstracte klasse**: een algemeen formulier waarvan je **zelf geen exemplaar kunt maken**. Je kunt geen "Vehicle" maken, wel een `Car`, `Motorcycle` of `Scooter`.
- `val id: Int`, `val brand: String`, ... → dit heeft **elk** voertuig:

  | Veld | Type | Betekenis |
  |---|---|---|
  | `id` | `Int` (heel getal) | Nummer van de advertentie |
  | `brand` | `String` (tekst) | Merk |
  | `model` | `String` | Model |
  | `year` | `Int` | Bouwjaar |
  | `price` | `Double` (kommagetal) | Prijs |
  | `mileage` | `Int` | Kilometerstand |

- `val` → een vakje dat na het invullen **niet meer verandert**.
- `abstract fun calculateListingFee(): Double` → een **verplichte functie zonder inhoud**. Elke subklasse (Car, Motorcycle, Scooter) **moet** zelf invullen wat de advertentiekosten zijn. Dat heet **polymorfisme**: dezelfde functie werkt per soort voertuig anders.

---

### 5.2 `model/Scooter.kt` – de scooter en de kentekenkleur

**Pad:** `src/main/kotlin/nl/vlugverkoop/model/Scooter.kt`

```kotlin
package nl.vlugverkoop.model

/**
 * Colour of a Dutch scooter licence plate. The colour is tied to the legal top speed.
 *
 * @property maxSpeedKmh legal top speed in km/h that belongs to this plate colour.
 */
enum class PlateColor(val maxSpeedKmh: Int) {
    /** Snorfiets, max 25 km/h. */
    BLUE(25),

    /** Bromfiets, max 45 km/h. */
    YELLOW(45)
}

/**
 * A scooter listed for sale on VlugVerkoop.
 *
 * @property maxSpeedKmh legal top speed in km/h (25 or 45 in the Netherlands).
 * @property plateColor licence plate colour that matches [maxSpeedKmh].
 * @throws IllegalArgumentException if [maxSpeedKmh] is not 25 or 45.
 */
class Scooter(
    id: Int,
    brand: String,
    model: String,
    year: Int,
    price: Double,
    mileage: Int,
    val maxSpeedKmh: Int
) : Vehicle(id, brand, model, year, price, mileage) {

    val plateColor: PlateColor = requireNotNull(PlateColor.entries.find { it.maxSpeedKmh == maxSpeedKmh }) {
        "Speed class must be 25 or 45 km/h, was $maxSpeedKmh"
    }

    override fun calculateListingFee(): Double = 2.95
}
```

**Wat was er eerst?** Eerst had de scooter alleen `maxSpeedKmh`, zonder controle. Je kon 80 of zelfs -5 invullen, de kentekenkleur ontbrak en de naam was gespeld als "Vlugverkoop".

**Wat hebben we veranderd?**
1. De keuzelijst `PlateColor` toegevoegd (blauw/geel).
2. `plateColor` toegevoegd, die zichzelf uitrekent uit de snelheid.
3. Een andere snelheid dan 25 of 45 geeft nu een foutmelding.
4. In de KDoc staat de naam nu goed: "VlugVerkoop".

#### Deel 1: de enum `PlateColor`

```kotlin
enum class PlateColor(val maxSpeedKmh: Int) {
    BLUE(25),
    YELLOW(45)
}
```

- `enum class` → een **vaste keuzelijst**, zoals een uitklapmenu. Er bestaan **alleen** `BLUE` en `YELLOW`.
- `(val maxSpeedKmh: Int)` → elke optie in de lijst krijgt een getal mee.
- `BLUE(25)` → blauw hoort bij 25 km/u.
- `YELLOW(45)` → geel hoort bij 45 km/u.
- De naam in de code is Engels (`PlateColor` = kentekenkleur), omdat de code volgens de afspraak in het Engels is.

#### Deel 2: de klasse `Scooter`

```kotlin
class Scooter(
    id: Int,
    brand: String,
    model: String,
    year: Int,
    price: Double,
    mileage: Int,
    val maxSpeedKmh: Int
) : Vehicle(id, brand, model, year, price, mileage) {
```

- `class Scooter(...)` → het formulier voor een scooter. Wat tussen `( )` staat heet de **constructor**: wat je moet invullen om een scooter te maken.
- `id: Int` **zonder** `val` → dit wordt alleen **doorgegeven** aan `Vehicle`. Vehicle bewaart het al.
- `val maxSpeedKmh: Int` **met** `val` → dit is een **eigen veld** van de scooter. Het zit niet in Vehicle.
- `: Vehicle(id, brand, model, year, price, mileage)` → **overerving**: "een Scooter **is een** Vehicle". De dubbele punt betekent "erft van". De gegevens gaan naar de constructor van Vehicle.

Een scooter maken ziet er zo uit:

```kotlin
val scooter = Scooter(1, "Vespa", "Primavera", 2021, 2650.0, 4200, 45)
```

#### Deel 3: de kentekenkleur uitrekenen

```kotlin
val plateColor: PlateColor = requireNotNull(PlateColor.entries.find { it.maxSpeedKmh == maxSpeedKmh }) {
    "Speed class must be 25 or 45 km/h, was $maxSpeedKmh"
}
```

In gewone taal:
> "Zoek in de keuzelijst de kleur die bij deze snelheid hoort. Gevonden? Dan is dat de kentekenkleur. Niet gevonden? Stop en geef een foutmelding."

Stukje voor stukje:

| Code | Betekenis |
|---|---|
| `val plateColor: PlateColor =` | Een vast veld `plateColor` van het type `PlateColor` |
| `PlateColor.entries` | De lijst met alle opties: `[BLUE, YELLOW]` |
| `.find { ... }` | Zoek de **eerste** optie die aan de voorwaarde voldoet. Niets gevonden → `null` (= "niets") |
| `it` | "Het ding dat we nu bekijken" (eerst BLUE, dan YELLOW) |
| `it.maxSpeedKmh` | De snelheid van **de kleur** (25 of 45) |
| `== maxSpeedKmh` | Is die gelijk aan de snelheid van **deze scooter**? |
| `requireNotNull(...)` | "Hier **moet** iets uitkomen." Is het `null`, dan stopt het programma met een `IllegalArgumentException` (een fout). |
| `{ "Speed class must ... $maxSpeedKmh" }` | De foutmelding. `$maxSpeedKmh` vult de ingevulde snelheid in de tekst in. |

Voorbeelden:

| Ingevulde snelheid | `find` geeft | Resultaat |
|---|---|---|
| 25 | `BLUE` | `plateColor = BLUE` |
| 45 | `YELLOW` | `plateColor = YELLOW` |
| 80 | `null` | Fout: `Speed class must be 25 or 45 km/h, was 80` |

#### Deel 4: de advertentiekosten

```kotlin
override fun calculateListingFee(): Double = 2.95
```

- `override` → "ik vul hier de verplichte functie van `Vehicle` in".
- `fun` → een functie (iets wat de klasse kan **doen**).
- `: Double` → de functie geeft een kommagetal terug.
- `= 2.95` → de functie geeft altijd 2,95 terug. Een functie van één regel met `=` heet een **single-expression function**.

---

### 5.3 `dto/ScooterResponse.kt` – de verpakking naar de app

**Pad:** `src/main/kotlin/nl/vlugverkoop/dto/ScooterResponse.kt`

```kotlin
package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable
import nl.vlugverkoop.model.PlateColor
import nl.vlugverkoop.model.Scooter

/**
 * Scooter data as it is sent to the Android app in JSON.
 */
@Serializable
data class ScooterResponse(
    val id: Int,
    val brand: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int,
    val maxSpeedKmh: Int,
    val plateColor: PlateColor,
    val listingFee: Double
)

/** Converts a Scooter into a ScooterResponse. */
fun Scooter.toResponse(): ScooterResponse = ScooterResponse(
    id = id,
    brand = brand,
    model = model,
    year = year,
    price = price,
    mileage = mileage,
    maxSpeedKmh = maxSpeedKmh,
    plateColor = plateColor,
    listingFee = calculateListingFee()
)
```

**Waarom een aparte verpakking?** Het model (`Scooter`) is voor de server zelf. De verpakking (`ScooterResponse`) bepaalt precies **wat de app te zien krijgt**. Zo kun je later het model veranderen zonder dat de app kapotgaat. Zo'n verpakking heet een **DTO** (Data Transfer Object).

**Uitleg:**

- `import ...` → "ik gebruik code uit een ander bestand of een andere bibliotheek".
- `@Serializable` → een **annotatie** (label). Hiermee kan de bibliotheek **kotlinx.serialization** dit object automatisch omzetten naar JSON en terug.
- `data class` → een klasse die alleen **gegevens** bewaart. Kotlin maakt er automatisch handige functies bij, zoals vergelijken (`==`) en netjes afdrukken (`toString`).
- De velden: dezelfde als bij de scooter, plus:
  - `plateColor: PlateColor` → in JSON wordt dit de tekst `"BLUE"` of `"YELLOW"`.
  - `listingFee: Double` → de advertentiekosten, uitgerekend met `calculateListingFee()`.

**De functie `toResponse()`:**

```kotlin
fun Scooter.toResponse(): ScooterResponse = ScooterResponse(...)
```

- `fun Scooter.toResponse()` → een **extension function**: je "plakt" een nieuwe functie aan `Scooter` vast zonder `Scooter.kt` aan te passen. Daarna kun je `scooter.toResponse()` schrijven.
- `id = id` → **named arguments**: links de naam van het veld in `ScooterResponse`, rechts de waarde van de scooter. Dat leest duidelijker.
- Bij auto's bestaat er ook een `Car.toResponse()`. Ze hebben dezelfde naam, maar Kotlin kiest de juiste op basis van het type (Car of Scooter).

---

### 5.4 `dto/CreateScooterRequest.kt` – het plaatsformulier van de app

**Pad:** `src/main/kotlin/nl/vlugverkoop/dto/CreateScooterRequest.kt`

```kotlin
package nl.vlugverkoop.dto

import kotlinx.serialization.Serializable

/**
 * Data the Android app sends to create a new scooter.
 * The plate colour is not sent: the scooter derives it from [maxSpeedKmh].
 */
@Serializable
data class CreateScooterRequest(
    val brand: String,
    val model: String,
    val year: Int,
    val price: Double,
    val mileage: Int,
    val maxSpeedKmh: Int
)
```

**Uitleg:**

Dit is wat de app naar de server stuurt als iemand een scooter te koop zet. Er ontbreken bewust drie dingen:

| Niet in het formulier | Waarom niet |
|---|---|
| `id` | Het nummer geeft de **server** (het magazijn). |
| `plateColor` | De scooter rekent de kleur **zelf** uit uit de snelheid. |
| `listingFee` | Die rekent de server uit. |

Voorbeeld van wat de app stuurt:

```json
{
  "brand": "Peugeot",
  "model": "Kisbee",
  "year": 2022,
  "price": 1750.0,
  "mileage": 2500,
  "maxSpeedKmh": 45
}
```

---

### 5.5 `repository/ScooterRepository.kt` – het magazijn

**Pad:** `src/main/kotlin/nl/vlugverkoop/repository/ScooterRepository.kt`

```kotlin
package nl.vlugverkoop.repository

import nl.vlugverkoop.model.Scooter

/**
 * Stores scooters in memory. Will be replaced by a real database later.
 */
class ScooterRepository {

    private val scooters = mutableListOf(
        Scooter(1, "Vespa", "Primavera", 2021, 2650.0, 4200, 45),
        Scooter(2, "Piaggio", "Zip", 2019, 1150.0, 9800, 25),
        Scooter(3, "Kymco", "Agility", 2020, 1395.0, 6100, 25)
    )

    /** Returns all scooters. */
    fun getAll(): List<Scooter> = scooters

    /** Deletes the scooter with the given id. Returns true if a scooter was removed. */
    fun delete(id: Int): Boolean = scooters.removeIf { it.id == id }

    /**
     * Adds a new scooter with a new id. Returns the saved scooter.
     *
     * @throws IllegalArgumentException if [maxSpeedKmh] is not 25 or 45.
     */
    fun add(brand: String, model: String, year: Int, price: Double, mileage: Int, maxSpeedKmh: Int): Scooter {
        val newId = (scooters.maxOfOrNull { it.id } ?: 0) + 1
        val scooter = Scooter(newId, brand, model, year, price, mileage, maxSpeedKmh)
        scooters.add(scooter)
        return scooter
    }
}
```

**Wat is een repository?** De plek waar de gegevens worden **bewaard en opgehaald**. Nu staan ze in het **geheugen** (een lijst). Later komt er een echte **database** voor in de plaats. De rest van de code hoeft dan niet te veranderen, want die praat alleen met de repository.

**Uitleg:**

- `private val scooters = mutableListOf(...)`
  - `private` → alleen deze klasse mag bij de lijst. Andere code moet de functies gebruiken.
  - `mutableListOf` → een lijst die je **kunt veranderen** (toevoegen, verwijderen).
  - Er staan 3 voorbeeldscooters in (dit heet **seed data**):

  | Id | Merk | Model | Jaar | Prijs | Km | Snelheid | Kenteken |
  |---|---|---|---|---|---|---|---|
  | 1 | Vespa | Primavera | 2021 | 2650.0 | 4200 | 45 | YELLOW |
  | 2 | Piaggio | Zip | 2019 | 1150.0 | 9800 | 25 | BLUE |
  | 3 | Kymco | Agility | 2020 | 1395.0 | 6100 | 25 | BLUE |

- `fun getAll(): List<Scooter> = scooters` → geeft alle scooters terug. Het type `List` (zonder "mutable") betekent dat wie de lijst krijgt hem niet via deze functie kan aanpassen.

- `fun delete(id: Int): Boolean = scooters.removeIf { it.id == id }`
  - `removeIf { ... }` → verwijder alle scooters waarvoor de voorwaarde klopt.
  - `it.id == id` → "het nummer van deze scooter is gelijk aan het gevraagde nummer".
  - Geeft `true` terug als er iets is verwijderd, anders `false`.

- `fun add(...): Scooter` → een nieuwe scooter toevoegen:
  1. `scooters.maxOfOrNull { it.id }` → het hoogste nummer in de lijst. Bij een lege lijst `null`.
  2. `?: 0` → de **Elvis-operator**: "is het `null`, gebruik dan 0".
  3. `+ 1` → het nieuwe nummer is één hoger. Met scooters 1, 2 en 3 wordt dat 4.
  4. `val scooter = Scooter(...)` → maak de scooter. **Hier controleert de scooter de snelheid.** Bij 80 gaat het hier al fout.
  5. `scooters.add(scooter)` → zet hem in de lijst.
  6. `return scooter` → geef de nieuwe scooter terug.

> **Belangrijk:** stap 4 (maken) gebeurt **vóór** stap 5 (opslaan). Gaat het maken fout (verkeerde snelheid), dan komt de code nooit bij het opslaan. Een foute scooter wordt dus **nooit** bewaard.

> **Let op:** de lijst staat in het geheugen. Herstart je de server, dan staan alleen de 3 voorbeeldscooters er weer.

---

### 5.6 `routes/ScooterRoutes.kt` – het loket (de endpoints)

**Pad:** `src/main/kotlin/nl/vlugverkoop/routes/ScooterRoutes.kt`

```kotlin
package nl.vlugverkoop.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import nl.vlugverkoop.dto.CreateScooterRequest
import nl.vlugverkoop.dto.toResponse
import nl.vlugverkoop.repository.ScooterRepository

/**
 * All endpoints for scooters.
 */
fun Route.scooterRoutes(repository: ScooterRepository) {
    route("/scooters") {

        // GET /scooters → returns all scooters as JSON
        get {
            val scooters = repository.getAll().map { it.toResponse() }
            call.respond(scooters)
        }

        // POST /scooters → creates a new scooter (speed must be 25 or 45 km/h)
        post {
            val request = call.receive<CreateScooterRequest>()
            val scooter = try {
                repository.add(
                    brand = request.brand,
                    model = request.model,
                    year = request.year,
                    price = request.price,
                    mileage = request.mileage,
                    maxSpeedKmh = request.maxSpeedKmh
                )
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid scooter")
                return@post
            }
            call.respond(HttpStatusCode.Created, scooter.toResponse())
        }

        // DELETE /scooters/{id} → deletes one scooter
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id")
                return@delete
            }

            if (repository.delete(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, "Scooter not found")
            }
        }
    }
}
```

**Uitleg van de opbouw:**

- `fun Route.scooterRoutes(repository: ScooterRepository)` → een functie die de scooter-adressen toevoegt aan de server. Ze krijgt het magazijn (`repository`) **van buitenaf** mee in plaats van het zelf te maken. Dat idee heet **Dependency Injection**, en het maakt testen en vervangen makkelijker.
- `route("/scooters") { ... }` → alles hierbinnen begint met `/scooters`.
- `// ...` → een gewone opmerking voor mensen (één regel).
- `call` → het huidige verzoek en het antwoord. Met `call` lees je wat de app stuurt en antwoord je terug.

#### Endpoint 1: `GET /scooters` – alles ophalen

```kotlin
get {
    val scooters = repository.getAll().map { it.toResponse() }
    call.respond(scooters)
}
```

1. `repository.getAll()` → haal alle scooters uit het magazijn.
2. `.map { it.toResponse() }` → `map` maakt van elke scooter iets nieuws, hier een `ScooterResponse` (de verpakking). Een lijst Scooters wordt zo een lijst ScooterResponses.
3. `call.respond(scooters)` → stuur de lijst terug. Ktor zet hem automatisch om naar **JSON** (dat is ingesteld in `Serialization.kt`). Zonder andere statuscode is het antwoord **200 OK**.

#### Endpoint 2: `POST /scooters` – een scooter plaatsen

```kotlin
post {
    val request = call.receive<CreateScooterRequest>()
    val scooter = try {
        repository.add(...)
    } catch (e: IllegalArgumentException) {
        call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid scooter")
        return@post
    }
    call.respond(HttpStatusCode.Created, scooter.toResponse())
}
```

1. `call.receive<CreateScooterRequest>()` → lees de JSON die de app stuurt en maak er een `CreateScooterRequest` (plaatsformulier) van.
2. `try { ... }` → "**probeer** dit". We proberen een scooter toe te voegen met de gegevens uit het formulier.
3. `catch (e: IllegalArgumentException) { ... }` → "**gaat het fout** met dit soort fout, doe dan dit". Die fout komt uit `Scooter.kt` als de snelheid niet 25 of 45 is.
   - `call.respond(HttpStatusCode.BadRequest, ...)` → antwoord met **400 Bad Request** ("foute invoer").
   - `e.message ?: "Invalid scooter"` → stuur de foutmelding mee. Is er geen melding, gebruik dan "Invalid scooter".
   - `return@post` → **stop hier** met dit verzoek. Het `@post` zegt: stop het `post { }`-blok, niet iets anders.
4. `call.respond(HttpStatusCode.Created, scooter.toResponse())` → alles goed: antwoord met **201 Created** ("aangemaakt") en de nieuwe scooter.

> **Waarom vangen we de fout zelf op?** In `StatusPages.kt` staat een algemene regel: elke fout die niemand opvangt wordt **500 Internal Server Error** ("de server is kapot"). Een verkeerde snelheid is geen serverfout maar een **fout van de gebruiker**. Daarom vangen we hem zelf op en geven we **400**.

#### Endpoint 3: `DELETE /scooters/{id}` – een scooter verwijderen

```kotlin
delete("/{id}") {
    val id = call.parameters["id"]?.toIntOrNull()
    if (id == null) {
        call.respond(HttpStatusCode.BadRequest, "Invalid id")
        return@delete
    }

    if (repository.delete(id)) {
        call.respond(HttpStatusCode.NoContent)
    } else {
        call.respond(HttpStatusCode.NotFound, "Scooter not found")
    }
}
```

1. `delete("/{id}")` → het adres is `/scooters/{id}`. `{id}` is een **plekhouder**: bij `/scooters/1` is `id` gelijk aan `"1"`.
2. `call.parameters["id"]` → haal de waarde uit het adres. Dat is **tekst** (`"1"`).
3. `?.toIntOrNull()` → maak er een getal van. Lukt dat niet (bijvoorbeeld bij `/scooters/abc`), dan wordt het `null`.
   - `?.` → de **safe call**: "doe dit alleen als het niet `null` is".
4. `if (id == null) { ... }` → geen geldig nummer? Dan **400 Bad Request** en stoppen.
5. `if (repository.delete(id))` → probeer te verwijderen.
   - Gelukt (`true`) → **204 No Content** ("gelukt, er is niets terug te sturen").
   - Niet gevonden (`false`) → **404 Not Found** + "Scooter not found".

---

### 5.7 `Routing.kt` – de lijst met loketten

**Pad:** `src/main/kotlin/Routing.kt`

```kotlin
package nl.vlugverkoop

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import nl.vlugverkoop.repository.CarRepository
import nl.vlugverkoop.repository.ScooterRepository
import nl.vlugverkoop.routes.carRoutes
import nl.vlugverkoop.routes.scooterRoutes

fun Application.configureRouting() {
    routing {
        carRoutes(CarRepository())
        scooterRoutes(ScooterRepository())
        get("/") {
            call.respondText("Hello, World!")
        }
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}
```

**Wat hebben wij hier toegevoegd?** Alleen deze regels:
- `import nl.vlugverkoop.repository.ScooterRepository`
- `import nl.vlugverkoop.routes.scooterRoutes`
- `scooterRoutes(ScooterRepository())`

**Uitleg:**

- `fun Application.configureRouting()` → zet alle adressen van de server klaar. De server roept deze functie aan bij het opstarten (dat staat in `application.yaml`).
- `routing { ... }` → hierbinnen staan alle loketten.
- `scooterRoutes(ScooterRepository())` → maak **één** magazijn en open het scooterloket. Zolang de server draait, gebruikt het loket steeds hetzelfde magazijn.
- `import io.ktor.server.routing.*` → de `*` betekent "alles uit dit pakket".

**Hoe start de server?** In `src/main/resources/application.yaml` staat de poort (8080) en de volgorde waarin de server zich instelt:

```yaml
ktor:
  deployment:
    port: 8080
  application:
    modules:
      - nl.vlugverkoop.HttpKt.configureHttp
      - nl.vlugverkoop.SerializationKt.configureSerialization
      - nl.vlugverkoop.StatusPagesKt.configureStatusPages
      - nl.vlugverkoop.ExposedKt.configureExposed
      - nl.vlugverkoop.RoutingKt.configureRouting
```

- `configureSerialization` → zorgt dat objecten automatisch **JSON** worden.
- `configureStatusPages` → zorgt dat onverwachte fouten **500** worden.
- `configureRouting` → onze adressen.

---

### 5.8 `ScooterRoutesTest.kt` – de 6 tests

**Pad:** `src/test/kotlin/ScooterRoutesTest.kt`

```kotlin
package nl.vlugverkoop

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import nl.vlugverkoop.dto.ScooterResponse
import nl.vlugverkoop.model.PlateColor
import kotlin.test.*

class ScooterRoutesTest {

    @Test
    fun `GET scooters returns 200 with JSON`() = testApplication {
        configure()

        val response = client.get("/scooters")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
    }

    @Test
    fun `GET scooters returns all scooters with their details`() = testApplication {
        configure()

        val scooters = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())

        assertEquals(3, scooters.size)
        val vespa = scooters.first { it.id == 1 }
        assertEquals("Vespa", vespa.brand)
        assertEquals("Primavera", vespa.model)
        assertEquals(2650.0, vespa.price)
        assertEquals(45, vespa.maxSpeedKmh)
    }

    @Test
    fun `GET scooters gives blue plate for 25 kmh and yellow plate for 45 kmh`() = testApplication {
        configure()

        val scooters = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())

        scooters.forEach { scooter ->
            val expected = if (scooter.maxSpeedKmh == 25) PlateColor.BLUE else PlateColor.YELLOW
            assertEquals(expected, scooter.plateColor, "Wrong plate colour for scooter ${scooter.id}")
        }
    }

    @Test
    fun `POST scooters creates a scooter with the right plate colour`() = testApplication {
        configure()

        val response = client.post("/scooters") {
            contentType(ContentType.Application.Json)
            setBody("""{"brand":"Peugeot","model":"Kisbee","year":2022,"price":1750.0,"mileage":2500,"maxSpeedKmh":25}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val scooter = Json.decodeFromString<ScooterResponse>(response.bodyAsText())
        assertEquals(4, scooter.id)
        assertEquals("Peugeot", scooter.brand)
        assertEquals(PlateColor.BLUE, scooter.plateColor)
    }

    @Test
    fun `POST scooters with a speed other than 25 or 45 returns 400`() = testApplication {
        configure()

        val response = client.post("/scooters") {
            contentType(ContentType.Application.Json)
            setBody("""{"brand":"Peugeot","model":"Kisbee","year":2022,"price":1750.0,"mileage":2500,"maxSpeedKmh":80}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val scooters = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())
        assertEquals(3, scooters.size, "Invalid scooter must not be saved")
    }

    @Test
    fun `DELETE scooters removes the scooter and returns 404 for an unknown id`() = testApplication {
        configure()

        assertEquals(HttpStatusCode.NoContent, client.delete("/scooters/1").status)
        val scooters = Json.decodeFromString<List<ScooterResponse>>(client.get("/scooters").bodyAsText())
        assertTrue(scooters.none { it.id == 1 })

        assertEquals(HttpStatusCode.NotFound, client.delete("/scooters/99").status)
    }
}
```

**Wat is een test?** Code die **automatisch controleert** of andere code goed werkt. Deze tests doen de app na: ze sturen verzoeken naar de server en controleren de antwoorden. In het plan van aanpak heten ze **integratietesten**, omdat alle onderdelen samen worden getest (loket, magazijn, model en verpakking).

**Algemene uitleg:**

| Code | Betekenis |
|---|---|
| `class ScooterRoutesTest` | Een groep tests |
| `@Test` | Label: "dit is een test" |
| `` fun `GET scooters returns 200 with JSON`() `` | Met backticks (`` ` ``) mag de naam spaties bevatten, zodat hij leest als een zin |
| `testApplication { ... }` | Start een **testversie van de server in het geheugen**. De echte server hoeft niet aan te staan en er wordt geen poort gebruikt. |
| `configure()` | Laad de instellingen uit `application.yaml`, zodat de testserver precies zo is ingesteld als de echte |
| `client` | Een nep-app die verzoeken stuurt |
| `assertEquals(verwacht, echt)` | "Controleer dat deze twee gelijk zijn." Zo niet, dan **faalt** de test. |
| `assertTrue(...)` | "Controleer dat dit waar is." |
| `Json.decodeFromString<...>(tekst)` | Zet JSON-tekst om naar Kotlin-objecten |
| `bodyAsText()` | De inhoud van het antwoord als tekst |

> Elke test start een **nieuwe** testserver met een **vers magazijn** (weer 3 scooters). Daarom krijgt een nieuwe scooter in test 4 nummer 4, ook al is in test 6 scooter 1 verwijderd.

**De 6 tests één voor één:**

| # | Test | Wat het doet | Wat het controleert |
|---|---|---|---|
| 1 | `GET scooters returns 200 with JSON` | Vraagt `/scooters` op | Statuscode **200 OK** en het antwoord is **JSON**. (`withoutParameters()` haalt de toevoeging `; charset=UTF-8` weg.) |
| 2 | `GET scooters returns all scooters with their details` | Vraagt `/scooters` op en zet de JSON om naar objecten | Er zijn **3** scooters en scooter 1 is een **Vespa Primavera** van **2650.0** met **45** km/u. `first { it.id == 1 }` zoekt de eerste scooter met nummer 1. |
| 3 | `GET scooters gives blue plate for 25 kmh and yellow plate for 45 kmh` | Loopt met `forEach` langs elke scooter | Bij 25 km/u moet het kenteken **BLUE** zijn, anders **YELLOW** |
| 4 | `POST scooters creates a scooter with the right plate colour` | Plaatst een Peugeot Kisbee van 25 km/u | Statuscode **201 Created**, nummer **4**, merk **Peugeot**, kenteken **BLUE** |
| 5 | `POST scooters with a speed other than 25 or 45 returns 400` | Plaatst een scooter van **80** km/u | Statuscode **400 Bad Request**, en er zijn nog steeds **3** scooters (de foute is niet opgeslagen) |
| 6 | `DELETE scooters removes the scooter and returns 404 for an unknown id` | Verwijdert scooter 1, daarna scooter 99 | Scooter 1: **204 No Content** en hij is echt weg (`none { it.id == 1 }`). Scooter 99: **404 Not Found**. |

**Nog een paar dingen uit de tests:**

- `client.post("/scooters") { ... }` → stuur een POST-verzoek. Tussen `{ }` stel je het verzoek in:
  - `contentType(ContentType.Application.Json)` → "wat ik stuur is JSON".
  - `setBody("""...""")` → de inhoud. `"""` is een **raw string**: daarin hoef je de aanhalingstekens van JSON niet apart te markeren.
- `if (...) PlateColor.BLUE else PlateColor.YELLOW` → in Kotlin geeft een `if` een **waarde** terug.
- `"... ${scooter.id}"` → zet een waarde in de tekst. Gebruik `${ }` als het meer is dan één naam.

**Resultaat:** 6 van 6 geslaagd, 0 fouten.

**3 endpoint-tests inleveren?** Kies test **1** (GET), **4** (POST) en **6** (DELETE). Dan heb je elk endpoint één keer getest.

**Tests uitvoeren:**
- IntelliJ: open het bestand en klik op de groene ▶ naast `class ScooterRoutesTest`.
- Terminal: `sh ./gradlew test --tests "nl.vlugverkoop.ScooterRoutesTest"`
- Alle tests: `sh ./gradlew test`

---

### 5.9 `requests.http` – met de hand testen

**Pad:** `requests.http` (in de hoofdmap). Het auto-gedeelte is van Jawad, het scooter-gedeelte hebben wij toegevoegd:

```http
### Get all scooters
GET http://127.0.0.1:8080/scooters

### Delete scooter with id 1
DELETE http://127.0.0.1:8080/scooters/1

### Delete a scooter that does not exist
DELETE http://127.0.0.1:8080/scooters/99

### Create a new scooter (45 km/h → yellow plate)
POST http://127.0.0.1:8080/scooters
Content-Type: application/json

{
  "brand": "Peugeot",
  "model": "Kisbee",
  "year": 2022,
  "price": 1750.0,
  "mileage": 2500,
  "maxSpeedKmh": 45
}

### Create a scooter with an invalid speed (gives 400 Bad Request)
POST http://127.0.0.1:8080/scooters
Content-Type: application/json

{
  "brand": "Peugeot",
  "model": "Kisbee",
  "year": 2022,
  "price": 1750.0,
  "mileage": 2500,
  "maxSpeedKmh": 80
}
```

**Uitleg:**
- Dit is een bestand voor de **HTTP-client van IntelliJ**. Hiermee stuur je verzoeken naar je **draaiende** server.
- `###` → begin van een nieuw verzoek, met een titel.
- `GET` / `POST` / `DELETE` + adres → wat je wilt doen.
- `Content-Type: application/json` → "wat ik stuur is JSON".
- Na een lege regel volgt de inhoud (de JSON).

**Gebruiken:**
1. Start de server (`sh ./gradlew run` of de groene ▶ bij `main.kt`).
2. Open `requests.http`.
3. Klik op de groene ▶ naast een verzoek.
4. Onderaan zie je het antwoord.

> Een browser kan alleen **GET**. Voor POST en DELETE heb je dit bestand nodig (of een programma zoals Postman).

---

## 6. Statuscodes

| Code | Naam | Betekenis | Wanneer bij scooters |
|---|---|---|---|
| **200** | OK | Gelukt, hier zijn de gegevens | `GET /scooters` |
| **201** | Created | Gelukt, iets nieuws aangemaakt | `POST /scooters` met 25 of 45 km/u |
| **204** | No Content | Gelukt, niets terug te sturen | `DELETE /scooters/1` |
| **400** | Bad Request | Foute invoer van de gebruiker | POST met 80 km/u, of DELETE met `/scooters/abc` |
| **404** | Not Found | Bestaat niet | `DELETE /scooters/99` |
| **500** | Internal Server Error | Er ging iets mis in de server | Als een fout door niemand wordt opgevangen |

Ezelsbruggetje: **2xx = gelukt**, **4xx = fout van de gebruiker**, **5xx = fout van de server**.

---

## 7. Ontwerpkeuzes: waarom we het zo hebben gedaan

| Keuze | Waarom |
|---|---|
| **De kentekenkleur wordt uitgerekend, niet ingevuld** | Snelheid en kleur horen altijd bij elkaar. Zo kan niemand "45 km/u + blauw" invullen. Een foute combinatie is gewoon onmogelijk. |
| **Een snelheid die niet 25 of 45 is geeft een fout** | Andere snelheden bestaan niet voor scooters in Nederland. Een fout (`IllegalArgumentException`) voorkomt dat er ooit een foute scooter bestaat. |
| **`PlateColor` staat in hetzelfde bestand als `Scooter`** | De eerste opdracht was om alleen `Scooter.kt` aan te passen. De Kotlin-conventies staan toe dat nauw verwante dingen in één bestand staan. Hij kan later naar een eigen bestand (`PlateColor.kt`). |
| **Dezelfde opbouw als auto (Jawad)** | Iedereen in het team herkent de structuur meteen, en later is alles makkelijker samen te voegen. |
| **Aparte Response en Request (DTO's)** | De app krijgt alleen wat hij nodig heeft. Het model kan veranderen zonder dat de app kapotgaat. |
| **Opslag in het geheugen (lijst)** | Simpel om mee te beginnen. Later komt er een database voor in de plaats, alleen in de repository. |
| **Fout zelf opvangen in POST (try/catch → 400)** | Anders wordt het 500 (serverfout), terwijl het een fout van de gebruiker is. |
| **Eerst de scooter maken, dan pas opslaan** | Gaat het maken fout, dan wordt er niets opgeslagen. |
| **Repository wordt meegegeven aan de routes** | Losse onderdelen zijn makkelijker te testen en te vervangen (Dependency Injection). |

---

## 8. Hoe we het hebben gebouwd (stap voor stap)

1. **Documentatie gelezen.** In het **plan van aanpak** staan de eisen (snelheid 25/45, kenteken geel/blauw, enum `PlateColor`, overerving, tests). Het **brandbook** geeft de schrijfwijze "VlugVerkoop".
2. **`Scooter.kt` verbeterd**, als enige bestand: `PlateColor` toegevoegd, de kleur afgeleid van de snelheid, foute snelheden geweigerd en de naam goed gespeld. Daarna gecompileerd om te zien dat alles werkt.
3. **Code van het team binnengehaald.** `git pull` gaf een fout vanwege lokale wijzigingen. Opgelost (zie hoofdstuk 9).
4. **Jawads opbouw voor auto's gevolgd.** Hij had `GET /cars` gemaakt met `CarResponse`, `CarRepository` en `CarRoutes`. Wij maakten hetzelfde voor scooters: `ScooterResponse`, `ScooterRepository`, `ScooterRoutes` en 1 regel in `Routing.kt`.
5. **Met de hand getest.** De server gestart en `/scooters` en `/cars` opgevraagd. Beide gaven 200 OK met de juiste JSON.
6. **3 endpoint-tests geschreven** voor `GET /scooters`. Alle 3 groen.
7. **Browserfout opgelost:** "HTTP header is larger than 8192 bytes" → `127.0.0.1` gebruiken in plaats van `localhost`.
8. **Uitleg geschreven:** `SCOOTER-UITLEG.md`.
9. **Gecommit en gepusht.** Eerst Younes' motorwerk binnengehaald, alle tests gedraaid, daarna 2 commits gepusht.
10. **Jawad voegde POST en DELETE toe voor auto's.** Wij deden hetzelfde voor scooters: `CreateScooterRequest`, `add` en `delete` in het magazijn, POST en DELETE in de routes en voorbeelden in `requests.http`. Plus de scooterregel: een foute snelheid geeft 400.
11. **3 extra tests** (plaatsen, weigeren, verwijderen). Nu 6 in totaal, alle 6 groen.
12. **Gecommit en gepusht.**

**Commits op GitHub:**

| Commit | Wat |
|---|---|
| `Add GET /scooters endpoint with plate colour and tests` | Scooter met kentekenkleur, GET /scooters, 3 tests, uitleg |
| `Use official Kotlin code style for the project` | IntelliJ maakt de code bij iedereen hetzelfde op |
| `Add POST /scooters and DELETE /scooters/{id} endpoints` | Plaatsen en verwijderen, 3 extra tests, requests.http |

---

## 9. Git: wat we hebben gedaan

**Git** houdt alle versies van de code bij. **GitHub** is de online plek waar het team de code deelt.

| Commando | Betekenis |
|---|---|
| `git status` | Laat zien wat je hebt veranderd |
| `git fetch` | Kijk wat er nieuw is op GitHub (verandert nog niets aan jouw bestanden) |
| `git pull` | Haal de nieuwe code van GitHub binnen |
| `git pull --autostash` | Hetzelfde, maar zet je eigen wijzigingen even opzij en daarna terug |
| `git merge --ff-only origin/master` | Neem de nieuwe code van GitHub over, alleen als dat zonder botsingen kan |
| `git add <bestand>` | Zet een bestand klaar voor de volgende commit |
| `git commit` | Sla de klaargezette wijzigingen op, met een beschrijving |
| `git push` | Stuur je commits naar GitHub |

**Het probleem dat we hadden:**
- `git pull` gaf: *"cannot pull with rebase: You have unstaged changes."*
- **Oorzaak:** er waren nog wijzigingen die niet gecommit waren, en op deze computer weigert `git pull` dan.
- **Oplossing:** eerst kijken wat er nieuw was (`git fetch`). De nieuwe code bevatte precies dezelfde wijziging als wij lokaal hadden, dus onze kopie kon weg. Daarna de nieuwe code overgenomen (`git merge --ff-only`).
- **Volgende keer:** `git pull --autostash`.

**Bestanden die we expres NIET naar GitHub sturen:**

| Bestand | Waarom niet |
|---|---|
| `h2.mv.db` | Tijdelijke database die ontstaat als je de server draait (staat nu in `.gitignore` via `*.db`) |
| `.idea/claudeCodeEditorTabs.xml` | Jouw persoonlijke instellingen |
| `.idea/claudeCodeTabState.xml` | Jouw persoonlijke instellingen |

---

## 10. Nog te doen

| Punt | Uitleg |
|---|---|
| **Eén adres voor alle voertuigen** | Volgens het plan wordt het uiteindelijk `GET /vehicles?type=SCOOTER`. Nu heeft elk voertuig zijn eigen adres (`/cars`, `/scooters`). |
| **`getType()` en `VehicleType`** | Volgens het plan moet elk voertuig zijn soort kunnen noemen (CAR, MOTORCYCLE, SCOOTER). Dat komt in `Vehicle.kt`, dus het raakt alle voertuigen. |
| **Database** | Nu staat alles in het geheugen. Later komt er H2 met Exposed (zit al in het project). Dan krijgt ook elk voertuig een eigen nummer, want nu hebben auto 1 en scooter 1 hetzelfde nummer. |
| **Unit-tests** | Het plan vraagt voor deel 1 ook 3 unit-tests, bijvoorbeeld "een negatieve prijs wordt geweigerd". |
| **Advertentiekosten** | Betalingen vallen volgens het plan buiten de scope en het brandbook belooft "geen verborgen kosten". Met het team bespreken of `calculateListingFee` eruit moet. |
| **Spelling** | In `Vehicle.kt`, `Car.kt` en `Motorcycle.kt` staat nog "Vlugverkoop" in plaats van "VlugVerkoop". |

---

## 11. Spiekbriefje Kotlin

| Begrip | Voorbeeld | Betekenis |
|---|---|---|
| `package` | `package nl.vlugverkoop.model` | In welk pakket (map) de code hoort |
| `import` | `import nl.vlugverkoop.model.Scooter` | Code uit een ander bestand gebruiken |
| `class` | `class Scooter(...)` | Een bouwtekening of formulier |
| constructor | `Scooter(1, "Vespa", ...)` | Wat je invult om een object te maken |
| `abstract class` | `abstract class Vehicle` | Een algemene klasse waarvan je zelf geen object kunt maken |
| overerving | `class Scooter(...) : Vehicle(...)` | "Scooter **is een** Vehicle" en krijgt alles van Vehicle |
| `override` | `override fun calculateListingFee()` | Een verplichte functie van de ouder invullen |
| `val` | `val maxSpeedKmh: Int` | Vaste waarde, verandert niet |
| `var` | (niet gebruikt) | Waarde die wél kan veranderen |
| `Int` | `45` | Heel getal |
| `Double` | `2650.0` | Kommagetal |
| `String` | `"Vespa"` | Tekst |
| `Boolean` | `true` / `false` | Waar of niet waar |
| `List` / `mutableListOf` | `mutableListOf(a, b)` | Lijst die je niet / wel kunt veranderen |
| `enum class` | `enum class PlateColor { BLUE, YELLOW }` | Vaste keuzelijst |
| `data class` | `data class ScooterResponse(...)` | Klasse alleen voor gegevens |
| `@Serializable` | boven een data class | Kan automatisch naar en van JSON |
| `fun` | `fun getAll(): List<Scooter>` | Een functie |
| `: Type` na een functie | `fun add(...): Scooter` | Wat de functie teruggeeft |
| `= ...` functie | `fun x(): Double = 2.95` | Functie van één regel |
| extension function | `fun Scooter.toResponse()` | Een functie "vastplakken" aan een bestaande klasse |
| named arguments | `brand = request.brand` | Bij het aanroepen zeggen welke waarde bij welke naam hoort |
| lambda `{ }` | `find { it.id == 1 }` | Een stukje code dat je meegeeft aan een functie |
| `it` | `{ it.id == id }` | Het element dat nu wordt bekeken |
| `null` | | "Niets", geen waarde |
| `?.` | `x?.toIntOrNull()` | Safe call: doe dit alleen als `x` niet `null` is |
| `?:` | `x ?: 0` | Elvis: is `x` `null`, gebruik dan `0` |
| `requireNotNull(x) { "..." }` | | Is `x` `null`, gooi dan een fout met deze melding |
| `$naam` / `${...}` | `"was $maxSpeedKmh"` | Een waarde in een tekst zetten |
| `"""..."""` | `"""{"a":1}"""` | Raw string: tekst waarin `"` gewoon mag |
| `try { } catch (e: ...) { }` | | Probeer iets. Gaat het fout, doe dan dit. |
| `return@post` | | Stop het `post { }`-blok |
| `if ... else` | `if (a) X else Y` | Keuze. In Kotlin geeft `if` ook een waarde terug. |
| `.map { }` | `list.map { it.toResponse() }` | Maak van elk element iets nieuws |
| `.find { }` | | Eerste element dat klopt, anders `null` |
| `.first { }` | | Eerste element dat klopt, anders een fout |
| `.forEach { }` | | Doe iets met elk element |
| `.none { }` | | `true` als **geen enkel** element klopt |
| `.removeIf { }` | | Verwijder de elementen die kloppen. `true` als er iets is verwijderd. |
| `.maxOfOrNull { }` | | Hoogste waarde, of `null` bij een lege lijst |
| `/** ... */` | | KDoc: uitleg boven code |
| `// ...` | | Opmerking op één regel |

**Ktor-begrippen:**

| Begrip | Betekenis |
|---|---|
| **Ktor** | Kotlin-bibliotheek om een server te bouwen |
| `routing { }` | Hierbinnen staan alle adressen |
| `route("/scooters") { }` | Groep adressen die met `/scooters` beginnen |
| `get { }` / `post { }` / `delete("/{id}") { }` | Wat er gebeurt bij dat soort verzoek |
| `call` | Het huidige verzoek en antwoord |
| `call.receive<T>()` | Lees de JSON van de app in als object van type T |
| `call.respond(x)` | Stuur een antwoord (automatisch als JSON) |
| `call.respond(HttpStatusCode.X, y)` | Stuur een antwoord met een bepaalde statuscode |
| `call.parameters["id"]` | Lees `{id}` uit het adres |
| `testApplication { }` | Testserver in het geheugen |
| `client.get(...)` / `client.post(...)` | Nep-app die verzoeken stuurt in een test |

**Architectuurbegrippen:**

| Begrip | Betekenis |
|---|---|
| **Model** | De klasse die beschrijft wat iets is (`Scooter`) |
| **DTO** | Data Transfer Object: de "verpakking" tussen server en app (`ScooterResponse`, `CreateScooterRequest`) |
| **Repository** | De plek waar gegevens worden bewaard en opgehaald |
| **Route / Endpoint** | Een adres op de server (`GET /scooters`) |
| **Overerving** | Een klasse neemt alles over van een andere klasse |
| **Polymorfisme** | Dezelfde functie werkt per klasse anders (`calculateListingFee`) |
| **Dependency Injection** | Een onderdeel krijgt wat het nodig heeft van buitenaf mee |
| **Integratietest** | Test waarin meerdere onderdelen samen worden getest |
| **Unit-test** | Test van één klein stukje code apart |

---

## 12. Overhoorvragen

Probeer eerst zelf te antwoorden. De antwoorden staan eronder.

1. Wat is het verschil tussen `Vehicle` en `Scooter`?
2. Waarom kun je geen `Vehicle` maken?
3. Welke twee velden heeft een scooter extra ten opzichte van een voertuig?
4. Wat gebeurt er als je `Scooter(1, "Vespa", "X", 2020, 1000.0, 500, 80)` maakt?
5. Waarom vult de app de kentekenkleur niet zelf in?
6. Wat is een `enum`, en welke waarden heeft `PlateColor`?
7. Wat doet `requireNotNull`?
8. Wat is het verschil tussen `Scooter` en `ScooterResponse`?
9. Waarom staan `id` en `plateColor` niet in `CreateScooterRequest`?
10. Hoe bepaalt het magazijn het nummer van een nieuwe scooter?
11. Waarom wordt een scooter met 80 km/u nooit opgeslagen?
12. Welke drie endpoints heeft de scooter, en welke statuscodes geven ze?
13. Waarom vangen we in POST de fout zelf op met `try/catch`?
14. Wat betekent `?.` en wat betekent `?:`?
15. Wat doet `testApplication`, en moet de echte server daarvoor aanstaan?
16. Waarom krijgt de nieuwe scooter in de POST-test nummer 4?
17. Wat is het verschil tussen een integratietest en een unit-test?
18. Wat doe je als `git pull` zegt dat je "unstaged changes" hebt?
19. Wat doe je als de browser "HTTP header is larger than 8192 bytes" geeft?
20. Wat is polymorfisme? Geef een voorbeeld uit dit project.

### Antwoorden

1. `Vehicle` is de **algemene basisklasse** met wat elk voertuig heeft. `Scooter` **erft** daarvan en voegt snelheid en kentekenkleur toe.
2. Omdat `Vehicle` **abstract** is. Het is alleen een basis voor Car, Motorcycle en Scooter.
3. `maxSpeedKmh` (snelheid) en `plateColor` (kentekenkleur).
4. Het gaat **fout**: `requireNotNull` gooit een `IllegalArgumentException` met *"Speed class must be 25 or 45 km/h, was 80"*.
5. Omdat de kleur altijd bij de snelheid hoort. Door hem uit te rekenen kan er nooit een foute combinatie ontstaan.
6. Een vaste keuzelijst. `PlateColor` heeft `BLUE(25)` en `YELLOW(45)`.
7. Controleert dat iets niet `null` is. Is het wel `null`, dan geeft het een fout met de meegegeven melding.
8. `Scooter` is het **model** dat de server zelf gebruikt. `ScooterResponse` is de **verpakking** (DTO) die naar de app gaat, met `@Serializable` voor JSON en met `listingFee` erbij.
9. Het `id` geeft de **server**, en de `plateColor` rekent de **scooter zelf** uit.
10. Hoogste nummer + 1 (`maxOfOrNull { it.id } ?: 0` plus 1). Bij een lege lijst wordt het 1.
11. Omdat de scooter eerst wordt **gemaakt** en daarna pas **opgeslagen**. Bij 80 gaat het maken al fout, dus het opslaan gebeurt nooit.
12. `GET /scooters` → 200. `POST /scooters` → 201, of 400 bij een foute snelheid. `DELETE /scooters/{id}` → 204, 404 als het nummer niet bestaat, of 400 als het geen getal is.
13. Anders maakt `StatusPages` er een **500** (serverfout) van, terwijl het een fout van de **gebruiker** is. Dat hoort een **400** te zijn.
14. `?.` = **safe call**: alleen uitvoeren als het niet `null` is. `?:` = **Elvis**: is het `null`, gebruik dan de waarde rechts.
15. Het start een **testserver in het geheugen**. De echte server hoeft **niet** aan te staan.
16. Elke test begint met een vers magazijn met scooter 1, 2 en 3. Het hoogste nummer is 3, dus de nieuwe wordt 4.
17. Een **integratietest** test meerdere onderdelen samen (loket, magazijn, model, verpakking). Een **unit-test** test één klein stukje apart, bijvoorbeeld alleen de klasse `Scooter`.
18. `git pull --autostash` gebruiken, of eerst je wijzigingen committen.
19. `http://127.0.0.1:8080` gebruiken in plaats van `localhost`, of een incognitovenster.
20. Dezelfde functie werkt per klasse anders. Voorbeeld: `calculateListingFee()` staat in `Vehicle`, maar Car, Motorcycle en Scooter vullen hem elk zelf in (Scooter: 2.95).
