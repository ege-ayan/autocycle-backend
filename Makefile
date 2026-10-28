.PHONY: help up down logs test package run local-aws

help:
	@echo "up         Build and start the API and Postgres"
	@echo "down       Stop the compose stack"
	@echo "logs       Follow API logs"
	@echo "test       Run the test suite"
	@echo "package    Build the jar"
	@echo "run        Start the API on the host (Postgres must be up)"
	@echo "local-aws  Start the stack with LocalStack S3"

up:
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f api

test:
	./mvnw -B test

package:
	./mvnw -B -DskipTests package

run:
	./mvnw -B spring-boot:run

local-aws:
	docker compose --profile local-aws up --build
