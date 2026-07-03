# INB Public Portal - Backend API

This repository contains the core backend microservice for the General Department of Information and Broadcasting (INB) public-facing portal and Content Management System. 

It is designed to serve news articles, departmental information, and public resources via a RESTful API.

## 🛠 Tech Stack
*   **Language:** Java 21
*   **Framework:** Spring Boot 3.x
*   **Database:** PostgreSQL 15+
*   **ORM:** Spring Data JPA + Hibernate
*   **Boilerplate Reduction:** Lombok
*   **Containerization (Local):** Docker Compose

## 📋 Prerequisites
To run this project locally, you will need:
*   [Java Development Kit (JDK) 21](https://aws.amazon.com/corretto/)
*   [Maven](https://maven.apache.org/) (or use your IDE's bundled Maven)
*   [Docker Desktop](https://www.docker.com/products/docker-desktop/) (for the local database)

## 🚀 Local Development Setup

### 1. Start the Database
We use Docker Compose to spin up a local, isolated PostgreSQL instance. This avoids conflicts with any native PostgreSQL installations on your machine.

```bash
# Start the database in the background
docker-compose up -d
```

*Note: The database is mapped to port **5433** on your localhost to prevent conflicts.*

### 2. Configure the Environment
The application is pre-configured to connect to the Docker database via `src/main/resources/application.yml`. 
*   **Database URL:** `jdbc:postgresql://localhost:5433/inb_portal_db`
*   **Username:** `inb_user`
*   **Password:** `MOI@123`

### 3. Run the Application
You can run the application directly through your IDE (IntelliJ IDEA recommended) by executing the `InbPortalApplication.java` main class, or via the terminal:

```bash
mvn spring-boot:run
```

*Note: The Spring Boot server runs on port **8085**.*

## 📡 API Endpoints

### Articles
*   `GET /api/articles` - Fetch a list of all articles.
*   `POST /api/articles` - Create a new article.
    *   **Payload Example:**
        ```json
        {
            "title": "Welcome to the INB Portal",
            "content": "Official content goes here.",
            "author": "System Admin"
        }
        ```
