# RES
Prüfungsrepo für Rechnernetzadministration / Verteilte Systeme

# 1. Microk8s-Cluster aufbauen 
Wenn bereits ein K8s-Cluster vorhanden ist, sind die hier beschriebenen Schritte nicht notwendig und es kann direkt mit Punkt 2 begonnnen werden.
**Voraussetzungen für das Cluster:

** Damit das unter 2 bereitgestellte Skript funktioniert, muss eine Default-Storage-Klasse im Cluster vorhanden sein, welche automatisch provisioniert werden kann. 
>  https://kubernetes.io/docs/tasks/administer-cluster/change-default-storage-class/)

Zudem sollte diese kein Local-/Hostpath-Storage sein, da die Provosionierung des Galera-Clusters damit nicht funktioniert hat. Eventuell ist die Nutzung von loaklem Storage dennoch möglich, für das beschriebene Setup wurde jedoch ein NFS-Share genutzt.

> Nutzung von lokalem Storage https://vocon-it.com/2018/12/20/kubernetes-local-persistent-volumes/

Da für die Einrichtung des LoadBalancers und des Galera-Clusters Helm-Charts verwendet werden, muss Helm installiert sein und per ```[microk8s] kubectl enable helm3``` auch in K8s aktiviert werden. 

Das Cluster wird hier im Beispiel auf Ubuntu-Server VMs auf einem ProxMox-Hypervisor aufgesetzt.

### 1.1 Ubuntu-Server installieren
Auf jeder der beteiligten VMs wurde Ubuntu-Server 20.04 LTS installiert.

### 1.2 Microk8s installieren 
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

# 4. Addons aktivieren (auf allen Nodes)
$ microk8s enable dns storage dashboard helm3
```

### 1.3 Auf Microk8s-Dashboard zugreifen 
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

### 1.4 NFS-Share für persistenten Storage bereitstellen
- dieser Schritt ist bei Setups mit einer Node oder bereits eingerichtetem nicht lokalem Storageprovider nicht notwendig
- das MariaDB-Galera Setup lässt sich jedcoh nicht einrichten, wenn lediglich lokaler Storage (hostpath) bereitgestellt wird
- daher wurde im Clusternetz noch ein NFS-Share bereitgestellt
> https://microk8s.io/docs/nfs wurde als Anleitung benutzt

### 1.5 Nodes hinzufügen

- auf (zukünftiger) Master-Node: 
```bash 
$ microk8s add-node
```
- den so erhaltenen Befehl ```microk8s join <Master-Node Link> --worker``` auf einer der anderen VMs ausführen 
- Link ist nur einmal gültig: daher für die andere VM wiederholen
- Ausgabe der nun vorhanden Maschinen im Cluster: 
```bash
$ microk8s kubectl get no
```
# 2. Bereitstellung des Anwendungsstacks im Cluster

## 2.1 Automatische Bereitstellung per Skript 
Für die Bereitstellung ist ein Skript vorhanden, welches genutzt werden kann, wenn die unter 1. genannten Voraussetzungen erfüllt sind. 
Folgende Schritte sind für die Nutzung zu befolgen: 

```bash
# Download des Shell-Skripts auf den Master-Node
# Folgender Link für Microk8s (Aufruf von Funktionen mit microk8s kubectl)
wget wget https://raw.githubusercontent.com/sthinbetween/RES/main/k8s-config/autoconfig-microk8s.sh
# Folgender Link für andere K8s-Versionen (Aufruf mit kubectl)
wget wget https://raw.githubusercontent.com/sthinbetween/RES/main/k8s-config/autoconfig-k8s.sh

# Skript ausführbar machen 
sudo chmod +x autoconfig-microk8s.sh
# oder
sudo chmod +x autoconfig-k8s.sh

# Skript ausführen 
sh autoconfig-microk8s.sh
# oder 
sh autoconfig-k8s.sh
```
Nach einer kurzen Wartezeit ist die API über die IPs der K8s-Nodes verfügbar (Port 30000 für HTTP, Port 30001 für HTTPS). Die Api-Nutzung ist unter 3. beschrieben

## 2.2 Manuelle Bereitstellung 
### 2.2.1 Bereitstellung des MariaDB-Galera-Clusters


- auf Master-Node: 
```bash
$ sudo snap install helm --classic
$ sudo microk8s enable helm3
$ microk8s.kubectl config view --raw > $HOME/.kube/config
$ helm repo add bitnami https://charts.bitnami.com/bitnami
$ 
```
- [Helm-Config](k8s-config/galera-helm.yaml) auf Zielsystem laden 

```bash
$ helm install maria-db -f <Pfad-zur-values.yaml> bitnami/mariadb-galera
```
- mit ```microk8s kubectl get pods``` Status der Pods checken und warten, bis alle drei DB-Pods Ready und Running erreicht haben
- unter den Namen ```maria-db-0```, ```maria-db-1``` und ```maria-db-2``` sind die Instanzen nun erreichbar und replizieren untereinander
- als Service (also für andere Pods erreichbar) steht ```maria-db``` zur Verfügung

### 2.2.2 LoadBalancer installieren und konfigurieren

> folgende Schritte wurden auf Basis der Dokumentation von HAProxy ausgeführt https://www.haproxy.com/documentation/kubernetes/latest/installation/community/kubernetes/

```bash
# Repository hinzufügen und Repo-Liste aktualisieren  
helm repo add haproxytech https://haproxytech.github.io/helm-charts
helm repo update

# Helm-Chart installieren, dafür wird ein separater Namespace erstellt und die nach außen freigegebenen NodePorts gemappt

helm install kubernetes-ingress haproxytech/kubernetes-ingress \
    --create-namespace \
    --namespace haproxy-controller \
    --set controller.service.nodePorts.http=30000 \
    --set controller.service.nodePorts.https=30001 \
    --set controller.service.nodePorts.stat=30002
```

### 2.2.3 API bereitstellen

- [Api-Deployment-YAML](k8s-config/todo-deployment.yaml) auf Zielsystem laden
- mit ```microk8s kubectl apply -f <Pfad zur YAML>``` das Deployment anwenden
- damit wird: 
  - das Deployment erstellt (3 Replicas der ToDo-Api)
  - ein Service erstellt um die ContainerPorts verfügbar zu machen 
  - auf Basis des HAProxy eine Ingress-Ressource erstellt, welche auf den NodeIPs einkommende Anfragen auf den vorab erstellten Ports zum Service weiterleitet    
- die API ist im Anschluss unter ```http://<NodeIP>:30000``` oder ```https://<NodeIP>:30001``` zu erreichen

## 3. API-Nutzung

für jede Abfrage ist ```secret = "asd45jASBD73-asdd3dfASd-!asF3"``` als Parameter mitzugeben 

- GET.../version -> zeigt 0.0.1 als Versionsnummer an
- GET.../list -> zeigt alle Einträge
- POST.../add -> fügt einen Eintrag hinzu
  - zusätzlicher Parameter: "entryTitle" für den Titel des ToDo-Eintrags
- DELETE.../delete -> löscht Einträge
  - zusätzlicher Parameter: "entryTitle" ODER "entryOid" für den zu löschenden Eintrag

# 4. RestAPI aus Sourcecode lokal bereitstellen 

Die vorab vorgestellte Lösung nutzt einen in der Docker-Registry bereitgestellten Container. 
Es ist auch möglich, diese lokal zu compilieren und bereitzustellen. 
Die notwendigen Schritte hierfür sind: 

## 4.1 Sourcecode laden 

- [Anwendungsordner](ResApi/) auf Zielsystem laden
- z.B. mit: 
```
svn checkout https://github.com/sthinbetween/RES/trunk/ResApi
```

## 4.2 Anwendung bauen (Ubuntu)

> genutzte Anleitung: https://www.baeldung.com/install-maven-on-windows-linux-mac
```
# in den Anwendungsordner wechseln 
cd ResApi

# Java-Komponenten installieren 
sudo apt install default-jre
sudo apt install default-jdk 
sudo apt install maven

# Anwendung kompilieren (mit skipTests, da die Verbindung zur Datenbank noch nicht funktioniert)
sudo mvn install -DskipTests
```

## 4.3 In lokaler Docker-Registry bereitstellen und in K8s registrieren 

> genutzte Anleitung: https://microk8s.io/docs/registry-images

```bash
# im /ResApi Ordner: 
docker build . -t todoapi:local
# prüfen, ob das Image in der Docker-Registry vorhanden ist: 
docker images
# Registrieren des Images im K8s-Image-Cache
docker save todoapi > todoapi.tar
microk8s ctr image import todoapi.tar
# Prüfen, ob das Image in K8s vorhanden ist: 
microk8s ctr images ls

```
## 4.4. Bereitstellung des Anwendungsstacks 

Die Bereitstellung kann mit den in 2. beschriebenen Schritten vollzogen werden. 
Jedoch muss für die Bereitstellung per Skript zusätzlich ```-l``` als Argument bei der Skriptausführung übergeben werden. Bei manueller Bereitstellung muss die todoapi-deployment.yaml durch die [Lokale-Deployment-YAML](k8s-config/local/todo-deployment.yaml) ersetzt werden.
