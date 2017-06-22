#!/bin/bash

#Konfgurieren Sie den Rechner so, dass man keine TCP-Server, die auf diesem Rechner
#laufen, über das Netz 172.16.1.0/24 ansprechen kann. Alle anderen Verbindungen (auch TCP) 
#über dieses Netz sollen dagegen möglich sein.

#Reset der Firewall
sudo /sbin/rcSuSEfirewall2 restart

sudo /sbin/iptables -I INPUT -s 172.16.1.0/24 -p all -j ACCEPT
sudo /sbin/iptables -I OUTPUT -s 172.16.1.0/24 -p all -j ACCEPT
sudo /sbin/iptables -I INPUT -s 172.16.1.0/24 -p tcp --tcp-flags ALL SYN -j DROP
