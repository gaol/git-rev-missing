#!/bin/bash

cwd=$(cd `dirname $0` && pwd)

app=`ls $cwd/target/git-rev-missing-*-app.jar`
if [ "$app" == "" ]; then
  echo -e "build the app jar"
  mvn -f $cwd/pom.xml clean install
fi

app=`ls $cwd/target/git-rev-missing-*-app.jar`
if [ "$app" == "" ]; then
  echo -e "Not built, something wrong"
  return 1
fi

java -jar $app $@


