# Projektvorgaben - Checkliste zur Umsetzung

## A. Technische Mindestanforderungen

### Prozessgesteuerte Architektur
- [X] Anwendung nach Referenzarchitektur für Anwendungssysteme implementiert (z.B. Java Architektur mit model, repository, service, usw)
- [X] Kompletter vertikaler Durchstich implementiert (Frontend/Postman, Deployte Services, Java, DB)
- [X] BPMN-Modelle erstellt

### BPMN & Messaging - Asynchrone Kommunikation
- [X] **Sende-Aufgabe**: Versand asynchroner Nachricht aus Prozess in JMS-Queue (Eventermittlungsprozess - Externen Anbieter abfragen)
- [X] **Nachrichtenbasiertes Zwischenereignis**: Prozess wartet auf Nachricht (Motivationsprozess)

### Apache Camel - Integration & Routing
- [X] Nachricht aus JMS-Queue lesen (Eventermittlungsprozess - Antwort empfangen)
- [X] **Content-Based Routing** zu mehreren Systemen (Dateisystem ODER DB-Tabelle) (Eventermittlungsprozess umgesetzt mit JMS Dateisystem)
- [X] **Bidirektionale Transformation** zwischen XML und/oder JSON (Eventermittlungsprozess als Parser mit Queue)

### Backend-System Simulation
- [X] Daten aus Dateisystem ODER DB lesen und in JMS-Queue ablegen (Eventermittlungsprozess umgesetzt mit JMS Dateisystem)
- [ ] Apache Camel nimmt Nachricht entgegen und leitet an Prozess weiter

### Technologie-Stack
- [X] **Java Development Kit (JDK) 17** verwendet
- [X] **Quarkus** als Microservices-Framework eingesetzt
- [X] **CIB seven** BPMN-basierte Process Engine verwendet
- [X] **Apache ActiveMQ (Classic)** für Messaging
- [X] **Apache Camel** für Integration
- [ ] **Esper ESP Engine** verwendet
- [X] **MySQL** als Datenbank (oder Äquivalent)
- [X] **Eclipse IDE** für Jakarta EE Entwickler (oder Äquivalent)

### Persistenz
- [X] Datenpersistenz implementiert (User Anlegen, Events, Stammdaten)

### Services
- [X] SOAP und/oder REST Services implementiert (Produktempfehlung)

---

## B. Fachliche Anforderungen

### Benutzeroberfläche
- [X] Funktionale (nicht ausgefeilte) UI erstellt (Landingpage, KI Chat (Mock))