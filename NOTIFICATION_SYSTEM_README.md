# XOXO Notification System - Dual Storage Architecture

## Tổng quan

Hệ thống notification của XOXO sử dụng kiến trúc dual-storage với MySQL và MongoDB, kết hợp với Kafka để xử lý async. Điều này đảm bảo cả tính persistent và real-time performance.

## Kiến trúc hệ thống

```
User Action → Service → NotificationService → MySQL (Persistent) + Kafka → MongoDB (Real-time)
                ↓
        Frontend queries from both sources
```

### Components

1. **MySQL (Persistent Storage)**
   - Lưu trữ notifications lâu dài
   - ACID compliance
   - Backup và recovery dễ dàng
   - Phù hợp cho phân trang, thống kê

2. **MongoDB (Real-time Storage)**
   - Lưu trữ notifications real-time
   - Schema flexible
   - Hiệu năng cao cho read operations
   - TTL index để tự động cleanup

3. **Kafka (Message Queue)**
   - Async processing
   - Fault tolerance
   - Scalability
   - Dead Letter Topics cho error handling

## Luồng hoạt động

### 1. Tạo Notification

```java
// Khi user thực hiện action (like, comment, share)
notificationService.sendPostLikeNotification(postId, postOwnerId, likerId);
```

**NotificationService.createNotification()**:
- Lưu vào MySQL (persistent)
- Gửi message qua Kafka
- Kafka Consumer lưu vào MongoDB (real-time)

### 2. Truy vấn Notifications

#### MySQL (Persistent Storage)
```java
// Lấy notifications cũ, phân trang, thống kê
GET /api/v1/notifications/mysql?page=0&size=20
GET /api/v1/notifications/mysql/unread/count
GET /api/v1/notifications/mysql/unread
```

#### MongoDB (Real-time Storage)
```java
// Lấy notifications real-time, hiển thị ngay lập tức
GET /api/v1/notifications/mongo?page=0&size=20
GET /api/v1/notifications/mongo/unread/count
GET /api/v1/notifications/mongo/unread
GET /api/v1/notifications/mongo/type/{type}
```

## API Endpoints

### MySQL Notifications
- `GET /api/v1/notifications/mysql` - Lấy notifications với phân trang
- `GET /api/v1/notifications/mysql/unread/count` - Đếm notifications chưa đọc
- `GET /api/v1/notifications/mysql/unread` - Lấy notifications chưa đọc
- `PUT /api/v1/notifications/mysql/{id}/read` - Đánh dấu đã đọc
- `PUT /api/v1/notifications/mysql/read-all` - Đánh dấu tất cả đã đọc
- `DELETE /api/v1/notifications/mysql/{id}` - Xóa notification

### MongoDB Notifications
- `GET /api/v1/notifications/mongo` - Lấy notifications real-time
- `GET /api/v1/notifications/mongo/unread/count` - Đếm notifications chưa đọc
- `GET /api/v1/notifications/mongo/unread` - Lấy notifications chưa đọc
- `GET /api/v1/notifications/mongo/type/{type}` - Lấy theo loại
- `PUT /api/v1/notifications/mongo/{id}/read` - Đánh dấu đã đọc
- `DELETE /api/v1/notifications/mongo/{id}` - Xóa notification

## Services

### 1. NotificationService
- Xử lý business logic cho notifications
- Tích hợp với MySQL và Kafka
- Gửi notifications cho các events: like, comment, share, friend request

### 2. RealTimeNotificationService
- Xử lý notifications real-time từ MongoDB
- Cung cấp các phương thức truy vấn nhanh
- Quản lý trạng thái đã đọc

### 3. NotificationSyncService
- Đồng bộ dữ liệu giữa MySQL và MongoDB
- Scheduled tasks để maintain data consistency
- Cleanup old notifications

## Kafka Topics

- `notifications` - Main topic cho notifications
- `notifications.DLT` - Dead Letter Topic cho failed messages
- `mail-topic` - Topic cho email notifications
- `mail-topic.DLT` - Dead Letter Topic cho email

## Cấu hình

### application.properties
```properties
# Kafka
spring.kafka.bootstrap-servers=localhost:29092
notification.topic=notifications

# MongoDB
spring.data.mongodb.uri=mongodb://admin:password123@localhost:27017/xoxo?authSource=admin

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/xoxo
```

### Docker Compose
```yaml
services:
  kafka:
    image: confluentinc/cp-kafka:7.4.0
    ports:
      - "9092:9092"
      - "29092:29092"
  
  mongodb:
    image: mongo:7.0
    ports:
      - "27017:27017"
  
  mysql:
    image: mysql:latest
    ports:
      - "3306:3306"
```

## Scheduled Tasks

### NotificationSyncService
- **Full Sync**: Mỗi giờ - Đồng bộ tất cả notifications từ MySQL sang MongoDB
- **Read Status Sync**: Mỗi 5 phút - Đồng bộ trạng thái đã đọc từ MongoDB về MySQL
- **Cleanup**: Mỗi ngày 2h sáng - Xóa notifications cũ (TTL index)
- **Consistency Check**: Mỗi 6 giờ - Kiểm tra tính nhất quán dữ liệu

## Error Handling

### Kafka Retry Configuration
```java
@Configuration
public class KafkaRetryConfig {
    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate),
            new FixedBackOff(3000, 5) // 3s delay, 5 attempts
        );
    }
}
```

### Dead Letter Topics
- Failed messages được gửi đến `.DLT` topics
- Có thể xử lý lại hoặc phân tích lỗi
- Không làm mất dữ liệu

## Monitoring & Logging

### Log Levels
```properties
logging.level.com.nhom4.xoxo=DEBUG
logging.level.org.springframework.kafka=DEBUG
```

### Metrics
- Notification counts (MySQL vs MongoDB)
- Kafka message processing rates
- Sync service performance
- Error rates và retry counts

## Best Practices

### 1. Data Consistency
- Sử dụng NotificationSyncService để maintain consistency
- Regular consistency checks
- Handle sync failures gracefully

### 2. Performance
- MongoDB cho real-time queries
- MySQL cho historical data và analytics
- Proper indexing trên cả hai databases

### 3. Scalability
- Kafka partitions cho parallel processing
- MongoDB sharding cho large datasets
- Connection pooling cho databases

### 4. Reliability
- Dead Letter Topics cho failed messages
- Retry mechanisms
- Circuit breakers cho external services

## Troubleshooting

### Common Issues

1. **Kafka Connection Issues**
   - Kiểm tra bootstrap servers
   - Verify topic existence
   - Check network connectivity

2. **MongoDB Connection Issues**
   - Verify authentication credentials
   - Check network connectivity
   - Verify database permissions

3. **Sync Issues**
   - Check scheduled task logs
   - Verify data consistency
   - Check for large data discrepancies

### Debug Commands

```bash
# Kafka topics
kafka-topics.sh --list --bootstrap-server localhost:29092

# MongoDB connection
mongosh "mongodb://admin:password123@localhost:27017/xoxo?authSource=admin"

# MySQL connection
mysql -h localhost -u root -p xoxo
```

## Future Enhancements

1. **WebSocket Integration**
   - Real-time push notifications
   - Live updates cho frontend

2. **Notification Templates**
   - Dynamic message generation
   - Multi-language support

3. **Advanced Analytics**
   - User engagement metrics
   - Notification effectiveness tracking

4. **Machine Learning**
   - Smart notification timing
   - Personalized content recommendations
