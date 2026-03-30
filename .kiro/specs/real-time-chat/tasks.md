# Implementation Plan

- [x] 1. Add WebSocket dependencies and create DTOs
  - Add spring-boot-starter-websocket dependency to pom.xml
  - Create ChatMessageDTO class with id, content, senderUsername, timestamp, and type fields
  - Create ChatHistoryResponse class with messages list and totalCount
  - _Requirements: 1.1, 1.2, 2.1, 2.2_

- [x] 2. Configure WebSocket and STOMP messaging
  - Create WebSocketConfig class implementing WebSocketMessageBrokerConfigurer
  - Configure STOMP endpoint at /ws with SockJS fallback support
  - Enable simple broker with /topic destination prefix
  - Set application destination prefix to /app
  - Configure CORS for WebSocket connections to allow frontend origins
  - _Requirements: 1.1, 2.1, 4.3_

- [x] 3. Implement WebSocket security and authentication
  - Create WebSocketAuthInterceptor to validate JWT tokens in WebSocket handshake
  - Update SecurityConfig to permit WebSocket endpoints
  - Configure channel interceptor to extract user from JWT token
  - _Requirements: 1.5, 4.2_

- [x] 4. Create chat service layer
  - Implement ChatService with saveMessage method to persist chat messages
  - Add getRecentMessages method to retrieve last 50 messages ordered by timestamp
  - Create method to convert ChatMessage entities to ChatMessageDTO
  - _Requirements: 1.2, 2.4_

- [x] 5. Implement WebSocket chat controller
  - Create ChatController with @MessageMapping for /chat.sendMessage
  - Implement sendMessage handler to validate content, save to database, and broadcast
  - Add @MessageMapping for /chat.addUser to handle user join events
  - Extract authenticated user from Principal to set sender information
  - Validate message content is not empty and under 2000 characters
  - Use @SendTo("/topic/public") to broadcast messages to all subscribers
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 3.1_

- [x] 6. Create REST endpoint for chat history
  - Create ChatRestController with GET /api/chat/history endpoint
  - Implement handler to return recent 50 messages as ChatHistoryResponse
  - Ensure endpoint requires authentication
  - _Requirements: 2.4_

- [x] 7. Install frontend WebSocket dependencies
  - Add sockjs-client package to frontend package.json
  - Add @stomp/stompjs package for STOMP protocol support
  - Run npm install to install dependencies
  - _Requirements: 4.3_

- [x] 8. Create frontend chat service
  - Create chatService.js with connect method accepting token and callbacks
  - Implement WebSocket connection to ws://localhost:8080/ws with SockJS
  - Configure STOMP client to send JWT token in connection headers
  - Implement subscribe to /topic/public for receiving messages
  - Add sendMessage method to publish to /app/chat.sendMessage
  - Add sendJoinNotification method to publish to /app/chat.addUser
  - Implement disconnect method to close WebSocket connection
  - Add getMessageHistory method to fetch history via REST API
  - _Requirements: 1.1, 2.1, 3.1, 4.3_

- [x] 9. Create Chat page component
  - Create Chat.js/Chat.jsx component with messages state array
  - Add connected state to track WebSocket connection status
  - Add currentMessage state for input field
  - Implement useEffect to connect on mount and disconnect on unmount
  - Load message history on successful connection
  - Implement handleMessageReceived callback to append new messages to state
  - Create sendMessage handler to validate and send messages via chatService
  - Send join notification with username on connection
  - _Requirements: 1.1, 2.1, 2.2, 2.4, 3.1, 4.1, 4.3_

- [x] 10. Implement Chat UI and styling
  - Create message list container with auto-scroll to bottom
  - Display each message with sender username and formatted timestamp
  - Style own messages differently from other users' messages
  - Display JOIN and LEAVE messages as system notifications with distinct styling
  - Create message input form with textarea and send button
  - Add connection status indicator showing connected/disconnected state
  - Disable message input when disconnected
  - Implement CSS styling in Chat.css for all chat elements
  - _Requirements: 2.2, 2.3, 3.3, 3.4, 4.4, 5.1, 5.4_

- [x] 11. Add chat navigation and routing
  - Add Chat route to App.js router configuration
  - Add "Chat" link to Navbar component
  - Ensure chat route requires authentication
  - _Requirements: 4.1, 4.2_

- [x] 12. Implement connection error handling and reconnection
  - Add error callback to chatService connect method
  - Display disconnection notification when connection is lost
  - Implement auto-reconnect logic with 5-second delay
  - Show error notification if message send fails
  - Update connection status indicator based on connection state
  - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ]* 13. Write backend tests
  - Write unit tests for ChatService message saving and retrieval
  - Write unit tests for ChatController message handling
  - Create integration test for WebSocket connection with authentication
  - Test message broadcasting to multiple connected clients
  - _Requirements: 1.1, 1.2, 2.1_

- [ ]* 14. Write frontend tests
  - Write component tests for Chat component rendering
  - Test message display and formatting
  - Test input handling and validation
  - Test connection status display
  - _Requirements: 2.2, 2.3, 4.4_
