#!/bin/bash
#Auf einem Ihrer beiden Rechner soll der Zugang vom und zum Netzwerk 172.16.1.0/24
#vollständig gesperrt werden.

sudo /sbin/iptables -I INPUT -s 172.16.1.0/24 -p all -j DROP
sudo /sbin/iptables -I OUTPUT -d 172.16.1.0/24 -p all -j DROP
