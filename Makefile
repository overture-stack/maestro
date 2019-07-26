.ONESHELL:
DOCKER_COMPOSE_LOCAL_DIR = ./run/docker-compose
VERSION=$(shell cat ./.mvn/maven.config | grep revision | cut -d '=' -f2)-SNAPSHOT
DOCS_SRC_DIR=sphinx-docs
PUBLISHED_DOCS_DIR=docs
###################
## MAVEN
###################
build:
	./mvnw clean compile
test:
	./mvnw clean test
package:
	./mvnw clean package -Dmaven.test.skip=true
run:
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
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose up -d

docker-stop:
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose down

# only starts the infrastructure containers needed by maestro
# use this when you run maestro from the ide (not as a container)
docker-start-dev:
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose -f docker-compose.dev.yml up -d

docker-clean:
	docker system prune

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
## the curl requests here run agains the kafka rest proxy
## SONG analysis topic
kafka-analysis-publish:
	curl -X POST \
	http://localhost:8082/topics/song-analysis \
	-H 'Accept: application/vnd.kafka.v2+json' \
	-H 'Content-Type: application/vnd.kafka.json.v2+json' \
	-H 'cache-control: no-cache' \
	-d '{
		"records": [
			{"value" : { "analysisId" : "EGAZ00001254368", "studyId" : "PEME-CA", "songServerId": "collab", "state": "PUBLISHED" }	}
		]
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
	-d '{
	"records": [
			{"value" : { "repositoryCode" : "collab", "studyId" : "PACA-CA" }	}
		]
	}'

kafka-index-analysis:
	curl -X POST \
	http://localhost:8082/topics/maestro_index_requests \
	-H 'Accept: application/vnd.kafka.v2+json' \
	-H 'Content-Type: application/vnd.kafka.json.v2+json' \
	-H 'cache-control: no-cache' \
	-d '{
	"records": [
			{"value" : { "repositoryCode" : "collab", "studyId" : "PEME-CA", "analysisId" : "EGAZ00001254247", "remove": false }	}
		]
	}'

kafka-index-repo:
	curl -X POST \
	http://localhost:8082/topics/maestro_index_requests \
	-H 'Accept: application/vnd.kafka.v2+json' \
	-H 'Content-Type: application/vnd.kafka.json.v2+json' \
	-H 'cache-control: no-cache' \
	-d '{
	"records": [
			{"value" : { "repositoryCode" : "aws" }	}
		]
	}'

########################################
## documentation
########################################
build-docs:
	cd $(DOCS_SRC_DIR)
	make singlehtml
	rm -r ../$(PUBLISHED_DOCS_DIR)/*
	cp -r ./build/singlehtml/* ../$(PUBLISHED_DOCS_DIR)