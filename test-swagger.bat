@echo off
echo Testing Swagger/OpenAPI endpoints...
echo.

echo 1. Testing /v3/api-docs endpoint:
curl -v http://localhost:8080/v3/api-docs
echo.
echo.

echo 2. Testing /swagger-ui.html endpoint:
curl -v http://localhost:8080/swagger-ui.html
echo.
echo.

echo 3. Testing debug public endpoint:
curl -v http://localhost:8080/api/debug/public
echo.
echo.

echo 4. Testing debug auth endpoint (should return 401):
curl -v http://localhost:8080/api/debug/auth
echo.
echo.

echo 5. Testing with Accept header for JSON:
curl -v -H "Accept: application/json" http://localhost:8080/v3/api-docs
echo.
echo.

echo 6. Testing with User-Agent header:
curl -v -H "User-Agent: Mozilla/5.0" http://localhost:8080/v3/api-docs
echo.
echo.

echo Testing completed.
pause 