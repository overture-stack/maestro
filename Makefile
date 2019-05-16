.ONESHELL:
DOCKER_COMPOSE_LOCAL_DIR = ./run/docker
VERSION=$(shell cat ./.mvn/maven.config | grep revision | cut -d '=' -f2)-SNAPSHOT

run:
	cd maestro-app
	../mvnw spring-boot:run

package:
	./mvnw clean package -Dmaven.test.skip=true

test:
	./mvnw clean test

docker-push:
	docker push overture/maestro:$(VERSION)

docker-build:
	docker build -f ci-cd/Dockerfile . -t overture/maestro:$(VERSION)

# use this when you want to run maestro as container
docker-start: mvn-i doc-clean
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose up -d

docker-stop:
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose down

docker-restart-app: mvn-i
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose up --build -d maestro

# only starts the infrastructure containers needed by maestro
# use this when you run maestro from the ide (not as a container)
docker-start-dev:
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose -f docker-compose.dev.yml up -d

docker-clean:
	docker system prune