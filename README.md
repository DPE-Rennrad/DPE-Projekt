# DPE-Projekt

## Übersicht
Dieses Quarkus-Projekt integriert BPMN-Prozesse mit CIB seven EE für eine Rennrad-Assistenz-App. Es bietet CRUD-APIs für Rennrad-Entitäten, SOAP-Services und automatisierte Workflows (z. B. Empfehlungen, Newsletter).

## Architektur
- **Backend**: Quarkus mit Hibernate Panache, MySQL.
- **BPMN**: Camunda Modeler für Prozesse, deployt zur Remote CIB seven Engine.
- **APIs**: REST (JAX-RS) und SOAP (Apache CXF).
- **Struktur**: `model/`, `repository/`, `service/`, `resource/`, `soap/`, `processes/`.

## Setup
1. **Voraussetzungen**: Java 17, Maven, MySQL, Docker (für CIB seven).
2. **CIB seven starten**: 
   ```
   docker run --name tomcat-cibseven-ee -d -p 8080:8080 --env CAMUNDA_BPM_HISTORY_ENFORCE_TTL=false artifacts.cibseven.org/docker-ee/cibseven:tomcat-2.1.0-ee
   ```
   (Deaktiviert TTL-Prüfung; siehe CONFIG_CHEAT_SHEET.md für Details).
3. **DB einrichten**: Erstelle DB `dpe_rennrad`, setze Credentials in `application.properties`.
4. **Projekt bauen**: `mvn clean install`.
5. **Starten**: `mvn quarkus:dev` (Port 8088).

## Deployment
- BPMN-Dateien in `processes/` legen.
- Quarkus startet automatisch Deployment zur Engine.
- Überprüfe in Cockpit: `http://localhost:8080/camunda/app/cockpit/`.

## APIs
- **REST**: `GET/POST/PUT/DELETE http://localhost:8088/rennrad` (CRUD für Rennrad).
- **SOAP**: `http://localhost:8088/soap/rennrad` (getAllRennraeder).
- **Prozess starten**: POST `http://localhost:8080/engine-rest/message` mit `{"messageName": "AppInstallMessage"}`.  
  Beispiel cURL:  
  ```
  curl -X POST http://localhost:8080/engine-rest/message \
    -H "Content-Type: application/json" \
    -d '{"messageName": "AppInstallMessage"}'
  ```

## Troubleshooting
- **TTL-Fehler**: Setze in Engine `--env CAMUNDA_BPM_HISTORY_ENFORCE_TTL=false`.
- **Parsing-Fehler**: Prüfe BPMN in Modeler (siehe Anleitung in CONFIG_CHEAT_SHEET.md).
- **DB-Verbindung**: Stelle sicher, MySQL läuft und Credentials stimmen.
- **Logs**: Quarkus-Console oder Engine-Logs.

## Beitrag
- BPMN in Modeler bearbeiten.
- Tests hinzufügen (z. B. für Services).
- Dokumentation erweitern.

## Lizenz
[Deine Lizenz]