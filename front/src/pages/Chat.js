import React, { useState, useEffect, useRef } from 'react';
import Navbar from '../components/Navbar';
import chatService from '../services/chatService';
import './Chat.css';

function Chat() {
  const [messages, setMessages] = useState([]);
  const [currentMessage, setCurrentMessage] = useState('');
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState(null);
  const [reconnecting, setReconnecting] = useState(false);
  const messagesEndRef = useRef(null);
  const reconnectTimeoutRef = useRef(null);
  const currentUserRef = useRef(JSON.parse(sessionStorage.getItem('user')));
  const currentUser = currentUserRef.current;

  useEffect(() => {
    const token = sessionStorage.getItem('token');
    const user = JSON.parse(sessionStorage.getItem('user'));
    
    if (!token || !user) {
      window.location.href = '/login';
      return;
    }

    const handleMessageReceived = (message) => {
      setMessages(prevMessages => [...prevMessages, message]);
    };

    const handleConnected = async () => {
      setConnected(true);
      setError(null);
      setReconnecting(false);
      
      try {
        const history = await chatService.getMessageHistory();
        setMessages(history);
      } catch (err) {
        console.error('Failed to load message history:', err);
        setError('Failed to load message history');
      }

      chatService.sendJoinNotification(user.username);
    };

    const handleError = (err) => {
      setConnected(false);
      setReconnecting(true);
      setError('Connection lost. Reconnecting...');
      console.error('Connection error:', err);
      
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      
      reconnectTimeoutRef.current = setTimeout(() => {
        setReconnecting(false);
      }, 5000);
    };

    chatService.connect(token, handleMessageReceived, handleConnected, handleError);

    return () => {
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
      }
      chatService.disconnect();
    };
  }, []); // Empty dependency array - only run once on mount

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSendMessage = (e) => {
    e.preventDefault();
    
    if (!currentMessage.trim()) {
      return;
    }

    if (!connected) {
      setError('Not connected to chat server');
      return;
    }

    if (currentMessage.length > 2000) {
      setError('Message is too long (max 2000 characters)');
      return;
    }

    try {
      chatService.sendMessage(currentMessage.trim());
      setCurrentMessage('');
      setError(null);
    } catch (err) {
      setError('Failed to send message. Please check your connection.');
      console.error('Failed to send message:', err);
      
      setTimeout(() => {
        setError(null);
      }, 3000);
    }
  };
  const formatTimestamp = (timestamp) => {
    if (!timestamp) return '';
    
    // JS can't parse microseconds (6 digits) — truncate to milliseconds (3 digits)
    const truncated = timestamp.substring(0, 23); // "2026-03-26T13:21:38.927"
    const date = new Date(truncated);
    
    if (isNaN(date.getTime())) return '';

    return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: true
    });
};

  return (
    <>
      <Navbar />
      <div className="chat-container">
      <div className="chat-header">
        <h2>Chat Room</h2>
        <div className={`connection-status ${connected ? 'connected' : reconnecting ? 'reconnecting' : 'disconnected'}`}>
          {connected ? '● Connected' : reconnecting ? '⟳ Reconnecting...' : '○ Disconnected'}
        </div>
      </div>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      <div className="messages-container">
        {messages.map((message, index) => (
          <div 
            key={index} 
            className={`message ${
              message.type === 'JOIN' || message.type === 'LEAVE' 
                ? 'system-message' 
                : message.senderUsername === currentUser.username 
                  ? 'own-message' 
                  : 'other-message'
            }`}
          >
            {message.type === 'CHAT' && (
              <>
                <div className="message-header">
                  <span className="message-sender">{message.senderUsername}</span>
                  <span className="message-time">{formatTimestamp(message.timestamp)}</span>
                </div>
                <div className="message-content">{message.content}</div>
              </>
            )}
            {(message.type === 'JOIN' || message.type === 'LEAVE') && (
              <div className="system-message-content">{message.content}</div>
            )}
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>

      <form className="message-input-form" onSubmit={handleSendMessage}>
        <textarea
          value={currentMessage}
          onChange={(e) => setCurrentMessage(e.target.value)}
          placeholder="Type your message..."
          disabled={!connected}
          rows="3"
          onKeyPress={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              handleSendMessage(e);
            }
          }}
        />
        <button type="submit" disabled={!connected || !currentMessage.trim()}>
          Send
        </button>
      </form>
    </div>
    </>
  );
}

export default Chat;
