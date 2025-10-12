package com.invoiceapp.client.application.implement;

import com.invoiceapp.auth.domain.entity.User;
import com.invoiceapp.auth.infrastructure.repositories.UserRepository;
import com.invoiceapp.client.application.mapper.ClientMapper;
import com.invoiceapp.client.application.service.ClientService;
import com.invoiceapp.client.domain.entity.Client;
import com.invoiceapp.client.infrastructure.repository.ClientRepository;
import com.invoiceapp.client.presentation.dto.request.ClientRequest;
import com.invoiceapp.client.presentation.dto.response.ClientResponse;
import com.invoiceapp.common.dto.PageDTO;
import com.invoiceapp.common.exception.ResourceConflictException;
import com.invoiceapp.common.exception.ResourceNotFoundException;
import com.invoiceapp.common.specification.BaseSpecification;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ClientMapper clientMapper;
    private final EntityManager entityManager;

    @Override
    @CacheEvict(value = "clients", allEntries = true)
    public ClientResponse createClient(ClientRequest request, UUID userId) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("deletedClientFilter");

        if (clientRepository.existsByEmailAndUserId(request.getEmail(), userId)) {
            throw new ResourceConflictException("A client with email '" + request.getEmail() + "' already exists.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Client client = Client.builder()
                .user(user)
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();

        client = clientRepository.save(client);
        return clientMapper.toResponse(client);
    }

    @Override
    @CacheEvict(value = "clients", allEntries = true)
    public ClientResponse updateClient(UUID id, ClientRequest request, UUID userId) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("deletedClientFilter");

        Client client = clientRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setPhone(request.getPhone());
        client.setAddress(request.getAddress());

        client = clientRepository.save(client);
        return clientMapper.toResponse(client);
    }

    @Override
    @CacheEvict(value = "clients", allEntries = true)
    public void deleteClient(UUID id, UUID userId) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("deletedClientFilter");

        if (!clientRepository.existsByIdAndUserId(id, userId)) {
            throw new ResourceNotFoundException("Client not found");
        }
        clientRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientById(UUID id, UUID userId) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("deletedClientFilter");

        Client client = clientRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        return clientMapper.toResponse(client);
    }

    @Override
    @Cacheable(
            value = "clients",
            key = "#userId + ':' + #page + ':' + #size + ':' + #sortBy + ':' + #sortDir + ':' + #search",
            unless = "#result == null || #result.getContent().isEmpty()"
    )
    @Transactional(readOnly = true)
    public PageDTO<ClientResponse> getAllClients(UUID userId, int page, int size,
                                                 String sortBy, String sortDir, String search) {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("deletedClientFilter");

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Client> spec = Specification.allOf(
                BaseSpecification.<Client>withUserId(userId, "user"),
                BaseSpecification.<Client>withSearch(search, "name")
        );

        Page<Client> clients = clientRepository.findAll(spec, pageable);
        Page<ClientResponse> clientResponsePage = clients.map(clientMapper::toResponse);

        return new PageDTO<>(
                clientResponsePage.getContent(),
                clientResponsePage.getTotalPages(),
                clientResponsePage.getTotalElements(),
                clientResponsePage.getNumber(),
                clientResponsePage.getSize()
        );
    }
}