#!/bin/bash
sudo cp ~/tsplugin.zip .
sudo unzip -o tsplugin.zip
sudo chmod +x *.sh
sudo chmod +x *.jar

sudo chmod +x lib/*.jar
sudo rm tsplugin.zip

