package com.linker.relia.consultation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linker.relia.consultation.domain.ConsultationDraft;
import com.linker.relia.consultation.dto.request.ConsultationDraftSaveRequest;
import com.linker.relia.consultation.dto.response.ConsultationDraftResponse;
import com.linker.relia.consultation.repository.ConsultationDraftRepository;
import com.linker.relia.contract.domain.Contract;
import com.linker.relia.contract.repository.ContractRepository;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.user.domain.User;
import com.linker.relia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ConsultationDraftServiceImpl implements ConsultationDraftService {

    private final ConsultationDraftRepository consultationDraftRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ConsultationDraftResponse saveDraft(UUID fpId, ConsultationDraftSaveRequest request){
        User fp = userRepository.findById(fpId)
                .orElseThrow(() -> new IllegalArgumentException("설계사를 찾을 수 없습니다."));

        Customer customer = null;
        if(request.getCustomerId() != null){
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("고객을 찾을 수 없습니다."));
        }

        Contract contract = null;
        if(request.getContractId() != null){
            contract = contractRepository.findById(request.getContractId())
                    .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
        }

        String draftDataJson = toJson(request.getDraftData());

        LocalDateTime now = LocalDateTime.now();

        ConsultationDraft draft = ConsultationDraft.builder()
                .id(UUID.randomUUID())
                .customer(customer)
                .fp(fp)
                .contract(contract)
                .consultationType(request.getConsultationType())
                .consultationChannel(request.getConsultationChannel())
                .consultedAt(request.getConsultedAt())
                .specialNote(request.getSpecialNote())
                .nextScheduledAt(request.getNextScheduledAt())
                .draftData(draftDataJson)
                .lastSavedAt(now)
                .createdAt(now)
                .createdBy(fp.getId())
                .updatedAt(now)
                .updatedBy(fp.getId())
                .build();

        ConsultationDraft savedDraft = consultationDraftRepository.save(draft);

        return toResponse(savedDraft);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultationDraftResponse> getDrafts(UUID fpId){
        return consultationDraftRepository
                .findAllByFpIdAndDeletedAtIsNullOrderByLastSavedAtDesc(fpId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationDraftResponse getDraft(UUID draftId, UUID fpId){
        ConsultationDraft draft = consultationDraftRepository
                .findByIdAndFpIdAndDeletedAtIsNull(draftId, fpId)
                .orElseThrow(() -> new IllegalArgumentException("임시저장 상담일지를 찾을 수 없습니다."));

        return toResponse(draft);
    }

    @Override
    public void deleteDraft(UUID draftId, UUID fpId){
        ConsultationDraft draft = consultationDraftRepository
                .findByIdAndFpIdAndDeletedAtIsNull(draftId, fpId)
                .orElseThrow(() -> new IllegalArgumentException("임시저장 상담일지를 찾을 수 없습니다."));

        draft.delete(fpId);
    }

    private ConsultationDraftResponse toResponse(ConsultationDraft draft){
        return ConsultationDraftResponse.builder()
                .draftId(draft.getId())
                .customerId(draft.getCustomer() != null ? draft.getCustomer().getId() : null)
                .contractId(draft.getContract() != null ? draft.getContract().getId() : null)
                .consultationType(draft.getConsultationType())
                .consultationChannel(draft.getConsultationChannel())
                .consultedAt(draft.getConsultedAt())
                .specialNote(draft.getSpecialNote())
                .nextScheduledAt(draft.getNextScheduledAt())
                .draftData(draft.getDraftData())
                .lastSavedAt(draft.getLastSavedAt())
                .build();

    }

    private String toJson(Object draftData){
        try{
            return objectMapper.writeValueAsString(draftData);
        } catch (JsonProcessingException e){
            throw new IllegalArgumentException("임시저장 데이터 형식이 올바르지 않습니다.");
        }
    }
}
