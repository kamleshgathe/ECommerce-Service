#! /bin/sh

if [ $# != 1 ]; then
	echo "ERROR - usage is,"
	echo "  $ run/cycle.sh <commit message>"
	exit 1
fi

set -x
git commit -am "$1"
gradle clean bootRepackage
run/build.sh
run/push.sh
