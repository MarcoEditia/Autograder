@echo off
REM compiles and copies config.properties to classes folder 
javac -d classes -cp "src;lib/*" src\Main.java && copy src\config\config.properties classes\config