# AgentHub

AgentHub is a Dockerized microservice platform for managing AI agent catalogs, customer accounts, AI agent deployment records, and agent chat sessions.

The project demonstrates:

- Canonical data modeling across bounded contexts
- Microservice architecture with service discovery and an API gateway
- REST CRUD APIs
- Relational (PostgreSQL) and document (MongoDB) persistence
- JWT-based authentication and authorization (RS256)
- Event-driven messaging with Kafka
- Distributed tracing with Zipkin
- Centralized configuration with `dev` and `prod` profiles
- Dockerized deployment (and an optional Terraform-based AWS deployment)
- Postman-based API testing

## Project Overview

AgentHub does not execute real AI agents autonomously. It provides a backend management system for registering AI agents, managing customers, tracking deployment records, authenticating users, and chatting with a registered agent (backed by the Google Gemini API) with the conversation persisted for later retrieval.

## Architecture

The system contains one configuration service, one discovery service, one API gateway, and five domain/support microservices.

```text
AgentHub
|
|-- config-service
|   |-- Central configuration server (Spring Cloud Config)
|
|-- discovery-service
|   |-- Service registry (Netflix Eureka)
|
|-- gateway-service
|   |-- API gateway / single entry point (Spring Cloud Gateway)
|
|-- auth-service
|   |-- Registration, login, and JWT issuance
|
|-- agent-catalog-service
|   |-- Manages AI agent metadata and chat sessions (Google Gemini)
|
|-- deployment-service
|   |-- Manages deployment records
|
|-- customer-service
|   |-- Manages customer organizations
|
|-- chat-service
|   |-- Persists chat messages and session events (MongoDB)
|
|-- config-repo
|   |-- Dev and prod configuration files for every service
|
|-- postman
|   |-- Postman collections for API testing
|
|-- terraform
|   |-- Optional single-instance AWS deployment
|
|-- k6
|   |-- Load test script
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
Database (PostgreSQL or MongoDB)
```

`gateway-service` and `discovery-service` are infrastructure services and do not follow this layering; `config-service` only serves configuration files from `config-repo`.

## Microservices

| Service                | Port | Description                                     |
|-------------------------|-----:|--------------------------------------------------|
| Config Service          | 8888 | Centralized Spring Cloud Config Server            |
| Discovery Service       | 8761 | Eureka service registry                           |
| Gateway Service         | 8080 | Spring Cloud Gateway — single entry point         |
| Auth Service            | 8086 | Registration, login, password reset, JWT issuance |
| Agent Catalog Service   | 8081 | CRUD API for AI agent records + chat sessions     |
| Deployment Service      | 8082 | CRUD API for deployment records                   |
| Customer Service        | 8083 | CRUD API for customer records                     |
| Chat Service            | 8084 | Persists and retrieves chat messages (MongoDB)    |

All domain services (auth, agent-catalog, deployment, customer, chat) register with Eureka and pull their configuration from `config-service` at startup.

## Databases

Each domain microservice owns its own database.

| Container      | Type       | Host Port | Internal Port | Used By                |
|-----------------|-----------|----------:|---------------:|--------------------------|
| agent-db        | PostgreSQL | 5433      | 5432           | Agent Catalog Service    |
| deployment-db   | PostgreSQL | 5434      | 5432           | Deployment Service       |
| customer-db     | PostgreSQL | 5435      | 5432           | Customer Service         |
| auth-db         | PostgreSQL | 5436      | 5432           | Auth Service             |
| chat-mongodb    | MongoDB    | 27017     | 27017          | Chat Service             |

## Supporting Infrastructure

| Container   | Image                             | Purpose                                                        |
|-------------|------------------------------------|------------------------------------------------------------------|
| zookeeper   | `confluentinc/cp-zookeeper:7.5.0`  | Coordination service required by Kafka                          |
| kafka       | `confluentinc/cp-kafka:7.5.0`      | Event bus for chat messages and session events                   |
| zipkin      | `openzipkin/zipkin:latest`         | Distributed tracing UI (`http://localhost:9411`)                 |

`agent-catalog-service` publishes chat messages and session events to Kafka (`chat.messages`, `chat.session.events`); `chat-service` consumes those topics and persists them to MongoDB. `gateway-service`, `agent-catalog-service`, and `chat-service` report traces to Zipkin.

## Bounded Contexts

### Agent Catalog Context

Responsible for managing AI agent metadata and driving chat sessions against the Google Gemini API.

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
sessionId
createdAt
updatedAt
```

Example responsibilities:

- Register a new AI agent
- View all available agents
- Update agent metadata
- Start/resume a chat session with an agent (`POST /api/agents/load-session`) and send it messages (`POST /api/agents/send-message`)
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

### Auth Context

Responsible for user credentials and issuing JWTs that the other services trust.

Entity: `AuthUser` (references a `Customer` via `customerId`)

Example responsibilities:

- Register a user tied to a customer organization
- Authenticate a user and issue a signed (RS256) JWT
- Serve the RSA public key so other services can verify tokens independently
- Handle forgot/reset/change password flows

### Chat Context

Responsible for durable storage of chat messages and session lifecycle events, backed by MongoDB instead of PostgreSQL.

Entity: `ChatMessage`

Fields:

```text
id
sessionId
sender
content
messageType
chatDescription
agentId
metadata
createdAt
updatedAt
```

Example responsibilities:

- Consume chat messages and session events published to Kafka by Agent Catalog Service
- Serve chat history for a session so a session can be resumed with full context

## Canonical Model Relationships

The canonical model is split across bounded contexts. Cross-service references are stored as plain IDs (not JPA relationships) because each microservice owns its own database.

```text
Customer 1..* Deployment
Agent    1..* Deployment
Customer 1..1 AuthUser     (AuthUser.customerId)
Agent    1..* ChatMessage  (ChatMessage.agentId, keyed by sessionId)
```

The `Deployment` entity stores `agentId` and `customerId`. The `AuthUser` entity stores `customerId`. The `ChatMessage` document stores `agentId` and `sessionId`.

## Authentication

`auth-service` issues RS256-signed JWTs. Every other domain service (agent-catalog, deployment, customer, chat) fetches the RSA public key from `auth-service` (`GET /api/auth/public-key`) at startup and validates incoming tokens itself — there is no shared secret and the gateway does not enforce auth on its own.

| Service                | Auth requirement                                                        |
|--------------------------|---------------------------------------------------------------------------|
| Auth Service            | `register`, `login`, `forgot-password`, `reset-password`, `public-key` are public; `me` and `change-password` require a valid JWT |
| Customer Service        | All `/api/customers/**` endpoints are currently public (no JWT required) |
| Agent Catalog Service   | All endpoints require a valid JWT                                        |
| Deployment Service      | All endpoints require a valid JWT                                        |
| Chat Service            | All endpoints require a valid JWT                                        |

To call a protected endpoint, register and log in first, then send the returned token as `Authorization: Bearer <token>`.

## Technology Stack

- Java 21
- Spring Boot 3.3.x
- Spring Web / Spring WebFlux (Gateway)
- Spring Data JPA (PostgreSQL) and Spring Data MongoDB
- Spring Cloud Config
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka (service discovery)
- Spring Security (JWT / RS256)
- Spring Kafka
- Micrometer Tracing + Zipkin
- Google Gen AI Java SDK (Gemini)
- PostgreSQL, MongoDB
- Apache Kafka, Zookeeper
- Maven
- Docker, Docker Compose
- Terraform (optional AWS deployment — see `terraform/README.md`)
- Postman

## Prerequisites

This project is intended to be run with Docker Compose.

Required for Docker-based deployment:

- Docker Desktop
- Docker Compose
- A Google Gemini API key (`GOOGLE_API_KEY`) — required by Agent Catalog Service for chat sessions; the rest of the system runs without it

Java and Maven are not required on the host machine for the Docker-based workflow because each service is built inside a Maven Docker image and then run inside a Java runtime Docker image.

Optional for local development without Docker:

- Java 21
- Maven
- k6 (for the load test in `k6/stress-test.js`)

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
auth-service-dev.yml
auth-service-prod.yml
chat-service-dev.yml
chat-service-prod.yml
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

(Chat Service uses MongoDB and has no `ddl-auto` setting; it does not require a schema.)

## Running the System

On macOS, Linux, or Git Bash, run:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

### [Required]

Edit `.env` and set `GOOGLE_API_KEY` to a valid Google Gemini API key. Without it, Agent Catalog Service will still start, but chat requests (`/api/agents/send-message`, `/api/agents/load-session`) will fail.

### [Optional]

Edit `.env` if you want to change the database names, passwords, or `APP_PROFILE`.

From the project root directory, run:

```bash
docker compose up --build
```

This command builds and starts:

- Config Service (config-service)
- Discovery Service (discovery-service)
- Gateway Service (gateway-service)
- Auth Service (auth-service)
- Agent Catalog Service (agent-catalog-service)
- Deployment Service (deployment-service)
- Customer Service (customer-service)
- Chat Service (chat-service)
- Four PostgreSQL database containers (agent-db, deployment-db, customer-db, auth-db)
- One MongoDB container (chat-mongodb)
- Zookeeper and Kafka
- Zipkin

## Stopping the System

To stop the containers:

```bash
docker compose down
```

To stop the containers and remove database volumes:

```bash
docker compose down -v
```

## Running with a Different Profile

By default, Docker Compose runs the domain services with the `dev` profile.

To make the active profile configurable, set `APP_PROFILE` in the environment or `.env` file.

### Running with the Prod profile

#### On macOS, Linux, or Git Bash, run:

```bash
APP_PROFILE=prod docker compose up --build
```

#### On Windows PowerShell, run:

```powershell
$env:APP_PROFILE="prod"
```

Then run:

```powershell
docker compose up --build
```

The `prod` profile is more production-like and disables SQL logging.

If the `prod` profile uses `spring.jpa.hibernate.ddl-auto=validate`, the database schema must already exist before services start.

Note: The `dev` profile is intended for local development. It uses automatic schema updates and SQL logging.

## Checking Container Status

Run:

```bash
docker compose ps
```

Expected running services:

```text
config-service
discovery-service
gateway-service
auth-service
agent-catalog-service
deployment-service
customer-service
chat-service
agent-db
deployment-db
customer-db
auth-db
chat-mongodb
zookeeper
kafka
zipkin
```

Example output (with `dev` profile):
```text
$ docker compose ps
NAME                    IMAGE                                   COMMAND                  SERVICE                 CREATED              STATUS                        PORTS
agent-catalog-service   semesterproject-agent-catalog-service   "java -jar app.jar"      agent-catalog-service   About a minute ago   Up About a minute             0.0.0.0:8081->8081/tcp, [::]:8081->8081/tcp
agent-db                postgres:16                             "docker-entrypoint.s…"   agent-db                About a minute ago   Up About a minute             0.0.0.0:5433->5432/tcp, [::]:5433->5432/tcp
auth-db                 postgres:16                             "docker-entrypoint.s…"   auth-db                 About a minute ago   Up About a minute             0.0.0.0:5436->5432/tcp, [::]:5436->5432/tcp
auth-service            semesterproject-auth-service            "java -jar app.jar"      auth-service            About a minute ago   Up About a minute             0.0.0.0:8086->8086/tcp, [::]:8086->8086/tcp
chat-mongodb            mongo:7.0                               "docker-entrypoint.s…"   chat-mongodb            About a minute ago   Up About a minute             0.0.0.0:27017->27017/tcp, [::]:27017->27017/tcp
chat-service            semesterproject-chat-service            "java -jar app.jar"      chat-service            About a minute ago   Up About a minute             0.0.0.0:8084->8084/tcp, [::]:8084->8084/tcp
config-service          semesterproject-config-service          "java -jar app.jar"      config-service          About a minute ago   Up About a minute             0.0.0.0:8888->8888/tcp, [::]:8888->8888/tcp
customer-db             postgres:16                             "docker-entrypoint.s…"   customer-db             About a minute ago   Up About a minute             0.0.0.0:5435->5432/tcp, [::]:5435->5432/tcp
customer-service        semesterproject-customer-service        "java -jar app.jar"      customer-service        About a minute ago   Up About a minute             0.0.0.0:8083->8083/tcp, [::]:8083->8083/tcp
deployment-db           postgres:16                             "docker-entrypoint.s…"   deployment-db           About a minute ago   Up About a minute             0.0.0.0:5434->5432/tcp, [::]:5434->5432/tcp
deployment-service      semesterproject-deployment-service      "java -jar app.jar"      deployment-service      About a minute ago   Up About a minute             0.0.0.0:8082->8082/tcp, [::]:8082->8082/tcp
discovery-service       semesterproject-discovery-service       "java -jar app.jar"      discovery-service       About a minute ago   Up About a minute             0.0.0.0:8761->8761/tcp, [::]:8761->8761/tcp
gateway-service         semesterproject-gateway-service         "java -jar app.jar"      gateway-service         About a minute ago   Up About a minute             0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp
kafka                   confluentinc/cp-kafka:7.5.0             "/etc/confluent/dock…"   kafka                   About a minute ago   Up About a minute             0.0.0.0:9092->9092/tcp, [::]:9092->9092/tcp, 0.0.0.0:29092->29092/tcp, [::]:29092->29092/tcp
zipkin                  openzipkin/zipkin:latest                "start-zipkin"           zipkin                  About a minute ago   Up About a minute (healthy)   9410/tcp, 0.0.0.0:9411->9411/tcp, [::]:9411->9411/tcp
zookeeper               confluentinc/cp-zookeeper:7.5.0         "/etc/confluent/dock…"   zookeeper               About a minute ago   Up About a minute             2888/tcp, 0.0.0.0:2181->2181/tcp, [::]:2181->2181/tcp, 3888/tcp
```

## Service Health Checks

After startup, test the services:

```text
GET http://localhost:8888/actuator/health
GET http://localhost:8761/actuator/health
GET http://localhost:8080/actuator/health
GET http://localhost:8086/actuator/health
GET http://localhost:8081/actuator/health
GET http://localhost:8082/actuator/health
GET http://localhost:8083/actuator/health
GET http://localhost:8084/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

The Eureka dashboard is also available at `http://localhost:8761` and lists every service once it has registered. The Zipkin UI is available at `http://localhost:9411`.

## Config Server Test URLs

The configuration server can be tested with:

### Dev Profile

```text
GET http://localhost:8888/agent-catalog-service/dev
GET http://localhost:8888/deployment-service/dev
GET http://localhost:8888/customer-service/dev
GET http://localhost:8888/auth-service/dev
GET http://localhost:8888/chat-service/dev
```

### Prod Profile

```text
GET http://localhost:8888/agent-catalog-service/prod
GET http://localhost:8888/deployment-service/prod
GET http://localhost:8888/customer-service/prod
GET http://localhost:8888/auth-service/prod
GET http://localhost:8888/chat-service/prod
```

These endpoints should return configuration data for each service.

## API Endpoints

Every endpoint below can be called either directly against its service port, or through the gateway at `http://localhost:8080`, which routes by path prefix (`/api/auth/**`, `/api/agents/**`, `/api/deployments/**`, `/api/customers/**`, `/api/messages/**`) to the matching service via Eureka. Except where noted under [Authentication](#authentication), requests need `Authorization: Bearer <token>`.

### Auth Service

Base URL:

```text
http://localhost:8086
```

Endpoints:

```text
POST   /api/auth/register          (public)
POST   /api/auth/login             (public)
POST   /api/auth/forgot-password   (public)
POST   /api/auth/reset-password    (public)
GET    /api/auth/public-key        (public)
GET    /api/auth/me                (requires JWT)
PUT    /api/auth/change-password   (requires JWT)
```

### Agent Catalog Service

Base URL:

```text
http://localhost:8081
```

Endpoints (all require a JWT):

```text
GET    /api/agents
GET    /api/agents/{id}
POST   /api/agents
PUT    /api/agents/{id}
DELETE /api/agents/{id}
POST   /api/agents/load-session?sessionId={sessionId}
POST   /api/agents/send-message
```

### Deployment Service

Base URL:

```text
http://localhost:8082
```

Endpoints (all require a JWT):

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

Endpoints (currently public, no JWT required):

```text
GET    /api/customers
GET    /api/customers/{id}
POST   /api/customers
PUT    /api/customers/{id}
DELETE /api/customers/{id}
```

### Chat Service

Base URL:

```text
http://localhost:8084
```

Endpoints (all require a JWT):

```text
POST   /api/messages
GET    /api/messages/session/{sessionId}
GET    /api/messages/session/{sessionId}/count
DELETE /api/messages/session/{sessionId}
```

Chat messages are normally created indirectly: Agent Catalog Service publishes them to Kafka, and Chat Service consumes and persists them. `POST /api/messages` exists for direct/manual writes.

## Example API Requests

### Register a User

Request:

```text
POST http://localhost:8086/api/auth/register
```

Body:

```json
{
  "customerId": 1,
  "username": "test.mctestface",
  "email": "test.mctestface@example.com",
  "password": "ChangeMe123!"
}
```

### Log In

Request:

```text
POST http://localhost:8086/api/auth/login
```

Body:

```json
{
  "username": "test.mctestface",
  "password": "ChangeMe123!"
}
```

The response includes a JWT. Use it as `Authorization: Bearer <token>` on subsequent requests to Agent Catalog, Deployment, and Chat Service.

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

### Create Agent

Request (requires `Authorization: Bearer <token>`):

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
  "provider": "Google",
  "status": "ACTIVE"
}
```

### Create Deployment

Request (requires `Authorization: Bearer <token>`):

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

The current, complete collection is:

```text
postman/CSI_5347-Semester-Project.postman_collection.json
```

It covers all eight services, including auth (register/login/JWT), agent chat sessions, and chat message retrieval.

To use it:

1. Open Postman.
2. Click `Import`.
3. Select `postman/CSI_5347-Semester-Project.postman_collection.json`.
4. Run the requests under **Setup (Run In Order)** first (Create Customer, Register User, Login) — this saves a JWT into the collection's environment for the protected requests.
5. Then exercise each service's folder (Auth, Customer, Agent, Deployment, Chat).

Note: `postman/AgentHub.postman_collection.json` predates Auth and Chat Service and only covers Agent, Deployment, and Customer CRUD. Prefer the CSI_5347 collection above.

## Suggested Testing Order

### 1. Copy `.env.example` to `.env`, set `GOOGLE_API_KEY`, then start Docker Compose.

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
GET http://localhost:8084/actuator/health
GET http://localhost:8086/actuator/health
```

### 4. Create a customer.

```text
POST http://localhost:8083/api/customers
```

### 5. Register a user against that customer, then log in to obtain a JWT.

```text
POST http://localhost:8086/api/auth/register
POST http://localhost:8086/api/auth/login
```

### 6. Create an agent (using the JWT).

```text
POST http://localhost:8081/api/agents
```

### 7. Create a deployment referencing the created customer and agent IDs (using the JWT).

```text
POST http://localhost:8082/api/deployments
```

### 8. Start a chat session with the agent and confirm the message was persisted.

```text
POST http://localhost:8081/api/agents/send-message
GET  http://localhost:8084/api/messages/session/{sessionId}
```

### 9. Test remaining GET, PUT, and DELETE endpoints for each service.

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
docker compose logs auth-service
docker compose logs chat-service
docker compose logs config-service
docker compose logs discovery-service
docker compose logs gateway-service
```

### Requests to a protected endpoint return 401

Agent Catalog, Deployment, and Chat Service require a JWT. Log in via `POST /api/auth/login` and send the token as `Authorization: Bearer <token>`. Also confirm `auth-service` is healthy — the other services fetch its RSA public key on startup and will fail to validate tokens (or fail to start) if they can't reach it.

### Agent chat requests fail or time out

Confirm `GOOGLE_API_KEY` is set in `.env` and that `agent-catalog-service` was restarted after setting it.

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

### Chat messages aren't showing up in Chat Service

Confirm `kafka` and `zookeeper` are both `Up`, and check `chat-service` logs for consumer errors:

```bash
docker compose logs kafka
docker compose logs zookeeper
docker compose logs chat-service
```

### Rebuild from scratch

If containers or volumes get into a bad state:

```bash
docker compose down -v
docker compose up --build
```
