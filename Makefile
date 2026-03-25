COMPOSE := /usr/local/bin/docker compose

DEV_AUTH_REPLICAS := 1
DEV_USER_REPLICAS := 1
DEV_AI_REPLICAS := 1
DEV_RECIPE_REPLICAS := 1
DEV_RECIPES_DB_REPLICAS := 1

STAGE_AUTH_REPLICAS := 2
STAGE_USER_REPLICAS := 2
STAGE_AI_REPLICAS := 2
STAGE_RECIPE_REPLICAS := 2
STAGE_RECIPES_DB_REPLICAS := 2

SERVICE ?=
REPLICAS ?=
TAIL ?= 200

.PHONY: up up-dev up-stage down stop restart ps logs logs-service urls traces metrics eureka scale

up:
	$(COMPOSE) up -d

up-dev:
	$(COMPOSE) up -d \
		--scale auth-service=$(DEV_AUTH_REPLICAS) \
		--scale user-service=$(DEV_USER_REPLICAS) \
		--scale ai-image-service=$(DEV_AI_REPLICAS) \
		--scale recipe-service=$(DEV_RECIPE_REPLICAS) \
		--scale recipes-from-db-service=$(DEV_RECIPES_DB_REPLICAS)

up-stage:
	$(COMPOSE) up -d \
		--scale auth-service=$(STAGE_AUTH_REPLICAS) \
		--scale user-service=$(STAGE_USER_REPLICAS) \
		--scale ai-image-service=$(STAGE_AI_REPLICAS) \
		--scale recipe-service=$(STAGE_RECIPE_REPLICAS) \
		--scale recipes-from-db-service=$(STAGE_RECIPES_DB_REPLICAS)

down:
	$(COMPOSE) down

stop:
	$(COMPOSE) stop

restart:
	$(COMPOSE) down
	$(COMPOSE) up -d

ps:
	$(COMPOSE) ps

logs:
	$(COMPOSE) logs -f --tail=$(TAIL)

logs-service:
	@if [ -z "$(SERVICE)" ]; then \
		echo "Usage: make logs-service SERVICE=auth-service [TAIL=100]"; \
		exit 1; \
	fi
	$(COMPOSE) logs -f --tail=$(TAIL) $(SERVICE)

urls:
	@echo "Gateway:    http://localhost:8090"
	@echo "Eureka:     http://localhost:8761"
	@echo "Zipkin:     http://localhost:9411"
	@echo "Prometheus: http://localhost:9090"
	@echo "Grafana:    http://localhost:3000"
	@echo "PgAdmin:    http://localhost:5050"

traces:
	@echo "Zipkin: http://localhost:9411"

metrics:
	@echo "Prometheus: http://localhost:9090"
	@echo "Grafana: http://localhost:3000"

eureka:
	@echo "Eureka: http://localhost:8761"

scale:
	@if [ -z "$(SERVICE)" ] || [ -z "$(REPLICAS)" ]; then \
		echo "Usage: make scale SERVICE=auth-service REPLICAS=3"; \
		exit 1; \
	fi
	$(COMPOSE) up -d --scale $(SERVICE)=$(REPLICAS) $(SERVICE)
