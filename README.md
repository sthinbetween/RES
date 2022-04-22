# RES
Prüfungsrepo für Rechnernetzadministration / Verteilte Systeme

Aufgabe ist der Aufbau eines Kubernetes-Clusters, welches folgende Struktur bereitstellen soll: 

- selbst entwickelte REST-API, welche Funktionen einer einfachen ToDo-Liste erfüllt: 
    - Einträge hinzufügen 
    - Einträge löschen 
    - Einträge anzeigen lassen 
    - Authentifizierung mit Secret
- Drei Instanzen der Anwendung sollen ausgeführt werden 
- persistente Speicherung der Einträge in MariaDB 
- synchrone Replizierung der Daten in einem MariaDB-Galera-Cluster mit drei MariaDB Instanzen 
- Loadbalancing durch HAProxy

# 1. Microk8s-Cluster aufbauen 
Wenn bereits ein K8s-Cluster vorhanden ist und die nachfolgenden Voraussetzungen erfüllt sind, sind die hier beschriebenen Schritte nicht notwendig und es kann direkt mit [Abschnitt 3](https://github.com/sthinbetween/RES#3-bereitstellung-des-anwendungsstacks-im-cluster) begonnnen werden.

**Voraussetzungen für das Cluster:**

Damit das unter 2 bereitgestellte Skript funktioniert, muss eine Default-Storage-Klasse im Cluster vorhanden sein, welche automatisch provisioniert werden kann. 
>  https://kubernetes.io/docs/tasks/administer-cluster/change-default-storage-class/)

Zudem sollte diese kein Local-/Hostpath-Storage sein, da die Provosionierung des Galera-Clusters damit nicht funktioniert hat. Eventuell ist die Nutzung von lokalem Storage dennoch möglich, für das beschriebene Setup wurde jedoch ein NFS-Share genutzt.

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
sudo apt install snapd 

# 2. microk8s installieren 
sudo snap install microk8s --classic

# 3. für Bequemlichkeit (sudo nicht nötig) aktuellen Nutzer zur microk8s-Gruppe hinzufügen
sudo usermod -aG microk8s $USER
su - $USER
newgrp microk8s

# 4. Addons aktivieren (auf der zukünftigen Masternode)
microk8s enable dns storage dashboard helm3
```

### 1.3 Auf Microk8s-Dashboard zugreifen 
- Für Zugriff auf das Dashboard wird eine weitere Maschine im selben Netz mit Browser benötigt (Firefox sinnvoll, Chrome macht Probleme mit den Zertifikaten)
- Auf der Master-VM zunächst den Zugriffstoken auslesen: 
```bash
token=$(microk8s kubectl -n kube-system get secret | grep default-token | cut -d " " -f1)
microk8s kubectl -n kube-system describe secret $token
```
- dann das Dashboard im lokalen Netz verfügbar machen:  
```bash
microk8s kubectl port-forward -n kube-system service/kubernetes-dashboard 10443:443 --address 0.0.0.0
```
- ```https://<Master-Node-IP>:10443``` im Browser aufrufen, Zertifikatswarnung ignorieren und Token für den Login eingeben

### 1.4 Nodes hinzufügen

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

### 1.5 NFS-Share für persistenten Storage bereitstellen
- dieser Schritt ist bei Setups mit einer Node oder bereits eingerichtetem nicht lokalem Storageprovider nicht notwendig
- das MariaDB-Galera Setup lässt sich jedoch nicht einrichten, wenn lediglich lokaler Storage (hostpath) bereitgestellt wird
- daher wurde im Clusternetz noch ein NFS-Share bereitgestellt
> https://microk8s.io/docs/nfs wurde als Anleitung benutzt

- auf dem NFS-Server:
```
sudo apt-get install nfs-kernel-server
sudo mkdir -p /srv/nfs
sudo chown nobody:nogroup /srv/nfs
sudo chmod 0777 /srv/nfs
sudo mv /etc/exports /etc/exports.bak
echo '/srv/nfs 10.0.0.0/24(rw,sync,no_subtree_check)' | sudo tee /etc/exports
sudo systemctl restart nfs-kernel-server
```
- auf der Master-Node: 
```
microk8s helm3 repo add csi-driver-nfs https://raw.githubusercontent.com/kubernetes-csi/csi-driver-nfs/master/charts
microk8s helm3 repo update
microk8s helm3 install csi-driver-nfs csi-driver-nfs/csi-driver-nfs \
    --namespace kube-system \
    --set kubeletDir=/var/snap/microk8s/common/var/lib/kubelet

# Download der YAML für die Definition des persistenten Volumes
wget https://raw.githubusercontent.com/sthinbetween/RES/main/k8s-config/sc-nfs.yaml
# Anwenden im Cluster
microk8s kubectl apply -f - < sc-nfs.yaml
# Als default definieren 
microk8s kubectl patch storageclass nfs-csi -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'
```
# 2. Alternative: K8s-Cluster verwenden 
Microk8s ist einfach zu konfigurieren und eignet sich für Testumgebungen sehr gut. Es scheiden sich allerdings die Geister daran, ob es für Produktivumgebungen geeignet ist. Daher wurde auch eine K8s-Installation getestet.

> genutzte Anleitung dafür: https://computingforgeeks.com/deploy-kubernetes-cluster-on-ubuntu-with-kubeadm/

1. Ausgangslage auch hier ein frisches Ubuntu-Server 20.04 LTS
2. Kubernetes-Repository hinzufügen (alle Nodes): 
```bash
sudo apt -y install curl apt-transport-https
curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -
echo "deb https://apt.kubernetes.io/ kubernetes-xenial main" | sudo tee /etc/apt/sources.list.d/kubernetes.list
```
3. Pakete installieren (alle Nodes)
```bash
sudo apt update
sudo apt -y install vim git curl wget kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
```
4. SWAP-Speicher deaktivieren (alle Nodes)
```bash
sudo sed -i '/ swap / s/^\(.*\)$/#\1/g' /etc/fstab
sudo swapoff -a
```
5. Kernel-Module aktivieren (alle Nodes)
```bash
# Kernel-Module aktivieren
sudo modprobe overlay
sudo modprobe br_netfilter

# sysctl konfigurieren
sudo tee /etc/sysctl.d/kubernetes.conf<<EOF
net.bridge.bridge-nf-call-ip6tables = 1
net.bridge.bridge-nf-call-iptables = 1
net.ipv4.ip_forward = 1
EOF

# systctl neu laden 
sudo sysctl --system
```
6. Docker-Runtime bereitstellen (alle Nodes)
```bash
# Repository hinzufügen und Pakete installieren
sudo apt update
sudo apt install -y curl gnupg2 software-properties-common apt-transport-https ca-certificates
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
sudo apt update
sudo apt install -y containerd.io docker-ce docker-ce-cli

# Verzeichnisse anlegen
sudo mkdir -p /etc/systemd/system/docker.service.d

# Daemon-Konfig anlegen
sudo tee /etc/docker/daemon.json <<EOF
{
  "exec-opts": ["native.cgroupdriver=systemd"],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m"
  },
  "storage-driver": "overlay2"
}
EOF

# Docker konfigurieren und starten (auch bei Boot) 
sudo systemctl daemon-reload 
sudo systemctl restart docker
sudo systemctl enable docker
```
7. K8s-Docker Interface installieren (alle Nodes)
> Anleitung: https://computingforgeeks.com/install-mirantis-cri-dockerd-as-docker-engine-shim-for-kubernetes/
```
# Tools installieren (falls noch nicht vorhanden)
sudo apt update
sudo apt install git wget curl

# Neueste dockerd-cri Version als Variable festlegen
VER=$(curl -s https://api.github.com/repos/Mirantis/cri-dockerd/releases/latest|grep tag_name | cut -d '"' -f 4)

# Binaries laden, entpacken und verschieben
wget https://github.com/Mirantis/cri-dockerd/releases/download/${VER}/cri-dockerd-${VER}-linux-amd64.tar.gz
tar xvf cri-dockerd-${VER}-linux-amd64.tar.gz
sudo mv cri-dockerd /usr/local/bin/

# systemd konfigurieren
wget https://raw.githubusercontent.com/Mirantis/cri-dockerd/master/packaging/systemd/cri-docker.service
wget https://raw.githubusercontent.com/Mirantis/cri-dockerd/master/packaging/systemd/cri-docker.socket
sudo mv cri-docker.socket cri-docker.service /etc/systemd/system/
sudo sed -i -e 's,/usr/bin/cri-dockerd,/usr/local/bin/cri-dockerd,' /etc/systemd/system/cri-docker.service

# Dienste neu starten
sudo systemctl daemon-reload
sudo systemctl enable cri-docker.service
sudo systemctl enable --now cri-docker.socket
```

8. Master-Node initialisieren (auf zukünftiger Master-Node)
```bash
# kubelet-Dienst aktivieren 
sudo systemctl enable kubelet

# Notwendige Container laden
sudo kubeadm config images pull

# Master-Node initialisieren
# Aufpassen, dass keine Adresskonflikte mit dem lokalen Netz entstehen (ggf. Subnetz anpassen)
sudo kubeadm init \
  --pod-network-cidr=192.168.0.0/16
```
9. Nach Ausführung des letzten Befehls, die Ausgabe nach `Then you can join any number of worker nodes by running the following on each as root:` kopieren und irgendwo zwischenspeichern

10. Konfiguration zur Nutzung von kubectl ohne sudo (Master-Node)
```bash
mkdir -p $HOME/.kube
sudo cp -f /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```
11. Netzworkplugin (Calico) installieren (auf Master-Node)
```bash
kubectl create -f https://docs.projectcalico.org/manifests/tigera-operator.yaml 
kubectl create -f https://docs.projectcalico.org/manifests/custom-resources.yaml
```
12. Warten, bis alle Pods READY sind (auf Master-Node)
```bash
watch kubectl get pods --all-namespaces
```
13. Ausführen des in 9. gespeicherten Befehls auf den Worker-Nodes (ggf. mit sudo davor)
14. Warten, bis die Nodes als READY erscheinen (auf Master-Node)
```bash
kubectl get nodes
```
15. ggf. Dashboard bereitstellen (https://computingforgeeks.com/how-to-install-kubernetes-dashboard-with-nodeport/)
16. 

# 3. Bereitstellung des Anwendungsstacks im Cluster

## 3.1 Automatische Bereitstellung per Skript 
Für die Bereitstellung ist ein Skript vorhanden, welches genutzt werden kann, wenn die unter [Abschnitt 1](https://github.com/sthinbetween/RES/blob/main/README.md#1-microk8s-cluster-aufbauen) genannten Voraussetzungen erfüllt sind. 

Folgende Schritte sind für die Nutzung zu befolgen: 

```bash
# Download des Shell-Skripts auf den Master-Node
# Folgender Link für Microk8s (Aufruf von Funktionen mit microk8s kubectl)
wget https://raw.githubusercontent.com/sthinbetween/RES/main/k8s-config/autoconfig-microk8s.sh
# Folgender Link für andere K8s-Versionen (Aufruf mit kubectl)
wget https://raw.githubusercontent.com/sthinbetween/RES/main/k8s-config/autoconfig-k8s.sh

# Skript ausführbar machen 
sudo chmod +x autoconfig-microk8s.sh
# oder
sudo chmod +x autoconfig-k8s.sh

# Skript ausführen 
./autoconfig-microk8s.sh -r 
# oder 
./autoconfig-k8s.sh -r 
```
Nach einer kurzen Wartezeit ist die API über die IPs der K8s-Nodes verfügbar (Port 30000 für HTTP, Port 30001 für HTTPS). Die Api-Nutzung ist unter [Abschnitt 4](https://github.com/sthinbetween/RES/blob/main/README.md#41-api-nutzung) beschrieben

## 3.2 Manuelle Bereitstellung 
### 3.2.1 Bereitstellung des MariaDB-Galera-Clusters

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

### 3.2.2 LoadBalancer installieren und konfigurieren

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

### 3.2.3 API bereitstellen

- [Api-Deployment-YAML](k8s-config/todo-deployment.yaml) auf Zielsystem laden
- mit ```microk8s kubectl apply -f <Pfad zur YAML>``` das Deployment anwenden
- damit wird: 
  - das Deployment erstellt (3 Replicas der ToDo-Api)
  - ein Service erstellt um die ContainerPorts verfügbar zu machen 
  - auf Basis des HAProxy eine Ingress-Ressource erstellt, welche auf den NodeIPs einkommende Anfragen auf den vorab erstellten Ports zum Service weiterleitet    
- die API ist im Anschluss unter ```http://<NodeIP>:30000``` oder ```https://<NodeIP>:30001``` zu erreichen, Nutzungshinweise finden sich in [Abschnitt 4](https://github.com/sthinbetween/RES/blob/main/README.md#41-api-nutzung).

## 4. API-Beschreibung
Die API ist eine Spring-Boot-Anwendung (JAVA) und stellt grundlegende Funktionen für eine ToDo-Liste bereit. 
Für Tests und den Build-Prozess wird Maven genutzt. 

### 4.1 API-Nutzung

für jede Abfrage ist ```secret = "asd45jASBD73-asdd3dfASd-!asF3"``` als Parameter mitzugeben 

- GET.../version -> zeigt 0.0.1 als Versionsnummer an
- GET.../list -> zeigt alle Einträge
- POST.../add -> fügt einen Eintrag hinzu
  - zusätzlicher Parameter: "entryTitle" für den Titel des ToDo-Eintrags
- DELETE.../delete -> löscht Einträge
  - zusätzlicher Parameter: "entryTitle" ODER "entryOid" für den zu löschenden Eintrag

# 5. RestAPI aus Sourcecode lokal bereitstellen 

Die vorab vorgestellte Lösung nutzt einen in der Docker-Registry bereitgestellten Container. 
Es ist auch möglich, diese lokal zu compilieren und bereitzustellen. 
Die notwendigen Schritte hierfür sind: 

## 5.1 Sourcecode laden 

- [Anwendungsordner](ResApi/) auf Zielsystem laden
- z.B. mit: 
```
svn checkout https://github.com/sthinbetween/RES/trunk/ResApi
```

## 5.2 Anwendung bauen (Ubuntu)

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

## 5.3 In lokaler Docker-Registry bereitstellen und in K8s registrieren 

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
## 5.4. Bereitstellung des Anwendungsstacks 

Die Bereitstellung kann mit den in [Abschnitt 3](https://github.com/sthinbetween/RES/blob/main/README.md#3-bereitstellung-des-anwendungsstacks-im-cluster) beschriebenen Schritten vollzogen werden. 
Jedoch muss für die Bereitstellung per Skript zusätzlich ```-l``` als Argument bei der Skriptausführung übergeben werden. 
Bei manueller Bereitstellung muss die todoapi-deployment.yaml durch die [Lokale-Deployment-YAML](k8s-config/local/todo-deployment.yaml) ersetzt werden.

# 6. Außerbetriebnahme des Setups
Anstatt alle bereitgestellten Ressourcen händisch zu löschen, kann das Bereitstellungskript mit ```-d``` genutzt werden um die vorgenommenen Konfigurationen zu löschen 
