package com.logmonitor.audit;

import com.logmonitor.entity.AuditLog;
import com.logmonitor.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditService}.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository);
    }

    @Test
    void log_persistsAuditEntry() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.log("admin", AuditAction.USER_LOGIN, "admin");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getAction()).isEqualTo("USER_LOGIN");
        assertThat(saved.getResource()).isEqualTo("admin");
        assertThat(saved.getTimestamp()).isNotNull();
    }

    @Test
    void auditLogFetch_formatsResourceCorrectly() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.auditLogFetch("devuser", 42L, "TOMCAT_LOG");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getAction()).isEqualTo("LOG_FETCH");
        assertThat(captor.getValue().getResource()).isEqualTo("serverId=42, commandKey=TOMCAT_LOG");
    }

    @Test
    void auditServerCreate_recordsServerName() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.auditServerCreate("admin", "prod-tomcat-01");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getAction()).isEqualTo("SERVER_CREATE");
        assertThat(captor.getValue().getResource()).isEqualTo("prod-tomcat-01");
    }

    @Test
    void findAll_delegatesToRepository() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<AuditLog> expected = new PageImpl<>(List.of(new AuditLog()));
        when(auditLogRepository.findAllByOrderByTimestampDesc(pageable)).thenReturn(expected);

        Page<AuditLog> result = auditService.findAll(pageable);

        assertThat(result).isSameAs(expected);
    }
}
