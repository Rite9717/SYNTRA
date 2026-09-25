package com.syntra.web;

import com.syntra.dto.AddMemberRequest;
import com.syntra.dto.AiAskRequest;
import com.syntra.dto.AiDraftRequest;
import com.syntra.dto.AiTextResponse;
import com.syntra.dto.ChatResponse;
import com.syntra.dto.DirectRoomRequest;
import com.syntra.dto.RoomRequest;
import com.syntra.dto.RoomSummary;
import com.syntra.security.CurrentUser;
import com.syntra.service.AiClient;
import com.syntra.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RoomController {
    private final RoomService rooms;
    private final AiClient ai;

    public RoomController(RoomService rooms, AiClient ai) {
        this.rooms = rooms;
        this.ai = ai;
    }

    @GetMapping("/rooms")
    public List<RoomSummary> list() {
        return rooms.list(CurrentUser.get().getId());
    }

    @PostMapping("/rooms")
    public RoomSummary create(@Valid @RequestBody RoomRequest request) {
        return rooms.createGroup(CurrentUser.get().getId(), request.name(), request.memberIds());
    }

    @PostMapping("/rooms/direct")
    public RoomSummary direct(@Valid @RequestBody DirectRoomRequest request) {
        return rooms.openDirect(CurrentUser.get().getId(), request.userId());
    }

    @PostMapping("/rooms/{id}/members")
    public RoomSummary add(@PathVariable Long id, @Valid @RequestBody AddMemberRequest request) {
        return rooms.addMember(CurrentUser.get().getId(), id, request.userId());
    }

    @GetMapping("/rooms/{id}/messages")
    public List<ChatResponse> history(@PathVariable Long id) {
        return rooms.history(CurrentUser.get().getId(), id);
    }

    @PostMapping("/rooms/{id}/read")
    public Map<String, String> read(@PathVariable Long id) {
        rooms.markRead(CurrentUser.get().getId(), id);
        return Map.of("message", "Read");
    }

    @GetMapping("/rooms/{id}/search")
    public List<ChatResponse> search(@PathVariable Long id, @RequestParam String q) {
        return rooms.search(CurrentUser.get().getId(), id, q);
    }

    @PostMapping("/rooms/{id}/summarize")
    public AiTextResponse summarize(@PathVariable Long id) {
        return new AiTextResponse(rooms.summarize(CurrentUser.get().getId(), id));
    }

    @PostMapping("/ai/ask")
    public AiTextResponse ask(@RequestBody AiAskRequest request) {
        return new AiTextResponse(rooms.ask(CurrentUser.get().getId(), request.roomId(), request.question()));
    }

    @PostMapping("/ai/draft")
    public AiTextResponse draft(@RequestBody AiDraftRequest request) {
        CurrentUser.get();
        return new AiTextResponse(ai.draft(request.subject(), request.instruction(), request.context()));
    }
}
