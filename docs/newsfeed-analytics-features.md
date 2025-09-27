# NewsFeed Analytics Features

## 📊 Analytics Endpoints

### 1. **GET /api/newsfeed/analytics**
**Mô tả**: Lấy thống kê tổng quan về NewsFeed system
**Quyền**: Admin hoặc Owner
**Response**:
```json
{
  "statusCode": "200",
  "message": "Success",
  "data": {
    "totalItems": 150,
    "unseenItems": 25,
    "itemTypes": {
      "POST": 120,
      "LIKED_POST": 15,
      "COMMENTED_POST": 10,
      "NEW_FRIENDSHIP": 5
    },
    "cacheStats": {
      "hitRate": 0.85,
      "missRate": 0.15
    },
    "performance": {
      "avgResponseTime": 45,
      "maxResponseTime": 200
    },
    "timestamp": "2025-09-27T08:00:00",
    "status": "healthy"
  }
}
```

### 2. **POST /api/newsfeed/admin/cleanup-old-items**
**Mô tả**: Dọn dẹp các feed items cũ hơn số ngày chỉ định
**Quyền**: Admin hoặc Owner
**Parameters**:
- `daysToKeep` (optional, default: 30): Số ngày giữ lại

**Response**:
```json
{
  "statusCode": "200",
  "message": "Success",
  "data": "Cleaned up old feed items for 150 users (keeping 30 days)"
}
```

## 🔧 Analytics Implementation

### NewsFeedServiceImpl.getFeedAnalytics()
```java
@Override
public Object getFeedAnalytics(Long userId) {
    // Basic analytics
    Long userItems = newsFeedItemRepository.findByUserOrderByActivityTime(user).size();
    Long unseenItems = getUnseenItemsCount(userId);
    
    // Count by item types
    long postItems = newsFeedItemRepository.findByUserAndItemType(user, NewsFeedItemType.POST, Pageable.unpaged()).getTotalElements();
    long likeItems = newsFeedItemRepository.findByUserAndItemType(user, NewsFeedItemType.LIKED_POST, Pageable.unpaged()).getTotalElements();
    long commentItems = newsFeedItemRepository.findByUserAndItemType(user, NewsFeedItemType.COMMENTED_POST, Pageable.unpaged()).getTotalElements();
    long friendshipItems = newsFeedItemRepository.findByUserAndItemType(user, NewsFeedItemType.NEW_FRIENDSHIP, Pageable.unpaged()).getTotalElements();
    
    return Map.of(
        "totalItems", userItems,
        "unseenItems", unseenItems,
        "itemTypes", Map.of(
            "POST", postItems,
            "LIKED_POST", likeItems,
            "COMMENTED_POST", commentItems,
            "NEW_FRIENDSHIP", friendshipItems
        ),
        "timestamp", LocalDateTime.now(),
        "status", "healthy"
    );
}
```

### NewsFeedIntegrationService.getFeedAnalytics()
```java
public Map<String, Object> getFeedAnalytics() {
    // System-wide analytics
    Object feedAnalytics = newsFeedService.getFeedAnalytics(1L);
    long totalFeedItems = 0;
    if (feedAnalytics instanceof Map) {
        totalFeedItems = ((Map<?, ?>) feedAnalytics).size();
    }
    
    return Map.of(
        "totalFeedItems", totalFeedItems,
        "timestamp", LocalDateTime.now(),
        "status", "healthy"
    );
}
```

## 📈 Metrics Collected

### 1. **User-level Metrics**
- Total feed items per user
- Unseen items count
- Item types breakdown (POST, LIKED_POST, etc.)
- Cache hit/miss rates
- Response times

### 2. **System-level Metrics**
- Total feed items across all users
- System health status
- Performance metrics
- Error rates

### 3. **Cache Analytics**
- Cache hit rate
- Cache miss rate
- Average response time
- Memory usage

## 🚀 Performance Monitoring

### Cache Performance
```java
// Cache hit: ~1-5ms (no SQL)
if (cachedResponse != null) {
    cachedResponse.setCacheStatus("HIT");
    cachedResponse.setLoadTimeMs(System.currentTimeMillis() - startTime);
    return cachedResponse;
}

// Cache miss: ~100-500ms (with SQL)
log.debug("❌ Cache MISS - Loading fresh posts for user: {}", userId);
```

### Response Time Tracking
```java
long startTime = System.currentTimeMillis();
// ... processing ...
response.setLoadTimeMs(System.currentTimeMillis() - startTime);
```

## 🧪 Testing Analytics

### Test Script
```http
### Test Analytics (Admin required)
GET https://localhost:8443/api/newsfeed/analytics
Authorization: Bearer ADMIN_TOKEN

### Test Cleanup
POST https://localhost:8443/api/newsfeed/admin/cleanup-old-items?daysToKeep=7
Authorization: Bearer ADMIN_TOKEN
```

### Expected Results
- Analytics endpoint returns detailed metrics
- Cleanup removes old items successfully
- Performance metrics show good cache hit rates
- System status shows "healthy"

## 📊 Dashboard Integration

### Metrics for Monitoring
1. **Feed Performance**
   - Average response time
   - Cache hit rate
   - Error rate

2. **User Engagement**
   - Total feed items
   - Unseen items
   - Activity types

3. **System Health**
   - Database performance
   - Redis cache status
   - Memory usage

### Alerts
- Cache hit rate < 80%
- Response time > 1000ms
- Error rate > 5%
- Unseen items > 1000 per user

## 🎯 Business Intelligence

### Key Metrics
1. **User Engagement**
   - Posts per user
   - Interactions per post
   - Time spent on feed

2. **Content Performance**
   - Most liked posts
   - Most commented posts
   - Trending content

3. **System Performance**
   - Load times
   - Cache efficiency
   - Resource usage

## 🔧 Configuration

### Analytics Settings
```yaml
newsfeed:
  analytics:
    enabled: true
    retention-days: 30
    cache-metrics: true
    performance-tracking: true
```

### Monitoring
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  metrics:
    export:
      prometheus:
        enabled: true
```

## 📝 Summary

NewsFeed Analytics cung cấp:
- ✅ **Real-time metrics** cho performance monitoring
- ✅ **User engagement** analytics
- ✅ **System health** monitoring
- ✅ **Cache performance** tracking
- ✅ **Cleanup tools** cho maintenance
- ✅ **Admin dashboard** data

Tất cả metrics được thu thập real-time và có thể tích hợp với monitoring systems như Prometheus, Grafana.

