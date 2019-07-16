#!/bin/bash
mkdir bin
javac src/Server.java -sourcepath src -d bin
javac src/Client.java -sourcepath src -d bin
