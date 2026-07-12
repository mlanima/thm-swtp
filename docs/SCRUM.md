# Scrum-Prozess – IdeaCamp

Software Engineering: Realisierung

Projekt: Ideacamp

Scrum — 4 Sprints, 2 Wochen pro Sprint

## Rollen und Aufgabenverteilung

| Rolle | Kurzbeschreibung der Verantwortlichkeit | Name |
|-------|----------------------------------------|------|
| Team Lead + Full Stack Assistance | Koordination, Priorisierung, Abstimmung im Team | Danylo Konovalenko |
| Backend Developer + Security Integration | Authentifizierung, Autorisierung, Sicherheitsaspekte | Halit Cinar |
| Backend Developer + Data Access | Datenmodell, Repositorys, Datenbankzugriffe | Mehmet Arslan |
| Backend Developer + DevOps | Build/Deployment, CI/CD, Docker | Torsten Schalz |
| Angular Frontend Developer | Seitenlogik, Komponenten, API-Anbindung, Guards etc. | Christian Heinz |
| UI/UX Frontend Developer | Designsystem, Mockups, Usability, Komponenten | Daniel Schmik |

Alle Entwickler durfen in allen Bereichen der Entwicklung mitarbeiten. Die Rollen beschreiben primar Verantwortungsfelder, aber jedes Teammitglied kann weiterhin als Full-Stack Developer arbeiten. Die Vorbereitung der Tickets, Definition of Done (DoD) und Definition of Ready (DoR) wurde von jedem in der Team ubernommen – die Rollenverteilung bildet lediglich Schwerpunkte und Verantwortungsbereiche ab.

## Kommunikation und Meetings

Pro Woche fanden mindestens zwei Meetings statt – fur Daily Scrum, Austausch oder kurzfristige Besprechung von Implementierungsdetails.

 Fur tagliche Stand-ups wurde Discord genutzt. Dort wurde fur jeden Tag ein eigener Chat-Thread erstellt, in dem alle Teammitglieder nach Moglichkeit berichteten, wie die Arbeit lauft, ob sie an dem Tag arbeiten, welche Aufgaben sie ubernehmen, und kurze relevante Themen zum Tagesgeschaft besprachen. Wichtigere Diskussionen wurden in separate themenspezifische Chats ausgelagert, sodass jederzeit darauf zuruckgegriffen werden konnte. Daneben gab es ein paar allgemeine Chats.

WhatsApp wurde ausschließlich fur projektfremde Kommunikation genutzt.

## Planung und Schatzung

Die Tickets wurden von jedem Teammitglied selbst geschrieben. Zur Schatzung des Aufwands fanden Scrum-Poker-Meetings statt. Am Ende jedes Sprints wurde eine Retrospective durchgefuhrt.

## Sprint 1

Im ersten Sprint wurden lediglich 10 Story Points erfolgreich abgeschlossen. Die Stories waren zu allgemein formuliert und hatten in kleinere Einheiten aufgeteilt werden sollen. Einige Stories waren teilweise nicht fertiggestellt und konnten daher nicht gewertet werden, obwohl im Sprint bereits eine fast vollstandige erste Version umgesetzt wurde.

Entwickelt wurden: Registrierung, Profile, Erstellung und Bearbeitung von Projekten. Vollstandig abgenommen wurden jedoch nur die Stories zur Suche (SERSOSE26G3-9, SERSOSE26G3-49). Die ubrigen Stories wurden besser aufgeteilt und in Sprint 2 verschoben.

Am Ende des Sprints fand eine Retrospective statt.

## Sprint 2

Der zweite Sprint verlief deutlich besser. Die Stories wurden so aufgeteilt, dass jede eine atomare Funktion abbildete – statt „Als Nutzer mochte ich einen vollstandigen Profilbereich" beispielsweise „Als Nutzer mochte ich den Beschreibungstext bearbeiten konnen". Im Sprint wurden 49 Story Points abgeschlossen; einige wenige wurden zuruck ins Backlog verschoben.

Umgesetzte Stories:

| Ticket | Story | SP |
|--------|-------|----|
| SERSOSE26G3-98 | Orga | Nicht geschatzt |
| SERSOSE26G3-157 | Projekt Einstellungen | Nicht geschatzt |
| SERSOSE26G3-233 | Als Grunder will ich angezeigt bekommen, wenn eine Projekt-URL schon vergeben ist | Nicht geschatzt |
| SERSOSE26G3-143 | CI/CD: Automatische Testumgebung nach Push | 5 |
| SERSOSE26G3-181 | Als Grunder mochte ich bei der Projekterstellung eine Kurzbeschreibung des Projektes unter dem Projekttitel erstellen konnen | 3 |
| SERSOSE26G3-112 | Als Grunder mochte ich anderen Nutzer zu meinem Projekt bei der Projekterstellung einladen konnen | 3 |
| SERSOSE26G3-119 | Als Grunder, mochte ich, dass die Nutzer fur meine Projekte Join Requests senden konnten | 5 |
| SERSOSE26G3-113 | Als Grunder, mochte ich Mitglieder auf der Projekt-Seite als Preview anschauen konnen | 3 |
| SERSOSE26G3-114 | Als Grunder, will ich Mitglieder meines Projektes entfernen konnen | 3 |
| SERSOSE26G3-111 | Als Grunder mochte ich meine Projekte loschen konnen | 2 |
| SERSOSE26G3-134 | Als Grunder mochte ich, dass ich mein Projekt nachtraglich auf Privat / nicht Privat einstellen | 1 |
| SERSOSE26G3-140 | Als Projektersteller mochte ich wissen was die Eingabefelder bei der Projekterstellung zu bedeuten haben | 1 |

Weitere umgesetzte Stories:

| Ticket | Story | SP |
|--------|-------|----|
| SERSOSE26G3-6 | Als Nutzer, will ich bei meinem Profil entsprechende Tags setzen konnen | 2 |
| SERSOSE26G3-115 | Als Nutzer mochte ich, dass andere Nutzer mein Profil ansehen konnen | 3 |
| SERSOSE26G3-136 | Als Nutzer will ich Projekten als Favoriten markieren konnen | 3 |
| SERSOSE26G3-137 | Als Nutzer will ich meine favorisierte Projekte in einer Ansicht anschauen konnen | 2 |
| SERSOSE26G3-142 | Als Nutzer, will ich die Einladungen zu den Projekten anschauen konnen | 3 |
| SERSOSE26G3-231 | Als Nutzer mochte ich vor der Anmeldung erstmal auf einer allgemeinen Seite empfangen werden | Nicht geschatzt |
| SERSOSE26G3-127 | Als Nutzer mochte ich das originale Logo der THM haben | 1 |
| SERSOSE26G3-8 | Als Nutzer mochte ich Projekte nach Tags filtern | 3 |
| SERSOSE26G3-128 | Als Nutzer mochte bei der Suche nach Nutzern auch auf deren Profile navigieren | 0 |
| SERSOSE26G3-135 | Als Grunder mochte ich, dass die Nutzer die Kennzahlen von meinem Projekt anschauen konnten | 2 |
| SERSOSE26G3-138 | Als Grunder mochte ich offentliche Links fur mein Projekt auf der Projektseite anzeigen lassen | 2 |
| SERSOSE26G3-190 | Bug Fixing | Nicht geschatzt |
| SERSOSE26G3-225 | Als Nutzer will ich die Seite auf deutsch haben | 2 |

Am Ende des Sprints fand eine Retrospective statt.

## Sprint 3

Im dritten Sprint wurden insgesamt 59 Story Points erreicht. Bei der Planung wurden Tasks feiner unterteilt, sodass sie parallel bearbeitet werden konnten. Das Vorgehen war: zunachst ein Ticket fur das Skelett eines Packages (Service, Entity, Repository usw.), anschließend separate Tickets fur jede einzelne Aktion (erstellen, loschen, bearbeiten usw.). Außerdem wurde CI/CD fur Review-Apps eingerichtet, was die Arbeit der gesamten Mannschaft deutlich beschleunigte.

Umgesetzte Stories:

| Ticket | Story | SP |
|--------|-------|----|
| SERSOSE26G3-203 | Als Prof, will ich Thesis-Angebote publizieren konnen | 2 |
| SERSOSE26G3-210 | Als Nutzer, will ich eine allgemeine Seite bzw. Dashboard mit unterschiedliche Features nutzen | 3 |
| SERSOSE26G3-216 | Als Prof, will ich konnen meine Thesis-Angebote loschen konnen | 2 |
| SERSOSE26G3-217 | Als Prof, will ich Theses-Projekt bearbeiten konnen | 2 |
| SERSOSE26G3-220 | Als Nutzer will ich meine Links auf meinem Profil teilen konnen | 2 |
| SERSOSE26G3-139 | Als Grunder will ich meinen Projektmitgliedern Interne Links darstellen | 3 |
| SERSOSE26G3-237 | Als Grunder will ich Dateien / Medien fur meine Mitglieder hochladen | Nicht geschatzt |
| SERSOSE26G3-194 | Keycloak Login / Registration stylen | 5 |
| SERSOSE26G3-195 | Als Nutzer will ich Eingaben mit Enter abschließen | 1 |
| SERSOSE26G3-196 | Als Nutzer will ich, dass mir eine Projekt-URL beim Erstellen vorgeschlagen werden | 1 |
| SERSOSE26G3-197 | Als Nutzer will ich Mehrsprachigkeit auf der Seite angeboten bekommen | 5 |
| SERSOSE26G3-198 | Als Nutzer mochte ich die App auf Mobilgeraten nutzen konnen | 1 |
| SERSOSE26G3-211 | Als Nutzer will ich Suchergebnisse uber mehrere Seiten sehen (falls viele Ergebnisse vorhanden sind) | 5 |
| SERSOSE26G3-13 | Als Nutzer, will ich uber neue Projekt Einladungen benachrichtigt werden | 5 |
| SERSOSE26G3-200 | Als Nutzer, will ich mich als Moderator anmelden konnen | 1 |
| SERSOSE26G3-201 | Als Moderator will ich anderen Projekte loschen konnen | 2 |
| SERSOSE26G3-202 | Als Moderator will ich anderen User Bannen konnen | 3 |
| SERSOSE26G3-218 | Als Nutzer, will ich anderen Nutzern folgen konnen | 2 |
| SERSOSE26G3-214 | Als Nutzer will ich Posts auf meiner Seite loschen | 3 |
| SERSOSE26G3-213 | Als Nutzer will ich Posts auf meiner Projekt-Seite publizieren konnen | 3 |
| SERSOSE26G3-287 | CI/CD: Review-Apps – isolierte Per-PR-Umgebungen & Auto-Deploy-Erweiterung | 5 |
| SERSOSE26G3-289 | CI/CD: KI-gestutztes PR-Review als Sticky-Comment + PR-Ubersichts-Dashboard | 3 |

Am Ende des Sprints fand eine Retrospective statt.

## Sprint 4

Im letzten Sprint lag der Fokus auf Funktionalitat und User Experience. Bestehende Features wurden verbessert, neue Funktionen hinzugefugt und Integrationen mit externen Diensten umgesetzt.

Umgesetzte Stories:

| Ticket | Story | SP |
|--------|-------|----|
| SERSOSE26G3-203 | Als Prof, will ich Thesis-Angebote publizieren konnen | 2 |
| SERSOSE26G3-216 | Als Prof, will ich konnen meine Thesis-Angebote loschen konnen | 2 |
| SERSOSE26G3-217 | Als Prof, will ich Theses-Projekt bearbeiten konnen | 2 |
| SERSOSE26G3-298 | Als Nutzer, will ich den Besitz von meinem Projekt anderem Nutzer ubergeben konnen | 5 |
| SERSOSE26G3-300 | Als Nutzer, will ich fur Post/Beschreibungen Markdown aufwandlos mit gutem UX nutzen konnen | 3 |
| SERSOSE26G3-313 | Als Nutzer, will ich Github zu meinem Projekt binden konnen, um auf der Webseite README verlinken oder anzeigen konnen | 5 |
| SERSOSE26G3-316 | Als Nutzer, will ich damit andere Nutzer auch Github Einladung bekommen, wenn sie zu einem Projekt eingeladet werden | 8 |
| SERSOSE26G3-295 | Als Nutzer mochte ich, dass Nutzereingaben ohne Beleidungen etc. moglich sind | 5 |
| SERSOSE26G3-314 | Als Projektinhaber mochte ich mein Projekt mit einem Discord-Server verbinden konnen | 3 |
| SERSOSE26G3-315 | Als Nutzer, will ich damit die Nutzer die ich zu einem Projekt anladen, auch Discord Einladung bekommen | 3 |
| SERSOSE26G3-317 | Als Nutzer, will ich damit die Posts aus der Webseite auch im Discord und umgekehrt erscheinen | 5 |
| SERSOSE26G3-210 | Als Nutzer, will ich eine allgemeine Seite bzw. Dashboard mit unterschiedliche Features nutzen | 3 |
| SERSOSE26G3-327 | Als Nutzer will ich Bilder Publizieren konnen | 3 |
| SERSOSE26G3-319 | Als Nutzer will ich die Nutzer/Projekten filtern konnen | 2 |
| SERSOSE26G3-320 | Als Nutzer, will ich die Moderator-Tabellen sortieren konnen | 2 |
| SERSOSE26G3-321 | Als Nutzer will alle Nutzer/Projekte/Posts reporten konnen | 5 |
| SERSOSE26G3-322 | Als Nutzer(Mod), will ich die Reports ansehen konnen | 3 |
| SERSOSE26G3-323 | Als Mod, will ich die Nutzer/Projekte wegen Verstose bannen konnen | 2 |
| SERSOSE26G3-324 | Als Nutzer will ich nach der Registrierung ein kurzes Tutorial erhalten | 2 |
| SERSOSE26G3-325 | Als Nutzer(Prof), will ich damit andere Nutzer sicher sein konnten, das ich verifiziert Prof bin | 2 |
| SERSOSE26G3-329 | Als Nutzer, will ich als Prof identifiziert werden konnte | 5 |
| SERSOSE26G3-360 | Als Nutzer mochte ich Posts als Entwurf speichern konnen und bereits erstellte Posts archivieren | 2 |
| SERSOSE26G3-370 | Als Moderator, will ich es wissen konnen wer was wann geloscht wurde | 5 |

Am Ende des Sprints fand eine Retrospective statt.
