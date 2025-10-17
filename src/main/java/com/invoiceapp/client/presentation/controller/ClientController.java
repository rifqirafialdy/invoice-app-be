package com.invoiceapp.client.presentation.controller;

import com.invoiceapp.client.application.service.ClientService;
import com.invoiceapp.client.presentation.dto.request.ClientRequest;
import com.invoiceapp.client.presentation.dto.response.ClientResponse;
import com.invoiceapp.common.dto.ApiResponse;
import com.invoiceapp.common.dto.PageDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(
            @Valid @RequestBody ClientRequest request,
            @RequestAttribute("userId") UUID userId
    ) {
        ClientResponse response = clientService.createClient(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Client created successfully", response));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<ApiResponse<ClientResponse>> updateClient(
            @PathVariable UUID clientId,
            @Valid @RequestBody ClientRequest request,
            @RequestAttribute("userId") UUID userId
    ) {
        ClientResponse response = clientService.updateClient(clientId, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Client updated successfully", response));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<ApiResponse<Void>> deleteClient(
            @PathVariable UUID clientId,
            @RequestAttribute("userId") UUID userId
    ) {
        clientService.deleteClient(clientId, userId);
        return ResponseEntity.ok(ApiResponse.success("Client deleted successfully", null));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<ApiResponse<ClientResponse>> getClient(
            @PathVariable UUID clientId,
            @RequestAttribute("userId") UUID userId
    ) {
        ClientResponse response = clientService.getClientById(clientId, userId);
        return ResponseEntity.ok(ApiResponse.success("Client retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ClientResponse>>> getAllClients(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search) {

        PageDTO<ClientResponse> clientDto = clientService.getAllClients(userId, page, size, sortBy, sortDir, search);

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ClientResponse> clientsPage = new PageImpl<>(clientDto.getContent(), pageable, clientDto.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success("Clients retrieved successfully", clientsPage));
    }

}