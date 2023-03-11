.PHONY: help
help:	## This help.
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

native-image-configs:
	java -agentlib:native-image-agent=config-output-dir=./configs -jar target/scala-2.13/zioquickstart-assembly-0.1.0-SNAPSHOT.jar

graalvm-native-image-local:
	docker build --progress=plain --platform linux/amd64 -t graalvm-native-image-local -f Dockerfile-native-image .

deployment-artifacts:
	docker build --progress=plain --platform linux/amd64 -t zioquickstart:latest -f Dockerfile-app .

run-local-it:
	docker run --rm -it -p 8080:8080 zioquickstart zioquickstart-local

run-fat-jar:
	native-image --verbose -jar target/scala-2.13/zioquickstart-assembly-0.1.0-SNAPSHOT.jar

