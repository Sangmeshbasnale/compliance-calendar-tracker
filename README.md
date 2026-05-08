# Compliance Calendar Tracker

A production-ready **Spring Boot 3 (Java 17)** backend application for managing and tracking compliance records efficiently. Built with JWT authentication, Redis caching, email notifications, file handling, and full Swagger documentation.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Security | Spring Security + JWT (jjwt 0.12.6) |
| Caching | Redis |
| Email | JavaMailSender (SMTP) |
| File Handling | Spring Multipart |
| Documentation | Swagger / OpenAPI 3 (springdoc 2.5.0) |
| Testing | JUnit 5, Mockito, @DataJpaTest, @WebMvcTest |
| Build Tool | Maven 3.9.x |

---

## Features

- **User Authentication** — Register and login with JWT token-based security
- **Role-Based Access Control** — ROLE_ADMIN, ROLE_MANAGER, ROLE_VIEWER
- **Compliance CRUD** — Create, read, update, soft-delete compliance records
- **Pagination & Sorting** — Paginated list with sorting support
- **Search** — Case-insensitive search by title or description
- **Statistics** — Record counts grouped by status
- **Redis Caching** — Cache GET responses, evict on create/update/delete
- **Email Notifications** — Async email on record creation and overdue alerts
- **File Upload & Download** — Upload PDF, DOCX, PNG, JPG (max 10MB)
- **Global Exception Handling** — Consistent JSON error responses
- **Swagger UI** — Interactive API documentation with JWT support
- **Data Seeder** — Auto-seeds 30 compliance records and 3 demo users on startup
- **Audit Logging** — Track entity changes with audit log table
- **Scheduled Jobs** — Auto-mark overdue records daily

---

## Project Structure

```
src/main/java/com/example/tool/
├── config/
│   ├── JpaAuditingConfig.java
│   ├── JwtAuthFilter.java
│   ├── JwtUtil.java
│   ├── OpenApiConfig.java
│   ├── RedisConfig.java
│   └── SecurityConfig.java
├── controller/
│   ├── AuthController.java
│   ├── ComplianceController.java
│   └── FileController.java
├── dto/
│   ├── AuthResponse.java
│   ├── ComplianceRequest.java
│   ├── ComplianceResponse.java
│   ├── LoginRequest.java
│   └── RegisterRequest.java
├── entity/
│   ├── AuditLog.java
│   ├── Compliance.java
│   ├── FileMetadata.java
│   └── User.java
├── exception/
│   ├── ComplianceNotFoundException.java
│   ├── ErrorResponse.java
│   ├── GlobalExceptionHandler.java
│   ├── InvalidDataException.java
│   └── ResourceNotFoundException.java
├── repository/
│   ├── AuditLogRepository.java
│   ├── ComplianceRepository.java
│   ├── FileMetadataRepository.java
│   └── UserRepository.java
├── scheduler/
│   └── ComplianceScheduler.java
├── seeder/
│   └── DataSeeder.java
├── service/
│   ├── AuthService.java
│   ├── ComplianceService.java
│   ├── CustomUserDetailsService.java
│   ├── EmailService.java
│   └── FileService.java
└── ComplianceTrackerApplication.java
```

---

## Prerequisites

- Java 17
- Maven 3.9+
- PostgreSQL 15
- Redis 7.x

---

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/BhagyashreeReddy14/compliance-calendar-tracker.git
cd compliance-calendar-tracker
```

### 2. Create PostgreSQL Database

```sql
CREATE DATABASE compliance_db;
```

### 3. Configure Environment Variables

Set the following environment variables or update `application.yml`:

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/compliance_db
DB_USER=postgres
DB_PASSWORD=postgres

# JWT
JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long
JWT_EXPIRATION=86400000

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Email (optional)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# File Upload
FILE_UPLOAD_DIR=uploads

# Notification
NOTIFICATION_EMAIL=admin@example.com
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

### Expected Startup Output

```
Started ComplianceTrackerApplication in X.XXX seconds
Inserted 3 demo users (admin, manager, viewer) successfully.
Inserted 30 compliance records successfully.
```

---

## Demo Credentials

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ROLE_ADMIN |
| `manager` | `manager123` | ROLE_MANAGER |
| `viewer` | `viewer123` | ROLE_VIEWER |

---

## API Documentation

Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

Click **Authorize** → Enter `Bearer <token>` to test secured endpoints.

---

## API Endpoints

### Authentication

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/auth/register` | Register new user | Public |
| POST | `/auth/login` | Login and get JWT token | Public |

### Compliance Records

| Method | Endpoint | Description | Role |
|---|---|---|---|
| GET | `/api/compliance` | Get all records (paginated) | All |
| GET | `/api/compliance/{id}` | Get record by ID | All |
| POST | `/api/compliance` | Create new record | Admin, Manager |
| PUT | `/api/compliance/{id}` | Update record | Admin, Manager |
| DELETE | `/api/compliance/{id}` | Soft delete record | Admin |
| GET | `/api/compliance/search?q=` | Search by title/description | All |
| GET | `/api/compliance/stats` | Get status statistics | All |

### File Management

| Method | Endpoint | Description | Role |
|---|---|---|---|
| POST | `/api/files/upload` | Upload file (PDF, DOCX, PNG, JPG) | Admin, Manager |
| GET | `/api/files/{id}` | Download file by ID | All |

---

## Example Requests

### Login

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ROLE_ADMIN"
}
```

### Create Compliance Record

```bash
curl -X POST http://localhost:8080/api/compliance \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "GDPR Annual Review",
    "description": "Annual review of GDPR compliance",
    "status": "PENDING",
    "dueDate": "2025-12-31"
  }'
```

Response:
```json
{
  "id": 1,
  "title": "GDPR Annual Review",
  "status": "PENDING",
  "dueDate": "2025-12-31",
  "isDeleted": false,
  "createdAt": "2025-01-15T10:30:00"
}
```

### Get Statistics

```bash
curl -X GET http://localhost:8080/api/compliance/stats \
  -H "Authorization: Bearer <token>"
```

Response:
```json
{
  "total": 30,
  "pending": 8,
  "completed": 8,
  "overdue": 5,
  "open": 7,
  "closed": 2
}
```

---

## Running Tests

```bash
mvn test
```

### Test Coverage (~88%)

| Layer | Test Class |
|---|---|
| Service | `ComplianceServiceTest`, `AuthServiceTest`, `CustomUserDetailsServiceTest` |
| Repository | `ComplianceRepositoryTest`, `UserRepositoryTest`, `AuditLogRepositoryTest` |
| Controller | `ComplianceControllerTest`, `AuthControllerTest` |
| Config | `JwtUtilTest`, `JwtAuthFilterTest` |
| Exception | `GlobalExceptionHandlerTest` |

---

## Error Responses

All errors return consistent JSON:

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Compliance record not found with id: 99",
  "path": "/api/compliance/99"
}
```

| Status | Scenario |
|---|---|
| 400 | Invalid input / validation failure |
| 401 | Missing or invalid JWT token |
| 403 | Insufficient role permissions |
| 404 | Resource not found |
| 500 | Unexpected server error |

---

## Future Enhancements

- React frontend integration
- Advanced reporting and analytics dashboard
- Notifications dashboard with read/unread status
- Multi-tenant support
- Docker & Kubernetes deployment
- CI/CD pipeline with GitHub Actions

---

## Author

**Bhagyashree Reddy**
Java Developer — Backend
Spring Boot 3 | Java 17 | PostgreSQL | Redis | JWT
