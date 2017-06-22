#!/bin/bash

#Stellen Sie die Firewall des Rechners so ein, dass dort über das Netz 172.16.1.0/24 nur ein
#TCP-Server (z.B. aus Aufgabe 2/3) auf Port 51000 genutzt werden kann. Alle anderen
#Verbindungen über dieses Netz sollen gesperrt sein.

#Aufgabe a blockiert schon alles andere
sudo /sbin/iptables -I INPUT -s 172.16.1.0/24 -p tcp --dport 51000 -j ACCEPT
sudo /sbin/iptables -I OUTPUT -d 172.16.1.0/24 -p tcp --sport 51000 -j ACCEPT