# NewsFeed Migration Guide - Populate từ Data Cũ

## 🎯 Vấn đề
Bạn đã có posts và friendships trong database, nhưng NewsFeed trống vì feature này được tạo sau. Cần populate NewsFeed từ data hiện có.

## ✅ Giải pháp đã cập nhật

### 1. Enhanced `generateNewsFeedForUser()` 
Đã được cải tiến để:
- ✅ Clear existing feed items trước
- ✅ Lấy posts từ friends (30 ngày thay vì 7 ngày)
- ✅ Fallback sang public posts nếu không đủ content
- ✅ Include friendship activities
- ✅ Detailed logging để track progress

### 2. New Migration Endpoint
```http
POST /api/newsfeed/admin/populate-from-existing-data
```

### 3. Enhanced PostRepository
Thêm method:
```java
List<Post> findTop20ByIsPublicTrueAndStatusOrderByCreatedAtDesc(PostStatus status);
```

## 🚀 Cách sử dụng

### Option 1: Populate từ Data Cũ (RECOMMENDED)
```http
POST https://localhost:8443/api/newsfeed/admin/populate-from-existing-data
Authorization: Bearer your-jwt-token
```

**Expected response:**
```json
{
  "statusCode": "200",
  "message": "Success", 
  "data": "✅ Đã populate NewsFeed từ data cũ thành công! NewsFeed sẽ bao gồm: posts từ bạn bè (30 ngày), posts public, và friendships gần đây. Analytics: {totalItems=15, unseenItems=15, ...}"
}
```

### Option 2: Standard Generate (cũng đã cải tiến)
```http
POST https://localhost:8443/api/newsfeed/generate
Authorization: Bearer your-jwt-token
```

## 📊 Logic Populate mới

### 1. Clear Existing Feed
- Xóa tất cả feed items cũ của user
- Đảm bảo fresh start

### 2. Friends' Posts (30 ngày)
```java
LocalDateTime postsSince = LocalDateTime.now().minusDays(30);
List<Post> friendsPosts = postRepository.findRecentPostsByUsers(friends, postsSince);
```

### 3. Public Posts Fallback
Nếu ít hơn 10 posts từ friends:
```java
List<Post> publicPosts = postRepository.findTop20ByIsPublicTrueAndStatusOrderByCreatedAtDesc(ACTIVE);
// Priority thấp hơn 30% so với friends' posts
priorityScore = priorityScore * 0.7;
```

### 4. Friendship Activities
```java
LocalDateTime friendshipSince = LocalDateTime.now().minusDays(30);
// Include recent friendships
```

## 📈 Kết quả mong đợi

### Trước khi populate:
```json
{
  "data": {
    "items": [],
    "totalElements": 0,
    "unseenCount": 0
  }
}
```

### Sau khi populate:
```json
{
  "data": {
    "items": [
      {
        "id": 1,
        "itemType": "POST",
        "actor": {
          "firstName": "Friend",
          "lastName": "Name"
        },
        "post": {
          "post": {
            "content": "Actual post content from database",
            "createdAt": "2024-09-20T..."
          },
          "media": [...]
        },
        "priorityScore": 7.2,
        "timeAgo": "3 ngày trước",
        "displayText": "Friend Name đã đăng một bài viết mới"
      }
    ],
    "totalElements": 25,
    "unseenCount": 25,
    "cacheStatus": "MISS"
  }
}
```

## 🔍 Debug & Monitoring

### Check Logs
Service sẽ log chi tiết:
```
Found 8 posts from 3 friends in last 30 days
Not enough posts from friends, adding public posts for user: 123
Generated 15 feed items for user: 123 (friends: 8, public: 5, friendships: 2)
```

### Analytics Endpoint
```http
GET /api/newsfeed/analytics
```

Response:
```json
{
  "data": {
    "totalItems": 15,
    "unseenItems": 15,
    "itemTypes": {
      "posts": 13,
      "friendships": 2
    },
    "lastGenerated": "2024-09-26T12:00:00"
  }
}
```

## 🔄 Migration Workflow

1. **Check current state:**
   ```http
   GET /api/newsfeed → should be empty
   ```

2. **Populate from existing data:**
   ```http
   POST /api/newsfeed/admin/populate-from-existing-data
   ```

3. **Verify results:**
   ```http
   GET /api/newsfeed → should have content
   GET /api/newsfeed/analytics → check counts
   ```

4. **Test pagination:**
   ```http
   GET /api/newsfeed?page=0&size=5
   GET /api/newsfeed?page=1&size=5
   ```

5. **Test cache:**
   ```http
   GET /api/newsfeed → cacheStatus: "HIT" 
   ```

## ⚠️ Important Notes

- **30 ngày window**: Lấy posts và friendships trong 30 ngày qua
- **Priority scoring**: Friends' posts > Public posts  
- **No duplicates**: Tránh trùng lặp posts
- **Media included**: Mỗi post sẽ có media đầy đủ
- **Cache clear**: Auto clear cache sau populate

## 🎉 Kết quả

Sau migration, bạn sẽ có:
- ✅ NewsFeed đầy đủ từ data cũ
- ✅ Posts với media hiển thị đúng
- ✅ Priority scoring hợp lý
- ✅ Cache hoạt động tối ưu
- ✅ Analytics để monitor

**Chạy ngay endpoint populate để có NewsFeed từ data hiện có!** 🚀

