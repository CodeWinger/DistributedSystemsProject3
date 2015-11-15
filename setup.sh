#!/bin/bash

clear

#ports
portFlight=1410 #if you change these, update the middle rm values in the constructor
portCar=1411
portRoom=1412
portMiddle=1413

#service name
serverFlight=flight
serverCar=car
serverRoom=room
serverMiddle=middle

#hosts
host=$HOSTNAME
hostCar=lab9-28 #lab9-25 #lab1-3
hostRoom=lab9-28 #lab9-25 #lab1-4
hostFlight=lab9-28 #lab9-25 #lab1-5
hostMiddle=lab9-25 #lab9-25 #lab1-2

#gnome-terminal -e "ssh  mbouch95@lab1-2 \"cd Documents/cp512/p1; ant server\""

#clean directories
#ant setup

#setup RM servers
echo "cmd is : ant server -Dservice.host=$host -Dservice.name=$serverFlight -Dservice.port=$portFlight"
#gnome-terminal -e "ant server -Dservice.host=$host -Dservice.name=$serverFlight -Dservice.port=$portFlight"

echo "cmd is : ant server -Dservice.host=$host -Dservice.name=$serverCar -Dservice.port=$portCar"
#gnome-terminal -e "ant server -Dservice.host=$host -Dservice.name=$serverCar -Dservice.port=$portCar"
#sleep 15s
echo "cmd is : ant server -Dservice.host=$host -Dservice.name=$serverRoom -Dservice.port=$portRoom"
#gnome-terminal -e "ant server -Dservice.host=$host -Dservice.name=$serverRoom -Dservice.port=$portRoom"


echo "ant middleRM -Dservice.host=$host -Dservice.name=$serverMiddle -Dservice.port=$portMiddle -Dservice2.name=$serverFlight -Dservice2.host=$hostFlight -Dservice2.port=$portFlight -Dservice3.name=$serverCar -Dservice3.port=$portCar -Dservice3.host=$hostCar -Dservice4.name=$serverRoom -Dservice4.port=$portRoom -Dservice4.host=$hostRoom"
#sleep 20s

#setup middleware
#gnome-terminal -e "ant middleRM -Dservice.host=$host -Dservice.name=$serverMiddle -Dservice.port=$portMiddle -Dservice2.name=$serverFlight -Dservice2.host=$hostFlight -Dservice2.port=$portFlight -Dservice3.name=$serverCar -Dservice3.port=$portCar -Dservice3.host=$hostCar -Dservice4.name=$serverRoom -Dservice4.port=$portRoom -Dservice4.host=$hostRoom"

#sleep 20s
#ant client -DclientFileCmds=clientcmds.txt -Dservice.name=$serverMiddle -Dservice.port=$portMiddle
#setup clients
gnome-terminal -e "ant client -Dservice.host=$host -Dservice.name=$serverMiddle -Dservice.port=$portMiddle"
#gnome-terminal -e "ant client -DclientFileCmds=clientcmds.txt -Dservice.host=$host -Dservice.name=$serverMiddle -Dservice.port=$portMiddle"
#gnome-terminal -e "ant client -DclientFileCmds=clientcmds.txt -Dservice.host=$host -Dservice.name=$serverMiddle -Dservice.port=$portMiddle"
#gnome-terminal -e "ant client -DclientFileCmds=clientcmds.txt -Dservice.host=lab9-25 -Dservice.name=$serverMiddle -Dservice.port=$portMiddle"
#gnome-terminal -e "ant client -DclientFileCmds=clientcmds.txt -Dservice.host=lab9-25 -Dservice.name=$serverMiddle -Dservice.port=$portMiddle"
#gnome-terminal -e "ant client -DclientFileCmds=clientcmds.txt -Dservice.host=lab9-25 -Dservice.name=$serverMiddle -Dservice.port=$portMiddle"
#gnome-terminal -e "ant client -DclientFileCmds=clientcmds.txt -Dservice.host=lab9-25 -Dservice.name=$serverMiddle -Dservice.port=$portMiddle"
#gnome-terminal -e "ant client -DclientFileCmds=clientcmds.txt -Dservice.host=lab9-25 -Dservice.name=$serverMiddle -Dservice.port=$portMiddle"
#gnome-terminal -e "ant client -DclientFileCmds=clientcmds.txt -Dservice.host=lab9-25 -Dservice.name=$serverMiddle -Dservice.port=$portMiddle"

#let some time pass for server to setup
#sleep 20s

#setup the client
#gnome-terminal -e "ant client -Dservice.host=$host -Dservice.port=$portCar"


     	     
	           
