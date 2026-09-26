# Installation

## Requirements
- Minecraft 1.26
- Paper 26.2-compatible server
- Java 25
- Maven 3.9+ for source builds

## Build
Run: mvn clean package

The shaded JAR is generated under target/.
Copy it to plugins/ and restart the server.

## Termux
Check java -version and javac -version. The project uses Java 25. Java 21 cannot compile a Maven project configured with release 25.

## First launch
/video opens the Video Center.
/video help opens the help UI.
/video reload reloads configuration and requires admin permission.
