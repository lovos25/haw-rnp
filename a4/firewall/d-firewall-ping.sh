#!/bin/bash

#Stellen Sie die Firewall Ihres Rechners so ein, dass von dort ein ping auf andere
#Rechner/Geräte im Netz 172.16.1.0/24 möglich ist, nicht aber umgekehrt!

#Drop alle eingehenden icmp echo requests = pings
#Pings nach außen senden keinen echo-request zurück
sudo /sbin/iptables -I INPUT -p icmp --icmp-type echo-request -j DROP
