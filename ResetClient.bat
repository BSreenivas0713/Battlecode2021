ECHO Killing Gradle Daemons!
call gradlew --stop
ECHO Removing Gradle Directory!
rmdir /s /q .gradle
ECHO Removing Client Directory!
rmdir /s /q client
ECHO Unpacking Client!
call gradlew unpackClient
ECHO Done!