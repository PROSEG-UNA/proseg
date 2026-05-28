package com.sssi.msvc_maintenance.service.impl;

import com.sssi.common.api.response.ApiResponse;
import com.sssi.common.kafka.UserAssignedDomainEvent;
import com.sssi.msvc_maintenance.client.AuthClient;
import com.sssi.msvc_maintenance.dto.response.KeycloakUserResponse;
import com.sssi.msvc_maintenance.entity.Company;
import com.sssi.msvc_maintenance.entity.UserCompany;
import com.sssi.msvc_maintenance.exception.CompanyException;
import com.sssi.msvc_maintenance.exception.UserCompanyException;
import com.sssi.msvc_maintenance.repository.CompanyRepository;
import com.sssi.msvc_maintenance.repository.UserCompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyUserManagementService {

    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthClient authClient;


    @Transactional
    public UserCompany assignUserToCompany(UUID companyId, String keycloakUserId) {
        Company company = findCompany(companyId);

        KeycloakUserResponse keycloakUser;
        try {
            ApiResponse<KeycloakUserResponse> response = authClient.findUserByKeycloakId(keycloakUserId);
            if (response == null || response.getData() == null) {
                throw CompanyException.invalidKeycloakUser(keycloakUserId);
            }
            keycloakUser = response.getData();
        } catch (CompanyException ex) {
            throw ex;
        } catch (Exception ex) {
            throw CompanyException.invalidKeycloakUser(keycloakUserId);
        }

        Optional<UserCompany> existing = userCompanyRepository
                .findByCompanyIdAndKeycloakUserIdIncludingDeleted(companyId, keycloakUserId);

        if (existing.isPresent()) {
            UserCompany uc = existing.get();
            if (!uc.isDeleted()) {
                throw UserCompanyException.duplicateRelation();
            }
            uc.markAsActive();
            uc.setUserEmail(keycloakUser.email());
            UserCompany saved = userCompanyRepository.save(uc);
            company.setUserCompanies(userCompanyRepository.findAllByCompanyId(companyId));
            eventPublisher.publishEvent(new UserAssignedDomainEvent(companyId, List.of(keycloakUserId)));
            return saved;
        }
        UserCompany userCompany = UserCompany.builder()
                .company(company)
                .keycloakUserId(keycloakUserId)
                .userEmail(keycloakUser.email())
                .build();
        UserCompany saved = userCompanyRepository.save(userCompany);
        company.setUserCompanies(userCompanyRepository.findAllByCompanyId(companyId));
        eventPublisher.publishEvent(new UserAssignedDomainEvent(companyId, List.of(keycloakUserId)));
        return saved;
    }

    @Transactional
    public List<UserCompany> assignUsersToCompany(UUID companyId, Collection<String> keycloakUserIds) {
        Company company = findCompany(companyId);
        List<String> normalized = normalizeIds(keycloakUserIds);
        List<String> added = new ArrayList<>();
        List<UserCompany> created = new ArrayList<>();

        for (String kid : normalized) {
            Optional<UserCompany> existing = userCompanyRepository
                    .findByCompanyIdAndKeycloakUserIdIncludingDeleted(companyId, kid);

            if (existing.isPresent()) {
                UserCompany uc = existing.get();
                if (!uc.isDeleted()) continue; // already active
                uc.markAsActive();
                // try to fetch email
                try {
                    ApiResponse<KeycloakUserResponse> response = authClient.findUserByKeycloakId(kid);
                    if (response != null && response.getData() != null) uc.setUserEmail(response.getData().email());
                } catch (Exception ex) {
                    log.debug("Could not fetch email for user {}: {}", kid, ex.getMessage());
                }
                created.add(userCompanyRepository.save(uc));
                added.add(kid);
                continue;
            }

            String userEmail = null;
            try {
                ApiResponse<KeycloakUserResponse> response = authClient.findUserByKeycloakId(kid);
                if (response != null && response.getData() != null) userEmail = response.getData().email();
            } catch (Exception ex) {
                log.debug("Could not fetch email for user {}: {}", kid, ex.getMessage());
            }

            UserCompany createdRelation = userCompanyRepository.save(UserCompany.builder()
                    .company(company)
                    .keycloakUserId(kid)
                    .userEmail(userEmail)
                    .build());
            created.add(createdRelation);
            added.add(kid);
        }

        if (!added.isEmpty()) {
            eventPublisher.publishEvent(new UserAssignedDomainEvent(companyId, added));
        }

        company.setUserCompanies(userCompanyRepository.findAllByCompanyId(companyId));
        return created;
    }

    @Transactional
    public void removeUserFromCompany(UUID companyId, String keycloakUserId) {
        UserCompany userCompany = userCompanyRepository.findByCompanyIdAndKeycloakUserId(companyId, keycloakUserId)
                .orElseThrow(UserCompanyException::notFound);

        userCompanyRepository.delete(userCompany);
    }

    @Transactional
    public List<UserCompany> syncUsers(Company company, Collection<String> requestedKeycloakUserIds) {
        if (requestedKeycloakUserIds == null) {
            return userCompanyRepository.findAllByCompanyId(company.getId());
        }

        List<String> normalizedRequestedIds = normalizeIds(requestedKeycloakUserIds);
        List<UserCompany> currentRelations = userCompanyRepository.findAllByCompanyId(company.getId());
        Map<String, UserCompany> currentByUserId = currentRelations.stream()
                .collect(Collectors.toMap(UserCompany::getKeycloakUserId, relation -> relation));

        List<UserCompany> updatedRelations = new ArrayList<>();
        List<String> addedUserIds = new ArrayList<>();

        for (String requestedId : normalizedRequestedIds) {
            UserCompany existing = currentByUserId.get(requestedId);
            if (existing != null) {
                updatedRelations.add(existing);
                continue;
            }

            // Fetch user email from Keycloak
            String userEmail = null;
            try {
                ApiResponse<KeycloakUserResponse> response = authClient.findUserByKeycloakId(requestedId);
                if (response != null && response.getData() != null) {
                    userEmail = response.getData().email();
                }
            } catch (Exception ex) {
                log.debug("Could not fetch email for user {}: {}", requestedId, ex.getMessage());
            }

            UserCompany created = userCompanyRepository.save(UserCompany.builder()
                    .company(company)
                    .keycloakUserId(requestedId)
                    .userEmail(userEmail)
                    .build());
            updatedRelations.add(created);
            addedUserIds.add(requestedId);
        }

        currentRelations.stream()
                .filter(relation -> !normalizedRequestedIds.contains(relation.getKeycloakUserId()))
                .forEach(userCompanyRepository::delete);

        if (!addedUserIds.isEmpty()) {
            eventPublisher.publishEvent(
                    new UserAssignedDomainEvent(company.getId(), addedUserIds)
            );
        }
        return updatedRelations;
    }

    @Transactional(readOnly = true)
    public List<UserCompany> findByCompanyId(UUID companyId) {
        findCompany(companyId);
        return userCompanyRepository.findAllByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<KeycloakUserResponse> findUsersByCompanyId(UUID companyId) {
        List<String> userIds = findKeycloakUserIdsByCompanyId(companyId);
        return userIds.stream()
                .map(userId -> {
                    ApiResponse<KeycloakUserResponse> response =
                            authClient.findUserByKeycloakId(userId);
                    return response.getData();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> findKeycloakUserIdsByCompanyId(UUID companyId) {
        return findByCompanyId(companyId).stream()
                .map(UserCompany::getKeycloakUserId)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<UserCompany> findByCompanyIdAndKeycloakUserId(UUID companyId, String keycloakUserId) {
        return userCompanyRepository.findByCompanyIdAndKeycloakUserId(companyId, keycloakUserId);
    }

    private Company findCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(CompanyException::notFound);
    }

    private List<String> normalizeIds(Collection<String> requestedKeycloakUserIds) {
        if (requestedKeycloakUserIds == null) {
            return List.of();
        }

        return requestedKeycloakUserIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
    }
}



