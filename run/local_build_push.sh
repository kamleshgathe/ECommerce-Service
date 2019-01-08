#! /bin/sh
app=$(basename $(pwd))
version=$(grep build.gradle -e @VERSION@ | awk '{print $2}' | tr -d "'")
lowercaseVersion=$(echo $version | tr "A-Z" "a-z")
buildNumber=$(git rev-parse --short HEAD)
branchName=$(git rev-parse --abbrev-ref HEAD)
buildTag="$branchName-$buildNumber"

if ./gradlew bootJar ; then
    if docker build --build-arg version=$version --build-arg app=$app \
        -t dctcontainerregistry.azurecr.io/$app:$buildTag . ; then
        docker push dctcontainerregistry.azurecr.io/$app:$buildTag
     else
        echo "docker build failed"
     fi
else
    echo "bootJar build failure"
fi