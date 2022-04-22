#!/bin/bash
# Farben definieren
GREEN='\033[1;32m'
NC='\033[0m'
RED='\033[0;31m'
# Flags überprüfen
while getopts ':lrd' OPTION; do
  case "$OPTION" in
    l)
      FILE=./todoapi-deployment.yaml
      if test -f "$FILE"; then
        echo -e "${RED}Konfigurationsdatei bereits vorhanden, lösche um neue Version zu laden.${NC}"
        rm $FILE
      fi
      wget "https://raw.githubusercontent.com/sthinbetween/RES/main/k8s-config/local/todoapi-deployment.yaml"
      echo -e "${GREEN}API-Konfig für lokales Image geladen${NC}"
      ;;
    d)
      echo -e "${RED}Bestehende Konfiguration wirklich löschen?${NC}"
      echo -n "j/n":
      read -r choice
      if [[ "${choice}" == "n" ]]
      then
        echo "Konfiguration wird beibehalten"
        exit 0
      fi
      if [[ "${choice}" == "j" ]]
      then
        echo -e "${GREEN}Konfiguration wird gelöscht...${NC}"
        echo -e "${GREEN}Entferne API-Deployment...${NC}"
        microk8s kubectl delete deployment todoapi
        microk8s kubectl delete service todo-service
        microk8s kubectl delete ingress todo-ingress
        echo -e "${GREEN}API-Deployment entfernt...${NC}"
        echo -e "${GREEN}Entferne Galera-Chart...${NC}"
        helm uninstall maria-db
        echo -e "${GREEN}Galera-Chart entfernt...${NC}"
        echo -e "${GREEN}Entferne HAProxy-Chart...${NC}"
        helm uninstall kubernetes-ingress --namespace haproxy-controller
        echo -e "${GREEN}HA-Proxy entfernt...${NC}"
        echo -e "${GREEN}Entferne Konfigurationsdateien...${NC}"
        rm ./galera-helm.yaml
        rm ./todoapi-deployment.yaml
        echo -e "${GREEN}Löschen der Konfiguration abgeschlossen${NC}"
        exit 0
      else
        echo "Eingabe nicht erkannt, bitte erneut versuchen."
        exit 1
      fi
      ;;
    r)
      FILE=./todoapi-deployment.yaml
      if test -f "$FILE"; then
        echo -e "${RED}Konfigurationsdatei bereits vorhanden, lösche um neue Version zu laden.${NC}"
        rm $FILE
      fi
      wget "https://raw.githubusercontent.com/sthinbetween/RES/main/k8s-config/todoapi-deployment.yaml"
      echo -e "${GREEN}API-Konfig für Image aus Docker-Hub geladen${NC}"
      ;;
   \?)
      echo -e  "${RED}Eingabe nicht erkannt:${NC}"
      echo  "-l für Nutzung der lokalen Image-Registry"
      echo  "-r für Nutzung des DockerHub-Images"
      echo  "-d um vorheriges Deployment zu entfernen"
      exit 1
      ;;
  esac
done
if [ $OPTIND -eq 1 ]
then
  echo -e  "${RED}Eingabe nicht erkannt:${NC}"
  echo  "-l für Nutzung der lokalen Image-Registry"
  echo  "-r für Nutzung des DockerHub-Images"
  echo  "-d um vorheriges Deployment zu entfernen"
  exit 1
fi
# Helm-Repos hinzufügen
echo -e "${GREEN}Füge Helm-Repositories hinzu...${NC}"
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add haproxytech https://haproxytech.github.io/helm-charts
helm repo update
echo -e "${GREEN}Repositories hinzugefügt...${NC}"
# Konfigurationsdateien herunterladen
FILE=./galera-helm.yaml
if test -f "$FILE"; then
   echo -e "${RED}Konfigurationsdatei bereits vorhanden, lösche um neue Version zu laden.${NC}"
   rm $FILE
   wget https://raw.githubusercontent.com/sthinbetween/RES/main/k8s-config/galera-helm.yaml
   echo -e "${GREEN}Galera-Konfig geladen${NC}"
else
   wget https://raw.githubusercontent.com/sthinbetween/RES/main/k8s-config/galera-helm.yaml
   echo -e "${GREEN}Galera-Konfig geladen${NC}"
fi
echo -e "${GREEN}Konfigurationen geladen...${NC}"
echo -e "${GREEN}Installiere Galera-Helm Chart...${NC}"
helm install maria-db -f ./galera-helm.yaml bitnami/mariadb-galera
echo -e "${GREEN}Galera-Helm Chart installiert...${NC}"
echo -e "${GREEN}Installiere HA-Proxy...${NC}"
helm install kubernetes-ingress haproxytech/kubernetes-ingress --create-namespace --namespace haproxy-controller --set controller.service.nodePorts.http=30000 --set controller.service.nodePorts.https=30001 --set controller.service.nodePorts.stat=30002
echo -e "${GREEN}HA-Proxy installiert...${NC}"
echo -e "${GREEN}Erstelle API-Service und konfiguriere LoadBalancer...${NC}"
microk8s kubectl apply -f ./todoapi-deployment.yaml
echo -e "${GREEN}Einrichtung abgeschlossen...${NC}"
