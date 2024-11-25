.ONESHELL:
DOCKER_COMPOSE_DEV_ES7_FILE = ./apps/server/docker-compose-es7.dev.yml
###################
## PNPM
###################
compile:
	pnpm i && pnpm run build:all
test:
	pnpm run test:all
start:
	cd apps/server && pwd && pnpm run start:dev

###################
## DOCKER
###################
# only starts the infrastructure containers needed by maestro
docker-start-dev:
	docker-compose -f ${DOCKER_COMPOSE_DEV_ES7_FILE} up -d

docker-stop-dev:
	docker-compose -f ${DOCKER_COMPOSE_DEV_ES7_FILE} down

###################
## REST API
###################
rest-health:
	curl -X GET http://localhost:11235/health

rest-index-study:
	curl -X POST \
	http://localhost:11235/index/repository/lyric1/organization/PACA-CA \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache' \
	-d '{}'

rest-index-analysis:
	curl -X POST \
	http://localhost:11235/index/repository/lyric1/organization/PACA-CA/id/ad7cabf8-df45-40f8-9fcb-67d8933f46e6 \
	-H 'Content-Type: application/json' \
	-H 'cache-control: no-cache'

rest-index-repo:
	curl -X POST \
	http://localhost:11235/index/repository/lyric1 \
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
