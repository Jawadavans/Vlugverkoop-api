# Kentekencheck – uitleg van wat we hebben gebouwd

Dit document legt uit wat de **kentekencheck** in de VlugVerkoop-API doet, hoe hij werkt en welke Kotlin erin zit.
De check is de basis voor de latere Android-app: de app stuurt een kenteken, de server zoekt het op bij de RDW en stuurt de gegevens terug, inclusief een vooraf ingevuld formulier "Advertentie plaatsen".

---

## In het kort

- Eén nieuw adres: **`GET /plates/{kenteken}`**, bijvoorbeeld `/plates/ab-123-c`.
- De server **schoont het kenteken op** (streepjes, spaties en punten weg, hoofdletters) en controleert of het 6 letters en cijfers zijn.
- De server haalt de gegevens op bij **RDW Open Data** uit twee datasets:
  - `m9d7-ebf2` (voertuigen): `voertuigsoort`, `merk`, `handelsbenaming`, `datum_eerste_toelating`, `cilinderinhoud`, `aantal_deuren`, `maximale_constructiesnelheid`
  - `8ys7-d773` (brandstof): `nettomaximumvermogen` (kW)
- De server herkent **auto** (`Personenauto`), **motor** (`Motorfiets`) of **scooter** (`Bromfiets`).
- Bij een motor: het **rijbewijs** (A1 ≤ 125 cc en ≤ 11 kW, A2 ≤ 35 kW, anders A) en of hij **gesmoord** kan worden voor A2 (tot en met 70 kW).
- Er zijn **11 nieuwe tests** (8 unittests, 3 integratietests). Ze gebruiken een nep-RDW en hebben dus geen internet nodig.

> **Let op:** tijdens het bouwen (21-11-2026) was opendata.rdw.nl onbereikbaar door een storing bij de RDW.
> De veldnamen komen uit het gegenereerde RDW-schema; de waarden `Motorfiets` en `Bromfiets` en het datumformaat
> (`jjjjmmdd`) moeten nog één keer live gecontroleerd worden zodra de RDW weer werkt.

---

## Het antwoord voor de app

```json
{
  "plate": "MZ12AB",
  "vehicleType": "MOTORCYCLE",
  "brand": "YAMAHA", "model": "MT-07", "year": 2019,
  "engineCc": 689, "doors": null, "powerKw": 54.0, "maxSpeedKmh": null,
  "licenseCategory": "A",
  "a2Restriction": "POSSIBLE",
  "carForm": null,
  "motorcycleForm": {
    "brand": "YAMAHA", "model": "MT-07", "year": 2019,
    "engineCc": 689, "powerKw": 54, "licenseCategory": "A"
  },
  "scooterForm": null
}
```

- `vehicleType` zegt welk formulier de app moet openen. Alleen dat formulier is gevuld (`carForm`, `motorcycleForm` of `scooterForm`), de andere twee zijn `null`.
- De velden in die formulieren zijn dezelfde als in `CreateCarRequest`, `CreateMotorcycleRequest` en `CreateScooterRequest`. Prijs en kilometerstand vult de gebruiker zelf in (die weet de RDW niet). Bij een motor kiest de gebruiker ook zelf de rijstijl.
- `a2Restriction`: `NOT_NEEDED` (≤ 35 kW, A2 is al genoeg), `POSSIBLE` (35–70 kW, smoren kan), `NOT_POSSIBLE` (> 70 kW).

## Foutmeldingen

Elke fout heeft een vaste `code`. De app zoekt bij die code de juiste tekst op in zijn eigen `strings.xml`.

| Status | Code | Engels (`values/strings.xml`) | Nederlands (`values-nl/strings.xml`) |
|---|---|---|---|
| 400 | `PLATE_INVALID` | That is not a valid licence plate. Use 6 letters and digits, for example AB-123-C. | Dit is geen geldig kenteken. Gebruik 6 letters en cijfers, bijvoorbeeld AB-123-C. |
| 404 | `PLATE_NOT_FOUND` | No vehicle was found with this licence plate. | Er is geen voertuig gevonden met dit kenteken. |
| 422 | `UNSUPPORTED_VEHICLE` | This vehicle is not a car, motorcycle or scooter. | Dit voertuig is geen auto, motor of scooter. |
| 503 | `RDW_UNAVAILABLE` | The vehicle register cannot be reached. Try again later. | Het kentekenregister is niet bereikbaar. Probeer het later opnieuw. |
| – (app) | geen internet | No internet connection. Check your connection and try again. | Geen internetverbinding. Controleer je verbinding en probeer het opnieuw. |
| – (app) | geen camera-toestemming | Camera access is needed to scan a licence plate. You can also type it. | Voor het scannen van een kenteken is toegang tot de camera nodig. Je kunt het ook typen. |

De laatste twee meldingen horen alleen in de app: de server merkt niet dat de telefoon geen internet of geen camera-toestemming heeft.

---

## Per bestand: wat het doet en welke Kotlin erin zit

Het niveau per bestand:
- 🟢 **beginner**: wat je in de eerste weken Kotlin leert
- 🟡 **gemiddeld**: na de basis
- 🔴 **gevorderd**: wat ervaren Kotlin-ontwikkelaars gebruiken (komt na het vereenvoudigen niet meer voor)

### `model/LicensePlate.kt`: het kenteken 🟢
Eén functie, `cleanLicensePlate()`, die van wat de gebruiker typt (of wat de camera leest) een schoon kenteken maakt, of `null` teruggeeft als het geen kenteken is.
- **Top-level functie** 🟢: een functie die niet in een class staat.
- **`filter { }`** 🟢: houdt alleen de tekens over die geen spatie, streepje of punt zijn.
- **`all { }` en `any { }`** 🟢: kijken of elk teken (of minstens één teken) aan een voorwaarde voldoet.
- **Nullable returntype `String?`** 🟢: `null` betekent "ongeldig".

### `model/VehicleType.kt`: auto, motor of scooter 🟢
Zet de RDW-tekst (`Personenauto`, `Motorfiets`, `Bromfiets`) om naar ons eigen type.
- **`enum class` met een property** (`rdwKind`) 🟢.
- **`entries.find { }`** 🟢: zoekt de eerste waarde die past, anders `null`.
- **Safe call `?.`** 🟢: `voertuigsoort?.trim()` geeft geen crash als de tekst `null` is.

### `model/LicenseCategory.kt`: rijbewijs (aangepast) 🟢
Er is een tweede versie van `minimumFor` bijgekomen die met decimalen werkt (de RDW geeft bijvoorbeeld 11,3 kW). De bestaande versie roept de nieuwe aan, dus de regels staan maar op één plek.
- **Function overloading** 🟢: twee functies met dezelfde naam, één voor `Int` en één voor `Double`.
- **`when` zonder argument** 🟢: de eerste voorwaarde die klopt, wint.

### `model/A2Restriction.kt`: smoren voor A2 🟢
Zegt of een motor gesmoord kan worden: niet nodig, mogelijk of niet mogelijk.
- **`enum class`** en een **`companion object`** met **`const val`** 🟢.

### `rdw/RdwVehicle.kt`: de RDW-gegevens 🟢
Houdt de RDW-gegevens bij elkaar die we gebruiken, al omgezet naar Kotlin-types.
- **`data class`** 🟢: krijgt automatisch `equals`, `toString` en `copy`.
- **Nullable properties** (`Int?`) 🟢: de RDW vult niet elk veld voor elk voertuig.

### `rdw/RdwClient.kt`: praten met de RDW 🟡
Vraagt eerst de voertuiggegevens op, daarna het vermogen, en maakt er een `RdwVehicle` van.
- **`suspend fun`** 🟡: de functie kan wachten op internet zonder de server te blokkeren. Je schrijft het gewoon van boven naar beneden, zoals gewone code.
- **Eigen exception** (`RdwUnavailableException`) met **`try`/`catch`** 🟢: alles wat misgaat wordt één duidelijke fout. Er zijn twee soorten fouten bij het verbinden: `IOException` (geen verbinding, timeout) en `UnresolvedAddressException` (het adres bestaat niet, zoals nu tijdens de RDW-storing).
- **`is` en `!is`** 🟢: controleren of de JSON echt een lijst is.
- **Private hulpfuncties** (`readText`, `readDecimal`, `readWholeNumber`) 🟢: lezen een veld, of het nu als tekst (`"1598"`) of als getal (`1598`) binnenkomt.
- **`mapNotNull` en `maxOrNull`** 🟡: bij een hybride (meerdere brandstofregels) het hoogste vermogen pakken.
- **Dependency injection via de constructor** 🟡: de `HttpClient` wordt meegegeven, zodat de tests een nep-RDW kunnen gebruiken.
- **`Closeable`** 🟡: de client wordt netjes gesloten als de server stopt.

### `dto/PlateCheckResponse.kt`: het antwoord voor de app 🟡
Bepaalt hoe het antwoord eruitziet (met drie formulieren: `CarForm`, `MotorcycleForm`, `ScooterForm`) en zet de RDW-gegevens om naar dat antwoord.
- **`@Serializable data class`** 🟢: wordt automatisch JSON.
- **Standaardwaarden** (`= null`) 🟢: de formulieren die niet bij het voertuig horen, blijven leeg.
- **`copy()`** 🟢: eerst het antwoord zonder formulier maken, daarna een kopie met het juiste formulier erin.
- **`when` over een enum** 🟡: de compiler controleert dat elk voertuigtype een formulier krijgt.
- **`var` met `null` en gewone `if`-checks** 🟢: de rijbewijsregels worden alleen toegepast als cc en vermogen bekend zijn.
- **Extension function** `RdwVehicle.toPlateCheckResponse()` 🟡: hetzelfde patroon als `toResponse()` bij auto's en motoren.
- **`ceil()`** 🟢: het vermogen wordt naar boven afgerond voor het formulier (11,3 → 12 kW), zodat het rijbewijs in het formulier hetzelfde blijft als het echte.

### `dto/PlateCheckErrorResponse.kt`: de foutmeldingen 🟢
Bevat de vier foutcodes met hun HTTP-status en Engelse uitleg, en de JSON-vorm van een fout.
- **`enum class` met meerdere properties** 🟢.
- **`name`** van een enum 🟢: geeft de code als tekst (`"PLATE_INVALID"`).

### `routes/PlateRoutes.kt`: het loket 🟡
Het adres `/plates/{kenteken}`: kenteken controleren, RDW vragen en het juiste antwoord of de juiste fout teruggeven.
- **Extension function op `Route`** 🟡: hetzelfde patroon als `carRoutes`.
- **`return@get`** 🟡: stopt alleen het blok van deze route (een "label").
- **`try`/`catch` als expressie** 🟡 en een **private suspend hulpfunctie** (`respondPlateError`).

### `Routing.kt` (aangepast) 🟡
Maakt één `RdwClient` aan, registreert `plateRoutes` en sluit de client als de server stopt (`monitor.subscribe(ApplicationStopped)`).

### `build.gradle.kts` (aangepast)
De Ktor **client** (`client.core`, `client.cio`) om de RDW aan te roepen, en `client.mock` voor de nep-RDW in de tests.

### `requests.http` (aangepast)
Twee requests om de kentekencheck met de hand uit te proberen.

---

## De tests

### `LicensePlateTest.kt`: unittests 🟢
- Opschonen: `ab-123-c`, ` AB 123 C `, `AB.123.C` en een regelafbreking (zoals de camera die kan lezen) worden allemaal `AB123C`.
- Afkeuren: leeg, 5 of 7 tekens, rare tekens, alleen letters, alleen cijfers.

### `PlateCheckTest.kt`: unittests 🟡
- Rijbewijs precies op de grenzen: 125/126 cc, 11/11,1 kW en 35/35,1 kW.
- Smoren precies op de grenzen: 35/35,1 kW en 70/70,1 kW.
- Herkennen van auto, motor en scooter, en weigeren van bijvoorbeeld een bedrijfsauto.
- Het vooraf ingevulde formulier per type, en dat alleen het juiste formulier gevuld is. Concept: **`copy()`** van een data class om één waarde te veranderen.

### `PlateRoutesTest.kt`: integratietests 🟡
Deze tests nemen de plaats in van de UI-tests: ze testen wat de app straks te zien krijgt.
- **Gevonden:** auto, motor en scooter, met het juiste formulier.
- **Niet gevonden** (404), **ongeldig** (400, en de RDW wordt dan niet eens gevraagd) en **geen auto/motor/scooter** (422).
- **RDW niet bereikbaar** (503): geen internet, een foutstatus of HTML in plaats van JSON.
- Concepten: **`MockEngine`** (nep-internet) 🟡: een lambda die voor elk verzoek een antwoord geeft, gevuld uit twee gewone **`mapOf`**-lijsten 🟢. De lijst **`askedPlates`** houdt bij welke kentekens de nep-RDW kreeg 🟡.

---

## Hoe test je het?

```bash
./gradlew test
```

Of met de hand: start de server en gebruik de requests onder **LICENCE PLATE CHECK** in `requests.http`. Die gaan naar de echte RDW, dus daarvoor heb je internet nodig.
