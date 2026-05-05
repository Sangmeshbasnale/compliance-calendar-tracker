# Day-13 — Full System Testing: Compliance Tracker API

## Prerequisites

| Item | Value |
|---|---|
| Base URL | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| Database | PostgreSQL — `compliance_db` |
| Cache | Redis on `localhost:6379` |
| Tool | Postman or Swagger UI |

---

## Environment Setup Checklist

- [ ] PostgreSQL running on port 5432
- [ ] Redis running on port 6379
- [ ] Application started (`mvn spring-boot:run`)
- [ ] Swagger UI loads at `http://localhost:8080/swagger-ui.html`
- [ ] No errors in application startup logs

---

## Section 1 — Authentication Flow

### TC-01 — Register a New User

**POST** `/auth/register`

Request:
```json
{
  "username": "testuser",
  "password": "test123"
}
```

Expected Response — `200 OK`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "testuser",
  "role": "ROLE_VIEWER"
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 200 OK | |
| Token present | Non-null JWT string | |
| Username matches | `testuser` | |
| Role assigned | `ROLE_VIEWER` | |

---

### TC-02 — Register Duplicate Username

**POST** `/auth/register`

Request:
```json
{
  "username": "testuser",
  "password": "test123"
}
```

Expected Response — `400 Bad Request`:
```json
"Username already exists"
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 400 Bad Request | |
| Error message | `Username already exists` | |

---

### TC-03 — Register with Invalid Input

**POST** `/auth/register`

Request:
```json
{
  "username": "ab",
  "password": "123"
}
```

Expected Response — `400 Bad Request`:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "username": "Username must be between 3 and 100 characters",
    "password": "Password must be at least 6 characters"
  }
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 400 Bad Request | |
| fieldErrors present | username + password errors | |

---

### TC-04 — Login with Valid Credentials

**POST** `/auth/login`

Request:
```json
{
  "username": "testuser",
  "password": "test123"
}
```

Expected Response — `200 OK`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "testuser",
  "role": "ROLE_VIEWER"
}
```

> **Save the token** — used in all subsequent requests as:
> `Authorization: Bearer <token>`

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 200 OK | |
| Token present | Non-null JWT string | |
| Username matches | `testuser` | |

---

### TC-05 — Login with Wrong Password

**POST** `/auth/login`

Request:
```json
{
  "username": "testuser",
  "password": "wrongpassword"
}
```

Expected Response — `401 Unauthorized`:
```json
"Invalid username or password"
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 401 Unauthorized | |
| Error message correct | `Invalid username or password` | |

---

### TC-06 — Access Secured API Without Token

**GET** `/api/compliance`

No Authorization header.

Expected Response — `401 Unauthorized`:
```json
{
  "error": "Unauthorized - token missing or invalid"
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 401 Unauthorized | |
| Error message present | Yes | |

---

### TC-07 — Access Secured API With Invalid Token

**GET** `/api/compliance`

Header: `Authorization: Bearer invalidtoken123`

Expected Response — `401 Unauthorized`:
```json
{
  "error": "Invalid or expired token"
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 401 Unauthorized | |
| Error message present | Yes | |

---

## Section 2 — Compliance CRUD Flow

> All requests require header: `Authorization: Bearer <token>`

---

### TC-08 — Create Compliance Record

**POST** `/api/compliance`

Request:
```json
{
  "title": "GDPR Annual Review",
  "description": "Annual review of GDPR data processing activities",
  "status": "PENDING",
  "dueDate": "2025-12-31"
}
```

Expected Response — `201 Created`:
```json
{
  "id": 1,
  "title": "GDPR Annual Review",
  "description": "Annual review of GDPR data processing activities",
  "status": "PENDING",
  "dueDate": "2025-12-31",
  "isDeleted": false,
  "createdAt": "2025-01-15T10:30:00",
  "updatedAt": "2025-01-15T10:30:00"
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 201 Created | |
| Location header present | `/api/compliance/1` | |
| ID assigned | Non-null Long | |
| isDeleted | false | |
| createdAt populated | Non-null timestamp | |

---

### TC-09 — Create Record with Missing Title

**POST** `/api/compliance`

Request:
```json
{
  "title": "",
  "status": "PENDING",
  "dueDate": "2025-12-31"
}
```

Expected Response — `400 Bad Request`:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "fieldErrors": {
    "title": "Title is required"
  }
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 400 Bad Request | |
| fieldErrors.title present | Yes | |

---

### TC-10 — Create Record with Past Due Date

**POST** `/api/compliance`

Request:
```json
{
  "title": "Old Record",
  "status": "PENDING",
  "dueDate": "2020-01-01"
}
```

Expected Response — `400 Bad Request`:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Due date must not be in the past"
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 400 Bad Request | |
| Message correct | `Due date must not be in the past` | |

---

### TC-11 — Get All Records (Paginated)

**GET** `/api/compliance?page=0&size=10&sort=dueDate,asc`

Expected Response — `200 OK`:
```json
{
  "content": [
    {
      "id": 1,
      "title": "GDPR Annual Review",
      "status": "PENDING",
      "dueDate": "2025-12-31",
      "isDeleted": false
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 200 OK | |
| content array present | Yes | |
| totalElements correct | 1 | |
| Soft-deleted excluded | Yes | |

---

### TC-12 — Get Record by ID

**GET** `/api/compliance/1`

Expected Response — `200 OK`:
```json
{
  "id": 1,
  "title": "GDPR Annual Review",
  "status": "PENDING",
  "dueDate": "2025-12-31",
  "isDeleted": false
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 200 OK | |
| Correct record returned | id = 1 | |

---

### TC-13 — Get Non-Existent Record

**GET** `/api/compliance/9999`

Expected Response — `404 Not Found`:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Compliance record not found with id: 9999"
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 404 Not Found | |
| Message correct | Yes | |

---

### TC-14 — Search Compliance Records

**GET** `/api/compliance/search?q=gdpr`

Expected Response — `200 OK`:
```json
[
  {
    "id": 1,
    "title": "GDPR Annual Review",
    "status": "PENDING"
  }
]
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 200 OK | |
| Case-insensitive match | Yes | |
| Soft-deleted excluded | Yes | |

---

### TC-15 — Get Compliance Statistics

**GET** `/api/compliance/stats`

Expected Response — `200 OK`:
```json
{
  "total": 1,
  "pending": 1,
  "completed": 0,
  "overdue": 0,
  "open": 0,
  "closed": 0
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 200 OK | |
| All 6 keys present | Yes | |
| Counts correct | Yes | |

---

### TC-16 — Update Compliance Record

**PUT** `/api/compliance/1`

Request:
```json
{
  "title": "GDPR Annual Review - Updated",
  "description": "Updated description",
  "status": "COMPLETED",
  "dueDate": "2025-12-31"
}
```

Expected Response — `200 OK`:
```json
{
  "id": 1,
  "title": "GDPR Annual Review - Updated",
  "status": "COMPLETED"
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 200 OK | |
| Title updated | Yes | |
| Status updated | `COMPLETED` | |
| updatedAt changed | Yes | |

---

### TC-17 — Soft Delete Record

**DELETE** `/api/compliance/1`

Expected Response — `204 No Content`

Then verify **GET** `/api/compliance/1` returns `404 Not Found`.

| Check | Expected | Pass/Fail |
|---|---|---|
| DELETE status code | 204 No Content | |
| GET after delete | 404 Not Found | |
| Record still in DB | Yes (isDeleted=true) | |
| Not in paginated list | Yes | |

---

### TC-18 — VIEWER Cannot Create Record

Login as ROLE_VIEWER, then **POST** `/api/compliance`

Expected Response — `403 Forbidden`:
```json
{
  "error": "Forbidden - insufficient permissions"
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 403 Forbidden | |
| Error message present | Yes | |

---

## Section 3 — File Upload & Download

### TC-19 — Upload Valid File

**POST** `/api/files/upload`

Form-data: `file = sample.pdf`

Expected Response — `201 Created`:
```json
{
  "id": 1,
  "originalName": "sample.pdf",
  "fileType": "application/pdf",
  "size": 12345,
  "uploadedAt": "2025-01-15T10:30:00",
  "downloadUrl": "http://localhost:8080/api/files/1"
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 201 Created | |
| id assigned | Non-null | |
| downloadUrl present | Yes | |
| File saved on disk | Yes | |

---

### TC-20 — Upload Invalid File Type

**POST** `/api/files/upload`

Form-data: `file = script.exe`

Expected Response — `400 Bad Request`:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "File type not allowed. Allowed types: PDF, DOCX, PNG, JPG"
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 400 Bad Request | |
| Message correct | Yes | |

---

### TC-21 — Download File by ID

**GET** `/api/files/1`

Expected Response — `200 OK`
- Content-Type: `application/pdf`
- Content-Disposition: `attachment; filename="sample.pdf"`

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 200 OK | |
| Content-Type correct | `application/pdf` | |
| File downloads correctly | Yes | |

---

### TC-22 — Download Non-Existent File

**GET** `/api/files/9999`

Expected Response — `404 Not Found`:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "File not found with id: 9999"
}
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Status code | 404 Not Found | |
| Message correct | Yes | |

---

## Section 4 — Redis Cache Validation

### TC-23 — Cache Miss on First Request

1. Restart app to clear cache
2. **GET** `/api/compliance?page=0&size=10`
3. Check logs for:
```
Cache MISS - fetching complianceRecords from DB for page: 0
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Log shows cache MISS | Yes | |
| Response time | ~50-200ms | |

---

### TC-24 — Cache Hit on Second Request

1. **GET** `/api/compliance?page=0&size=10` (second call)
2. No cache MISS log should appear
3. Response should be faster

| Check | Expected | Pass/Fail |
|---|---|---|
| No cache MISS log | Yes | |
| Response time faster | Yes (~5-20ms) | |

---

### TC-25 — Cache Eviction on Create

1. **GET** `/api/compliance` — cache populated
2. **POST** `/api/compliance` — create new record
3. **GET** `/api/compliance` — cache MISS again

Check logs for:
```
Cache evicted on createRecord - cache: complianceRecords, complianceById
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Cache evicted after create | Yes | |
| New record appears in list | Yes | |

---

## Section 5 — Database Validation

### TC-26 — Verify Record Stored in DB

```sql
SELECT * FROM compliance WHERE id = 1;
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Record exists | Yes | |
| is_deleted = false | Yes | |
| created_at populated | Yes | |

---

### TC-27 — Verify Soft Delete in DB

```sql
SELECT id, title, is_deleted FROM compliance WHERE id = 1;
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Record still exists | Yes | |
| is_deleted = true | Yes | |
| Not returned by API | Yes | |

---

### TC-28 — Verify Audit Log in DB

```sql
SELECT * FROM audit_log WHERE entity_type = 'Compliance' ORDER BY created_at DESC;
```

| Check | Expected | Pass/Fail |
|---|---|---|
| Audit log entry exists | Yes | |
| entity_type = Compliance | Yes | |
| created_at populated | Yes | |

---

## Section 6 — Swagger UI Validation

### TC-29 — Swagger UI Loads

Open: `http://localhost:8080/swagger-ui.html`

| Check | Expected | Pass/Fail |
|---|---|---|
| Page loads without error | Yes | |
| Title: Compliance Tracker API | Yes | |
| 3 tag groups visible | Compliance, Authentication, File Management | |
| All 11 endpoints visible | Yes | |
| Authorize button present | Yes (JWT Bearer) | |

---

### TC-30 — Test API via Swagger UI

1. Click **Authorize**
2. Enter: `Bearer <token>`
3. Execute **GET** `/api/compliance`

| Check | Expected | Pass/Fail |
|---|---|---|
| Authorization works | Yes | |
| Response shown in UI | Yes | |
| Schema examples visible | Yes | |

---

## Full Test Checklist Summary

| TC | Scenario | Pass/Fail |
|---|---|---|
| TC-01 | Register new user | |
| TC-02 | Register duplicate username | |
| TC-03 | Register invalid input | |
| TC-04 | Login valid credentials | |
| TC-05 | Login wrong password | |
| TC-06 | Access API without token | |
| TC-07 | Access API with invalid token | |
| TC-08 | Create compliance record | |
| TC-09 | Create with missing title | |
| TC-10 | Create with past due date | |
| TC-11 | Get all records paginated | |
| TC-12 | Get record by ID | |
| TC-13 | Get non-existent record | |
| TC-14 | Search records | |
| TC-15 | Get statistics | |
| TC-16 | Update record | |
| TC-17 | Soft delete record | |
| TC-18 | VIEWER cannot create | |
| TC-19 | Upload valid file | |
| TC-20 | Upload invalid file type | |
| TC-21 | Download file | |
| TC-22 | Download non-existent file | |
| TC-23 | Cache miss on first request | |
| TC-24 | Cache hit on second request | |
| TC-25 | Cache eviction on create | |
| TC-26 | Verify record in DB | |
| TC-27 | Verify soft delete in DB | |
| TC-28 | Verify audit log in DB | |
| TC-29 | Swagger UI loads | |
| TC-30 | Test via Swagger UI | |

---

## Bug Report Format

```
Bug ID     : BUG-XXX
Title      : [Short description]
Severity   : Critical / High / Medium / Low
Date Found : YYYY-MM-DD
Reported By: [Name]

Environment:
  OS      : Windows 11
  Java    : 17
  DB      : PostgreSQL 15
  Redis   : 7.x
  App URL : http://localhost:8080

Steps to Reproduce:
  1.
  2.
  3.

Expected Result:
  [What should happen]

Actual Result:
  [What actually happened]

Request:
  [HTTP method + endpoint + body]

Response:
  [Actual HTTP status + body]

Status: Open / Fixed / Closed
```

---

## Sample Bug Report

```
Bug ID     : BUG-001
Title      : 500 error returned instead of 401 for wrong password
Severity   : High
Date Found : 2025-01-15
Reported By: Developer Team

Steps to Reproduce:
  1. POST /auth/login
  2. Body: { "username": "testuser", "password": "wrongpass" }

Expected Result:
  HTTP 401 - "Invalid username or password"

Actual Result:
  HTTP 500 - Internal Server Error

Status: Fixed
```
