# Day 18 — Fresh Environment Setup & Verification

## Prerequisites — Install in This Order

### 1. Java 17 JDK
- Download: https://adoptium.net/temurin/releases/?version=17
- Select: Windows x64 JDK 17
- Install and set environment variable:
```
JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot
PATH      = %JAVA_HOME%\bin;%PATH%
```
- Verify:
```bash
java -version
# Expected: openjdk version "17.x.x"
```

---

### 2. Maven 3.9+
- Download: https://maven.apache.org/download.cgi
- File: `apache-maven-3.9.x-bin.zip`
- Extract to `C:\maven`
- Add to PATH: `C:\maven\bin`
- Verify:
```bash
mvn -version
# Expected: Apache Maven 3.9.x
```

---

### 3. PostgreSQL 15
- Download: https://www.postgresql.org/download/windows/
- Install with:
  - Password: `postgres`
  - Port: `5432`
- After install, open pgAdmin or SQL Shell and run:
```sql
CREATE DATABASE compliance_db;
```
- Verify:
```bash
psql -U postgres -c "\l"
# Expected: compliance_db listed
```

---

### 4. Redis
- Download: https://github.com/microsoftarchive/redis/releases
- File: `Redis-x64-3.0.504.msi`
- Install — runs as Windows service automatically
- Verify:
```bash
redis-cli ping
# Expected: PONG
```

---

### 5. Postman (for API testing)
- Download: https://www.postman.com/downloads/

---

## Step-by-Step Fresh Setup

### Step 1 — Clone the Repository
```bash
git clone https://github.com/BhagyashreeReddy14/compliance-calendar-tracker.git
cd compliance-calendar-tracker
```

---

### Step 2 — Set Environment Variables
Open terminal and set these before running:
```bash
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%
set DB_URL=jdbc:postgresql://localhost:5432/compliance_db
set DB_USER=postgres
set DB_PASSWORD=postgres
set JWT_SECRET=compliance-tracker-secret-key-must-be-32-chars-long
set JWT_EXPIRATION=86400000
set REDIS_HOST=localhost
set REDIS_PORT=6379
set FILE_UPLOAD_DIR=uploads
set NOTIFICATION_EMAIL=admin@example.com
```

---

### Step 3 — Build the Project
```bash
mvn clean install -DskipTests
```

Expected output:
```
BUILD SUCCESS
```

If build fails check:
- Java version: `java -version` must show 17
- Maven version: `mvn -version` must show 3.9+

---

### Step 4 — Start the Application
```bash
mvn spring-boot:run
```

Expected startup logs:
```
Started ComplianceTrackerApplication in X.XXX seconds
Seeding demo users...
Inserted 3 demo users (admin, manager, viewer) successfully.
Seeding compliance records...
Inserted 30 compliance records successfully.
```

---

### Step 5 — Verify Swagger UI
Open browser:
```
http://localhost:8080/swagger-ui.html
```

Expected:
- Page loads without error
- Title: **Compliance Tracker API**
- 3 groups visible: Authentication, Compliance, File Management
- 11 endpoints visible
- Authorize button present

---

## Feature Verification Checklist

### Authentication

**TC-01 — Login**
```
POST http://localhost:8080/auth/login
{
  "username": "admin",
  "password": "admin123"
}
```
Expected: `200 OK` with token ✅

**TC-02 — Register**
```
POST http://localhost:8080/auth/register
{
  "username": "newuser",
  "password": "password123"
}
```
Expected: `200 OK` with token ✅

**TC-03 — No token**
```
GET http://localhost:8080/api/compliance
```
Expected: `401 Unauthorized` ✅

---

### Compliance CRUD

**TC-04 — Get all records**
```
GET http://localhost:8080/api/compliance
Authorization: Bearer <token>
```
Expected: `200 OK` with 30 seeded records ✅

**TC-05 — Create record**
```
POST http://localhost:8080/api/compliance
Authorization: Bearer <admin-token>
{
  "title": "Fresh Setup Test",
  "description": "Testing fresh environment",
  "status": "PENDING",
  "dueDate": "2025-12-31"
}
```
Expected: `201 Created` ✅

**TC-06 — Get by ID**
```
GET http://localhost:8080/api/compliance/1
Authorization: Bearer <token>
```
Expected: `200 OK` ✅

**TC-07 — Update**
```
PUT http://localhost:8080/api/compliance/1
Authorization: Bearer <admin-token>
{
  "title": "Updated Title",
  "description": "Updated",
  "status": "COMPLETED",
  "dueDate": "2025-12-31"
}
```
Expected: `200 OK` ✅

**TC-08 — Soft Delete**
```
DELETE http://localhost:8080/api/compliance/1
Authorization: Bearer <admin-token>
```
Expected: `204 No Content` ✅

**TC-09 — Search**
```
GET http://localhost:8080/api/compliance/search?q=gdpr
Authorization: Bearer <token>
```
Expected: `200 OK` with matching records ✅

**TC-10 — Stats**
```
GET http://localhost:8080/api/compliance/stats
Authorization: Bearer <token>
```
Expected: `200 OK` with counts ✅

---

### Error Handling

**TC-11 — 404 Not Found**
```
GET http://localhost:8080/api/compliance/9999
Authorization: Bearer <token>
```
Expected:
```json
{ "status": 404, "error": "Not Found", "message": "Compliance record not found with id: 9999" }
```
✅

**TC-12 — 400 Validation Error**
```
POST http://localhost:8080/api/compliance
Authorization: Bearer <admin-token>
{ "title": "", "status": "PENDING", "dueDate": "2025-12-31" }
```
Expected:
```json
{ "status": 400, "error": "Bad Request", "fieldErrors": { "title": "Title is required" } }
```
✅

**TC-13 — 400 Missing Parameter**
```
GET http://localhost:8080/api/compliance/search
Authorization: Bearer <token>
```
Expected:
```json
{ "status": 400, "error": "Bad Request", "message": "Required parameter 'q' is missing" }
```
✅

**TC-14 — 403 Forbidden**
Login as viewer, then:
```
POST http://localhost:8080/api/compliance
Authorization: Bearer <viewer-token>
```
Expected: `403 Forbidden` ✅

---

### File Upload & Download

**TC-15 — Upload**
```
POST http://localhost:8080/api/files/upload
Authorization: Bearer <admin-token>
Body: form-data → key=file, value=<any PDF>
```
Expected: `201 Created` with metadata ✅

**TC-16 — Download**
```
GET http://localhost:8080/api/files/1
Authorization: Bearer <token>
```
Expected: `200 OK` with file content ✅

---

### Redis Cache

**TC-17 — Cache Miss**
- Call `GET /api/compliance` first time
- Check logs: `Cache MISS - fetching complianceRecords from DB`
✅

**TC-18 — Cache Hit**
- Call `GET /api/compliance` second time
- No MISS log = cache hit
✅

**TC-19 — Cache Eviction**
- Create a record
- Check logs: cache evicted
- Call `GET /api/compliance` again — MISS appears
✅

---

### Data Seeder

**TC-20 — First startup**
```
Inserted 3 demo users successfully
Inserted 30 compliance records successfully
```
✅

**TC-21 — Second startup (no duplicates)**
```
Users already seeded — skipping.
Compliance records already seeded — skipping.
```
✅

---

## Common Errors & Fixes

| Error | Cause | Fix |
|---|---|---|
| `UnsupportedClassVersionError` | Wrong Java version | Set `JAVA_HOME` to Java 17 |
| `Connection refused: 5432` | PostgreSQL not running | Start PostgreSQL service |
| `Connection refused: 6379` | Redis not running | Start Redis service |
| `relation "compliance" does not exist` | DB not created | Run `CREATE DATABASE compliance_db` |
| `BUILD FAILURE - compiler` | Java version mismatch | Ensure `JAVA_HOME` points to JDK 17 |
| `Port 8080 already in use` | Another process on 8080 | Run: `netstat -ano \| findstr :8080` then `taskkill /PID <id> /F` |
| `Flyway migration failed` | DB schema mismatch | Drop and recreate DB: `DROP DATABASE compliance_db; CREATE DATABASE compliance_db;` |
| `401 on all requests` | Token expired or missing | Re-login to get fresh token |
| `Email not sending` | No SMTP config | App still works — email logs a warning, not an error |
| `File upload 400` | Wrong file type | Use PDF, DOCX, PNG or JPG only |

---

## Final Verification Checklist

| Check | Expected | Pass/Fail |
|---|---|---|
| Java 17 installed | `java -version` shows 17 | |
| Maven installed | `mvn -version` shows 3.9+ | |
| PostgreSQL running | `psql -U postgres` connects | |
| Redis running | `redis-cli ping` returns PONG | |
| `mvn clean install` succeeds | BUILD SUCCESS | |
| App starts without errors | No exceptions in logs | |
| 30 records seeded | Logs confirm insertion | |
| 3 users seeded | Logs confirm insertion | |
| Swagger UI loads | All 11 endpoints visible | |
| Login returns JWT token | 200 OK | |
| GET /api/compliance works | 200 OK with records | |
| POST /api/compliance works | 201 Created | |
| PUT /api/compliance works | 200 OK | |
| DELETE /api/compliance works | 204 No Content | |
| 404 returns clean JSON | Correct error format | |
| 400 returns field errors | Correct error format | |
| 401 without token | Correct error format | |
| 403 for viewer on create | Correct error format | |
| File upload works | 201 Created | |
| File download works | 200 OK | |
| Cache miss on first call | Log shows MISS | |
| Cache hit on second call | No MISS log | |
| No duplicate seeded data on restart | Skipping logs shown | |
| `mvn test` passes | BUILD SUCCESS | |
