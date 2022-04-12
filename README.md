# RES
Prüfungsrepo für Rechnernetzadministration / Verteilte Systeme

## Java bauen 

- mit "$sudo mvn install -DskipTests" bauen, da sonst der Datenbankstring Fehler wirft

## Docker

- docker-compose build
- docker-compose up

## K8S

- microk8s auf allen beteiligten Servern installieren
- auf Master mit "microk8s add-node" den Join-Link ausgeben lassen 
- auf anderen Nodes mit "microk8s join <JOIN_LINK>" beitreten 

