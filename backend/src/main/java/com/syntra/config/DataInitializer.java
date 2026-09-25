package com.syntra.config;

import com.syntra.domain.ChatMessage;
import com.syntra.domain.ChatRoom;
import com.syntra.domain.ChatType;
import com.syntra.domain.MailMessage;
import com.syntra.domain.Priority;
import com.syntra.domain.RoomMember;
import com.syntra.domain.RoomType;
import com.syntra.domain.User;
import com.syntra.repo.ChatMessageRepository;
import com.syntra.repo.ChatRoomRepository;
import com.syntra.repo.MailRepository;
import com.syntra.repo.RoomMemberRepository;
import com.syntra.repo.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final MailRepository mail;
    private final ChatRoomRepository rooms;
    private final RoomMemberRepository members;
    private final ChatMessageRepository chat;

    public DataInitializer(UserRepository users, PasswordEncoder encoder, MailRepository mail, ChatRoomRepository rooms,
                           RoomMemberRepository members, ChatMessageRepository chat) {
        this.users = users;
        this.encoder = encoder;
        this.mail = mail;
        this.rooms = rooms;
        this.members = members;
        this.chat = chat;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User admin = ensure("admin", "admin@syntra.local", "Admin@123", "Asha", "ROLE_ADMIN");
        User aisha = ensure("aisha", "aisha@syntra.local", "User@123", "Aisha", "ROLE_USER");
        User kabir = ensure("kabir", "kabir@syntra.local", "User@123", "Kabir", "ROLE_USER");
        if (mail.count() == 0) {
            mail.save(letter(admin, aisha, "Welcome to SYNTRA",
                    "Your account is ready. Use Inbox for private mail and Chat for direct messages and group rooms.",
                    Priority.NORMAL));
            mail.save(letter(kabir, aisha, "Lab deadline moved",
                    "The systems lab deadline is Friday. Please treat this as urgent and confirm you saw it.",
                    Priority.URGENT));
        }
        if (rooms.count() == 0) {
            ChatRoom room = new ChatRoom();
            room.setName("General");
            room.setType(RoomType.GROUP);
            room.setCreatedBy(admin);
            ChatRoom saved = rooms.save(room);
            join(saved, admin);
            join(saved, aisha);
            join(saved, kabir);
            say(saved, admin, "General is the shared room. Direct chats stay between two people.");
            say(saved, aisha, "I will post the lab notes here after class.");
            say(saved, kabir, "@aisha the deadline moved to Friday.");
        }
    }

    private User ensure(String username, String email, String password, String firstName, String role) {
        return users.findByUsername(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(encoder.encode(password));
            user.setFirstName(firstName);
            user.setRoles(Set.of(role));
            return users.save(user);
        });
    }

    private MailMessage letter(User from, User to, String subject, String body, Priority priority) {
        MailMessage message = new MailMessage();
        message.setSender(from);
        message.setReceiver(to);
        message.setSubject(subject);
        message.setBody(body);
        message.setPriority(priority);
        MailMessage saved = mail.save(message);
        saved.setThreadId(saved.getId());
        return saved;
    }

    private void join(ChatRoom room, User user) {
        RoomMember member = new RoomMember();
        member.setRoom(room);
        member.setUser(user);
        members.save(member);
    }

    private void say(ChatRoom room, User user, String content) {
        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSender(user);
        message.setContent(content);
        message.setType(ChatType.CHAT);
        chat.save(message);
    }
}
