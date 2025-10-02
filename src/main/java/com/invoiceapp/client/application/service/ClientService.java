package com.invoiceapp.client.application.service;

import com.invoiceapp.client.presentation.dto.request.ClientRequest;
import com.invoiceapp.client.presentation.dto.response.ClientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ClientService {
    ClientResponse createClient(ClientRequest request, UUID userId);
    ClientResponse updateClient(UUID clientId, ClientRequest request, UUID userId);
    void deleteClient(UUID clientId, UUID userId);
    ClientResponse getClientById(UUID clientId, UUID userId);
    Page<ClientResponse> getAllClients(UUID userId, Pageable pageable);
}