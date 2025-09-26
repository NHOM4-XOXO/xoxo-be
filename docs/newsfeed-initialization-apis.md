# NewsFeed Initialization APIs

## 🎯 2 APIs riêng biệt cho User và Admin

### 👤 USER API - Tự khởi tạo

```http
POST /api/newsfeed/initialize
Authorization: Bearer user-jwt-token
```

**Mô tả:**
- User tự khởi tạo NewsFeed cho chính mình
- Lấy data từ 30 ngày qua
- Không cần quyền admin
- Chỉ khởi tạo cho chính user đang đăng nhập

**Request:**
```http
POST https://localhost:8443/api/newsfeed/initialize
Authorization: Bearer your-jwt-token
```

**Response:**
```json
{
  "statusCode": "200",
  "message": "Success",
  "data": {
    "message": "✅ Đã khởi tạo bảng tin thành công!",
    "userId": 6,
    "username": "nguyenhoangtuan",
    "analytics": {
      "totalItems": 20,
      "unseenItems": 20,
      "itemTypes": {
        "posts": 17,
        "friendships": 3
      }
    },
    "nextStep": "Gọi GET /api/newsfeed để xem bảng tin của bạn",
    "timestamp": "2024-09-26T12:00:00"
  }
}
```

### 🔧 ADMIN API - Khởi tạo cho user khác

```http
POST /api/newsfeed/admin/initialize-user/{userId}
Authorization: Bearer admin-jwt-token
```

**Mô tả:**
- Admin khởi tạo NewsFeed cho user được chỉ định
- Cần quyền admin
- Có thể khởi tạo cho bất kỳ user nào bằng userId
- Includes detailed logging và analytics

**Request:**
```http
POST https://localhost:8443/api/newsfeed/admin/initialize-user/123
Authorization: Bearer admin-jwt-token
```

**Response:**
```json
{
  "statusCode": "200", 
  "message": "Success",
  "data": {
    "message": "✅ Admin đã khởi tạo NewsFeed thành công!",
    "adminUserId": 1,
    "targetUserId": 123,
    "targetUsername": "johndoe",
    "targetUserName": "John Doe",
    "analytics": {
      "totalItems": 15,
      "unseenItems": 15,
      "itemTypes": {
        "posts": 12,
        "friendships": 3
      }
    },
    "timestamp": "2024-09-26T12:00:00"
  }
}
```

**Error Response (không có quyền):**
```json
{
  "statusCode": "200",
  "message": "Success", 
  "data": "❌ Chỉ admin mới có quyền khởi tạo NewsFeed cho user khác"
}
```

**Error Response (user không tồn tại):**
```json
{
  "statusCode": "200",
  "message": "Success",
  "data": "❌ User không tồn tại: 999"
}
```

### 🚀 ADMIN API - Bulk migration tất cả users

```http
POST /api/newsfeed/admin/populate-all-users
Authorization: Bearer admin-jwt-token
```

**Mô tả:**
- Admin khởi tạo NewsFeed cho TẤT CẢ users active
- One-time migration tool
- Progress tracking
- Error handling per user

**Response:**
```json
{
  "statusCode": "200",
  "message": "Success",
  "data": {
    "totalUsers": 50,
    "successCount": 48,
    "errorCount": 2,
    "errors": [
      "User 23 (invaliduser): User not found",
      "User 45 (disableduser): Database constraint error"
    ],
    "executionTimeMs": 45000,
    "averageTimePerUser": 937,
    "timestamp": "2024-09-26T12:05:00"
  }
}
```

## 🎯 Use Cases

### For Regular Users:
```javascript
// Khi user đăng ký hoặc đăng nhập lần đầu
fetch('/api/newsfeed/initialize', {
  method: 'POST',
  headers: {'Authorization': 'Bearer ' + userToken}
});

// Sau đó lấy NewsFeed
fetch('/api/newsfeed?page=0&size=20');
```

### For Admin Panel:
```javascript
// Khởi tạo cho 1 user cụ thể
fetch(`/api/newsfeed/admin/initialize-user/${userId}`, {
  method: 'POST', 
  headers: {'Authorization': 'Bearer ' + adminToken}
});

// Bulk migration cho tất cả users
fetch('/api/newsfeed/admin/populate-all-users', {
  method: 'POST',
  headers: {'Authorization': 'Bearer ' + adminToken}
});
```

## 🔐 Permission Matrix

| Endpoint | User Permission | Admin Permission | Description |
|----------|----------------|------------------|-------------|
| `POST /initialize` | ✅ Self only | ✅ Self only | User khởi tạo cho chính mình |
| `POST /admin/initialize-user/{id}` | ❌ Forbidden | ✅ Any user | Admin khởi tạo cho user khác |
| `POST /admin/populate-all-users` | ❌ Forbidden | ✅ All users | Bulk migration |

## 📋 Workflow Recommendations

### New User Onboarding:
1. User đăng ký → Auto call `POST /initialize` 
2. User đăng nhập → Check if NewsFeed exists
3. If empty → Call `POST /initialize`

### Admin Management:
1. Admin panel → List users without NewsFeed
2. Admin select user → `POST /admin/initialize-user/{id}`
3. Bulk operations → `POST /admin/populate-all-users`

### Production Deployment:
1. Deploy NewsFeed features
2. Run `POST /admin/populate-all-users` once
3. Enable auto-generation for new posts
4. Monitor analytics via admin endpoints

## 🔍 Testing

### Test User API:
```bash
curl -X POST https://localhost:8443/api/newsfeed/initialize \
  -H "Authorization: Bearer user-token"
```

### Test Admin API:
```bash
curl -X POST https://localhost:8443/api/newsfeed/admin/initialize-user/123 \
  -H "Authorization: Bearer admin-token"
```

### Expected Flow:
1. Call initialization API → Success
2. Call `GET /api/newsfeed` → See posts with media
3. Call `GET /api/newsfeed/analytics` → See statistics
