.ONESHELL:
DOCKER_COMPOSE_DEV_FILE = ./apps/server/docker-compose.dev.yml
DOCKER_COMPOSE_FILE = ./apps/server/docker-compose.yml
VERSION=$(shell cat ./.mvn/maven.config | grep revision | cut -d '=' -f2)-SNAPSHOT
###################
## MAVEN
###################
compile:
	./mvnw clean compile
build:
	./mvnw clean install -DskipTests
test:
	./mvnw clean test
package:
	./mvnw clean package -Dmaven.test.skip=true
start:
	cd maestro-app
	../mvnw spring-boot:run

###################
## DOCKER
###################
docker-push:
	docker push overture/maestro:$(VERSION)

docker-build:
	docker build -f ci-cd/Dockerfile . -t overture/maestro:$(VERSION)

# use this when you want to run maestro as container
docker-start:
	docker-compose -f ${DOCKER_COMPOSE_FILE} up -d

docker-stop:
	docker-compose -f ${DOCKER_COMPOSE_FILE} down

# only starts the infrastructure containers needed by maestro
# use this when you run maestro from the ide (not as a container)
docker-start-dev:
	docker-compose -f ${DOCKER_COMPOSE_DEV_FILE} up -d

docker-stop-dev:
	docker-compose -f ${DOCKER_COMPOSE_DEV_FILE} down

###################
## REST API
###################
rest-health:
	curl -X GET http://localhost:11235/

rest-index-study:
	curl -X POST \
	http://localhost:11235/index/repository/collab/study/PACA-CA \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache' \
	-d '{}'

rest-index-analysis:
	curl -X POST \
	http://localhost:11235/index/repository/collab/study/PACA-CA/analysis/ad7cabf8-df45-40f8-9fcb-67d8933f46e6 \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache'

rest-index-repo:
	curl -X POST \
	http://localhost:11235/index/repository/collab \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache'

###################
## KAFKA
###################
## the curl requests here run against the kafka rest proxy
## SONG analysis topic
kafka-analysis-publish:
	curl -X POST \
	http://localhost:8082/topics/song-analysis \
	-H 'Accept: application/vnd.kafka.v2+json' \
	-H 'Content-Type: application/vnd.kafka.json.v2+json' \
	-H 'cache-control: no-cache' \
	-d '{ \
		"records": [ \
			{"value" : { "analysisId" : "EGAZ00001254368", "studyId" : "PEME-CA", "songServerId": "collab", "state": "PUBLISHED" }	} \
		] \
	}'

kafka-song-queue:
	docker exec -t kafka.maestro.dev bash -c "/usr/bin/kafka-console-consumer --bootstrap-server localhost:9092 \
	--topic song-analysis --from-beginning"

kafka-song-dlq:
	docker exec -t kafka.maestro.dev bash -c "/usr/bin/kafka-console-consumer --bootstrap-server localhost:9092 \
	--topic maestro_song_analysis_dlq --from-beginning"

## Maestro requests topic
kafka-maestro-topic:
	curl -X GET \
	http://localhost:8082/topics/maestro_index_requests \
	-H 'cache-control: no-cache'

kafka-index-study:
	curl -X POST \
	http://localhost:8082/topics/maestro_index_requests \
	-H 'Accept: application/vnd.kafka.v2+json' \
	-H 'Content-Type: application/vnd.kafka.json.v2+json' \
	-H 'cache-control: no-cache' \
	-d '{ \
	"records": [ \
			{"value" : { "repositoryCode" : "collab", "studyId" : "PACA-CA" }	} \
		] \
	}'

kafka-index-analysis:
	curl -X POST \
	http://localhost:8082/topics/maestro_index_requests \
	-H 'Accept: application/vnd.kafka.v2+json' \
	-H 'Content-Type: application/vnd.kafka.json.v2+json' \
	-H 'cache-control: no-cache' \
	-d '{ \
	"records": [ \
			{"value" : { "repositoryCode" : "collab", "studyId" : "PEME-CA", "analysisId" : "EGAZ00001254247", "removeAnalysis": false }	} \
		] \
	}'

kafka-index-repo:
	curl -X POST \
	http://localhost:8082/topics/maestro_index_requests \
	-H 'Accept: application/vnd.kafka.v2+json' \
	-H 'Content-Type: application/vnd.kafka.json.v2+json' \
	-H 'cache-control: no-cache' \
	-d '{ \
	"records": [ \
			{"value" : { "repositoryCode" : "aws" }	} \
		] \
	}'
