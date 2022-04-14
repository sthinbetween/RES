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

# 4. Auf einer der drei VMs (zukünftiger Master) Addons aktivieren 
$ microk8s enable dns storage dashboard
```

### Auf Microk8s-Dashboard zugreifen 
- Für Zugriff auf das Dashboard wird eine weitere Maschine im selben Netz mit Browser benötigt (Firefox sinnvoll, Chrome macht Probleme mit den Zertifikaten)
- Auf der Master VM zunächst den Zugriffstoken auslesen: 
```bash
$ token=$(microk8s kubectl -n kube-system get secret | grep default-token | cut -d " " -f1)
microk8s kubectl -n kube-system describe secret $token
```
- dann das Dashboard im lokalen Netz verfügbar machen:  
```bash
$ microk8s kubectl port-forward -n kube-system service/kubernetes-dashboard 10443:443 --address 0.0.0.0
```
- ```https://<MasterIP>:10443``` im Browser aufrufen, Zertifikatswarnung ignorieren und Token für den Login eingeben

### Nodes hinzufügen 

- auf (zukünftiger) Master-Node: 
```bash 
$ microk8s add-node
```
- den so erhaltenen Befehl ```microk8s join <Master-Node Link>``` auf einer der anderen VMs ausführen 
- Link ist nur einmal gültig: daher für die andere VM wiederholen
- Ausgabe der nun vorhanden Maschinen im Cluster: 
```bash
$ microk8s kubectl get no
```

## Bereitstellung des MariaDB-Galera-Clusters

- auf Master-Node: 
```bash
$ sudo snap install helm --classic
$ sudo microk8s enable helm3
$ microk8s.kubectl config view --raw > $HOME/.kube/config
$ helm repo add bitnami https://charts.bitnami.com/bitnami
$ 
```



## RestAPI selbst bauen 

### Java bauen 

- mit "$sudo mvn install -DskipTests" bauen, da sonst der Datenbankstring Fehler wirft

### Docker

- docker-compose build
- docker-compose up

## Anwendung und Datenbank auf Microk8s bereitstellen

- 

