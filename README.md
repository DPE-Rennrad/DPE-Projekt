# DPE-Projekt

## Übersicht
Dieses Quarkus-Projekt integriert BPMN-Prozesse mit Camunda Community für eine Rennrad-Assistenz-App. Es bietet CRUD-APIs für Rennrad-Entitäten, SOAP-Services und automatisierte Workflows (z. B. Empfehlungen, Newsletter).

## Architektur
- **Backend**: Quarkus mit Hibernate Panache, MySQL.
- **BPMN**: Camunda Modeler für Prozesse, deployt zur Remote Camunda Engine.
- **APIs**: REST (JAX-RS) und SOAP (Apache CXF).
- **Struktur**: `model/`, `repository/`, `service/`, `resource/`, `soap/`, `processes/`.

## Setup
1. **Voraussetzungen**: Java 17, Maven, MySQL, Docker (für Camunda).
2. **Camunda starten** (Community Edition):
   ```
   docker run -d --name camunda -p 8080:8080 camunda/camunda-bpm-platform:latest
   ```
   (Community hat keine TTL-Prüfung; siehe CONFIG_CHEAT_SHEET.md für Details).
3. **DB einrichten**: Erstelle DB `dpe_rennrad`, setze Credentials in `application.properties`.
4. **Projekt bauen**: `mvn clean install`.
5. **Starten**: `mvn quarkus:dev` (Port 8088).

## Deployment
- BPMN-Dateien liegen in `processes/`
- Mit Camunda Modeler per Deployement (Rakete) Prozess deployen
- Überprüfe in Cockpit: `http://localhost:8080/camunda/app/cockpit/`.

## APIs
- **REST**: `GET/POST/PUT/DELETE http://localhost:8088/rennrad` (CRUD für Rennrad).
- **SOAP**: `http://localhost:8088/soap/rennrad` (getAllRennraeder).
- **Prozess starten**: POST `http://localhost:8080/engine-rest/message` mit JSON-Body für Kundendaten.  
  **WICHTIG**: Füge `"resultEnabled": true` hinzu, um die Process Instance ID zu erhalten.  
  Beispiel cURL:  
  ```
  curl -X POST http://localhost:8080/engine-rest/message \
    -H "Content-Type: application/json" \
    -d '{
      "messageName": "AppInstallMessage",
      "resultEnabled": true,
      "processVariables": {
        "kundeName": {
          "value": "Max Mustermann",
          "type": "String"
        },
        "kundeEmail": {
          "value": "max.mustermann@example.com",
          "type": "String"
        }
      }
    }'
  ```
  Response enthält: `[{"processInstance": {"id": "abc-123-xyz", ...}}]`
  
- **Prozess beenden**: POST `http://localhost:8088/kunde/deinstallieren/{processInstanceId}`  
  Beispiel: `curl -X POST http://localhost:8088/kunde/deinstallieren/abc-123-xyz`

## Troubleshooting
- **Parsing-Fehler**: Prüfe BPMN in Modeler (siehe Anleitung in CONFIG_CHEAT_SHEET.md).