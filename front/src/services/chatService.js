import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import api from './api';

class ChatService {
  constructor() {
    this.stompClient = null;
  }

  connect(token, onMessageReceived, onConnected, onError) {
    const socket = new SockJS(`${process.env.REACT_APP_API_URL}/ws`);
    
    this.stompClient = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      debug: (str) => {
        console.log('STOMP: ' + str);
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('Connected to WebSocket');
        
        this.stompClient.subscribe('/topic/public', (message) => {
          const receivedMessage = JSON.parse(message.body);
          onMessageReceived(receivedMessage);
        });
        
        if (onConnected) {
          onConnected();
        }
      },
      onStompError: (frame) => {
        console.error('STOMP error: ' + frame.headers['message']);
        console.error('Details: ' + frame.body);
        if (onError) {
          onError(frame);
        }
      },
      onWebSocketError: (error) => {
        console.error('WebSocket error:', error);
        if (onError) {
          onError(error);
        }
      }
    });

    this.stompClient.activate();
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.deactivate();
      console.log('Disconnected from WebSocket');
    }
  }

  sendMessage(content) {
    if (this.stompClient && this.stompClient.connected) {
      const message = {
        content: content,
        type: 'CHAT'
      };
      
      this.stompClient.publish({
        destination: '/app/chat.sendMessage',
        body: JSON.stringify(message)
      });
    } else {
      console.error('Not connected to WebSocket');
      throw new Error('Not connected to chat server');
    }
  }

  sendJoinNotification(username) {
    if (this.stompClient && this.stompClient.connected) {
      const message = {
        content: username + ' joined',
        type: 'JOIN'
      };
      
      this.stompClient.publish({
        destination: '/app/chat.addUser',
        body: JSON.stringify(message)
      });
    }
  }

  async getMessageHistory() {
    try {
      const response = await api.get('/chat/history');
      return response.data.messages;
    } catch (error) {
      console.error('Error fetching chat history:', error);
      throw error;
    }
  }

  isConnected() {
    return this.stompClient && this.stompClient.connected;
  }
}

export default new ChatService();
