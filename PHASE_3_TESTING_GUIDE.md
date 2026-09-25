# Phase 3: Integration & Verification Guide

## Overview
This document guides you through end-to-end testing and verification of the User Registration and Document Storage implementation.

## Prerequisites
- PostgreSQL running locally (port 5432)
- Both services can reach the database
- Docker Desktop (LocalStack supplies local S3 emulation)
- Postman or curl for API testing

## Setup Steps

### 1. Database Setup
```bash
# Connect to PostgreSQL
psql -U postgres

# Create user service database
CREATE DATABASE sllp_user_db;

# Create document service database
CREATE DATABASE sllp_document_db;
```

### 2. Environment Configuration

**Create `.env` with:**
```env
DB_PASSWORD=postgres
JWT_SECRET=your-secret-key-change-in-production-min-64-chars-for-HS512
# LocalStack credentials and endpoints are supplied automatically by
# docker-compose.override.yaml. Do not add real AWS credentials for local testing.
```

### 3. Build Backend
```bash
mvn clean install
```

### 4. Start local S3 and run the storage integration test
```powershell
docker compose up -d localstack
$env:RUN_S3_INTEGRATION_TESTS='true'
mvn -pl sllp-document-service -am test
```

The test verifies upload, presigned download, and deletion against LocalStack with
signature validation enabled.

### 5. Run Services
```bash
docker compose up --build
```

## Test Scenarios

### Story 1: User Registration

#### Scenario 1.1: Successful Registration
```bash
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePassword123!"
  }'
```

**Expected Response (201 Created):**
```json
{
  "accessToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john@example.com",
  "name": "John Doe",
  "role": "STUDENT"
}
```

**Verification Checklist:**
- ✅ Response status is 201 Created
- ✅ JWT token is non-empty and valid
- ✅ User ID is UUID format
- ✅ Email matches request
- ✅ Role defaults to "STUDENT"
- ✅ User record created in database

#### Scenario 1.2: Duplicate Email Validation
```bash
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Jane Doe",
    "email": "john@example.com",
    "password": "AnotherPassword123!"
  }'
```

**Expected Response (409 Conflict):**
```json
{
  "code": "USER_ALREADY_EXISTS",
  "message": "User with email 'john@example.com' already exists"
}
```

#### Scenario 1.3: Invalid Email Format
```bash
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Invalid User",
    "email": "not-an-email",
    "password": "GoodPassword123!"
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "email: Email should be valid"
}
```

#### Scenario 1.4: Password Too Short
```bash
curl -X POST http://localhost:8082/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test User",
    "email": "test@example.com",
    "password": "short"
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "password: Password must be between 8 and 128 characters"
}
```

### Story 2: User Login

#### Scenario 2.1: Successful Login
```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePassword123!"
  }'
```

**Expected Response (200 OK):**
```json
{
  "accessToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "john@example.com",
  "name": "John Doe",
  "role": "STUDENT"
}
```

**Verification:**
- ✅ New JWT token generated
- ✅ Token differs from registration token
- ✅ User can use this token for authenticated endpoints

#### Scenario 2.2: Invalid Password
```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "WrongPassword123!"
  }'
```

**Expected Response (401 Unauthorized):**
```json
{
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid email or password"
}
```

#### Scenario 2.3: User Not Found
```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "notfound@example.com",
    "password": "AnyPassword123!"
  }'
```

**Expected Response (404 Not Found):**
```json
{
  "code": "USER_NOT_FOUND",
  "message": "User with email 'notfound@example.com' not found"
}
```

### Story 3: Get Current User (Requires Auth)

#### Scenario 3.1: Successful Get Current User
```bash
curl -X GET http://localhost:8082/api/auth/me \
  -H "Authorization: Bearer <JWT_TOKEN_FROM_LOGIN>"
```

**Expected Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "email": "john@example.com",
  "role": "STUDENT",
  "emailVerified": false,
  "createdAt": "2026-04-13T12:34:56Z"
}
```

#### Scenario 3.2: Missing Authorization Header
```bash
curl -X GET http://localhost:8082/api/auth/me
```

**Expected Response (401 Unauthorized)**

#### Scenario 3.3: Invalid JWT Token
```bash
curl -X GET http://localhost:8082/api/auth/me \
  -H "Authorization: Bearer invalid.token.here"
```

**Expected Response (401 Unauthorized)**

---

## Document Storage Tests

### Story 4: Upload Document

#### Scenario 4.1: Successful Upload
```bash
curl -X POST http://localhost:8089/api/documents/upload \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -F "file=@/path/to/document.pdf"
```

**Expected Response (201 Created):**
```json
{
  "document": {
    "id": "doc-550e8400",
    "filename": "document.pdf",
    "fileType": "pdf",
    "fileSize": 1024000,
    "status": "UPLOADED",
    "createdAt": "2026-04-13T12:34:56Z"
  },
  "downloadUrl": "https://s3.amazonaws.com/...<presigned-url>"
}
```

**Verification:**
- ✅ Response status is 201 Created
- ✅ Document ID is UUID format
- ✅ Original filename preserved
- ✅ File type correctly identified
- ✅ File size matches uploaded file
- ✅ Presigned URL is valid and accessible
- ✅ Document stored in database
- ✅ Document stored in S3

#### Scenario 4.2: Unsupported File Type
```bash
curl -X POST http://localhost:8089/api/documents/upload \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -F "file=@/path/to/malware.exe"
```

**Expected Response (400 Bad Request):**
```json
{
  "code": "FILE_UPLOAD_ERROR",
  "message": "File type not allowed. Supported types: PDF, DOCX, PPTX, TXT"
}
```

#### Scenario 4.3: File Too Large
```bash
# Create 60MB file
dd if=/dev/zero of=large.pdf bs=1M count=60

curl -X POST http://localhost:8089/api/documents/upload \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -F "file=@large.pdf"
```

**Expected Response (400 Bad Request):**
```json
{
  "code": "FILE_UPLOAD_ERROR",
  "message": "File size exceeds maximum allowed size of 50MB"
}
```

#### Scenario 4.4: Missing Authorization
```bash
curl -X POST http://localhost:8089/api/documents/upload \
  -F "file=@document.pdf"
```

**Expected Response (401 Unauthorized)**

#### Scenario 4.5: Path Traversal Attempt
```bash
# File named with path traversal attempt
curl -X POST http://localhost:8089/api/documents/upload \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -F "file=@../../../etc/passwd.txt"
```

**Expected Response (400 Bad Request):**
```json
{
  "code": "FILE_UPLOAD_ERROR",
  "message": "Filename contains forbidden characters"
}
```

### Story 5: List Documents

#### Scenario 5.1: Successful List
```bash
curl -X GET "http://localhost:8089/api/documents?page=0&size=20" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Expected Response (200 OK):**
```json
{
  "documents": [
    {
      "id": "doc-550e8400",
      "filename": "document.pdf",
      "fileType": "pdf",
      "fileSize": 1024000,
      "status": "UPLOADED",
      "createdAt": "2026-04-13T12:34:56Z"
    }
  ],
  "total": 1,
  "page": 0,
  "pageSize": 20,
  "hasMore": false
}
```

**Verification:**
- ✅ Only user's documents returned
- ✅ Pagination works correctly
- ✅ `hasMore` flag accurate
- ✅ Documents ordered by creation date (newest first)

#### Scenario 5.2: Empty List
```bash
# New user with no documents
curl -X GET "http://localhost:8089/api/documents?page=0&size=20" \
  -H "Authorization: Bearer <JWT_TOKEN_FOR_NEW_USER>"
```

**Expected Response (200 OK):**
```json
{
  "documents": [],
  "total": 0,
  "page": 0,
  "pageSize": 20,
  "hasMore": false
}
```

### Story 6: Get Document with Download URL

#### Scenario 6.1: Successful Get
```bash
curl -X GET http://localhost:8089/api/documents/doc-550e8400 \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Expected Response (200 OK):**
```json
{
  "document": {
    "id": "doc-550e8400",
    "filename": "document.pdf",
    "fileType": "pdf",
    "fileSize": 1024000,
    "status": "UPLOADED",
    "createdAt": "2026-04-13T12:34:56Z"
  },
  "downloadUrl": "https://s3.amazonaws.com/...<presigned-url>"
}
```

**Verification:**
- ✅ Presigned URL is accessible
- ✅ Can download file from URL
- ✅ Downloaded file matches original

#### Scenario 6.2: Document Not Found
```bash
curl -X GET http://localhost:8089/api/documents/non-existent-id \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Expected Response (404 Not Found):**
```json
{
  "code": "DOCUMENT_NOT_FOUND",
  "message": "Document with ID 'non-existent-id' not found or access denied"
}
```

#### Scenario 6.3: Access Control - Other User's Document
```bash
# User A owns doc-550e8400
# User B tries to access it
curl -X GET http://localhost:8089/api/documents/doc-550e8400 \
  -H "Authorization: Bearer <JWT_TOKEN_FOR_USER_B>"
```

**Expected Response (404 Not Found):**
```json
{
  "code": "DOCUMENT_NOT_FOUND",
  "message": "Document with ID 'doc-550e8400' not found or access denied"
}
```

**Verification:**
- ✅ Access denied to other users' documents
- ✅ No indication document exists for other user (security best practice)

### Story 7: Delete Document

#### Scenario 7.1: Successful Delete
```bash
curl -X DELETE http://localhost:8089/api/documents/doc-550e8400 \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Expected Response (204 No Content)**

**Verification:**
- ✅ No body in response
- ✅ Document removed from database
- ✅ File deleted from S3
- ✅ Subsequent GET request returns 404

#### Scenario 7.2: Delete Non-Existent Document
```bash
curl -X DELETE http://localhost:8089/api/documents/non-existent-id \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Expected Response (404 Not Found):**
```json
{
  "code": "DOCUMENT_NOT_FOUND",
  "message": "Document with ID 'non-existent-id' not found or access denied"
}
```

---

## Security Verification

### Auth Token Handling
- ✅ JWT tokens are cryptographically signed
- ✅ Token tampering detected (invalid signature)
- ✅ Expired tokens rejected
- ✅ Passwords never logged or exposed in errors
- ✅ Tokens not stored in HTTP-only cookies (localStorage only - frontend responsibility)

### File Upload Security
- ✅ File MIME type validated server-side
- ✅ Filename sanitized (path traversal blocked)
- ✅ File size enforced (50MB limit)
- ✅ Malicious file types rejected
- ✅ No script execution from uploaded files

### Access Control
- ✅ Users can only access their own documents
- ✅ Query by `userId + documentId` enforces ownership
- ✅ No directory traversal possible in S3
- ✅ Presigned URLs time-limited (1 hour)
- ✅ No hardcoded credentials in code

### Database
- ✅ Email unique constraint prevents duplicates
- ✅ Foreign key constraints maintain referential integrity
- ✅ Passwords hashed with BCrypt (not plaintext)
- ✅ S3 keys not accessible without authentication

---

## Performance Validation

### Latency Targets (Per Proposal)
| Operation | Target | Actual |
|-----------|--------|--------|
| Registration | < 2s | TBD |
| Login | < 2s | TBD |
| Document Upload | < 15s | TBD |
| Document List | < 1s | TBD |
| Get Document | < 1s | TBD |
| Delete Document | < 2s | TBD |

**Test Command:**
```bash
curl -w "\n%{time_total}s\n" -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePassword123!"
  }'
```

---

## Database Verification

### Users Table
```sql
-- Verify table exists
\dt users

-- Check data
SELECT id, name, email, role, active, email_verified, created_at FROM users LIMIT 5;

-- Verify email uniqueness
SELECT email, COUNT(*) FROM users GROUP BY email HAVING COUNT(*) > 1;
```

### Documents Table
```sql
-- Verify table exists
\dt documents

-- Check data
SELECT id, user_id, filename, file_type, file_size, status FROM documents LIMIT 5;

-- Verify foreign key cascade
DELETE FROM users WHERE id = '<test-user-id>';
SELECT COUNT(*) FROM documents WHERE user_id = '<test-user-id>';
-- Should be 0 after cascade
```

---

## Unit Test Execution

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthServiceTest

# Run with coverage
mvn test jacoco:report

# View coverage
open target/site/jacoco/index.html
```

---

## Integration Test Checklist

### User Registration Flow
- [ ] Successful registration with valid data
- [ ] Duplicate email rejected (409)
- [ ] Invalid email format rejected (400)
- [ ] Password too short rejected (400)
- [ ] Missing required fields rejected (400)
- [ ] User record created in database
- [ ] Password hashed correctly
- [ ] JWT token returned
- [ ] Email verification email triggered (stubbed)

### User Login Flow
- [ ] Successful login with correct password
- [ ] Invalid password rejected (401)
- [ ] User not found rejected (404)
- [ ] Inactive user rejected (401)
- [ ] JWT token can be used for protected endpoints

### Document Upload Flow
- [ ] Successful PDF upload
- [ ] Successful DOCX upload
- [ ] Successful PPTX upload
- [ ] Successful TXT upload
- [ ] Unsupported file type rejected (400)
- [ ] File too large rejected (400)
- [ ] Path traversal blocked (400)
- [ ] Document metadata saved to database
- [ ] File uploaded to S3
- [ ] Presigned URL generated and accessible
- [ ] Missing auth header rejected (401)
- [ ] Invalid token rejected (401)

### Document List Flow
- [ ] Only user's documents returned
- [ ] Pagination works (page, size, hasMore)
- [ ] Default page size applied
- [ ] Documents ordered by creation date (newest first)
- [ ] Empty list returned for new user

### Document Retrieval Flow
- [ ] Get document by ID succeeds
- [ ] Presigned URL is accessible
- [ ] Non-existent document returns 404
- [ ] Other user's document returns 404 (no ownership leak)
- [ ] Downloaded file matches original

### Document Deletion Flow
- [ ] Delete document succeeds (204)
- [ ] Document removed from database
- [ ] File deleted from S3
- [ ] Subsequent GET returns 404
- [ ] Non-existent document returns 404

---

## Troubleshooting

### Service Won't Start
```bash
# Check port conflict
lsof -i :8082
lsof -i :8089

# Check database connection
psql -U postgres -d sllp_user_db

# View logs
tail -f target/sllp-user-service.log
```

### Database Migration Fails
```bash
# Check Liquibase status
# Verify changeset XML syntax
# Ensure databases exist
psql -U postgres -l | grep sllp
```

### JWT Validation Fails
- Verify JWT_SECRET is set and >32 characters
- Ensure secret is consistent across services
- Check token expiry (15 minutes for access tokens)

### S3 Upload Fails
- Verify AWS credentials
- Check bucket exists and permissions
- For local testing, use LocalStack

---

## Sign-Off Checklist

- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Security tests pass
- [ ] Latency targets met
- [ ] Database schema verified
- [ ] API documentation complete (Swagger UI)
- [ ] No hardcoded secrets in code
- [ ] Error messages don't leak sensitive info
- [ ] JWT tokens validated on all protected endpoints
- [ ] File uploads validated server-side
- [ ] Access control enforced (users can't access others' data)
- [ ] Ready for Phase 4: Deployment
