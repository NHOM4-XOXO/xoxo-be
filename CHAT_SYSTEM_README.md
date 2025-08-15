# Chat System Documentation

## Overview
The chat system is a real-time messaging solution built with WebSocket, MySQL, MongoDB, and Kafka. It supports both direct (1-1) and group chat functionality with features like message delivery status, typing indicators, and user presence.

## Architecture

### Components
1. **WebSocket Server**: Handles real-time communication
2. **MySQL**: Stores chat rooms, participants, and message metadata
3. **MongoDB**: Stores chat messages for fast retrieval and real-time access
4. **Kafka**: Message queue for processing chat events
5. **Spring Security**: Authentication and authorization

### Database Schema

#### MySQL Tables
- `chat_rooms`: Chat room information
- `chat_participants`: User participation in chat rooms
- `chat_messages`: Message metadata and content

#### MongoDB Collections
- `chat_messages`: Full message content and metadata

## API Endpoints

### Chat Rooms
- `POST /api/v1/chat/rooms` - Create chat room
- `GET /api/v1/chat/rooms` - Get user's chat rooms
- `GET /api/v1/chat/rooms/{id}` - Get chat room by ID
- `PUT /api/v1/chat/rooms/{id}` - Update chat room
- `DELETE /api/v1/chat/rooms/{id}` - Delete chat room

### Messages
- `GET /api/v1/chat/rooms/{id}/messages` - Get chat messages
- `GET /api/v1/chat/messages/{id}` - Get message by ID
- `DELETE /api/v1/chat/messages/{id}` - Delete message
- `POST /api/v1/chat/messages/{id}/read` - Mark message as read
- `POST /api/v1/chat/messages/{id}/delivered` - Mark message as delivered

### Participants
- `POST /api/v1/chat/rooms/{id}/participants/{userId}` - Add participant
- `DELETE /api/v1/chat/rooms/{id}/participants/{userId}` - Remove participant
- `POST /api/v1/chat/rooms/{id}/leave` - Leave chat room

### Direct Chat
- `POST /api/v1/chat/direct/{userId}` - Get or create direct chat

## WebSocket Endpoints

### Connection
```
ws://localhost:8080/ws
```

### Message Destinations
- `/app/send-message` - Send message to chat room
- `/app/private-message` - Send private message
- `/topic/chat/{chatRoomId}` - Receive messages from chat room
- `/queue/chat/{chatRoomId}` - Receive private messages
- `/topic/chat/{chatRoomId}/typing` - Typing indicators
- `/topic/user/{userId}/status` - User online/offline status

## Message Types

### ChatMessageRequest
```json
{
  "chatRoomId": 1,
  "content": "Hello!",
  "type": "TEXT",
  "mediaUrl": null,
  "mediaType": null,
  "replyToMessageId": null
}
```

### ChatRoomType
- `DIRECT` - One-to-one chat
- `GROUP` - Group chat
- `CHANNEL` - Broadcast channel

### MessageType
- `TEXT` - Text message
- `IMAGE` - Image message
- `VIDEO` - Video message
- `AUDIO` - Audio message
- `FILE` - File message
- `STICKER` - Sticker
- `EMOJI` - Emoji
- `SYSTEM` - System message

## Features

### Real-time Messaging
- Instant message delivery
- Message delivery status (sent, delivered, read)
- Typing indicators
- User presence (online/offline)

### Message Management
- Reply to messages
- Delete messages
- Media support (images, videos, files)
- Message search and pagination

### Chat Room Management
- Create group chats
- Add/remove participants
- Admin controls
- Room settings and customization

### Security
- JWT authentication
- User authorization
- Message privacy controls
- Rate limiting

## Setup and Configuration

### Prerequisites
- Java 17+
- MySQL 8.0+
- MongoDB 5.0+
- Kafka 3.0+
- Redis (optional, for session management)

### Environment Variables
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/xoxo
spring.datasource.username=root
spring.datasource.password=password

# MongoDB
spring.data.mongodb.uri=mongodb://admin:password@localhost:27017/xoxo

# Kafka
spring.kafka.bootstrap-servers=localhost:29092

# JWT
app.jwt-secret=your-jwt-secret
app.jwt-expiration-milliseconds=86400000
```

### Docker Compose
```bash
docker-compose up -d
```

## Usage Examples

### JavaScript Client (SockJS)
```javascript
// Connect to WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

// Connect with authentication
stompClient.connect({
  'Authorization': 'Bearer ' + jwtToken
}, function(frame) {
  console.log('Connected: ' + frame);
  
  // Subscribe to chat room
  stompClient.subscribe('/topic/chat/1', function(message) {
    const chatMessage = JSON.parse(message.body);
    console.log('New message:', chatMessage);
  });
  
  // Subscribe to private messages
  stompClient.subscribe('/user/queue/chat/1', function(message) {
    const chatMessage = JSON.parse(message.body);
    console.log('Private message:', chatMessage);
  });
});

// Send message
stompClient.send("/app/send-message", {}, JSON.stringify({
  chatRoomId: 1,
  content: "Hello, world!",
  type: "TEXT"
}));
```

### REST API Client
```bash
# Create chat room
curl -X POST http://localhost:8080/api/v1/chat/rooms \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Chat Room",
    "description": "A test chat room",
    "type": "GROUP",
    "participantIds": [1, 2, 3]
  }'

# Get chat messages
curl -X GET "http://localhost:8080/api/v1/chat/rooms/1/messages?page=0&size=50" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

## Monitoring and Troubleshooting

### Logs
- WebSocket connections: `logging.level.org.springframework.web.socket=DEBUG`
- Chat operations: `logging.level.com.nhom4.xoxo.chat=DEBUG`
- Kafka operations: `logging.level.org.springframework.kafka=DEBUG`

### Health Checks
- WebSocket endpoint: `/ws`
- REST API: `/api/v1/chat/rooms`
- Kafka topics: `chat-messages`

### Common Issues
1. **WebSocket connection failed**: Check JWT token and authentication
2. **Messages not delivered**: Verify Kafka configuration and consumer status
3. **Database errors**: Check MySQL and MongoDB connectivity
4. **Performance issues**: Monitor database indexes and query performance

## Performance Considerations

### Database Optimization
- Use appropriate indexes on frequently queried fields
- Implement message pagination
- Consider message archiving for old chats

### WebSocket Optimization
- Implement connection pooling
- Use message compression for large payloads
- Monitor connection limits and memory usage

### Kafka Optimization
- Configure appropriate partition counts
- Set retention policies for chat messages
- Monitor consumer lag and throughput

## Security Best Practices

1. **Authentication**: Always validate JWT tokens
2. **Authorization**: Check user permissions for chat room access
3. **Input Validation**: Sanitize message content
4. **Rate Limiting**: Prevent message spam
5. **Encryption**: Use HTTPS/WSS for production

## Future Enhancements

- End-to-end encryption
- Message reactions and emojis
- File sharing and storage
- Chat bots and automation
- Advanced search and filtering
- Message threading and conversations
- Push notifications
- Chat analytics and insights
