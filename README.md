# RES
Prüfungsrepo für Rechnernetzadministration / Verteilte Systeme

## Microk8s-Cluster aufbauen 
Das Cluster wird hier im Beispiel auf Ubuntu-Server VMs auf einem ProxMox-Hypervisor aufgesetzt.

### Ubuntu-Server installieren
Auf jeder der beteiligten VMs wurde Ubuntu-Server 20.04 LTS installiert.

### Microk8s installieren 
Für die Installation von microk8s wurde folgendes Tutorial genutzt: 
https://adamtheautomator.com/microk8s/

Schritte:

```bash
# 1. snap installieren 
$ sudo apt install snapd 

# 2. microk8s installieren 
$ sudo snap install microk8s --classic

# 3. für Bequemlichkeit (sudo nicht nötig) aktuellen Nutzer zur microk8s-Gruppe hinzufügen
$ sudo usermod -aG microk8s $USER
$ su - $USER
$ newgrp microk8s

```





## Java bauen 

- mit "$sudo mvn install -DskipTests" bauen, da sonst der Datenbankstring Fehler wirft

## Docker

- docker-compose build
- docker-compose up

## K8S

- microk8s auf allen beteiligten Servern installieren
- auf Master mit "microk8s add-node" den Join-Link ausgeben lassen 
- auf anderen Nodes mit "microk8s join <JOIN_LINK>" beitreten 

