.ONESHELL:
DOCKER_COMPOSE_LOCAL_DIR = ./run/docker
VERSION=0.0.1-SNAPSHOT

boot-run:
	cd maestro-app
	../mvnw spring-boot:run

mvn-i:
	./mvnw clean install -Dmaven.test.skip=true

mvn-t:
	./mvnw clean test

#doc stands for docker
doc-login:
	docker login

doc-push: doc-login doc-build
	cd maestro-app
	../mvnw dockerfile:push

doc-push-directly:doc-build
	docker push overture/maestro:$(VERSION)

doc-build: mvn-i
	cd maestro-app
	../mvnw dockerfile:build

# use this when you want to run maestro as container
doc-start: mvn-i doc-clean
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose up -d

doc-stop:
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose down

doc-restart-app: mvn-i
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose up --build -d maestro

# only starts the infrastructure containers needed by maestro
# use this when you run maestro from the ide (not as a container)
doc-start-dev:
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose -f docker-compose.dev.yml up -d

doc-clean:
	docker system prune