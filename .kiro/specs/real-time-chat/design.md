# Chat Functionality Design Document

## Overview

This design document outlines the implementation of a real-time chat system for the IMS application. The solution uses WebSocket technology with STOMP protocol for bidirectional communication between the backend and frontend. The chat will be a global chat room where all authenticated users can send and receive messages in real-time.

### Technology Stack
- **Backend**: Spring Boot with WebSocket support (spring-boot-starter-websocket)
- **Protocol**: STOMP (Simple Text Oriented Messaging Protocol) over WebSocket
- **Message Broker**: Spring's built-in SimpleBroker
- **Frontend**: React with SockJS and STOMP.js libraries
- **Database**: MySQL (existing chat_messages table)

## Architecture

### High-Level Architecture

```
┌─────────────┐         WebSocket/STOMP          ┌─────────────┐
│   React     │◄──────────────────────────────►  │   Spring    │
│   Client    │         (SockJS fallback)        │   Boot      │
└─────────────┘                                  └─────────────┘
      │                                                  │
      │                                                  │
      ▼                                                  ▼
┌─────────────┐                                  ┌─────────────┐
│  STOMP.js   │                                  │   STOMP     │
│  Client     │                                  │  Handler    │
└─────────────┘                                  └─────────────┘
                                                        │
                                                        ▼
                                                 ┌─────────────┐
                                                 │   MySQL     │
                                                 │  Database   │
                                                 └─────────────┘
```

### Communication Flow

1. **Connection Establishment**:
   - Client connects to `/ws` endpoint with JWT token
   - WebSocket handshake with authentication
   - Client subscribes to `/topic/public` for receiving messages

2. **Message Sending**:
   - Client sends message to `/app/chat.sendMessage`
   - Server processes, saves to database, and broadcasts to `/topic/public`
   - All subscribed clients receive the message

3. **User Join/Leave**:
   - Client sends join notification to `/app/chat.addUser`
   - Server broadcasts join notification to all clients
   - On disconnect, server broadcasts leave notification

## Components and Interfaces

### Backend Components

#### 1. WebSocket Configuration (`WebSocketConfig.java`)
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    // Configure message broker and STOMP endpoints
    // Enable SockJS fallback
    // Set application destination prefix
}
```

**Responsibilities**:
- Configure STOMP endpoint at `/ws` with SockJS support
- Enable simple in-memory message broker for `/topic` destinations
- Set application destination prefix to `/app`
- Configure CORS for WebSocket connections

#### 2. WebSocket Security Configuration
**Integration with existing SecurityConfig**:
- Allow WebSocket handshake requests
- Authenticate WebSocket connections using JWT
- Create custom channel interceptor for token validation

#### 3. Chat Controller (`ChatController.java`)
```java
@Controller
public class ChatController {
    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessageDTO sendMessage(@Payload ChatMessageDTO message, Principal principal);
    
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessageDTO addUser(@Payload ChatMessageDTO message, Principal principal);
}
```

**Responsibilities**:
- Handle incoming chat messages
- Validate message content
- Save messages to database
- Broadcast messages to all connected clients
- Handle user join events

#### 4. Chat Service (`ChatService.java`)
```java
@Service
public class ChatService {
    public ChatMessage saveMessage(String content, User sender, MessageType type);
    public List<ChatMessageDTO> getRecentMessages(int limit);
    public void notifyUserLeft(String username);
}
```

**Responsibilities**:
- Business logic for chat operations
- Save messages to database
- Retrieve message history
- Transform entities to DTOs

#### 5. DTOs

**ChatMessageDTO.java**:
```java
public class ChatMessageDTO {
    private Long id;
    private String content;
    private String senderUsername;
    private LocalDateTime timestamp;
    private MessageType type; // CHAT, JOIN, LEAVE
}
```

**ChatHistoryResponse.java**:
```java
public class ChatHistoryResponse {
    private List<ChatMessageDTO> messages;
    private int totalCount;
}
```

#### 6. REST Endpoint for Message History
```java
@RestController
@RequestMapping("/api/chat")
public class ChatRestController {
    @GetMapping("/history")
    public ResponseEntity<ChatHistoryResponse> getChatHistory();
}
```

### Frontend Components

#### 1. Chat Service (`chatService.js`)
```javascript
class ChatService {
    connect(token, onMessageReceived, onConnected, onError);
    disconnect();
    sendMessage(content);
    sendJoinNotification(username);
    getMessageHistory();
}
```

**Responsibilities**:
- Establish WebSocket connection with authentication
- Subscribe to chat topic
- Send messages through STOMP
- Handle connection lifecycle
- Fetch message history via REST API

#### 2. Chat Component (`Chat.js` / `Chat.jsx`)
```javascript
function Chat() {
    // State: messages, connected, currentMessage
    // Effects: connect on mount, disconnect on unmount
    // Handlers: sendMessage, handleMessageReceived
    // Render: message list, input form, connection status
}
```

**Responsibilities**:
- Manage chat UI state
- Display messages with sender and timestamp
- Handle user input
- Show connection status
- Auto-scroll to latest messages
- Display join/leave notifications differently

#### 3. Chat Styles (`Chat.css`)
- Message list container with scrolling
- Message bubbles (own messages vs others)
- System notification styling
- Input form styling
- Connection status indicator

## Data Models

### Existing ChatMessage Entity
```java
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Enumerated(EnumType.STRING)
    private MessageType type; // CHAT, JOIN, LEAVE
}
```

**No schema changes required** - the existing table structure supports all requirements.

## Error Handling

### Backend Error Handling

1. **Message Validation Errors**:
   - Empty message content → Return error to sender only
   - Message too long (>2000 chars) → Return error to sender only
   - Invalid message type → Log and ignore

2. **Authentication Errors**:
   - Missing JWT token → Reject WebSocket connection
   - Invalid JWT token → Reject WebSocket connection
   - Expired token → Close existing connection

3. **Database Errors**:
   - Save failure → Log error, still broadcast message (eventual consistency)
   - Query failure → Return empty history with error flag

4. **Connection Errors**:
   - Client disconnect → Broadcast leave notification
   - Broker failure → Log critical error, attempt restart

### Frontend Error Handling

1. **Connection Errors**:
   - Connection failed → Display error message, retry after 5 seconds
   - Connection lost → Display disconnected status, auto-reconnect
   - Authentication failed → Redirect to login

2. **Message Send Errors**:
   - Send failed → Display error notification
   - Timeout → Display retry option

3. **History Load Errors**:
   - API failure → Display error message, provide retry button
   - Empty history → Display welcome message

## Testing Strategy

### Backend Testing

1. **Unit Tests**:
   - ChatService: Test message saving, history retrieval
   - ChatController: Test message handling with mocked Principal
   - WebSocketConfig: Test configuration beans

2. **Integration Tests**:
   - WebSocket connection with authentication
   - Message broadcasting to multiple clients
   - Message persistence to database
   - History API endpoint

### Frontend Testing

1. **Component Tests**:
   - Chat component rendering
   - Message display formatting
   - Input handling and validation
   - Connection status display

2. **Integration Tests**:
   - WebSocket connection establishment
   - Message sending and receiving
   - Auto-reconnection logic
   - History loading on mount

### Manual Testing Scenarios

1. **Basic Flow**:
   - User logs in and navigates to chat
   - User sends a message
   - Message appears for all connected users
   - Message persists after page refresh

2. **Multi-User**:
   - Multiple users connect simultaneously
   - Messages from different users display correctly
   - Join/leave notifications appear

3. **Connection Resilience**:
   - Disconnect network, verify status indicator
   - Reconnect network, verify auto-reconnect
   - Send message while disconnected, verify error

4. **Edge Cases**:
   - Very long messages (>2000 chars)
   - Empty messages
   - Special characters and emojis
   - Rapid message sending

## Security Considerations

1. **Authentication**:
   - JWT token required for WebSocket connection
   - Token validated on handshake
   - Token included in STOMP headers

2. **Authorization**:
   - Only authenticated users can send messages
   - User identity extracted from JWT, not client payload
   - Sender information set server-side to prevent spoofing

3. **Input Validation**:
   - Message content sanitized on backend
   - Length limits enforced
   - XSS prevention through proper escaping

4. **Rate Limiting** (Future Enhancement):
   - Limit messages per user per minute
   - Prevent spam and abuse

## Performance Considerations

1. **Message History**:
   - Limit to 50 most recent messages
   - Indexed query on timestamp
   - Lazy loading of user relationships

2. **Broadcasting**:
   - Use SimpleBroker for small-scale deployment
   - Consider external broker (RabbitMQ/Redis) for scaling

3. **Connection Management**:
   - Heartbeat mechanism to detect stale connections
   - Automatic cleanup of disconnected sessions

4. **Frontend Optimization**:
   - Virtual scrolling for large message lists (future)
   - Debounce typing indicators (future)
   - Efficient re-rendering with React.memo

## Deployment Considerations

1. **WebSocket Support**:
   - Ensure reverse proxy (nginx) supports WebSocket upgrade
   - Configure proper timeout values
   - Enable SockJS fallback for restrictive networks

2. **CORS Configuration**:
   - Add WebSocket endpoints to CORS allowed origins
   - Configure allowed headers for STOMP

3. **Database**:
   - Existing chat_messages table ready
   - Consider archiving old messages (future)

## Future Enhancements

1. **Private Messaging**: One-to-one chat between users
2. **Chat Rooms**: Multiple topic-based chat rooms
3. **Typing Indicators**: Show when users are typing
4. **Read Receipts**: Track message read status
5. **File Sharing**: Send images and files in chat
6. **Message Reactions**: Emoji reactions to messages
7. **Message Search**: Search through chat history
8. **User Presence**: Online/offline status indicators
