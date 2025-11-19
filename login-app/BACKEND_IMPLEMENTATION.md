# Backend Implementation Guide - Task 2
## Three-Attempt Password Rule with Account Locking

This document provides the complete backend implementation for the login validation with a three-attempt password rule.

---

## Requirements
1. Validate login credentials (username, password, NIC)
2. Track failed login attempts
3. Lock account after 3 consecutive failed attempts
4. Display appropriate error messages

---

## Implementation Approach

### 1. Database Schema Updates

You need to add two fields to your User model/table:

```java
// Add these fields to your User entity/model
private int failedLoginAttempts = 0;
private boolean accountLocked = false;
```

**SQL Migration (if using SQL database):**
```sql
ALTER TABLE users ADD COLUMN failed_login_attempts INT DEFAULT 0;
ALTER TABLE users ADD COLUMN account_locked BOOLEAN DEFAULT FALSE;
```

---

### 2. Backend Controller Implementation

#### Java Spring Boot Example:

**AuthController.java**
```java
package com.yourapp.controller;

import com.yourapp.dto.LoginRequest;
import com.yourapp.dto.LoginResponse;
import com.yourapp.model.User;
import com.yourapp.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.authenticateUser(
                loginRequest.getUsername(),
                loginRequest.getPassword(),
                loginRequest.getNicNumber()
            );
            return ResponseEntity.ok(response);
        } catch (AccountLockedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage()));
        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An error occurred during login"));
        }
    }
}
```

**LoginRequest.java (DTO)**
```java
package com.yourapp.dto;

public class LoginRequest {
    private String username;
    private String password;
    private String nicNumber;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNicNumber() {
        return nicNumber;
    }

    public void setNicNumber(String nicNumber) {
        this.nicNumber = nicNumber;
    }
}
```

---

### 3. Service Layer Implementation

**AuthService.java**
```java
package com.yourapp.service;

import com.yourapp.dto.LoginResponse;
import com.yourapp.exception.AccountLockedException;
import com.yourapp.exception.InvalidCredentialsException;
import com.yourapp.model.User;
import com.yourapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 3;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider; // If using JWT

    @Transactional
    public LoginResponse authenticateUser(String username, String password, String nicNumber) 
            throws AccountLockedException, InvalidCredentialsException {
        
        // Find user by username
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        // Check if account is locked
        if (user.isAccountLocked()) {
            throw new AccountLockedException(
                "Account locked due to multiple failed login attempts. Please contact administrator."
            );
        }

        // Validate password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            handleFailedLogin(user);
            int remainingAttempts = MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts();
            
            if (user.isAccountLocked()) {
                throw new AccountLockedException(
                    "Account locked due to multiple failed login attempts. Please contact administrator."
                );
            }
            
            throw new InvalidCredentialsException(
                "Invalid credentials. " + remainingAttempts + " attempt(s) remaining."
            );
        }

        // Validate NIC (optional - depending on your requirements)
        if (user.getNicNumber() != null && !user.getNicNumber().equals(nicNumber)) {
            handleFailedLogin(user);
            int remainingAttempts = MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts();
            
            if (user.isAccountLocked()) {
                throw new AccountLockedException(
                    "Account locked due to multiple failed login attempts. Please contact administrator."
                );
            }
            
            throw new InvalidCredentialsException(
                "Invalid credentials. " + remainingAttempts + " attempt(s) remaining."
            );
        }

        // Successful login - reset failed attempts
        resetFailedAttempts(user);

        // Generate token
        String token = jwtTokenProvider.generateToken(user);

        return new LoginResponse(token, "Login successful", user.getUsername());
    }

    private void handleFailedLogin(User user) {
        int newFailedAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newFailedAttempts);

        if (newFailedAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
        }

        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        userRepository.save(user);
    }
}
```

---

### 4. Custom Exception Classes

**AccountLockedException.java**
```java
package com.yourapp.exception;

public class AccountLockedException extends Exception {
    public AccountLockedException(String message) {
        super(message);
    }
}
```

**InvalidCredentialsException.java**
```java
package com.yourapp.exception;

public class InvalidCredentialsException extends Exception {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
```

---

### 5. Response DTOs

**LoginResponse.java**
```java
package com.yourapp.dto;

public class LoginResponse {
    private String token;
    private String message;
    private String username;

    public LoginResponse(String token, String message, String username) {
        this.token = token;
        this.message = message;
        this.username = username;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
```

**ErrorResponse.java**
```java
package com.yourapp.dto;

public class ErrorResponse {
    private String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
```

---

### 6. User Entity Updates

**User.java**
```java
package com.yourapp.model;

import javax.persistence.*;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "nic_number")
    private String nicNumber;
    
    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;
    
    @Column(name = "account_locked")
    private boolean accountLocked = false;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNicNumber() {
        return nicNumber;
    }

    public void setNicNumber(String nicNumber) {
        this.nicNumber = nicNumber;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }
}
```

---

## Testing the Implementation

### Test Cases:

1. **Valid Login**: Should succeed and reset failed attempts
2. **First Failed Attempt**: Should show "2 attempts remaining"
3. **Second Failed Attempt**: Should show "1 attempt remaining"
4. **Third Failed Attempt**: Should lock account and show lock message
5. **Subsequent Attempts**: Should show account locked message

### Example Test with cURL:

```bash
# First failed attempt
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"wrongpass","nicNumber":"123456789012"}'

# Expected Response (after 3rd attempt):
# {"message":"Account locked due to multiple failed login attempts. Please contact administrator."}
```

---

## Key Features Implemented:

✅ Track failed login attempts per user
✅ Lock account after 3 consecutive failures
✅ Display remaining attempts to user
✅ Clear error messages for locked accounts
✅ Reset attempts on successful login
✅ NIC validation support
✅ Proper exception handling
✅ Transaction management for data consistency

---

## Additional Enhancements (Optional):

1. **Auto-unlock after time period**: Add timestamp and unlock after 30 minutes
2. **Email notification**: Send email when account is locked
3. **Admin unlock functionality**: Allow admins to manually unlock accounts
4. **IP-based tracking**: Track attempts per IP address
5. **Audit logging**: Log all login attempts for security analysis

---

## Notes:

- Remember to update your database schema before deploying
- Test thoroughly with different scenarios
- Ensure password encryption is properly configured
- Add proper CORS configuration for frontend integration
- Consider implementing JWT for token-based authentication
