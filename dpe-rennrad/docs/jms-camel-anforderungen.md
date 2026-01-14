# Umsetzung der 3 Anforderungen (JMS + Apache Camel)

Stand: 2026-01-14  
Projekt: dpe-rennrad (Quarkus 3.17.0, Camel Quarkus 3.17.0, ActiveMQ 6.2.0)

---

## Überblick
Dieses Dokument erklärt, wie die drei Anforderungen im Projekt umgesetzt wurden. Der Fokus liegt auf:
- JMS‑Integration mit ActiveMQ 6.2.0
- Apache Camel für Content‑Based Routing
- XML/JSON‑Transformation in beide Richtungen
- Datenfluss aus Prozess, Datei/DB und externen Systemen

Konfigurationsdateien und zentrale Klassen:
- [src/main/resources/application.properties](../src/main/resources/application.properties)
- [src/main/java/edu/thi/demo/jms/JmsConfiguration.java](../src/main/java/edu/thi/demo/jms/JmsConfiguration.java)
- [src/main/java/edu/thi/demo/camel/ProviderRoutingRoutes.java](../src/main/java/edu/thi/demo/camel/ProviderRoutingRoutes.java)

---

## Architektur (Kurzfassung)
1. **BPMN‑Prozess** sendet eine Nachricht asynchron in eine JMS‑Queue (ActiveMQ).
2. **Apache Camel** liest aus dieser Queue und entscheidet anhand `providerId`, wohin die Nachricht geht (Dateisystem oder „DB“‑Simulation). Dabei wird je nach Ziel JSON → XML oder JSON → JSON geschrieben.
3. **Dateisystem/Backend‑Simulation** liefert Daten zurück, die wieder in die JMS‑Queue gelegt werden. Camel kann XML → JSON transformieren und an die Responses‑Queue senden.

Queues:
- `provider-requests`: eingehende Requests aus dem Prozess
- `provider-responses`: Antworten aus Zielsystemen

---

## 1) Versand einer asynchronen Nachricht aus dem Prozess (Send Task → JMS)

### Was passiert?
- Im Prozess wird nach dem Ermitteln des Providers eine **Send Task** genutzt.
- Die Nutzdaten (z. B. `providerId`, Prozess‑Kontext, Kundendaten) werden als JSON an die JMS‑Queue gesendet.
- Ziel‑Queue: `provider-requests`.

### Umsetzung im Projekt
- ActiveMQ wird über eine `ConnectionFactory` (CDI Bean) bereitgestellt:
  - `JmsConfiguration.java`
- Die Queue‑Namen sind in `application.properties` konfiguriert:
  - `jms.queue.provider-requests`

### Ergebnis
- Der Prozess ist **asynchron**: Er sendet die Nachricht und läuft weiter bzw. wartet auf eine Antwort.
- Die JMS‑Queue entkoppelt Prozess und Zielsysteme.

---

## 2) Apache Camel: Content‑Based Routing + XML/JSON Transformation

### Was passiert?
- Camel liest die Requests aus `provider-requests`.
- Die Nachricht wird in JSON geparst.
- Routing erfolgt anhand `providerId`:
  - **Provider 1** → JSON ins Dateisystem
  - **Provider 2** → XML ins Dateisystem (JSON → XML)
  - **Provider 3** → „DB‑Simulation“ (JSON Datei)

### Umsetzung im Projekt
- Routen in `ProviderRoutingRoutes.java`:
  - `from("jms:queue:" + providerRequestsQueue)`
  - `choice()` + `when()` auf `providerId`
  - `marshal().json(...)` oder `convertJsonToXml(...)`
- Zielpfade werden über `application.properties` konfiguriert:
  - `camel.file.output.path`

### Transformation (bidirektional)
- **JSON → XML** bei Provider 2 (`convertJsonToXml`)
- **XML → JSON** bei Rückfluss (siehe Requirement 3)

### Ergebnis
- Content‑Based Routing ist umgesetzt.
- Transformationen stellen sicher, dass jedes Zielsystem das benötigte Format erhält.

---

## 3) Daten aus Dateisystem/DB in JMS‑Queue oder via Postman

### Was passiert?
- Camel überwacht ein Input‑Verzeichnis.
- JSON‑Dateien werden direkt als Antwort in `provider-responses` gelegt.
- XML‑Dateien werden zuerst in JSON transformiert und dann in `provider-responses` gelegt.

### Umsetzung im Projekt
- Route in `ProviderRoutingRoutes.java`:
  - `from("file:" + fileInputPath + "?noop=true&delay=5000")`
  - `when()` auf `.json` oder `.xml`
  - `convertXmlToJson(...)` bei XML
  - `to("jms:queue:" + providerResponsesQueue)`

### Postman‑Simulation
- Alternativ kann man per REST‑Call eine Nachricht in die JMS‑Queue legen (z. B. über eine interne REST‑API, falls vorhanden), um den „Backend‑System‑Versand“ zu simulieren.
- Ergebnis ist identisch: Camel verarbeitet die Nachricht und routet sie weiter.

---

## Konfiguration (relevant)
In [application.properties](../src/main/resources/application.properties):
- ActiveMQ:
  - `activemq.broker.url=tcp://localhost:61616`
  - `activemq.username=admin`
  - `activemq.password=admin`
- JMS Queues:
  - `jms.queue.provider-requests=provider-requests`
  - `jms.queue.provider-responses=provider-responses`
- Camel Dateipfade:
  - `camel.file.output.path=target/camel-output`
  - `camel.file.input.path=target/camel-input`
- Camel JMS (Wiring):
  - `camel.component.jms.connection-factory=#jmsConnectionFactory`

---

## Zusammenfassung
- **Anforderung 1:** Prozess sendet JMS‑Nachricht asynchron in `provider-requests`.
- **Anforderung 2:** Camel routet in Datei/„DB“, inkl. JSON/XML‑Transformation.
- **Anforderung 3:** Camel liest Dateien (JSON/XML) und schreibt Antworten in `provider-responses` oder simuliert Backend‑Nachrichten.

Damit sind alle drei Anforderungen vollständig umgesetzt und entkoppelt.
