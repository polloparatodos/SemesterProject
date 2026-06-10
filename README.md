# AgentHub

AgentHub is a Dockerized microservice system for managing AI agent catalogs, customer accounts, and AI agent deployment records.

The project demonstrates:

- Canonical data modeling
- Microservice bounded contexts
- REST CRUD APIs
- Relational database persistence
- Repository and service layers
- Dockerized deployment
- Centralized configuration with `dev` and `prod` profiles
- Postman-based API testing

## Project Overview

AgentHub does not execute real AI agents. Instead, it provides a backend management system for registering AI agents, managing customers, and tracking deployment records.

This keeps the project AI-themed while keeping the checkpoint scope focused on microservices, persistence, configuration, Docker, and REST APIs.

## Architecture

The system contains one configuration service and three domain microservices.

```text
AgentHub
|
|-- config-service
|   |-- Central configuration server
|
|-- agent-catalog-service
|   |-- Manages AI agent metadata
|
|-- deployment-service
|   |-- Manages deployment records
|
|-- customer-service
|   |-- Manages customer organizations
|
|-- config-repo
|   |-- Dev and prod configuration files
|
|-- postman
|   |-- Postman collection for testing
|
|-- docker-compose.yml
|-- pom.xml
|-- README.md
```

Each domain microservice follows a layered architecture:

```text
Controller
Service
Repository
Entity
Relational Database
```

## Microservices

| Service               | Port | Description                            |
|-----------------------|-----:|----------------------------------------|
| Config Service        | 8888 | Centralized Spring Cloud Config Server |
| Agent Catalog Service | 8081 | CRUD API for AI agent records          |
| Deployment Service    | 8082 | CRUD API for deployment records        |
| Customer Service      | 8083 | CRUD API for customer records          |

## Databases

Each domain microservice owns its own PostgreSQL database.

| Database Container | Host Port | Internal Port | Used By               |
|--------------------|----------:|--------------:|-----------------------|
| agent-db           |      5433 |          5432 | Agent Catalog Service |
| deployment-db      |      5434 |          5432 | Deployment Service    |
| customer-db        |      5435 |          5432 | Customer Service      |

## Bounded Contexts

### Agent Catalog Context

Responsible for managing AI agent metadata.

Entity: `Agent`

Fields:

```text
id
name
description
version
agentType
provider
status
createdAt
updatedAt
```

Example responsibilities:

- Register a new AI agent
- View all available agents
- Update agent metadata
- Delete an agent record

### Deployment Context

Responsible for tracking deployment requests and environments.

Entity: `Deployment`

Fields:

```text
id
agentId
customerId
deploymentName
environmentName
region
status
requestedBy
requestedAt
updatedAt
```

Example responsibilities:

- Create deployment records
- Track deployment status
- Associate deployments with an agent ID and customer ID
- Delete deployment records

### Customer Context

Responsible for managing customer organizations.

Entity: `Customer`

Fields:

```text
id
organizationName
contactName
email
subscriptionPlan
status
createdAt
updatedAt
```

Example responsibilities:

- Register a customer organization
- Update customer contact information
- Track subscription plan
- Delete customer records

## Technology Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Cloud Config
- PostgreSQL
- Maven
- Docker
- Docker Compose
- Postman

## Prerequisites

This project is intended to be run with Docker Compose.

Required for Docker-based deployment:

- Docker Desktop
- Docker Compose

Java and Maven are not required on the host machine for the Docker-based workflow because each service is built inside a Maven Docker image and then run inside a Java runtime Docker image.

Optional for local development without Docker:

- Java 21
- Maven

## Project Structure

```text
SemesterProject
|
|-- agent-catalog-service
|   |-- src
|   |-- Dockerfile
|   |-- pom.xml
|
|-- config-repo
|   |-- agent-catalog-service-dev.yml
|   |-- agent-catalog-service-prod.yml
|   |-- customer-service-dev.yml
|   |-- customer-service-prod.yml
|   |-- deployment-service-dev.yml
|   |-- deployment-service-prod.yml
|
|-- config-service
|   |-- src
|   |-- Dockerfile
|   |-- pom.xml
|
|-- customer-service
|   |-- src
|   |-- Dockerfile
|   |-- pom.xml
|
|-- deployment-service
|   |-- src
|   |-- Dockerfile
|   |-- pom.xml
|
|-- postman
|   |-- AgentHub.postman_collection.json
|
|-- docker-compose.yml
|-- pom.xml
|-- README.md
```

## Configuration Profiles

The system supports two profiles:

```text
dev
prod
```

Configuration files are stored in:

```text
config-repo/
```

The Docker Compose setup runs the domain services using the `dev` profile.

Example files:

```text
agent-catalog-service-dev.yml
agent-catalog-service-prod.yml
deployment-service-dev.yml
deployment-service-prod.yml
customer-service-dev.yml
customer-service-prod.yml
```

The `dev` profile uses:

```text
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

The `prod` profile uses:

```text
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
```

## Running the System

From the project root directory, run:

```bash
docker compose up --build
```

This command builds and starts:

- Config Service (config-service)
- Agent Catalog Service (agent-catalog-service)
- Deployment Service (deployment-service)
- Customer Service (customer-service)
- Three PostgreSQL database containers
  - agent-db
  - deployment-db
  - customer-db

## Stopping the System

To stop the containers:

```bash
docker compose down
```

To stop the containers and remove database volumes:

```bash
docker compose down -v
```

## Checking Container Status

Run:

```bash
docker compose ps
```

Expected running services:

```text
config-service
agent-catalog-service
deployment-service
customer-service
agent-db
deployment-db
customer-db
```

Example output (with `dev` profile):
```text
> docker compose ps
NAME                    IMAGE                                   COMMAND                  SERVICE                 CREATED          STATUS          PORTS
agent-catalog-service   semesterproject-agent-catalog-service   "java -jar app.jar"      agent-catalog-service   15 minutes ago   Up 25 seconds   0.0.0.0:8081->8081/tcp, [::]:8081->8081/tcp
agent-db                postgres:16                             "docker-entrypoint.s…"   agent-db                15 minutes ago   Up 15 minutes   0.0.0.0:5433->5432/tcp, [::]:5433->5432/tcp
config-service          semesterproject-config-service          "java -jar app.jar"      config-service          15 minutes ago   Up 36 seconds   0.0.0.0:8888->8888/tcp, [::]:8888->8888/tcp
customer-db             postgres:16                             "docker-entrypoint.s…"   customer-db             15 minutes ago   Up 15 minutes   0.0.0.0:5435->5432/tcp, [::]:5435->5432/tcp
customer-service        semesterproject-customer-service        "java -jar app.jar"      customer-service        15 minutes ago   Up 25 seconds   0.0.0.0:8083->8083/tcp, [::]:8083->8083/tcp
deployment-db           postgres:16                             "docker-entrypoint.s…"   deployment-db           15 minutes ago   Up 15 minutes   0.0.0.0:5434->5432/tcp, [::]:5434->5432/tcp
deployment-service      semesterproject-deployment-service      "java -jar app.jar"      deployment-service      15 minutes ago   Up 25 seconds   0.0.0.0:8082->8082/tcp, [::]:8082->8082/tcp
```

## Service Health Checks

After startup, test the services:

```text
GET http://localhost:8888/actuator/health
GET http://localhost:8081/actuator/health
GET http://localhost:8082/actuator/health
GET http://localhost:8083/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

## Config Server Test URLs

The configuration server can be tested with:

```text
GET http://localhost:8888/agent-catalog-service/dev
GET http://localhost:8888/deployment-service/dev
GET http://localhost:8888/customer-service/dev
```

These endpoints should return configuration data for each service.

## API Endpoints

### Agent Catalog Service

Base URL:

```text
http://localhost:8081
```

Endpoints:

```text
GET    /api/agents
GET    /api/agents/{id}
POST   /api/agents
PUT    /api/agents/{id}
DELETE /api/agents/{id}
```

### Deployment Service

Base URL:

```text
http://localhost:8082
```

Endpoints:

```text
GET    /api/deployments
GET    /api/deployments/{id}
POST   /api/deployments
PUT    /api/deployments/{id}
DELETE /api/deployments/{id}
```

### Customer Service

Base URL:

```text
http://localhost:8083
```

Endpoints:

```text
GET    /api/customers
GET    /api/customers/{id}
POST   /api/customers
PUT    /api/customers/{id}
DELETE /api/customers/{id}
```

## Example API Requests

### Create Agent

Request:

```text
POST http://localhost:8081/api/agents
```

Body:

```json
{
  "name": "Research Assistant Agent",
  "description": "Summarizes research papers and extracts key findings.",
  "version": "1.0.0",
  "agentType": "RESEARCH",
  "provider": "OpenAI",
  "status": "ACTIVE"
}
```

### Create Customer

Request:

```text
POST http://localhost:8083/api/customers
```

Body:

```json
{
  "organizationName": "Example University Lab",
  "contactName": "Test McTestface",
  "email": "test.mctestface@example.com",
  "subscriptionPlan": "STANDARD",
  "status": "ACTIVE"
}
```

### Create Deployment

Request:

```text
POST http://localhost:8082/api/deployments
```

Body:

```json
{
  "agentId": 1,
  "customerId": 1,
  "deploymentName": "research-agent-dev",
  "environmentName": "development",
  "region": "us-east-1",
  "status": "PENDING",
  "requestedBy": "test.mctestface@example.com"
}
```

## Postman Collection

A Postman collection is included at:

```text
postman/AgentHub.postman_collection.json
```

To use it:

1. Open Postman.
2. Click `Import`.
3. Select `postman/AgentHub.postman_collection.json`.
4. Run the _Create_ requests first:
    - Create Agent
    - Create Customer
    - Create Deployment
5. Then run the _Get_, _Update_, and _Delete_ requests in that order.

The collection includes variables for:

```text
agentCatalogBaseUrl
deploymentBaseUrl
customerBaseUrl
agentId
deploymentId
customerId
```

## Suggested Testing Order

### 1. Start Docker Compose.

```bash
docker compose up --build
```

### 2. Confirm containers are running.

```bash
docker compose ps
```

### 3. Test health endpoints.

```text
GET http://localhost:8888/actuator/health
GET http://localhost:8081/actuator/health
GET http://localhost:8082/actuator/health
GET http://localhost:8083/actuator/health
```

### 4. Create a customer.

```text
POST http://localhost:8083/api/customers
```

### 5. Create an agent.

```text
POST http://localhost:8081/api/agents
```

### 6. Create a deployment referencing the created customer and agent IDs.

```text
POST http://localhost:8082/api/deployments
```

### 7. Test all GET, PUT, and DELETE endpoints in that order.

## Troubleshooting

### Postman returns connection refused

This means the service container is not running.

Check containers:

```bash
docker compose ps
```

Check logs:

```bash
docker compose logs agent-catalog-service
docker compose logs deployment-service
docker compose logs customer-service
docker compose logs config-service
```

### DataSource URL is missing

If a service fails with:

```text
Failed to configure a DataSource: 'url' attribute is not specified
```

Check that the config server is serving the correct configuration:

```text
GET http://localhost:8888/agent-catalog-service/dev
```

Also check that the corresponding file exists in `config-repo`.

### Rebuild from scratch

If containers or volumes get into a bad state:

```bash
docker compose down -v
docker compose up --build
```