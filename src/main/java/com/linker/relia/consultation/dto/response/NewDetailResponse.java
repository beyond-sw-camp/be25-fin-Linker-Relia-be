package com.linker.relia.consultation.dto.response;

import com.linker.relia.consultation.domain.ConsultationNewCoverageNeed;
import com.linker.relia.consultation.domain.ConsultationNewDetail;
import com.linker.relia.consultation.domain.ConsultationNewProposedProduct;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class NewDetailResponse {

    private BigDecimal monthlyIncome;
    private Boolean hasExistingInsurance;
    private BigDecimal monthlyInsurancePremium;
    private String existingInsuranceNote;
    private String insurancePriority;

    private List<String> coverageTypes;
    private List<ProposedProductResponse> proposedProducts;

    public static NewDetailResponse from(
            ConsultationNewDetail detail,
            List<ConsultationNewCoverageNeed> coverageNeeds,
            List<ConsultationNewProposedProduct> proposedProducts
    ) {
        return NewDetailResponse.builder()
                .monthlyIncome(detail.getMonthlyIncome())
                .hasExistingInsurance(detail.getHasExistingInsurance())
                .monthlyInsurancePremium(detail.getMonthlyInsurancePremium())
                .existingInsuranceNote(detail.getExistingInsuranceNote())
                .insurancePriority(detail.getInsurancePriority())
                .coverageTypes(
                        coverageNeeds.stream()
                                .map(ConsultationNewCoverageNeed::getCoverageType)
                                .toList()
                )
                .proposedProducts(
                        proposedProducts.stream()
                                .map(ProposedProductResponse::from)
                                .toList()
                )
                .build();
    }

    @Getter
    @Builder
    public static class ProposedProductResponse {
        private UUID insuranceProductId;
        private String insuranceProductName;

        public static ProposedProductResponse from(
                ConsultationNewProposedProduct proposedProduct
        ) {
            return ProposedProductResponse.builder()
                    .insuranceProductId(proposedProduct.getInsuranceProduct().getId())
                    .insuranceProductName(proposedProduct.getInsuranceProductName())
                    .build();
        }
    }
}