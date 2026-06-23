package com.logmonitor.mapper;

import com.logmonitor.dto.ServerResponse;
import com.logmonitor.entity.Server;
import org.springframework.stereotype.Component;

/**
 * Maps server entities to API response DTOs.
 */
@Component
public class ServerMapper {

    /**
     * Converts a server entity to a response DTO without exposing the private key.
     *
     * @param server the entity
     * @return response DTO
     */
    public ServerResponse toResponse(Server server) {
        ServerResponse response = new ServerResponse();
        response.setId(server.getId());
        response.setServerName(server.getServerName());
        response.setHost(server.getHost());
        response.setPort(server.getPort());
        response.setUsername(server.getUsername());
        response.setEnvironment(server.getEnvironment());
        response.setActive(server.isActive());
        response.setCreatedAt(server.getCreatedAt());
        return response;
    }
}
