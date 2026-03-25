# Scaling Guide

## When automatic replica creation is needed
For this project, automatic replica creation is not needed in local development.

`docker compose` is good for:
- bringing the stack up quickly
- checking that Eureka registration works
- verifying that Gateway load balances requests between replicas
- smoke-testing observability and tracing

True autoscaling makes sense only when you move to an orchestrator such as Kubernetes or Docker Swarm, where scaling can react to CPU, memory, queue depth, or custom metrics.

## Recommended approach for this repo
Use fixed replica counts:
- `dev`: 1 replica for every backend service
- `stage`: 2 replicas for the main backend services

This gives you predictable behavior and makes debugging much easier.

## Ready commands
From `/Users/faysonin/Documents/foodAnalyzing`:

```bash
make up-dev
make up-stage
make scale SERVICE=auth-service REPLICAS=3
make ps
make logs-service SERVICE=auth-service
make urls
```

## What is scaled
These services are ready for horizontal scaling:
- `auth-service`
- `user-service`
- `ai-image-service`
- `recipe-service`
- `recipes-from-db-service`

These services should stay single-instance in your current setup:
- `api-gateway`
- `eureka`
- `postgres`
- `rabbitmq`
- `redis`
- `zipkin`
- `prometheus`
- `grafana`
- `pgadmin`

## Custom stage sizing
You can override the defaults without changing the `Makefile`:

```bash
make up-stage STAGE_AUTH_REPLICAS=3 STAGE_USER_REPLICAS=3 STAGE_AI_REPLICAS=2 STAGE_RECIPE_REPLICAS=2 STAGE_RECIPES_DB_REPLICAS=2
```

## How load balancing works here
1. Multiple replicas of a backend service start.
2. Each replica registers in Eureka with its own unique instance id.
3. Gateway routes requests using `lb://SERVICE_NAME`.
4. Spring Cloud LoadBalancer distributes requests across healthy replicas.

## Good enough signal that scaling works
Check these after startup:
- Eureka shows multiple `UP` instances for the same service.
- Requests through Gateway succeed.
- Zipkin shows traces going through Gateway into backend services.
