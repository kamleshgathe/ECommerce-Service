#! /bin/sh
app=$(basename $(pwd))
version=$(grep build.gradle -e @VERSION@ | awk '{print $2}' | tr -d "'")
lowercaseVersion=$(echo $version | tr "A-Z" "a-z")
buildNumber=$(git rev-parse --short HEAD)
branchName=$(git rev-parse --abbrev-ref HEAD)
buildTag="$branchName-$buildNumber"
if [ $CLOUD = "GCP" ]; then
	gcloud docker -- push gcr.io/dallassandbox/$app-$lowercaseVersion:$buildTag
elif [ $CLOUD = "AZURE" ]; then
	docker push dctcontainerregistry.azurecr.io/$app-$lowercaseVersion:$buildTag 
else
	echo "ERROR - unknown cloud '$CLOUD'" ; exit 1
fi
