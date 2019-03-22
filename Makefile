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

# start using images only, don't build from code.
doc-start-img:
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose -f docker-compose.yml up -d

doc-start: mvn-i doc-clean
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose up --build -d

doc-stop:
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose down

doc-restart-app: mvn-i
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose up --build -d maestro

doc-clean:
	docker system prune