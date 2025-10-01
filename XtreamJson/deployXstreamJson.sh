#!/bin/bash
cd /home/jonathan/git/JxTreamPlayer/XtreamJson
mvn clean package
cp ./target/xtream-1.0-SNAPSHOT.jar /home/jonathan/XstreamJson

 

# cd /home/jonathan/videoDownloader
# sudo java -jar -Xms1024M -Xmx4096M SpringVideoDownloader-0.0.1-SNAPSHOT.jar
