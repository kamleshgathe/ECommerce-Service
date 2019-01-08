#! /bin/sh
artifactName=$(basename $(pwd))
version=$(grep build.gradle -e @VERSION@ | awk '{print $2}' | tr -d "'")
java -Djava.util.logging.config.file=none \
	-jar build/libs/$artifactName-$version.jar $@
