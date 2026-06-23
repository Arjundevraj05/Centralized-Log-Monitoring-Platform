package com.logmonitor.mapper;

import com.logmonitor.dto.AuditLogResponse;
import com.logmonitor.entity.AuditLog;
import org.springframework.stereotype.Component;

/**
 * Maps audit log entities to API response DTOs.
 */
@Component
public class AuditLogMapper {

    /**
     * Converts an audit log entity to a response DTO.
     *
     * @param auditLog the entity
     * @return response DTO
     */
    public AuditLogResponse toResponse(AuditLog auditLog) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(auditLog.getId());
        response.setUsername(auditLog.getUsername());
        response.setAction(auditLog.getAction());
        response.setResource(auditLog.getResource());
        response.setTimestamp(auditLog.getTimestamp());
        return response;
    }
}
