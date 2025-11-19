# Login App Backend - Spring Boot

## Setup Instructions

### 1. Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL Server 8.0+

### 2. Database Setup

Open MySQL and create the database:

```sql
CREATE DATABASE login_app;
USE login_app;
```

### 3. Configure Database

Edit `src/main/resources/application.properties` and update:
- `spring.datasource.username` (default: root)
- `spring.datasource.password` (your MySQL password)

### 4. Run the Application

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The server will start at `http://localhost:8080`

### 5. Test the API

**Register a new user:**
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

## API Endpoints

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login user

## Database Schema

The `users` table will be auto-created with:
- `id` (Primary Key, Auto Increment)
- `email` (Unique, Not Null)
- `password` (Not Null)
- `created_at` (Timestamp)

## Note
This is a basic implementation. For production:
- Use BCrypt for password hashing
- Implement JWT token authentication
- Add proper validation
- Add security configurations
