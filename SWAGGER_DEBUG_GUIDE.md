# Swagger/OpenAPI 401 Error Debug Guide

## Issue
You're getting a 401 Unauthorized error when trying to access `/v3/api-docs` endpoint.

## Possible Causes and Solutions

### 1. Security Configuration
The security configuration has been updated to explicitly allow:
- `/v3/api-docs`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

### 2. SpringDoc Configuration
Added explicit enablement in `application.properties`:
```properties
springdoc.api-docs.enabled=true
springdoc.swagger-ui.enabled=true
```

### 3. Debug Steps

#### Step 1: Test the Application
1. Start your application
2. Run the `test-swagger.bat` script to test various endpoints
3. Check the console logs for any security-related messages

#### Step 2: Check Browser Access
1. Open your browser and go to: `http://localhost:8080/swagger-ui.html`
2. Try accessing: `http://localhost:8080/v3/api-docs` directly
3. Check browser developer tools for any errors

#### Step 3: Test with curl
```bash
# Test basic access
curl -v http://localhost:8080/v3/api-docs

# Test with JSON accept header
curl -v -H "Accept: application/json" http://localhost:8080/v3/api-docs

# Test with different user agent
curl -v -H "User-Agent: Mozilla/5.0" http://localhost:8080/v3/api-docs
```

#### Step 4: Check Debug Endpoints
1. Test public endpoint: `http://localhost:8080/api/debug/public`
2. Test auth endpoint: `http://localhost:8080/api/debug/auth` (should return 401)
3. Test security info: `http://localhost:8080/api/debug/security-info` (requires auth)

### 4. Common Issues and Fixes

#### Issue: JWT Filter Interference
- The JWT filter should not block requests without tokens
- Check if there are any exceptions in the JWT filter logs

#### Issue: CORS Configuration
- CORS is configured to allow all origins
- Check if CORS is causing issues with browser requests

#### Issue: SpringDoc Version Compatibility
- Using SpringDoc 2.3.0 which is compatible with Spring Boot 3.5.3
- If issues persist, try downgrading to SpringDoc 2.2.0

### 5. Alternative Solutions

#### Option 1: Disable Security for OpenAPI (Development Only)
If the issue persists, you can temporarily disable security for OpenAPI endpoints:

```java
.authorizeHttpRequests(authz -> authz
    .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
    .anyRequest().authenticated())
```

#### Option 2: Use Different OpenAPI Path
Change the OpenAPI path in `application.properties`:
```properties
springdoc.api-docs.path=/api-docs
```

#### Option 3: Disable SpringDoc Temporarily
Comment out the SpringDoc dependency in `pom.xml` to test if it's a SpringDoc issue.

### 6. Logging
Enhanced logging has been added to help debug:
- `logging.level.org.springframework.security=DEBUG`
- `logging.level.org.springframework.web=DEBUG`

Check the console output for any security-related messages when accessing `/v3/api-docs`.

### 7. Expected Behavior
- `/v3/api-docs` should return JSON OpenAPI specification
- `/swagger-ui.html` should show the Swagger UI interface
- `/api/debug/public` should return "Public endpoint is accessible"
- `/api/debug/auth` should return 401 (requires authentication)

### 8. Next Steps
1. Run the test script and check the results
2. Check the application logs for any error messages
3. Try accessing the endpoints in a browser
4. If the issue persists, check if there are any custom filters or interceptors

## Files Modified
- `SecurityConfig.java` - Updated security configuration
- `application.properties` - Added SpringDoc configuration and logging
- `DebugController.java` - Added debug endpoints
- `test-swagger.bat` - Created test script 