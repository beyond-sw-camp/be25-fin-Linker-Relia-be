package com.linker.relia.hr.service;

import com.linker.relia.hr.dto.HrClosingProcessRequest;
import com.linker.relia.hr.dto.HrClosingProcessResponse;
import com.linker.relia.hr.dto.HrClosingSummaryResponse;
import com.linker.relia.hr.dto.HrClosingUserListResponse;
import com.linker.relia.hr.dto.OrganizationClosingListResponse;

public interface HrClosingService {
    HrClosingSummaryResponse getSummary(String closingMonth);

    OrganizationClosingListResponse getOrganizations(String closingMonth);

    HrClosingUserListResponse getUsers(String closingMonth);

    HrClosingProcessResponse close(HrClosingProcessRequest request);

    boolean isClosed(String closingMonth);
}
