# Kafka-Based Chat Implementation

## Overview

The chat system has been updated to use Apache Kafka for message distribution instead of direct WebSocket broadcasting. This provides better scalability and reliability.

## Architecture

```
Frontend (React) 
    ↓ WebSocket/STOMP
Backend (Spring Boot)
    ↓ Kafka Producer
Kafka Broker (localhost:9092)
    ↓ Kafka Consumer
Backend (Spring Boot)
    ↓ WebSocket/STOMP
Frontend (React)
```

### How It Works

1. **User sends message** → Frontend sends via WebSocket to backend
2. **Backend receives** → Validates and saves to database
3. **Backend publishes** → Sends message to Kafka topic `chat-messages`
4. **Kafka distributes** → All backend instances consume the message
5. **Backend broadcasts** → Sends to all connected WebSocket clients
6. **Users receive** → Message appears in all chat windows

## Prerequisites

- Docker installed and running
- Java 17+
- Node.js and npm
- MySQL running on localhost:3306

## Setup Instructions

### 1. Start Kafka with Docker

You mentioned you have a Docker image with Kafka. Start it with:

```bash
docker-compose up -d
```

Or if using a standalone Kafka container:

```bash
# Start Zookeeper
docker run -d --name zookeeper -p 2181:2181 -e ZOOKEEPER_CLIENT_PORT=2181 wurstmeister/zookeeper

# Start Kafka
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ADVERTISED_HOST_NAME=localhost \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_PORT=9092 \
  --link zookeeper:zookeeper \
  wurstmeister/kafka
```

### 2. Verify Kafka is Running

```bash
# Check if Kafka container is running
docker ps | grep kafka

# Check Kafka logs
docker logs kafka
```

### 3. Start the Backend

```bash
cd backend
mvn clean install
mvn spring-boot:run
```

The backend will:
- Connect to Kafka at localhost:9092
- Create the `chat-messages` topic automatically
- Start consuming messages from Kafka
- Start the WebSocket server

### 4. Start the Frontend

```bash
cd front
npm install
npm start
```

### 5. Test the Chat

1. Open browser and navigate to `http://localhost:3000`
2. Login with your credentials
3. Click "Chat" in the navigation menu
4. You should see:
   - The Navbar at the top
   - Chat Room header with connection status
   - Message history (if any)
   - Message input form at the bottom

5. Open another browser window/tab (or incognito mode)
6. Login with a different user
7. Navigate to Chat
8. Send messages from both windows - they should appear in real-time

## Configuration

### Backend Configuration (application.properties)

```properties
# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.bootstrap-servers=localhost:9092
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.group-id=my-new-group-2
spring.kafka.producer.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
```

### Kafka Topic

- **Topic Name**: `chat-messages`
- **Partitions**: 1
- **Replication Factor**: 1

## Troubleshooting

### Issue: "Connection lost. Reconnecting..."

**Possible Causes:**
1. Backend is not running
2. Kafka is not running
3. Network issues

**Solutions:**
```bash
# Check backend logs
cd backend
mvn spring-boot:run

# Check Kafka is running
docker ps | grep kafka

# Check Kafka logs
docker logs kafka

# Restart Kafka if needed
docker restart kafka
```

### Issue: Messages not appearing

**Possible Causes:**
1. Kafka consumer not connected
2. Topic not created
3. Serialization issues

**Solutions:**
```bash
# Check backend logs for Kafka errors
# Look for: "Error processing Kafka message"

# Verify topic exists
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Check topic details
docker exec -it kafka kafka-topics.sh --describe --topic chat-messages --bootstrap-server localhost:9092
```

### Issue: "Not connected to chat server"

**Possible Causes:**
1. WebSocket connection failed
2. JWT token expired
3. Backend not running

**Solutions:**
1. Check browser console for errors
2. Verify token in localStorage
3. Try logging out and logging back in
4. Check backend is running on port 8080

### Issue: Navbar not showing

**Fixed!** The Chat component now includes the Navbar component.

## Benefits of Kafka Implementation

1. **Scalability**: Can run multiple backend instances
2. **Reliability**: Messages are persisted in Kafka
3. **Decoupling**: Frontend and backend are loosely coupled
4. **Message History**: Kafka retains messages for replay
5. **Load Balancing**: Kafka distributes load across consumers

## Monitoring

### Check Kafka Topics

```bash
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

### Monitor Messages

```bash
# Consume messages from the topic (for debugging)
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic chat-messages \
  --from-beginning
```

### Check Consumer Groups

```bash
docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --list
```

## API Endpoints

### WebSocket
- **Endpoint**: `ws://localhost:8080/ws`
- **Protocol**: STOMP over SockJS
- **Authentication**: JWT token in Authorization header

### REST API
- **GET** `/api/chat/history` - Retrieve last 50 messages

### STOMP Destinations
- **Subscribe**: `/topic/public` - Receive broadcast messages
- **Send**: `/app/chat.sendMessage` - Send chat message
- **Send**: `/app/chat.addUser` - Send join notification

## Files Modified

### Backend
- `pom.xml` - Added Kafka dependency
- `application.properties` - Added Kafka configuration
- `KafkaConfig.java` - Kafka topic configuration
- `KafkaProducerService.java` - Sends messages to Kafka
- `KafkaConsumerService.java` - Consumes messages from Kafka
- `ChatController.java` - Updated to use Kafka
- `AppConfig.java` - ObjectMapper configuration

### Frontend
- `Chat.js` - Added Navbar component

## Next Steps

1. Start Kafka using Docker
2. Start the backend
3. Start the frontend
4. Test the chat functionality
5. Monitor Kafka logs for any issues

The chat should now work reliably with Kafka handling message distribution!
