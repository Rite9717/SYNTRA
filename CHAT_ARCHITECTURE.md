# Chat Architecture Overview

## System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Chat.js (React Component)                                │  │
│  │  - Displays chat messages                                 │  │
│  │  - Handles user input                                     │  │
│  │  - Manages WebSocket connection                           │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                          │
│  ┌────────────────────▼─────────────────────────────────────┐  │
│  │  chatService.js                                           │  │
│  │  - connect() - WebSocket connection                       │  │
│  │  - sendMessage() - Send via WebSocket                     │  │
│  │  - getMessageHistory() - Fetch via REST                   │  │
│  └────────────────────┬─────────────────────────────────────┘  │
│                       │                                          │
└───────────────────────┼──────────────────────────────────────────┘
                        │
        ┌───────────────┴───────────────┐
        │                               │
        │ WebSocket                     │ REST API
        │ /ws                           │ /api/chat/*
        │                               │
┌───────▼───────────────────────────────▼───────────────────────┐
│                         BACKEND                                │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  WebSocketConfig                                          │ │
│  │  - Configures STOMP broker                                │ │
│  │  - Maps /app prefix                                       │ │
│  │  - Broadcasts to /topic/public                            │ │
│  └────────────────────┬─────────────────────────────────────┘ │
│                       │                                         │
│  ┌────────────────────▼─────────────────────────────────────┐ │
│  │  ChatController (@RestController)                         │ │
│  │                                                            │ │
│  │  WebSocket Endpoints:                                     │ │
│  │  - @MessageMapping("/chat.sendMessage")                   │ │
│  │  - @MessageMapping("/chat.addUser")                       │ │
│  │                                                            │ │
│  │  REST Endpoints:                                          │ │
│  │  - GET  /api/chat/history                                 │ │
│  │  - POST /api/chat/send                                    │ │
│  │  - POST /api/chat/join                                    │ │
│  └────────────────────┬─────────────────────────────────────┘ │
│                       │                                         │
│  ┌────────────────────▼─────────────────────────────────────┐ │
│  │  ChatService                                              │ │
│  │  - saveMessage()                                          │ │
│  │  - getRecentMessages()                                    │ │
│  └────────────────────┬─────────────────────────────────────┘ │
│                       │                                         │
│         ┌─────────────┴─────────────┐                          │
│         │                           │                          │
│  ┌──────▼──────────┐       ┌───────▼──────────┐              │
│  │  Database       │       │  KafkaProducer   │              │
│  │  (MySQL)        │       │  Service         │              │
│  │  - ChatMessage  │       └───────┬──────────┘              │
│  │  - User         │               │                          │
│  └─────────────────┘               │                          │
│                                    │                          │
│                            ┌───────▼──────────┐              │
│                            │  Kafka Broker    │              │
│                            │  Topic: chat     │              │
│                            └───────┬──────────┘              │
│                                    │                          │
│                            ┌───────▼──────────┐              │
│                            │  KafkaConsumer   │              │
│                            │  Service         │              │
│                            └───────┬──────────┘              │
│                                    │                          │
│                            ┌───────▼──────────┐              │
│                            │  SimpMessaging   │              │
│                            │  Template        │              │
│                            │  (WebSocket)     │              │
│                            └───────┬──────────┘              │
│                                    │                          │
└────────────────────────────────────┼──────────────────────────┘
                                     │
                                     │ Broadcast to
                                     │ /topic/public
                                     │
                        ┌────────────▼────────────┐
                        │  All Connected Clients  │
                        │  (WebSocket)            │
                        └─────────────────────────┘
```

## Message Flow Diagrams

### 1. Real-time Message Flow (WebSocket)

```
User A                  Frontend              Backend              Kafka              All Users
  │                        │                     │                   │                    │
  │ Type "Hello"           │                     │                   │                    │
  ├───────────────────────>│                     │                   │                    │
  │                        │ WebSocket           │                   │                    │
  │                        │ /app/chat.sendMessage                   │                    │
  │                        ├────────────────────>│                   │                    │
  │                        │                     │ Save to DB        │                    │
  │                        │                     ├──────────┐        │                    │
  │                        │                     │          │        │                    │
  │                        │                     │<─────────┘        │                    │
  │                        │                     │ Send to Kafka     │                    │
  │                        │                     ├──────────────────>│                    │
  │                        │                     │                   │ Consume            │
  │                        │                     │                   ├──────────┐         │
  │                        │                     │                   │          │         │
  │                        │                     │<──────────────────┤<─────────┘         │
  │                        │                     │ Broadcast         │                    │
  │                        │                     │ /topic/public     │                    │
  │                        │                     ├───────────────────────────────────────>│
  │                        │<────────────────────┤                   │                    │
  │<───────────────────────┤                     │                   │                    │
  │ Display "Hello"        │                     │                   │                    │
```

### 2. REST API Message Flow

```
User                    Frontend              Backend              Database
  │                        │                     │                     │
  │ Type "Hello"           │                     │                     │
  ├───────────────────────>│                     │                     │
  │                        │ POST /api/chat/send │                     │
  │                        ├────────────────────>│                     │
  │                        │                     │ Save message        │
  │                        │                     ├────────────────────>│
  │                        │                     │                     │
  │                        │                     │<────────────────────┤
  │                        │                     │ Return saved msg    │
  │                        │<────────────────────┤                     │
  │<───────────────────────┤                     │                     │
  │ Display "Hello"        │                     │                     │
  │                        │                     │                     │
  │ (Need polling to see   │                     │                     │
  │  messages from others) │                     │                     │
```

### 3. Chat History Flow

```
User                    Frontend              Backend              Database
  │                        │                     │                     │
  │ Open chat page         │                     │                     │
  ├───────────────────────>│                     │                     │
  │                        │ GET /api/chat/history                     │
  │                        ├────────────────────>│                     │
  │                        │                     │ Query last 50 msgs  │
  │                        │                     ├────────────────────>│
  │                        │                     │                     │
  │                        │                     │<────────────────────┤
  │                        │                     │ Return messages     │
  │                        │<────────────────────┤                     │
  │<───────────────────────┤                     │                     │
  │ Display history        │                     │                     │
```

## File Locations

### Backend Files
```
backend/src/main/java/com/project/ims/
├── controller/
│   └── ChatController.java          ← Main chat controller (WebSocket + REST)
├── service/
│   ├── ChatService.java              ← Business logic
│   ├── KafkaProducerService.java     ← Send to Kafka
│   └── KafkaConsumerService.java     ← Receive from Kafka
├── config/
│   ├── WebSocketConfig.java          ← WebSocket configuration
│   ├── KafkaConfig.java              ← Kafka configuration
│   └── SecurityConfig.java           ← Security rules
├── dto/
│   ├── ChatMessageDTO.java           ← Message data transfer object
│   └── ChatHistoryResponse.java      ← History response wrapper
├── model/
│   └── ChatMessage.java              ← Database entity
└── repository/
    └── ChatMessageRepository.java    ← Database access
```

### Frontend Files
```
front/src/
├── pages/
│   └── Chat.js                       ← Main chat UI component
├── services/
│   ├── chatService.js                ← WebSocket & REST API calls
│   └── api.js                        ← Axios instance with auth
└── components/
    └── Navbar.js                     ← Navigation (includes chat link)
```

## Key Technologies

| Technology | Purpose | Used In |
|------------|---------|---------|
| **WebSocket** | Real-time bidirectional communication | Chat messaging |
| **STOMP** | Simple Text Oriented Messaging Protocol | WebSocket message format |
| **SockJS** | WebSocket fallback | Frontend connection |
| **Kafka** | Message broker for scalability | Broadcasting messages |
| **Spring Messaging** | WebSocket support in Spring | Backend WebSocket handling |
| **Axios** | HTTP client | REST API calls |
| **MySQL** | Database | Persistent message storage |

## Security

### WebSocket Security
- JWT token passed in WebSocket connection headers
- Validated by `WebSocketAuthInterceptor`
- User identity from `Principal` object

### REST API Security
- JWT token in `Authorization: Bearer <token>` header
- Validated by `AuthTokenFilter`
- User identity from `Authentication` object

### Endpoints Access
```java
// In SecurityConfig.java
.requestMatchers("/ws/**").permitAll()           // WebSocket (auth via headers)
.requestMatchers("/api/chat/**").authenticated() // REST (auth via Bearer token)
```

## Scalability Considerations

### Current Architecture
- ✅ Kafka enables horizontal scaling
- ✅ Multiple backend instances can share Kafka topic
- ✅ WebSocket connections distributed across instances
- ✅ Database stores all messages persistently

### Limitations
- ⚠️ WebSocket connections are stateful (sticky sessions needed)
- ⚠️ No message pagination (loads last 50 only)
- ⚠️ No read receipts or delivery status

### Future Improvements
1. Add Redis for WebSocket session management
2. Implement message pagination
3. Add read receipts
4. Add typing indicators
5. Add file/image sharing
6. Add private messaging (currently only group chat)

## Summary

The ChatController is a **hybrid controller** that supports:
- ✅ **WebSocket** for real-time messaging (primary method)
- ✅ **REST API** for history and fallback scenarios
- ✅ **Kafka** for scalable message broadcasting
- ✅ **MySQL** for persistent storage

**Currently used by**: `Chat.js` component via `chatService.js` (WebSocket for messaging, REST for history)
