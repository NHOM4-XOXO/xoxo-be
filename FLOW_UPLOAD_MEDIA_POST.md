# 🔄 Flow Upload Media và Tạo Post

## 📋 Tổng quan
Khi user muốn tạo bài post có ảnh/video, flow sẽ như sau:

### 1️⃣ **Upload Media trước**
```bash
# Upload 1 file
curl -X POST "http://localhost:8080/api/media/upload" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@image.jpg" \
  -F "mediaType=IMAGE"

# Upload nhiều file
curl -X POST "http://localhost:8080/api/media/upload-multiple" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "files=@image1.jpg" \
  -F "files=@image2.jpg" \
  -F "files=@video.mp4" \
  -F "mediaType=IMAGE"
```

**Response:**
```json
{
  "status": "SUCCESS",
  "data": {
    "id": 1,
    "mediaUrl": "/uploads/abc123.jpg",
    "mediaType": "IMAGE",
    "originalFilename": "image.jpg",
    "fileSize": 1024000,
    "uploadedBy": {
      "id": 1,
      "email": "user@example.com"
    }
  }
}
```

### 2️⃣ **Tạo Post với Media IDs**
```bash
curl -X POST "http://localhost:8080/api/posts" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Hello World! Đây là bài viết có ảnh 🖼️",
    "status": "ACTIVE",
    "type": "USER_POST",
    "location": "Hà Nội, Việt Nam",
    "hashtags": "hello,image,post",
    "isPublic": true,
    "allowComments": true,
    "allowLikes": true,
    "allowShares": true,
    "mediaIds": [1, 2, 3]
  }'
```

## 🎯 **Flow chi tiết:**

### **Frontend:**
1. **User chọn ảnh/video** → Preview
2. **Click "Upload Media"** → Gọi API `/api/media/upload`
3. **Nhận Media IDs** → Lưu vào state
4. **Nhập nội dung post** → Gọi API `/api/posts` với `mediaIds`

### **Backend:**
1. **MediaController.uploadMedia()** → Lưu file + tạo Media entity
2. **PostController.createPost()** → Tạo Post + liên kết Media qua MediaRoom

## 🔧 **Các API endpoints:**

### **Media APIs:**
- `POST /api/media/upload` - Upload 1 file
- `POST /api/media/upload-multiple` - Upload nhiều file
- `GET /api/media/{mediaId}` - Lấy thông tin media
- `GET /api/media/my-media` - Lấy media của user
- `DELETE /api/media/{mediaId}` - Xóa media

### **Post APIs:**
- `POST /api/posts` - Tạo post (có thể kèm mediaIds)
- `GET /api/posts/{postId}` - Lấy post theo ID
- `GET /api/posts/public` - Lấy tất cả post public
- `GET /api/posts/author/{userId}` - Lấy post theo tác giả
- `PUT /api/posts/{postId}` - Cập nhật post
- `DELETE /api/posts/{postId}` - Xóa post

## 📝 **Ví dụ Frontend (React):**

```javascript
// 1. Upload media
const uploadMedia = async (files) => {
  const formData = new FormData();
  files.forEach(file => formData.append('files', file));
  formData.append('mediaType', 'IMAGE');
  
  const response = await fetch('/api/media/upload-multiple', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${token}` },
    body: formData
  });
  
  const result = await response.json();
  return result.data; // Array of Media objects
};

// 2. Create post with media
const createPost = async (content, mediaIds) => {
  const response = await fetch('/api/posts', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      content,
      mediaIds,
      status: 'ACTIVE',
      type: 'USER_POST',
      isPublic: true,
      allowComments: true,
      allowLikes: true,
      allowShares: true
    })
  });
  
  return await response.json();
};

// 3. Usage
const handleSubmit = async () => {
  // Upload media first
  const uploadedMedia = await uploadMedia(selectedFiles);
  const mediaIds = uploadedMedia.map(media => media.id);
  
  // Then create post
  const post = await createPost(content, mediaIds);
  console.log('Post created:', post);
};
```

## ⚡ **Lợi ích của flow này:**

1. **Tách biệt concerns** - Upload media và tạo post riêng biệt
2. **Reusable media** - Media có thể dùng cho nhiều post/comment
3. **Better UX** - User có thể preview media trước khi tạo post
4. **Error handling** - Có thể retry upload media nếu fail
5. **Progress tracking** - Có thể hiển thị progress upload
6. **Validation** - Validate media trước khi tạo post

## 🚀 **Next steps:**
- Tích hợp Cloudinary/AWS S3 cho file storage
- Thêm image compression/resize
- Thêm video thumbnail generation
- Implement media preview/lightbox
- Add media gallery cho user 