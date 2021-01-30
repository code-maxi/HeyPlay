#!/bin/bash

# Parameter gefordert
if [ -z "$1" ]; then
  echo "Bitte Version angeben!"
  echo "muster: ./build.sh [version] [b/-] [w/-] [s/-] [s/-]"
  exit
fi

cd /home/maximilian/Programmieren/IntelliJ/HeyPlay/

echo "---- Version aktualisieren"
sed -i "10s/.*/      $1/" pom.xml
sed -i "1s/.*/var version = \"$1\"/" /home/maximilian/Programmieren/WebStorm/heyplay.de/js/own/script.ts

if [ "$2" == "b" ]; then
  sed -i "3s/.*/var version = \"$1\"/" /home/maximilian/Programmieren/IntelliJ/HeyPlay/src/main/java/main/version.kt

  echo "---- Maven build"

  cd /home/maximilian/Programmieren/IntelliJ/HeyPlay/
  mvn clean package -Dfile=/home/maximilian/Programmieren/IntelliJ/HeyPlay/addons/hoverball.jar
  zip -r target/HeyPlay-$1-src.zip src/

  cd /home/maximilian/Programmieren/IntelliJ/HeyPlay/target/

  echo "---- Nach out kopieren"
  rm -rf out
  mkdir out
  mv HeyPlay-$1.jar out/
  mv HeyPlay-$1-src.zip out/
fi

if [ "$2" == "b" ] || [ "$3" == "w" ] ; then
  cd /home/maximilian/Programmieren/WebStorm/heyplay.de
  rm -rf src
  mkdir src
  cp /home/maximilian/Programmieren/IntelliJ/HeyPlay/target/out/* src/
fi


if [ "$3" == "w" ]; then
  echo "---- Zur website kopieren"
  echo
  cd /home/maximilian/Programmieren/WebStorm/heyplay.de
  build/build.sh js css
  echo

  cp main.html ./index.html
  sed -i "s/<\!-- refresh -->/<meta http-equiv=\"expires\" content=\"0\">/g" index.html

  echo "---- Website Upload"
  ~/apps/websync.sh heyplay.de
fi

if [ "$4" == "s" ]; then
  echo "---- Server Upload"
  cd /home/maximilian/Programmieren/WebStorm/heyplay.de/src/
  sshpass -p 'wetter56' ssh maximilian@server -T 'cd HeyPlay/; rm -rf Programm; mkdir Programm'
  smbclient -U maximilian%wetter56 //server/maximilian -c 'cd HeyPlay/Programm; prompt; mput HeyPlay-*.jar'
fi

if [ "$5" == "s" ]; then
  echo "---- Server Start"
  sshpass -p 'wetter56' ssh maximilian@server -T './scs.sh'
fi

if [ "$6" == "t" ]; then
  cd /home/maximilian/Programmieren/WebStorm/heyplay.de/
  echo $(jq '.descriptions += [{"date": "'"$(date +'%d.%b %Y')"'", "version": "'"$1"'", "content": "'"$(cat /home/maximilian/Programmieren/IntelliJ/HeyPlay/description.xml)"'"}]' descriptions.json) > descriptions.json
  if [ $(jq '.descriptions| length' descriptions.json) >= 5 ]; then

  fi
fi
