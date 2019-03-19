.ONESHELL:
DOCKER_COMPOSE_LOCAL_DIR = ./run

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

doc-push: doc-login doc-img
	cd maestro-app
	../mvnw dockerfile:push

doc-build:
	cd maestro-app
	../mvnw clean install dockerfile:build -Dmaven.test.skip=true

doc-start-img:
	cd $(DOCKER_COMPOSE_LOCAL_DIR);
	docker-compose -f docker-compose.yml up -d

doc-start: doc-clean
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