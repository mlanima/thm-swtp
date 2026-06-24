# Entscheidung: Rollenkonzept (ADMIN → MODERATOR)

## Problem

Die App brauchte eine Moderationsfunktion: jemand, der Benutzer und Projekte
verwalten kann, ohne Zugriff auf die Keycloak-Admin-Konsole zu haben.

## Erster Ansatz (DB-Rollen)

Die erste Implementierung speicherte Rollen (`ADMIN`, `USER`) in der
Anwendungsdatenbank. Ein `ADMIN` sollte ursprünglich über einen
`CommandLineRunner` beim Backend-Start angelegt werden, mit einer
hartcodierten Keycloak-ID. Diese Idee wurde jedoch **nie implementiert** –
sie war nur eine Diskussionsgrundlage.

Probleme dieses Ansatzes:

- Einen Moderator hinzuzufügen oder zu entfernen hätte eine Code-Änderung
  oder einen Datenbank-Eingriff erfordert.
- Der technische Admin (Keycloak-Admin) und der App-Moderator wären
  begrifflich vermischt worden.
- Moderatoren müssten als DB-Entity (`UserAccount`) existieren, was
  willkürlich ausschloss, dass sie ein Benutzerprofil haben können.

## Diskussion

Im Meeting haben wir zwei Optionen besprochen:

1. **DB-Rollen behalten**, Moderatoren per `CommandLineRunner` mit einer
   hartcodierten Keycloak-ID anlegen.
2. **Eine Keycloak-Realm-Rolle** (`MODERATOR`) verwenden und sie im Backend
   aus dem JWT auslesen.

Wir haben uns für Option 2 entschieden. Keycloak verwaltet bereits Benutzer
und Rollen – es wäre redundant gewesen, das in der Anwendungsdatenbank zu
duplizieren. Mit der Keycloak-Rolle kann der technische Admin die Rolle
`MODERATOR` direkt in Keycloak an jeden Benutzer vergeben, ohne dass Code
oder Datenbank angefasst werden müssen.

## Entscheidung: Keycloak-Realm-Rolle + DB-Professor-Flag

- **Moderator** ist eine Keycloak-Realm-Rolle. Das Backend liest sie aus dem
  JWT und gewährt Übersichtsrechte (Projekte und Benutzerprofile ansehen und
  löschen).
- **Professor** ist ein reines Datenbank-Boolean auf `UserProfile`.
  Moderatoren können es Benutzern zuweisen für zukünftige
  Professor-Funktionen. Es wurde bewusst nicht in Keycloak abgebildet, damit
  Moderatoren es vergeben können, ohne Keycloak-Admin-Rechte zu benötigen und
  um die Rechte für die Vergabe bei dem Keycloan nicht zu abfragen zu müssen
- Die alte `UserAccount`-Tabelle und die `ADMIN`-Rolle wurden entfernt.
  Jeder authentifizierte Benutzer bekommt automatisch `ROLE_USER`; nur wer
  die `MODERATOR`-Realm-Rolle im JWT hat, erhält zusätzlich
  `ROLE_MODERATOR`.
