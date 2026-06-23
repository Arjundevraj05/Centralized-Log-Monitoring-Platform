package com.logmonitor.service;

import com.logmonitor.dto.ServerRequest;
import com.logmonitor.entity.Server;
import com.logmonitor.exception.ResourceNotFoundException;
import com.logmonitor.mapper.ServerMapper;
import com.logmonitor.repository.ServerRepository;
import com.logmonitor.util.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ServerService}.
 */
@ExtendWith(MockitoExtension.class)
class ServerServiceTest {

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private ServerMapper serverMapper;

    @Mock
    private EncryptionService encryptionService;

    private ServerService serverService;

    @BeforeEach
    void setUp() {
        serverService = new ServerService(serverRepository, serverMapper, encryptionService);
    }

    @Test
    void create_encryptsPrivateKey() {
        ServerRequest request = new ServerRequest();
        request.setServerName("prod-01");
        request.setHost("10.0.0.1");
        request.setPort(22);
        request.setUsername("deploy");
        request.setPrivateKey("pem-key");
        request.setEnvironment("prod");
        request.setActive(true);

        when(serverRepository.existsByServerName("prod-01")).thenReturn(false);
        when(encryptionService.encrypt("pem-key")).thenReturn("encrypted-key");
        when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> {
            Server saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(serverMapper.toResponse(any())).thenReturn(new com.logmonitor.dto.ServerResponse());

        serverService.create(request);

        ArgumentCaptor<Server> captor = ArgumentCaptor.forClass(Server.class);
        verify(serverRepository).save(captor.capture());
        assertThat(captor.getValue().getEncryptedPrivateKey()).isEqualTo("encrypted-key");
    }

    @Test
    void findById_throwsWhenMissing() {
        when(serverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serverService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
