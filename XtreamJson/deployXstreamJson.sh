#!/bin/bash
cd /home/jonathan/git/JxTreamPlayer/XtreamJson
mvn clean package
echo "Stopping xtreamJson.service"
sudo systemctl stop xtreamJson.service
rm /home/jonathan/XstreamJson/XstreamJson.log.*.gz
cp ./target/xtream-1.0-SNAPSHOT.jar /home/jonathan/XstreamJson
echo "Starting xtreamJson.service"
sudo systemctl start xtreamJson.service
 