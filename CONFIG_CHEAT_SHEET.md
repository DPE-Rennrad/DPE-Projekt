# Konfigurations-CheatSheet: DPE-Projekt

Dieses CheatSheet listet alle Konfigurationsänderungen in `application.properties` und der CIB seven Engine mit Erklärungen (warum und wofür). Es basiert auf den Schritten, die wir durchgeführt haben, um Quarkus mit CIB seven EE zu integrieren.

## application.properties (Quarkus-Client)
Diese Datei konfiguriert den Quarkus-Client für DB, BPMN und APIs.

- `quarkus.http.port=8088`  
  **Warum**: Vermeidet Port-Konflikt mit CIB seven (8080).  
  **Wofür**: Quarkus läuft auf 8088, Engine auf 8080.

- `quarkus.datasource.db-kind=mysql`  
  **Warum**: Aktiviert den MySQL-JDBC-Treiber in Quarkus.  
  **Wofür**: DB-Integration für Rennrad-Entitäten.

- `quarkus.datasource.jdbc.url=jdbc:mysql://localhost:3306/dpe_rennrad?allowPublicKeyRetrieval=true&useSSL=false`  
  **Warum**: Verbindet zu MySQL ohne SSL (Dev-freundlich).  
  **Wofür**: Speichert Rennrad-Daten in DB `dpe_rennrad`.

- `quarkus.datasource.username=root` & `quarkus.datasource.password=abc#1234`  
  **Warum**: Authentifizierung für MySQL.  
  **Wofür**: Zugriff auf DB; ändere für Prod-Sicherheit.

- `quarkus.hibernate-orm.database.generation=update`  
  **Warum**: Auto-Update des DB-Schemas bei Änderungen.  
  **Wofür**: Erstellt Tabellen für Rennrad-Entity ohne manuelles SQL.

- `camunda.bpm.client.base-url=http://localhost:8080/engine-rest`  
  **Warum**: Verbindet den Quarkus-Client zur Remote CIB seven Engine.  
  **Wofür**: Deployment und Interaktion mit BPMN-Prozessen.

- `quarkus.camunda.job-execution.enabled=false`  
  **Warum**: Deaktiviert lokale Job-Ausführung im Client.  
  **Wofür**: Remote Engine übernimmt Jobs (Timer, etc.).

- `quarkus.cxf.endpoint."/soap/rennrad".implementor=edu.thi.demo.soap.RennradSoapService`  
  **Warum**: Registriert den SOAP-Service.  
  **Wofür**: SOAP-API unter `/soap/rennrad` für Rennrad-Abfragen.

## CIB seven Engine (Docker)
Diese Änderungen gelten für die Remote Engine via Docker.

- `--env CAMUNDA_BPM_HISTORY_ENFORCE_TTL=false`  
  **Warum**: Deaktiviert die TTL-Prüfung bei Deployment.  
  **Wofür**: Behebt Parsing-Fehler bei fehlendem TTL in BPMN; nicht für Prod (setze TTL stattdessen).

- Optional: `--env CAMUNDA_BPM_HISTORY_TIME_TO_LIVE=30`  
  **Warum**: Setzt globale TTL (30 Tage) für alle Prozesse.  
  **Wofür**: Fallback, wenn BPMN kein eigenes TTL hat.

## Warum diese Konfiguration?
- **Ziel**: Nahtlose Integration von Quarkus-App (CRUD, SOAP) mit CIB seven (BPMN-Workflows).
- **Dev-Friendly**: Einfache DB-Setup, Hot-Reload, Remote-Deployment ohne lokale Engine.
- **Fehlervermeidung**: TTL-Deaktivierung löst Deployment-Issues; BPMN-Anpassungen (Messages, Conditions) verhindern Parsing-Fehler.
- **Sicherheit**: Lizenz und Credentials; verschlüssle Passwörter in Prod.
- **Erweiterung**: Füge mehr env für Engine hinzu (z. B. DB für Engine, wenn nötig).

## BPMN-Fehler-Anleitung (Kurzfassung)
Siehe README.md für Details. Häufige Fixes:
- TTL: In Process setzen oder Engine-env.
- Messages: Definieren und zuweisen.
- Conditions: `${true}` (Boolean!).
- Call Elements: Auf Process-ID setzen.
- Timer: Cron wie `0 0 9 * * ?`.

Für Änderungen: Bearbeite in Camunda Modeler und dann deployen