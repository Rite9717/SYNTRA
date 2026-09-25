package com.syntra.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class StompAuthInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            MapUsername username = new MapUsername(accessor);
            if (username.name == null) {
                return null;
            }
            accessor.setUser(username.principal);
        }
        return message;
    }

    private static final class MapUsername {
        private final String name;
        private final Principal principal;

        private MapUsername(StompHeaderAccessor accessor) {
            Object raw = accessor.getSessionAttributes() == null ? null : accessor.getSessionAttributes().get("username");
            this.name = raw == null ? null : raw.toString();
            this.principal = () -> name;
        }
    }
}
