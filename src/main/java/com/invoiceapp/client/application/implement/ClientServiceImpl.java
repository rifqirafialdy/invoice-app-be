package com.invoiceapp.client.application.implement;

import com.invoiceapp.auth.domain.entity.User;
import com.invoiceapp.auth.infrastructure.repositories.UserRepository;
import com.invoiceapp.client.application.service.ClientService;
import com.invoiceapp.client.domain.entity.Client;
import com.invoiceapp.client.infrastructure.repository.ClientRepository;
import com.invoiceapp.client.presentation.dto.request.ClientRequest;
import com.invoiceapp.client.presentation.dto.response.ClientResponse;
import com.invoiceapp.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Override
    public ClientResponse createClient(ClientRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Client client = Client.builder()
                .user(user)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .paymentPreferences(request.getPaymentPreferences())
                .build();

        client = clientRepository.save(client);
        return mapToResponse(client);
    }

    @Override
    public ClientResponse updateClient(UUID clientId, ClientRequest request, UUID userId) {
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setAddress(request.getAddress());
        client.setPaymentPreferences(request.getPaymentPreferences());

        client = clientRepository.save(client);
        return mapToResponse(client);
    }

    @Override
    public void deleteClient(UUID clientId, UUID userId) {
        if (!clientRepository.existsByIdAndUserId(clientId, userId)) {
            throw new ResourceNotFoundException("Client not found");
        }
        clientRepository.deleteById(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientById(UUID clientId, UUID userId) {
        Client client = clientRepository.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        return mapToResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientResponse> getAllClients(UUID userId, Pageable pageable) {
        return clientRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    private ClientResponse mapToResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .address(client.getAddress())
                .paymentPreferences(client.getPaymentPreferences())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }
}