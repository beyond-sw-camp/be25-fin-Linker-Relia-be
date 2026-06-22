package com.linker.relia.consultation.service.stt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.consultation.dto.response.ConsultationAiResolutionResponse;
import com.linker.relia.consultation.repository.stt.ConsultationAiNoteRepository;
import com.linker.relia.contract.repository.ContractRepository;
import com.linker.relia.customer.repository.DiseaseCodeRepository;
import com.linker.relia.insurance.domain.InsuranceCompany;
import com.linker.relia.insurance.domain.InsuranceProduct;
import com.linker.relia.insurance.repository.InsuranceProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConsultationAiNoteServiceImplTest {

    @Mock
    private ConsultationSttSessionService consultationSttSessionService;

    @Mock
    private ConsultationAiNoteRepository consultationAiNoteRepository;

    @Mock
    private ConsultationAiDraftGenerator consultationAiDraftGenerator;

    @Mock
    private InsuranceProductRepository insuranceProductRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private DiseaseCodeRepository diseaseCodeRepository;

    private ConsultationAiNoteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ConsultationAiNoteServiceImpl(
                consultationSttSessionService,
                consultationAiNoteRepository,
                consultationAiDraftGenerator,
                insuranceProductRepository,
                contractRepository,
                diseaseCodeRepository,
                new ObjectMapper()
        );
    }

    @Test
    void findProductCandidates_autoMapsWhenTrailingNumberCanResolveCode() {
        InsuranceProduct product = insuranceProduct("LP007", "스마트정기보험 07");
        when(insuranceProductRepository.findByInsuranceProductCodeAndDeletedAtIsNull("LP007"))
                .thenReturn(Optional.of(product));

        Object result = ReflectionTestUtils.invokeMethod(service, "findProductCandidates", "스마트 전기보험 본 7");

        assertThat((String) ReflectionTestUtils.invokeMethod(result, "autoMappedCode")).isEqualTo("LP007");
        @SuppressWarnings("unchecked")
        List<ConsultationAiResolutionResponse.Candidate> candidates =
                (List<ConsultationAiResolutionResponse.Candidate>) ReflectionTestUtils.invokeMethod(result, "candidates");
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).getCode()).isEqualTo("LP007");
        assertThat(candidates.get(0).getLabel()).isEqualTo("스마트정기보험 07");
    }

    @Test
    void findProductCandidates_fallsBackToKeywordSearchWhenFullPhraseHasNoDirectMatch() {
        InsuranceProduct product = insuranceProduct("LP001", "스마트정기보험 01");

        when(insuranceProductRepository.findByInsuranceProductCodeAndDeletedAtIsNull(anyString()))
                .thenReturn(Optional.empty());
        when(insuranceProductRepository.findByInsuranceProductNameAndDeletedAtIsNull("스마트 전기보험"))
                .thenReturn(Optional.empty());
        lenient().when(insuranceProductRepository.findTop10ByInsuranceProductNameContainingAndDeletedAtIsNullOrderByInsuranceProductNameAsc("스마트 전기보험"))
                .thenReturn(List.of());
        when(insuranceProductRepository.findTop10ByInsuranceProductNameContainingAndDeletedAtIsNullOrderByInsuranceProductNameAsc("스마트"))
                .thenReturn(List.of(product));
        Object result = ReflectionTestUtils.invokeMethod(service, "findProductCandidates", "스마트 전기보험");

        assertThat((String) ReflectionTestUtils.invokeMethod(result, "autoMappedCode")).isEqualTo("LP001");
    }

    private InsuranceProduct insuranceProduct(String code, String name) {
        InsuranceCompany company = InsuranceCompany.builder()
                .id(UUID.randomUUID())
                .insuranceCompanyName("삼성생명")
                .build();

        return InsuranceProduct.builder()
                .id(UUID.randomUUID())
                .insuranceProductCode(code)
                .insuranceProductName(name)
                .insuranceCompany(company)
                .build();
    }
}
