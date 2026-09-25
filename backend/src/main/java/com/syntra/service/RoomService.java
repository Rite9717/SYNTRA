package com.syntra.service;

import com.syntra.domain.ChatMessage;
import com.syntra.domain.ChatRoom;
import com.syntra.domain.ChatType;
import com.syntra.domain.RoomMember;
import com.syntra.domain.RoomType;
import com.syntra.domain.User;
import com.syntra.dto.ChatResponse;
import com.syntra.dto.MemberSummary;
import com.syntra.dto.RoomSummary;
import com.syntra.repo.ChatMessageRepository;
import com.syntra.repo.ChatRoomRepository;
import com.syntra.repo.RoomMemberRepository;
import com.syntra.support.MessageRules;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoomService {
    private final ChatRoomRepository rooms;
    private final RoomMemberRepository members;
    private final ChatMessageRepository messages;
    private final AuthService auth;
    private final PresenceService presence;
    private final NotificationService notifications;
    private final EventPublisher events;
    private final SyntraMetrics metrics;
    private final AiClient ai;

    public RoomService(ChatRoomRepository rooms, RoomMemberRepository members, ChatMessageRepository messages,
                       AuthService auth, PresenceService presence, NotificationService notifications,
                       EventPublisher events, SyntraMetrics metrics, AiClient ai) {
        this.rooms = rooms;
        this.members = members;
        this.messages = messages;
        this.auth = auth;
        this.presence = presence;
        this.notifications = notifications;
        this.events = events;
        this.metrics = metrics;
        this.ai = ai;
    }

    @Transactional(readOnly = true)
    public List<RoomSummary> list(Long userId) {
        return rooms.findForUser(userId).stream().map(room -> toSummary(room, userId)).toList();
    }

    @Transactional
    public RoomSummary openDirect(Long userId, Long otherId) {
        if (userId.equals(otherId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose another person");
        }
        User me = auth.require(userId);
        User other = auth.require(otherId);
        String key = "dm:" + Math.min(userId, otherId) + ":" + Math.max(userId, otherId);
        ChatRoom room = rooms.findByDirectKey(key).orElseGet(() -> {
            ChatRoom created = new ChatRoom();
            created.setName(other.getUsername());
            created.setType(RoomType.DIRECT);
            created.setDirectKey(key);
            created.setCreatedBy(me);
            ChatRoom saved = rooms.save(created);
            addMember(saved, me);
            addMember(saved, other);
            return saved;
        });
        return toSummary(room, userId);
    }

    @Transactional
    public RoomSummary createGroup(Long userId, String name, List<Long> memberIds) {
        User me = auth.require(userId);
        ChatRoom room = new ChatRoom();
        room.setName(MessageRules.requireText(name, "Room name", 120));
        room.setType(RoomType.GROUP);
        room.setCreatedBy(me);
        ChatRoom saved = rooms.save(room);
        Set<Long> ids = new HashSet<>();
        ids.add(userId);
        if (memberIds != null) {
            ids.addAll(memberIds);
        }
        for (Long id : ids) {
            User user = auth.require(id);
            addMember(saved, user);
            if (!id.equals(userId)) {
                notifications.push(user, "ROOM", "Added to " + saved.getName(), me.getUsername() + " added you to a room",
                        "/chat/" + saved.getId());
            }
        }
        return toSummary(saved, userId);
    }

    @Transactional
    public RoomSummary addMember(Long actorId, Long roomId, Long userId) {
        requireMember(roomId, actorId);
        ChatRoom room = rooms.findById(roomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        if (room.getType() != RoomType.GROUP) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Direct rooms stay between two people");
        }
        User user = auth.require(userId);
        if (!members.existsByRoomIdAndUserId(roomId, userId)) {
            addMember(room, user);
            notifications.push(user, "ROOM", "Added to " + room.getName(), "You were added to a group room", "/chat/" + roomId);
        }
        return toSummary(room, actorId);
    }

    @Transactional
    public ChatResponse post(String username, Long roomId, String content) {
        User sender = auth.requireUsername(username);
        requireMember(roomId, sender.getId());
        ChatRoom room = rooms.findById(roomId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSender(sender);
        message.setContent(MessageRules.cleanChat(content));
        message.setType(ChatType.CHAT);
        ChatMessage saved = messages.save(message);
        room.setUpdatedAt(LocalDateTime.now());
        metrics.chatSent();
        for (RoomMember member : members.findByRoomId(roomId)) {
            if (!member.getUser().getId().equals(sender.getId())) {
                presence.incrementUnread(member.getUser().getId(), roomId);
                if (room.getType() == RoomType.DIRECT || message.getContent().contains("@" + member.getUser().getUsername())) {
                    notifications.push(member.getUser(), "CHAT", sender.getUsername(), message.getContent(), "/chat/" + roomId);
                }
            }
        }
        ChatResponse response = toChat(saved);
        events.chat(response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> history(Long userId, Long roomId) {
        requireMember(roomId, userId);
        List<ChatMessage> recent = new ArrayList<>(messages.findTop50ByRoomIdOrderByTimestampDesc(roomId));
        recent.sort(Comparator.comparing(ChatMessage::getTimestamp));
        return recent.stream().map(this::toChat).toList();
    }

    @Transactional
    public void markRead(Long userId, Long roomId) {
        requireMember(roomId, userId);
        presence.clearUnread(userId, roomId);
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> search(Long userId, Long roomId, String query) {
        requireMember(roomId, userId);
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search needs at least 2 characters");
        }
        return messages.findTop200ByRoomIdAndContentContainingIgnoreCaseOrderByTimestampDesc(roomId, query.trim())
                .stream().map(this::toChat).toList();
    }

    @Transactional(readOnly = true)
    public String summarize(Long userId, Long roomId) {
        List<String> lines = history(userId, roomId).stream()
                .map(item -> item.senderUsername() + ": " + item.content())
                .toList();
        return ai.summarize(lines);
    }

    @Transactional(readOnly = true)
    public String ask(Long userId, Long roomId, String question) {
        requireMember(roomId, userId);
        String clean = MessageRules.requireText(question, "Question", 500);
        List<String> lines = history(userId, roomId).stream()
                .map(item -> item.senderUsername() + ": " + item.content())
                .toList();
        return ai.ask(clean, lines);
    }

    private void addMember(ChatRoom room, User user) {
        RoomMember member = new RoomMember();
        member.setRoom(room);
        member.setUser(user);
        members.save(member);
    }

    private void requireMember(Long roomId, Long userId) {
        if (!members.existsByRoomIdAndUserId(roomId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not in this room");
        }
    }

    private RoomSummary toSummary(ChatRoom room, Long userId) {
        List<RoomMember> roomMembers = members.findByRoomId(room.getId());
        String name = room.getName();
        if (room.getType() == RoomType.DIRECT) {
            name = roomMembers.stream()
                    .map(RoomMember::getUser)
                    .filter(user -> !user.getId().equals(userId))
                    .map(User::getUsername)
                    .findFirst()
                    .orElse(room.getName());
        }
        List<ChatMessage> latest = messages.findTop50ByRoomIdOrderByTimestampDesc(room.getId());
        String last = latest.isEmpty() ? null : latest.get(0).getContent();
        LocalDateTime at = latest.isEmpty() ? room.getUpdatedAt() : latest.get(0).getTimestamp();
        List<MemberSummary> people = roomMembers.stream()
                .map(member -> new MemberSummary(member.getUser().getId(), member.getUser().getUsername(),
                        member.getUser().getFirstName(), presence.online(member.getUser().getUsername())))
                .toList();
        return new RoomSummary(room.getId(), name, room.getType().name(), last, at, presence.unread(userId, room.getId()), people);
    }

    private ChatResponse toChat(ChatMessage message) {
        return new ChatResponse(message.getId(), message.getRoom().getId(), message.getContent(),
                message.getSender().getUsername(), message.getType().name(), message.getTimestamp());
    }
}
