# NewsFeed Setup & Testing Guide

## Tình huống hiện tại
API NewsFeed đã hoạt động (`200 OK`) nhưng trả về empty data:
```json
{
  "data": {
    "items": [],
    "totalElements": 0,
    "unseenCount": 0,
    "cacheStatus": "MISS",
    "loadTimeMs": 759
  }
}
```

Điều này bình thường vì:
1. User chưa có bạn bè → không có posts từ friends
2. Chưa có feed items nào được tạo
3. Database newsfeed tables còn trống

## Cách tạo test data

### Bước 1: Generate feed ban đầu
```http
POST https://localhost:8443/api/newsfeed/generate
Authorization: Bearer your-jwt-token
```

**Expected response:**
```json
{
  "statusCode": "200", 
  "message": "Success",
  "data": "Đã tạo bảng tin ban đầu thành công"
}
```

### Bước 2: Kiểm tra lại feed
```http
GET https://localhost:8443/api/newsfeed?page=0&size=20
Authorization: Bearer your-jwt-token
```

Nếu vẫn empty, có nghĩa là:
- User chưa có friends
- Friends chưa có posts trong 7 ngày qua

### Bước 3: Tạo test data thủ công

#### 3.1. Tạo friendship trước
```http
# Gửi friend request
POST https://localhost:8443/api/friendships
Content-Type: application/json
{
  "friendId": 2  // ID của user khác
}

# Accept friend request (từ user khác)
POST https://localhost:8443/api/friendships/{friendshipId}/accept
```

#### 3.2. Tạo post mới
```http
POST https://localhost:8443/api/posts
Content-Type: application/json
{
  "content": "Hello world! This is my first post 🎉",
  "isPublic": true,
  "hashtags": "#hello #test #newsfeed"
}
```

#### 3.3. Generate feed lại
```http
POST https://localhost:8443/api/newsfeed/generate
```

## Debug thông tin

### Kiểm tra user có friends không:
```http
GET https://localhost:8443/api/friendships/friends
```

### Kiểm tra posts gần đây:
```http
GET https://localhost:8443/api/posts/public
```

### Kiểm tra analytics:
```http
GET https://localhost:8443/api/newsfeed/analytics
```

**Expected analytics response:**
```json
{
  "data": {
    "totalItems": 5,
    "unseenItems": 3,
    "itemTypes": {
      "posts": 2,
      "likes": 1,
      "comments": 1,
      "friendships": 1
    },
    "lastGenerated": "2024-09-26T11:40:00",
    "cacheStatus": "CALCULATED"
  }
}
```

## Tạo feed items thủ công (for testing)

Nếu cần tạo feed items ngay lập tức, có thể thêm endpoint test:

```java
// Thêm vào NewsFeedController
@PostMapping("/test/create-sample-data")
public ResponseEntity<WrapRes<String>> createSampleFeedData(Principal principal) {
    try {
        User currentUser = getCurrentUser(principal);
        
        // Tạo sample feed items
        newsFeedService.addFeedItem(
            currentUser.getId(),
            currentUser, // Actor
            NewsFeedItemType.POST,
            null, // Post - có thể null cho test
            null, // Group
            null, // Target user
            "Sample test data"
        );
        
        return ResponseEntity.ok(WrapRes.success("Đã tạo sample data"));
    } catch (Exception e) {
        return ResponseEntity.ok(WrapRes.error("Lỗi tạo sample data: " + e.getMessage()));
    }
}
```

## Workflow testing đầy đủ

1. **Setup users & friendships:**
   - Tạo ít nhất 2 users
   - Tạo friendship giữa họ

2. **Create content:**
   - User A tạo posts
   - User B like/comment posts của A

3. **Generate feeds:**
   - Gọi `/generate` cho cả 2 users
   - Check feeds của cả 2

4. **Test interactions:**
   - Mark items as seen
   - Check unseen count
   - Test pagination

5. **Test cache:**
   - Gọi API 2 lần → lần 2 sẽ có `"cacheStatus": "HIT"`
   - Clear cache và test lại

## Kết quả mong đợi

Sau khi có data, response sẽ như:
```json
{
  "data": {
    "items": [
      {
        "id": 1,
        "itemType": "POST", 
        "actor": {...},
        "post": {
          "post": {...},
          "media": [...]
        },
        "priorityScore": 5.2,
        "isSeen": false,
        "timeAgo": "2 giờ trước",
        "displayText": "John Doe đã đăng một bài viết mới"
      }
    ],
    "totalElements": 10,
    "unseenCount": 7,
    "cacheStatus": "HIT"
  }
}
```

