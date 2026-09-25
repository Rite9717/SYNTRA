package com.syntra.service;

import com.syntra.domain.MailMessage;
import com.syntra.domain.User;
import com.syntra.repo.AttachmentRepository;
import com.syntra.repo.FolderRepository;
import com.syntra.repo.MailRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailServiceTest {
    @Test
    void blocksReadersOutsideTheThread() {
        MailRepository mail = mock(MailRepository.class);
        User sender = user(1L);
        User receiver = user(2L);
        MailMessage message = new MailMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        when(mail.findById(9L)).thenReturn(Optional.of(message));

        AuthService auth = mock(AuthService.class);
        when(auth.require(3L)).thenReturn(user(3L));
        MailService service = new MailService(mail, mock(AttachmentRepository.class), mock(FolderRepository.class),
                auth, mock(NotificationService.class), mock(EventPublisher.class), mock(AiClient.class),
                mock(SyntraMetrics.class), mock(StorageService.class));

        assertThrows(ResponseStatusException.class, () -> service.get(3L, 9L));
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        return user;
    }
}
