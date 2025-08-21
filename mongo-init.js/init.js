// MongoDB initialization script for XOXO application
// This script creates the necessary database, collections, and indexes

// Switch to xoxo database
db = db.getSiblingDB('xoxo');

// Create collections
db.createCollection('notifications');
db.createCollection('users');
db.createCollection('posts');

// Create indexes for notifications collection
db.notifications.createIndex({ "userId": 1 });
db.notifications.createIndex({ "createdAt": -1 });
db.notifications.createIndex({ "userId": 1, "read": 1 });
db.notifications.createIndex({ "userId": 1, "type": 1 });
db.notifications.createIndex({ "createdAt": 1 }, { expireAfterSeconds: 7776000 }); // 90 days TTL

// Create indexes for users collection
db.users.createIndex({ "userId": 1 }, { unique: true });
db.users.createIndex({ "username": 1 }, { unique: true });
db.users.createIndex({ "email": 1 }, { unique: true });

// Create indexes for posts collection
db.posts.createIndex({ "postId": 1 }, { unique: true });
db.posts.createIndex({ "authorId": 1 });
db.posts.createIndex({ "createdAt": -1 });

// Insert sample data for testing (optional)
db.notifications.insertOne({
    userId: 1,
    message: "Welcome to XOXO! This is a sample notification.",
    type: "SYSTEM_ALERT",
    targetId: null,
    targetType: "SYSTEM",
    senderId: null,
    actionType: "WELCOME",
    payload: "{}",
    read: false,
    createdAt: new Date(),
    readAt: null
});

print("MongoDB initialization completed successfully!");
print("Database: " + db.getName());
print("Collections created: " + db.getCollectionNames().join(", "));
