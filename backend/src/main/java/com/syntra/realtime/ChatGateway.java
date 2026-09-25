package com.syntra.realtime;

import com.syntra.dto.ChatSendRequest;
import com.syntra.dto.TypingEvent;
import com.syntra.service.EventPublisher;
import com.syntra.service.PresenceService;
import com.syntra.service.RoomService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatGateway {
    private final RoomService rooms;
    private final PresenceService presence;
    private final EventPublisher events;

    public ChatGateway(RoomService rooms, PresenceService presence, EventPublisher events) {
        this.rooms = rooms;
        this.presence = presence;
        this.events = events;
    }

    @MessageMapping("/chat.send")
    public void send(@Payload ChatSendRequest request, Principal principal) {
        presence.touch(principal.getName());
        rooms.post(principal.getName(), request.roomId(), request.content());
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingEvent event, Principal principal) {
        events.broadcast("/topic/rooms/" + event.roomId() + "/typing",
                new TypingEvent(event.roomId(), principal.getName(), event.typing()));
    }

    @MessageMapping("/presence.heartbeat")
    public void heartbeat(Principal principal) {
        presence.touch(principal.getName());
    }
}
