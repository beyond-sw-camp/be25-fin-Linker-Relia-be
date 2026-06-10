package com.linker.relia.consultation.controller;

import com.linker.relia.common.dto.response.ApiResponse;
import com.linker.relia.consultation.dto.request.ConsultationCreateRequest;
import com.linker.relia.consultation.dto.response.ConsultationCreateResponse;
import com.linker.relia.consultation.dto.response.ConsultationListResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consultations")
@SecurityRequirement(name = "Bearer Authentication")
public class ConsultationController {

    private final ConsultationService consultationService;

    /**
     * Create a consultation record for the authenticated user.
     *
     * The request body must include one detail object appropriate to `consultationType`
     * (one of `newDetail`, `claimDetail`, `renewalDetail`, or `cancelDetail`).
     *
     * @param request the consultation creation payload
     * @param principalDetails the authenticated principal used to identify the creator
     * @return a ResponseEntity containing an ApiResponse with the created ConsultationCreateResponse;
     *         the response uses HTTP 201 and a success message ("상담일지가 등록되었습니다.")
     */
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
                                                        "proposedProductIds": []
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
                                                        "claimStage": "RECEIPT",
                                                        "claimEventDate": "2026-06-01",
                                                        "claimReasonDetail": "통원 치료",
                                                        "hospitalName": "서울병원",
                                                        "diagnosisOrTreatment": "허리 통증 치료",
                                                        "hospitalizationStatus": "OUTPATIENT",
                                                        "surgeryStatus": "NONE",
                                                        "claimResult": "GUIDED",
                                                        "guidanceSummary": "보험금 청구 필요 서류 안내",
                                                        "claimTypes": ["OUTPATIENT"],
                                                        "reviewTypes": ["COVERAGE_ELIGIBLE"]
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

    /**
     * Retrieve a paginated list of consultation entries.
     *
     * @param pageable pagination and sorting parameters (default page size: 10)
     * @return a ResponseEntity containing an ApiResponse whose payload is a Page of ConsultationListResponse representing the requested page of consultations
     */
    @Operation(summary = "상담일지 목록조회")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ConsultationListResponse>>> getConsultations(
            @ParameterObject
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ConsultationListResponse> response =
                consultationService.getConsultations(pageable);

        return ApiResponse.success(
                HttpStatus.OK,
                "상담일지 목록 조회에 성공했습니다.",
                response
        );
    }
}
