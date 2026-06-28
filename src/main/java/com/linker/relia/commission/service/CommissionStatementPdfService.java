package com.linker.relia.commission.service;

import com.linker.relia.security.principal.PrincipalDetails;

import java.util.UUID;

public interface CommissionStatementPdfService {
    byte[] generateHqStatement(PrincipalDetails principalDetails, String closingMonth);

    byte[] generateBranchStatement(PrincipalDetails principalDetails, String closingMonth, String organizationCode);

    byte[] generateFpStatement(PrincipalDetails principalDetails, String closingMonth, UUID fpId);
}
