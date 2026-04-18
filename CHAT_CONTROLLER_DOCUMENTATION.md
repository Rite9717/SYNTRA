# Chat Controller Documentation

## Overview

The `ChatController` has been converted to a unified REST controller that supports **both WebSocket and REST API** endpoints for chat functionality.

## Changes Made

### 1. Merged Controllers
- **Before**: Separate `ChatController` (WebSocket) and `ChatRestController` (REST)
- **After**: Single `ChatController` with both WebSocket and REST endpoints

### 2. Added Annotations
- `@RestController` - Marks it as a REST controller
- `@RequestMapping("/api/chat")` - Base path for all REST endpoints
- `@CrossOrigin(origins = "*", maxAge = 3600)` - CORS support

### 3. New REST Endpoints Added
- `POST /api/chat/send` - Send message via REST (alternative to WebSocket)
- `POST /api/chat/join` - Join notification via REST
- `GET /api/chat/history` - Get chat history (moved from ChatRestController)

---

## API Endpoints

### WebSocket Endpoints (Real-time)

#### 1. Send Message
**Destination**: `/app/chat.sendMessage`  
**Protocol**: WebSocket/STOMP  
**Authentication**: Required (JWT via WebSocket headers)

**Request Payload**:
```json
{
  "content": "Hello, world!",
  "type": "CHAT"
}
```

**Behavior**:
- Validates message content (not empty, max 2000 chars)
- Saves message to database
- Broadcasts to all connected clients via Kafka

#### 2. Add User (Join Notification)
**Destination**: `/app/chat.addUser`  
**Protocol**: WebSocket/STOMP  
**Authentication**: Required

**Request Payload**:
```json
{
  "content": "username joined",
  "type": "JOIN"
}
```

**Behavior**:
- Creates join notification
- Broadcasts to all connected clients via Kafka

---

### REST API Endpoints

#### 1. Get Chat History
**Endpoint**: `GET /api/chat/history`  
**Authentication**: Required (Bearer token)

**Response**:
```json
{
  "messages": [
    {
      "id": 1,
      "content": "Hello!",
      "senderUsername": "john",
      "timestamp": "2026-04-18T10:30:00",
      "type": "CHAT"
    }
  ],
  "totalCount": 50
}
```

**Usage**:
```javascript
// Frontend
const response = await api.get('/chat/history');
const messages = response.data.messages;
```

#### 2. Send Message (REST Alternative)
**Endpoint**: `POST /api/chat/send`  
**Authentication**: Required (Bearer token)

**Request Body**:
```json
{
  "content": "Hello via REST!"
}
```

**Response**:
```json
{
  "id": 123,
  "content": "Hello via REST!",
  "senderUsername": "john",
  "timestamp": "2026-04-18T10:30:00",
  "type": "CHAT"
}
```

**Usage**:
```javascript
// Frontend
const response = await api.post('/chat/send', {
  content: 'Hello via REST!'
});
```

#### 3. Join Chat (REST Alternative)
**Endpoint**: `POST /api/chat/join`  
**Authentication**: Required (Bearer token)

**Response**:
```json
{
  "content": "john joined the chat",
  "senderUsername": "john",
  "timestamp": "2026-04-18T10:30:00",
  "type": "JOIN"
}
```

---

## Where ChatController is Used

### Backend

1. **WebSocket Configuration** (`WebSocketConfig.java`)
   - Configures STOMP message broker
   - Maps `/app` prefix to controller methods
   - Broadcasts to `/topic/public`

2. **Kafka Integration**
   - `KafkaProducerService` - Sends messages to Kafka topic
   - `KafkaConsumerService` - Receives messages and broadcasts via WebSocket

3. **Security Configuration** (`SecurityConfig.java`)
   - WebSocket endpoints: `/ws/**` - permitAll (auth via WebSocket headers)
   - REST endpoints: `/api/chat/**` - authenticated

### Frontend

1. **Chat Service** (`front/src/services/chatService.js`)
   - **WebSocket Usage**:
     - `connect()` - Establishes WebSocket connection
     - `sendMessage()` - Sends via `/app/chat.sendMessage`
     - `sendJoinNotification()` - Sends via `/app/chat.addUser`
   - **REST Usage**:
     - `getMessageHistory()` - Calls `GET /api/chat/history`

2. **Chat Component** (`front/src/pages/Chat.js`)
   - Imports and uses `chatService`
   - Connects to WebSocket on mount
   - Displays real-time messages
   - Sends messages via WebSocket

---

## Architecture Flow

### Real-time Message Flow (WebSocket)
```
User types message
    ↓
Chat.js calls chatService.sendMessage()
    ↓
WebSocket sends to /app/chat.sendMessage
    ↓
ChatController.sendMessage() receives it
    ↓
Saves to database
    ↓
Sends to Kafka topic
    ↓
KafkaConsumerService receives from Kafka
    ↓
Broadcasts to /topic/public via WebSocket
    ↓
All connected clients receive the message
    ↓
Chat.js displays the message
```

### REST API Flow (Alternative)
```
User types message
    ↓
Frontend calls POST /api/chat/send
    ↓
ChatController.sendMessageRest() receives it
    ↓
Saves to database
    ↓
Sends to Kafka topic (for WebSocket clients)
    ↓
Returns response to REST client
```

---

## When to Use WebSocket vs REST

### Use WebSocket (Current Implementation)
✅ **Recommended for**:
- Real-time chat messaging
- Live notifications
- Instant updates to all users
- Low latency requirements

**Pros**:
- Real-time bidirectional communication
- Lower overhead for frequent messages
- Push notifications from server

**Cons**:
- More complex to implement
- Requires persistent connection
- Harder to debug

### Use REST API
✅ **Recommended for**:
- Fetching chat history
- Mobile apps (easier to implement)
- Fallback when WebSocket fails
- Simple request-response patterns

**Pros**:
- Simpler to implement and debug
- Better for mobile apps
- Easier to cache
- Standard HTTP tools work

**Cons**:
- No real-time updates (need polling)
- Higher latency
- More server load with polling

---

## Current Implementation Status

### ✅ Implemented
- WebSocket endpoints for real-time chat
- REST endpoint for chat history
- REST endpoints for sending messages (alternative)
- Kafka integration for message broadcasting
- Authentication for both WebSocket and REST

### 🔄 Frontend Currently Uses
- **WebSocket** for sending/receiving messages (real-time)
- **REST** for fetching chat history

### 💡 Recommendation
**Keep the current WebSocket implementation** for the chat feature because:
1. Real-time messaging is essential for chat
2. Better user experience
3. Already implemented and working
4. REST endpoints are available as fallback

---

## Testing

### Test WebSocket Endpoints
Use the existing frontend Chat component - it's already connected.

### Test REST Endpoints

#### Get Chat History
```bash
curl -X GET http://localhost:8080/api/chat/history \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Send Message via REST
```bash
curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"content":"Hello from REST!"}'
```

#### Join via REST
```bash
curl -X POST http://localhost:8080/api/chat/join \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Migration Notes

### No Frontend Changes Required
The frontend (`Chat.js` and `chatService.js`) continues to work as-is because:
- WebSocket endpoints remain unchanged
- REST history endpoint path is the same
- Only internal controller organization changed

### Optional: Switch to REST API
If you want to switch from WebSocket to REST API:

1. Update `chatService.js`:
```javascript
// Replace WebSocket sendMessage with REST
async sendMessage(content) {
  const response = await api.post('/chat/send', { content });
  return response.data;
}
```

2. Implement polling for new messages:
```javascript
// Poll for new messages every 2 seconds
setInterval(async () => {
  const messages = await this.getMessageHistory();
  // Update UI with new messages
}, 2000);
```

**Note**: This is NOT recommended as it loses real-time functionality and increases server load.

---

## Summary

✅ **ChatController is now a proper REST controller** with:
- `@RestController` annotation
- `@RequestMapping("/api/chat")` base path
- Both WebSocket and REST endpoints
- Merged functionality from ChatRestController

✅ **Used by**:
- Frontend: `Chat.js` component via `chatService.js`
- Backend: WebSocket config, Kafka services, Security config

✅ **Recommendation**: Keep using WebSocket for real-time chat, use REST for history and fallback scenarios.
