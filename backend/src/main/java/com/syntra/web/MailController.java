package com.syntra.web;

import com.syntra.domain.Folder;
import com.syntra.dto.AiSummaryRequest;
import com.syntra.dto.AiTextResponse;
import com.syntra.dto.AttachmentResponse;
import com.syntra.dto.FolderRequest;
import com.syntra.dto.ForwardRequest;
import com.syntra.dto.MailRequest;
import com.syntra.dto.MailResponse;
import com.syntra.dto.MoveFolderRequest;
import com.syntra.dto.ReplyRequest;
import com.syntra.security.CurrentUser;
import com.syntra.service.MailService;
import com.syntra.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MailController {
    private final MailService mail;
    private final StorageService storage;

    public MailController(MailService mail, StorageService storage) {
        this.mail = mail;
        this.storage = storage;
    }

    @PostMapping("/messages")
    public MailResponse send(@Valid @RequestBody MailRequest request) {
        return mail.send(CurrentUser.get().getId(), request);
    }

    @GetMapping("/messages/inbox")
    public List<MailResponse> inbox() {
        return mail.inbox(CurrentUser.get().getId());
    }

    @GetMapping("/messages/sent")
    public List<MailResponse> sent() {
        return mail.sent(CurrentUser.get().getId());
    }

    @GetMapping("/messages/starred")
    public List<MailResponse> starred() {
        return mail.starred(CurrentUser.get().getId());
    }

    @GetMapping("/messages/archived")
    public List<MailResponse> archived() {
        return mail.archived(CurrentUser.get().getId());
    }

    @GetMapping("/messages/search")
    public List<MailResponse> search(@RequestParam String q) {
        return mail.search(CurrentUser.get().getId(), q);
    }

    @GetMapping("/messages/unread-count")
    public Map<String, Long> unread() {
        return Map.of("count", mail.unread(CurrentUser.get().getId()));
    }

    @GetMapping("/messages/folder/{folderId}")
    public List<MailResponse> byFolder(@PathVariable Long folderId) {
        return mail.byFolder(CurrentUser.get().getId(), folderId);
    }

    @GetMapping("/messages/{id}")
    public MailResponse one(@PathVariable Long id) {
        return mail.get(CurrentUser.get().getId(), id);
    }

    @GetMapping("/messages/{id}/thread")
    public List<MailResponse> thread(@PathVariable Long id) {
        return mail.thread(CurrentUser.get().getId(), id);
    }

    @PostMapping("/messages/{id}/reply")
    public MailResponse reply(@PathVariable Long id, @Valid @RequestBody ReplyRequest request) {
        return mail.reply(CurrentUser.get().getId(), id, request);
    }

    @PostMapping("/messages/{id}/forward")
    public MailResponse forward(@PathVariable Long id, @Valid @RequestBody ForwardRequest request) {
        return mail.forward(CurrentUser.get().getId(), id, request);
    }

    @PutMapping("/messages/{id}/read")
    public MailResponse read(@PathVariable Long id) {
        return mail.get(CurrentUser.get().getId(), id);
    }

    @PutMapping("/messages/{id}/star")
    public MailResponse star(@PathVariable Long id) {
        return mail.star(CurrentUser.get().getId(), id);
    }

    @PutMapping("/messages/{id}/archive")
    public MailResponse archive(@PathVariable Long id) {
        return mail.archive(CurrentUser.get().getId(), id);
    }

    @PutMapping("/messages/{id}/folder")
    public MailResponse move(@PathVariable Long id, @RequestBody MoveFolderRequest request) {
        return mail.move(CurrentUser.get().getId(), id, request.folderId());
    }

    @DeleteMapping("/messages/{id}")
    public Map<String, String> delete(@PathVariable Long id) {
        mail.delete(CurrentUser.get().getId(), id);
        return Map.of("message", "Message deleted");
    }

    @PostMapping(value = "/messages/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentResponse attach(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return mail.attach(CurrentUser.get().getId(), id, file);
    }

    @GetMapping("/attachments/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        var attachment = mail.loadAttachment(CurrentUser.get().getId(), id);
        Resource resource = storage.load(attachment.getStorageKey());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .body(resource);
    }

    @GetMapping("/folders")
    public List<Map<String, Object>> folders() {
        return mail.folders(CurrentUser.get().getId()).stream()
                .map(folder -> Map.<String, Object>of("id", folder.getId(), "name", folder.getName()))
                .toList();
    }

    @PostMapping("/folders")
    public Map<String, Object> createFolder(@Valid @RequestBody FolderRequest request) {
        Folder folder = mail.createFolder(CurrentUser.get().getId(), request.name());
        return Map.of("id", folder.getId(), "name", folder.getName());
    }

    @PostMapping("/ai/summarize")
    public AiTextResponse summarize(@RequestBody AiSummaryRequest request) {
        Long userId = CurrentUser.get().getId();
        String text = "room".equalsIgnoreCase(request.kind())
                ? "Use /api/ai/ask for rooms"
                : mail.summarizeThread(userId, request.id());
        return new AiTextResponse(text);
    }
}
