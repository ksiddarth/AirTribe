# Task Tracker & Team Collaboration API

Backend REST API built with **Java 21**, **Spring Boot 3**, **Maven**, **Spring Security (JWT)**, and **MySQL** for managing projects, tasks, comments, file attachments, real-time notifications (SSE), and optional AI task drafting.

## Prerequisites
- Java 21+
- Maven 3.9+
- MySQL 8.x

## Environment Variables
Configure secrets and runtime settings via environment variables:

| Variable | Default | Description |
| :--- | :--- | :--- |
| `DB_URL` | `jdbc:mysql://localhost:3306/task_tracker?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC` | MySQL JDBC URL |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | *(empty)* | Database password |
| `JWT_SECRET` | *(dev fallback)* | Base64 or plain secret (>= 32 chars) for signing JWTs |
| `UPLOAD_DIR` | `./uploads` | Directory for storing uploaded task attachments |
| `AI_ENABLED` | `false` | Enable external Gemini task draft generation |
| `AI_API_KEY` | *(empty)* | API key when `AI_ENABLED=true` |

## Running the Application
```bash
mvn clean test
mvn spring-boot:run
```

## API Endpoints Overview
- **Auth & Profile:**
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/logout`
  - `GET /api/v1/users/me` | `PATCH /api/v1/users/me`
- **Projects & Team Members:**
  - `POST /api/v1/projects`
  - `GET /api/v1/projects`
  - `POST /api/v1/projects/{projectId}/members`
  - `GET /api/v1/projects/{projectId}/members`
- **Tasks (CRUD, Filter, Sort, Search):**
  - `POST /api/v1/projects/{projectId}/tasks`
  - `GET /api/v1/tasks` (`?projectId=&assigneeId=&status=&q=&sort=dueDate,asc&page=0&size=10`)
  - `GET /api/v1/tasks/{taskId}`
  - `PATCH /api/v1/tasks/{taskId}`
  - `DELETE /api/v1/tasks/{taskId}`
- **Comments & Attachments:**
  - `POST /api/v1/tasks/{taskId}/comments` | `GET /api/v1/tasks/{taskId}/comments`
  - `POST /api/v1/tasks/{taskId}/attachments` | `GET /api/v1/tasks/{taskId}/attachments/{attachmentId}/download`
- **Extensions:**
  - `GET /api/v1/notifications/stream` (Server-Sent Events) & `GET /api/v1/notifications`
  - `POST /api/v1/ai/tasks/draft`
