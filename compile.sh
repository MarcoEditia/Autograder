# compiles and copies config.properties to classes folder 
javac -d classes -cp "src:lib/*" src/autograder/Main.java && cp src/autograder/config/config.properties classes/autograder/config/