package com.logmonitor.controller;

import com.logmonitor.dto.ServerRequest;
import com.logmonitor.dto.ServerResponse;
import com.logmonitor.service.ServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for SSH server management.
 */
@RestController
@RequestMapping("/api/servers")
@Tag(name = "Servers", description = "SSH server registration and management")
@SecurityRequirement(name = "bearerAuth")
public class ServerController {

    private final ServerService serverService;

    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }

    /**
     * Returns all registered servers.
     *
     * @return list of servers
     */
    @GetMapping
    @Operation(summary = "List servers", description = "Returns all registered SSH servers")
    @ApiResponse(responseCode = "200", description = "Servers retrieved successfully")
    public ResponseEntity<List<ServerResponse>> getAllServers() {
        return ResponseEntity.ok(serverService.findAll());
    }

    /**
     * Returns a server by ID.
     *
     * @param id server identifier
     * @return server details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get server", description = "Returns a single server by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Server found"),
            @ApiResponse(responseCode = "404", description = "Server not found")
    })
    public ResponseEntity<ServerResponse> getServer(@PathVariable Long id) {
        return ResponseEntity.ok(serverService.findById(id));
    }

    /**
     * Registers a new SSH server.
     *
     * @param request server details including PEM private key
     * @return created server
     */
    @PostMapping
    @Operation(summary = "Create server", description = "Registers a new SSH server (ADMIN only)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Server created",
                    content = @Content(schema = @Schema(implementation = ServerResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate name")
    })
    public ResponseEntity<ServerResponse> createServer(@Valid @RequestBody ServerRequest request) {
        ServerResponse created = serverService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing server.
     *
     * @param id      server identifier
     * @param request updated server details
     * @return updated server
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update server", description = "Updates an existing server (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Server updated"),
            @ApiResponse(responseCode = "404", description = "Server not found")
    })
    public ResponseEntity<ServerResponse> updateServer(@PathVariable Long id,
                                                       @Valid @RequestBody ServerRequest request) {
        return ResponseEntity.ok(serverService.update(id, request));
    }

    /**
     * Deletes a server.
     *
     * @param id server identifier
     * @return empty response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete server", description = "Removes a server registration (ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Server deleted"),
            @ApiResponse(responseCode = "404", description = "Server not found")
    })
    public ResponseEntity<Void> deleteServer(@PathVariable Long id) {
        serverService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
