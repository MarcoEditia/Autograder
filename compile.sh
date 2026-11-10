# compiles and copies config.properties to classes folder 
javac -d classes -cp "src:lib/*" src/Main.java && cp src/config/config.properties classes/config/