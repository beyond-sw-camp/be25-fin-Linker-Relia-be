package com.linker.relia.organization.service;

import com.linker.relia.organization.domain.Organization;
import com.linker.relia.organization.domain.OrganizationStatus;
import com.linker.relia.organization.domain.OrganizationType;
import com.linker.relia.organization.dto.BranchOrganizationResponse;
import com.linker.relia.organization.dto.OrganizationChartItemResponse;
import com.linker.relia.organization.dto.OrganizationChartRequest;
import com.linker.relia.organization.dto.OrganizationChartResponse;
import com.linker.relia.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {
    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BranchOrganizationResponse> getBranchOrganizations() {
        return organizationRepository
                .findAllByOrganizationTypeAndOrganizationStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
                        OrganizationType.BRANCH,
                        OrganizationStatus.ACTIVE
                )
                .stream()
                .map(BranchOrganizationResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationChartResponse getOrganizationChart(OrganizationChartRequest request) {
        List<Organization> organizations = request.getStatus() == null
                ? organizationRepository.findAllByDeletedAtIsNullOrderByCreatedAtAsc()
                : organizationRepository.findAllByOrganizationStatusAndDeletedAtIsNullOrderByCreatedAtAsc(request.getStatus());

        Map<UUID, OrganizationChartItemResponse> organizationMap = new LinkedHashMap<>();
        organizations.forEach(organization ->
                organizationMap.put(organization.getId(), OrganizationChartItemResponse.from(organization)));

        List<OrganizationChartItemResponse> roots = new ArrayList<>();
        organizationMap.values().forEach(organization -> {
            UUID parentOrganizationId = organization.getParentOrganizationId();
            OrganizationChartItemResponse parent = organizationMap.get(parentOrganizationId);

            if (parent == null) {
                roots.add(organization);
                return;
            }

            parent.getChildren().add(organization);
        });

        return OrganizationChartResponse.builder()
                .organizations(roots)
                .build();
    }
}
