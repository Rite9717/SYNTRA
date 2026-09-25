package com.syntra.service;

import com.syntra.domain.Attachment;
import com.syntra.domain.Folder;
import com.syntra.domain.MailMessage;
import com.syntra.domain.User;
import com.syntra.dto.AttachmentResponse;
import com.syntra.dto.ForwardRequest;
import com.syntra.dto.MailRequest;
import com.syntra.dto.MailResponse;
import com.syntra.dto.ReplyRequest;
import com.syntra.repo.AttachmentRepository;
import com.syntra.repo.FolderRepository;
import com.syntra.repo.MailRepository;
import com.syntra.support.MessageRules;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MailService {
    private final MailRepository mail;
    private final AttachmentRepository attachments;
    private final FolderRepository folders;
    private final AuthService auth;
    private final NotificationService notifications;
    private final EventPublisher events;
    private final AiClient ai;
    private final SyntraMetrics metrics;
    private final StorageService storage;

    public MailService(MailRepository mail, AttachmentRepository attachments, FolderRepository folders, AuthService auth,
                       NotificationService notifications, EventPublisher events, AiClient ai, SyntraMetrics metrics,
                       StorageService storage) {
        this.mail = mail;
        this.attachments = attachments;
        this.folders = folders;
        this.auth = auth;
        this.notifications = notifications;
        this.events = events;
        this.ai = ai;
        this.metrics = metrics;
        this.storage = storage;
    }

    @Transactional
    public MailResponse send(Long senderId, MailRequest request) {
        User sender = auth.require(senderId);
        User receiver = auth.require(request.receiverId());
        if (sender.getId().equals(receiver.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose another recipient");
        }
        MailMessage saved = persist(sender, receiver, MessageRules.requireText(request.subject(), "Subject", 200),
                MessageRules.requireText(request.body(), "Body", 8000), null, null);
        return toResponse(saved);
    }

    @Transactional
    public MailResponse reply(Long userId, Long messageId, ReplyRequest request) {
        MailMessage parent = loadParticipant(userId, messageId);
        User me = auth.require(userId);
        User other = parent.getSender().getId().equals(userId) ? parent.getReceiver() : parent.getSender();
        String subject = parent.getSubject().startsWith("Re:") ? parent.getSubject() : "Re: " + parent.getSubject();
        MailMessage saved = persist(me, other, subject, MessageRules.requireText(request.body(), "Body", 8000),
                parent.getThreadId(), parent.getId());
        return toResponse(saved);
    }

    @Transactional
    public MailResponse forward(Long userId, Long messageId, ForwardRequest request) {
        MailMessage source = loadParticipant(userId, messageId);
        User me = auth.require(userId);
        User receiver = auth.require(request.receiverId());
        String note = request.note() == null ? "" : request.note().trim();
        String body = (note.isEmpty() ? "" : note + "\n\n") + "---------- Forwarded ----------\n"
                + source.getSender().getUsername() + ": " + source.getBody();
        String subject = source.getSubject().startsWith("Fwd:") ? source.getSubject() : "Fwd: " + source.getSubject();
        return toResponse(persist(me, receiver, subject, body, null, null));
    }

    @Transactional(readOnly = true)
    public List<MailResponse> inbox(Long userId) {
        return mail.findByReceiverIdAndDeletedFalseAndArchivedFalseOrderBySentAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MailResponse> sent(Long userId) {
        return mail.findBySenderIdAndDeletedFalseOrderBySentAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MailResponse> starred(Long userId) {
        return mail.findByReceiverIdAndDeletedFalseAndStarredTrueOrderBySentAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MailResponse> archived(Long userId) {
        return mail.findByReceiverIdAndDeletedFalseAndArchivedTrueOrderBySentAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MailResponse> byFolder(Long userId, Long folderId) {
        folders.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        return mail.findByFolderIdAndReceiverIdAndDeletedFalseOrderBySentAtDesc(folderId, userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MailResponse> search(Long userId, String query) {
        if (query == null || query.trim().length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search needs at least 2 characters");
        }
        return mail.search(userId, query.trim()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public MailResponse get(Long userId, Long id) {
        MailMessage message = loadParticipant(userId, id);
        if (message.getReceiver().getId().equals(userId) && !message.isReadFlag()) {
            message.setReadFlag(true);
            message.setReadAt(LocalDateTime.now());
        }
        return toResponse(message);
    }

    @Transactional(readOnly = true)
    public List<MailResponse> thread(Long userId, Long id) {
        MailMessage message = loadParticipant(userId, id);
        return mail.findByThreadIdAndDeletedFalseOrderBySentAtAsc(message.getThreadId()).stream()
                .filter(item -> item.getSender().getId().equals(userId) || item.getReceiver().getId().equals(userId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MailResponse star(Long userId, Long id) {
        MailMessage message = loadParticipant(userId, id);
        message.setStarred(!message.isStarred());
        return toResponse(message);
    }

    @Transactional
    public MailResponse archive(Long userId, Long id) {
        MailMessage message = loadParticipant(userId, id);
        if (!message.getReceiver().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the recipient can archive this message");
        }
        message.setArchived(!message.isArchived());
        return toResponse(message);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        MailMessage message = loadParticipant(userId, id);
        message.setDeleted(true);
    }

    @Transactional
    public MailResponse move(Long userId, Long id, Long folderId) {
        MailMessage message = loadParticipant(userId, id);
        if (folderId == null) {
            message.setFolder(null);
            return toResponse(message);
        }
        Folder folder = folders.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));
        message.setFolder(folder);
        return toResponse(message);
    }

    public long unread(Long userId) {
        return mail.countByReceiverIdAndDeletedFalseAndReadFlagFalse(userId);
    }

    @Transactional
    public AttachmentResponse attach(Long userId, Long messageId, MultipartFile file) throws IOException {
        MailMessage message = loadParticipant(userId, messageId);
        if (!message.getSender().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the sender can attach files");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a file");
        }
        Attachment attachment = new Attachment();
        attachment.setMessage(message);
        attachment.setOriginalName(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        attachment.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        attachment.setSizeBytes(file.getSize());
        attachment.setStorageKey(storage.store(file));
        Attachment saved = attachments.save(attachment);
        return new AttachmentResponse(saved.getId(), saved.getOriginalName(), saved.getContentType(), saved.getSizeBytes());
    }

    @Transactional(readOnly = true)
    public Attachment loadAttachment(Long userId, Long attachmentId) {
        Attachment attachment = messageAttachment(attachmentId);
        loadParticipant(userId, attachment.getMessage().getId());
        return attachment;
    }

    @Transactional
    public List<Folder> folders(Long userId) {
        return folders.findByUserIdOrderByNameAsc(userId);
    }

    @Transactional
    public Folder createFolder(Long userId, String name) {
        String clean = MessageRules.requireText(name, "Folder name", 80);
        if (folders.existsByUserIdAndNameIgnoreCase(userId, clean)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Folder already exists");
        }
        Folder folder = new Folder();
        folder.setUser(auth.require(userId));
        folder.setName(clean);
        return folders.save(folder);
    }

    @Transactional(readOnly = true)
    public String summarizeThread(Long userId, Long id) {
        List<String> lines = thread(userId, id).stream()
                .map(item -> item.senderUsername() + ": " + item.body())
                .toList();
        return ai.summarize(lines);
    }

    public MailMessage loadParticipant(Long userId, Long id) {
        MailMessage message = mail.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        if (message.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found");
        }
        boolean participant = message.getSender().getId().equals(userId) || message.getReceiver().getId().equals(userId);
        if (!participant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot read this message");
        }
        return message;
    }

    private Attachment messageAttachment(Long attachmentId) {
        return attachments.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
    }

    private MailMessage persist(User sender, User receiver, String subject, String body, Long threadId, Long parentId) {
        MailMessage message = new MailMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setSubject(subject);
        message.setBody(body);
        message.setPriority(ai.priority(subject, body));
        message.setParentId(parentId);
        message.setThreadId(threadId);
        MailMessage saved = mail.save(message);
        if (saved.getThreadId() == null) {
            saved.setThreadId(saved.getId());
            saved = mail.save(saved);
        }
        metrics.mailSent();
        notifications.push(receiver, "MAIL", "New message from " + sender.getUsername(), subject, "/inbox/" + saved.getId());
        events.mail(receiver.getId(), subject);
        return saved;
    }

    private MailResponse toResponse(MailMessage message) {
        List<AttachmentResponse> files = message.getAttachments().stream()
                .map(file -> new AttachmentResponse(file.getId(), file.getOriginalName(), file.getContentType(), file.getSizeBytes()))
                .toList();
        return new MailResponse(
                message.getId(),
                message.getSender().getId(), message.getSender().getUsername(), display(message.getSender()),
                message.getReceiver().getId(), message.getReceiver().getUsername(), display(message.getReceiver()),
                message.getSubject(), message.getBody(), message.getSentAt(), message.getReadAt(),
                message.isReadFlag(), message.isStarred(), message.isArchived(), message.getPriority().name(),
                message.getThreadId(), message.getParentId(),
                message.getFolder() == null ? null : message.getFolder().getId(),
                message.getFolder() == null ? null : message.getFolder().getName(),
                files);
    }

    private String display(User user) {
        if (user.getFirstName() == null || user.getFirstName().isBlank()) {
            return user.getUsername();
        }
        return user.getFirstName();
    }

}
