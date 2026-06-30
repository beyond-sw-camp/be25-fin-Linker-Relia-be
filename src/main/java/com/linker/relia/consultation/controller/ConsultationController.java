package com.linker.relia.consultation.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.consultation.dto.response.ConsultationDetailResponse;
import com.linker.relia.consultation.dto.response.ConsultationListPageResponse;
import com.linker.relia.consultation.service.ConsultationService;
import com.linker.relia.security.principal.PrincipalDetails;
import com.linker.relia.user.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consultations")
@SecurityRequirement(name = "Bearer Authentication")
public class ConsultationController {

    private final ConsultationService consultationService;

    @Operation(
            summary = "상담일지 등록",
            description = "상담 유형에 따라 newDetail, claimDetail, renewalDetail, cancelDetail 중 하나를 입력합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "신규 상담",
                                            value = """
                                                    {
                                                      "customerId": "고객 UUID",
                                                      "consultationType": "NEW_CONTRACT",
                                                      "consultationChannel": "PHONE",
                                                      "consultedAt": "2026-06-10T11:00:00",
                                                      "specialNote": "신규 가입 상담",
                                                      "nextScheduledAt": "2026-06-15T14:00:00",
                                                      "newDetail": {
                                                        "monthlyIncome": 3000000,
                                                        "hasExistingInsurance": false,
                                                        "monthlyInsurancePremium": 0,
                                                        "existingInsuranceNote": null,
                                                        "insurancePriority": "보장 범위",
                                                        "coverageTypes": ["CANCER", "HEART"],
                                                        "proposedProductCodes": []
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "청구 상담",
                                            value = """
                                                    {
                                                      "customerId": "고객 UUID",
                                                      "contractId": "계약 UUID",
                                                      "consultationType": "CLAIM",
                                                      "consultationChannel": "PHONE",
                                                      "consultedAt": "2026-06-10T11:00:00",
                                                      "specialNote": "보험금 청구 상담",
                                                      "nextScheduledAt": "2026-06-15T14:00:00",
                                                      "claimDetail": {
                                                        "incidentDate": "2026-06-01",
                                                        "claimReason": "통원 치료",
                                                        "hospitalName": "서울병원",
                                                        "diagnosisOrTreatment": "허리 통증 치료",
                                                        "hospitalizationStatus": "OUTPATIENT",
                                                        "surgeryStatus": "NONE",
                                                        "result": "GUIDED",
                                                        "claimType": "OUTPATIENT",
                                                        "reviewItems": ["COVERAGE_ELIGIBLE"],
                                                        "nextActions": ["보험사 접수"]
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "갱신 상담",
                                            value = """
                                                    {
                                                      "customerId": "고객 UUID",
                                                      "contractId": "계약 UUID",
                                                      "consultationType": "RENEWAL",
                                                      "consultationChannel": "PHONE",
                                                      "consultedAt": "2026-06-10T11:00:00",
                                                      "specialNote": "갱신 보험료 안내",
                                                      "nextScheduledAt": "2026-06-15T14:00:00",
                                                      "renewalDetail": {
                                                        "renewalReason": "보험료 갱신",
                                                        "renewalScheduledDate": "2026-07-01",
                                                        "currentPremium": 100000,
                                                        "renewalPremium": 120000,
                                                        "premiumChangeRate": 20.00,
                                                        "coverageChangeType": "NONE",
                                                        "coverageChangeDetail": null,
                                                        "customerReaction": "POSITIVE",
                                                        "consultationResult": "RENEWAL_ACCEPTED",
                                                        "premiumChangeReasonTypes": ["AGE_INCREASE"],
                                                        "otherReason": null,
                                                        "interestTypes": ["PREMIUM"]
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "해지 상담",
                                            value = """
                                                    {
                                                      "customerId": "고객 UUID",
                                                      "contractId": "계약 UUID",
                                                      "consultationType": "TERMINATION",
                                                      "consultationChannel": "PHONE",
                                                      "consultedAt": "2026-06-10T11:00:00",
                                                      "specialNote": "해지 의사 확인 상담",
                                                      "nextScheduledAt": "2026-06-15T14:00:00",
                                                      "cancelDetail": {
                                                        "premiumBurden": true,
                                                        "renewalPremiumBurden": false,
                                                        "paymentDifficulty": false,
                                                        "coverageDissatisfaction": false,
                                                        "duplicateInsurance": false,
                                                        "productRemodelingReview": true,
                                                        "comparingOtherCompany": false,
                                                        "movingToOtherCompany": false,
                                                        "plannerContactDissatisfaction": false,
                                                        "managementDissatisfaction": false,
                                                        "retentionPossibility": "MEDIUM"
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )

    @PostMapping
    public ResponseEntity<ApiResponse<ConsultationCreateResponse>> createConsultation(
            @Valid @RequestBody ConsultationCreateRequest request,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        User fp = principalDetails.getUser();

        ConsultationCreateResponse response =
                consultationService.createConsultation(request, fp);

        return ApiResponse.success(
                HttpStatus.CREATED,
                "상담일지가 등록되었습니다.",
                response
        );
    }

    @Operation(summary = "상담일지 목록조회")
    @GetMapping
    public ResponseEntity<ApiResponse<ConsultationListPageResponse>> getConsultations(
            @ParameterObject
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String organizationCode,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        ConsultationListPageResponse response =
                consultationService.getConsultations(pageable, principalDetails, organizationCode);

        return ApiResponse.success(
                HttpStatus.OK,
                "상담일지 목록 조회에 성공했습니다.",
                response
        );
    }

    @Operation(summary = "상담일지 상세조회")
    @GetMapping("/{consultationId}")
    public ResponseEntity<ApiResponse<ConsultationDetailResponse>> getConsultationDetail(
            @PathVariable UUID consultationId,
            @AuthenticationPrincipal PrincipalDetails principalDetails
    ) {
        User fp = principalDetails.getUser();

        ConsultationDetailResponse response =
                consultationService.getConsultationDetail(consultationId, fp);

        return ApiResponse.success(
                HttpStatus.OK,
                "상담일지 상세 조회에 성공했습니다.",
                response
        );
    }
}
