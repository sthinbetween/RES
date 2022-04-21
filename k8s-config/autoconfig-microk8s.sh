#!/bin/bash
# Notwendige Repos hinzufügen
GREEN='\033[1;32m'
NC='\033[0m'
echo -e "${GREEN}Füge Helm-Repositories hinzu...${NC}"
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add haproxytech https://haproxytech.github.io/helm-charts
helm repo update
echo -e "${GREEN}Repositories hinzugefügt...${NC}"
echo -e "${GREEN}Installiere Galera-Helm Chart...${NC}"
helm install maria-db -f ./galera-helm.yaml bitnami/mariadb-galera
echo -e "${GREEN}Galera-Helm Chart installiert...${NC}"
echo -e "${GREEN}Installiere HA-Proxy...${NC}"
helm install kubernetes-ingress haproxytech/kubernetes-ingress --create-namespace --namespace haproxy-controller --set controller.service.nodePorts.http=30000 --set controller.service.nodePorts.https=30001 --set controller.service.nodePorts.stat=30002
echo -e "${GREEN}HA-Proxy installiert...${NC}"
echo -e "${GREEN}Erstelle API-Service und konfiguriere LoadBalancer...${NC}"
microk8s kubectl apply -f ./todoapi-deployment.yaml
echo -e "${GREEN}Einrichtung abgeschlossen...${NC}"
