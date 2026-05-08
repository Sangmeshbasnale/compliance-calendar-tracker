# Day 17 — Demo Preparation & Bug Fix Report

## Bug Fixes Applied

| # | Bug | Fix | File |
|---|---|---|---|
| 1 | App crashes on startup without SMTP credentials | Changed `starttls.required` from `true` to `false` | `application.yml` |
| 2 | Missing request param returns 500 instead of 400 | Added `MissingServletRequestParameterException` handler | `GlobalExceptionHandler.java` |
| 3 | Wrong path variable type returns 500 instead of 400 | Added `MethodArgumentTypeMismatchException` handler | `GlobalExceptionHandler.java` |
| 4 | Java 25 used instead of Java 17 | Added `maven.compiler.source/target=17` | `pom.xml` |
| 5 | Accidental `main` file in root directory | Removed from git and disk | root |
| 6 | Cache type not configurable | Added `spring.cache.type` env variable support | `application.yml` |

---

## Pre-Demo Checklist

### Services Running
- [ ] PostgreSQL running on port 5432
- [ ] Redis running on port 6379
- [ ] Application started (`mvn spring-boot:run`)
- [ ] No errors in startup logs
- [ ] Swagger UI loads at `http://localhost:8080/swagger-ui.html`

### Data Ready
- [ ] 3 demo users seeded (admin, manager, viewer)
- [ ] 30 compliance records seeded
- [ ] Logs show: `Inserted 30 compliance records successfully`

---

## Step-by-Step Demo Script

### Step 1 — Show Swagger UI (30 seconds)
1. Open `http://localhost:8080/swagger-ui.html`
2. Show 3 API groups: **Authentication**, **Compliance**, **File Management**
3. Show all 11 endpoints with descriptions

---

### Step 2 — Authentication (1 minute)

**Login as Admin:**
```
POST /auth/login
{
  "username": "admin",
  "password": "admin123"
}
```
- Show 200 OK response with JWT token
- Copy the token
- Click **Authorize** in Swagger → paste `Bearer <token>`

**Show role-based access:**
```
POST /auth/login
{
  "username": "viewer",
  "password": "viewer123"
}
```
- Login as viewer
- Try to create a record → show 403 Forbidden

---

### Step 3 — Compliance CRUD (2 minutes)

**Get all records (paginated):**
```
GET /api/compliance?page=0&size=5
```
- Show 30 seeded records with pagination

**Get by ID:**
```
GET /api/compliance/1
```
- Show single record with all fields

**Create new record:**
```
POST /api/compliance
{
  "title": "Demo GDPR Review",
  "description": "Live demo compliance record",
  "status": "PENDING",
  "dueDate": "2025-12-31"
}
```
- Show 201 Created with Location header
- Show email trigger in logs

**Update record:**
```
PUT /api/compliance/{id}
{
  "title": "Demo GDPR Review - Updated",
  "description": "Updated during demo",
  "status": "COMPLETED",
  "dueDate": "2025-12-31"
}
```
- Show 200 OK with updated fields

**Soft Delete:**
```
DELETE /api/compliance/{id}
```
- Show 204 No Content
- Then GET same ID → show 404 Not Found

---

### Step 4 — Search & Stats (30 seconds)

**Search:**
```
GET /api/compliance/search?q=gdpr
```
- Show case-insensitive results

**Stats:**
```
GET /api/compliance/stats
```
- Show counts by status

---

### Step 5 — Redis Caching (30 seconds)

1. Call `GET /api/compliance` — show log: `Cache MISS`
2. Call same endpoint again — no MISS log = **Cache HIT**
3. Create a record — show cache eviction in logs
4. Call GET again — MISS appears again

---

### Step 6 — File Upload & Download (1 minute)

**Upload:**
```
POST /api/files/upload
form-data: file = <select any PDF>
```
- Show 201 Created with file metadata and download URL

**Download:**
```
GET /api/files/1
```
- Show file download with correct Content-Type

---

### Step 7 — Exception Handling (30 seconds)

**404:**
```
GET /api/compliance/9999
```
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Compliance record not found with id: 9999"
}
```

**400 — Validation:**
```
POST /api/compliance
{ "title": "", "status": "PENDING", "dueDate": "2025-12-31" }
```
```json
{
  "status": 400,
  "error": "Bad Request",
  "fieldErrors": { "title": "Title is required" }
}
```

**401 — No token:**
```
GET /api/compliance (no Authorization header)
```
```json
{ "error": "Unauthorized - token missing or invalid" }
```

---

## Common Issues & Fixes

| Issue | Cause | Fix |
|---|---|---|
| App won't start | Java 25 used instead of 17 | Set `JAVA_HOME` to Java 17 path |
| `Connection refused 5432` | PostgreSQL not running | Start PostgreSQL service |
| `Connection refused 6379` | Redis not running | Start Redis service |
| `relation does not exist` | Flyway migrations not run | Check DB connection and restart |
| Port 8080 in use | Another process using port | Run: `netstat -ano \| findstr :8080` then kill PID |
| Email not sending | No SMTP config | App still works — email just logs a warning |
| 500 on search without `?q=` | Missing param | Fixed — now returns 400 with clear message |

---

## Final Verification Checklist

| Check | Expected | Status |
|---|---|---|
| App starts without errors | Yes | |
| 30 records seeded | Yes | |
| 3 users seeded | Yes | |
| POST /auth/login returns token | 200 OK | |
| GET /api/compliance returns records | 200 OK | |
| POST /api/compliance creates record | 201 Created | |
| PUT /api/compliance/{id} updates | 200 OK | |
| DELETE /api/compliance/{id} soft deletes | 204 No Content | |
| GET /api/compliance/9999 returns 404 | 404 Not Found | |
| Invalid input returns 400 | 400 Bad Request | |
| No token returns 401 | 401 Unauthorized | |
| Viewer cannot create | 403 Forbidden | |
| File upload works | 201 Created | |
| File download works | 200 OK | |
| Swagger UI loads | Yes | |
| Cache MISS on first call | Yes | |
| Cache HIT on second call | Yes | |
| All tests pass (`mvn test`) | BUILD SUCCESS | |
