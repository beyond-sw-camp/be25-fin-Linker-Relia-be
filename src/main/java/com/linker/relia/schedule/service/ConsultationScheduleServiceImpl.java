package com.linker.relia.schedule.service;

import com.linker.relia.common.exception.BusinessException;
import com.linker.relia.common.exception.CommonErrorCode;
import com.linker.relia.consultation.domain.Consultation;
import com.linker.relia.consultation.repository.ConsultationRepository;
import com.linker.relia.contract.domain.Contract;
import com.linker.relia.contract.repository.ContractRepository;
import com.linker.relia.customer.domain.Customer;
import com.linker.relia.customer.repository.CustomerRepository;
import com.linker.relia.schedule.domain.ConsultationSchedule;
import com.linker.relia.schedule.domain.ScheduleStatus;
import com.linker.relia.schedule.domain.ScheduleType;
import com.linker.relia.schedule.dto.request.ConsultationScheduleCreateRequest;
import com.linker.relia.schedule.dto.request.ConsultationScheduleUpdateRequest;
import com.linker.relia.schedule.dto.response.ConsultationScheduleCreateResponse;
import com.linker.relia.schedule.dto.response.ConsultationScheduleItemResponse;
import com.linker.relia.schedule.dto.response.ConsultationScheduleListResponse;
import com.linker.relia.schedule.dto.response.ScheduleCalendarDayResponse;
import com.linker.relia.schedule.dto.response.ScheduleCalendarResponse;
import com.linker.relia.schedule.exception.ScheduleErrorCode;
import com.linker.relia.schedule.repository.ConsultationScheduleRepository;
import com.linker.relia.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultationScheduleServiceImpl implements ConsultationScheduleService {

    private final ConsultationScheduleRepository consultationScheduleRepository;
    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final ConsultationRepository consultationRepository;

    @Override
    @Transactional
    public ConsultationScheduleCreateResponse createSchedule(
            ConsultationScheduleCreateRequest request,
            User fp
    ) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST, "고객 정보를 찾을 수 없습니다."));

        Contract contract = null;
        if (request.getContractId() != null) {
            contract = contractRepository.findById(request.getContractId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST, "계약 정보를 찾을 수 없습니다."));
        }

        Consultation consultation = null;
        if (request.getConsultationId() != null) {
            consultation = consultationRepository.findById(request.getConsultationId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST, "상담일지를 찾을 수 없습니다."));
        }

        if (request.getScheduleType() == ScheduleType.CONTRACT_EXPIRY) {
            if (request.getContractId() == null) {
                throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "계약 만기 일정은 계약 ID가 필수입니다.");
            }
        }

        ConsultationSchedule schedule = ConsultationSchedule.builder()
                .fp(fp)
                .customer(customer)
                .contract(contract)
                .consultation(consultation)
                .scheduleType(request.getScheduleType())
                .consultationType(request.getConsultationType())
                .consultationChannel(request.getConsultationChannel())
                .title(request.getTitle())
                .memo(request.getMemo())
                .scheduledAt(request.getScheduledAt())
                .scheduleStatus(ScheduleStatus.SCHEDULED)
                .build();

        ConsultationSchedule savedSchedule = consultationScheduleRepository.save(schedule);

        return ConsultationScheduleCreateResponse.builder()
                .scheduleId(savedSchedule.getId())
                .scheduledAt(savedSchedule.getScheduledAt())
                .status(savedSchedule.getScheduleStatus())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ConsultationScheduleListResponse getSchedulesByDate(
            LocalDate date,
            User fp
    ) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<ConsultationScheduleItemResponse> schedules = consultationScheduleRepository
                .findAllByFp_IdAndScheduledAtBetweenAndDeletedAtIsNullOrderByScheduledAtAsc(fp.getId(), start, end)
                .stream()
                .map(this::toItemResponse)
                .toList();

        return ConsultationScheduleListResponse.builder()
                .date(date)
                .schedules(schedules)
                .build();
    }

    @Override
    @Transactional
    public ConsultationScheduleCreateResponse updateSchedule(
            UUID fpId,
            UUID scheduleId,
            ConsultationScheduleUpdateRequest request
    ) {
        ConsultationSchedule schedule = consultationScheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new BusinessException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        validateOwner(schedule, fpId);

        Customer customer = schedule.getCustomer();
        if (request.getCustomerId() != null) {
            customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_REQUEST, "고객 정보를 찾을 수 없습니다."));
        }

        schedule.update(
                customer,
                request.getConsultationType() != null ? request.getConsultationType() : schedule.getConsultationType(),
                request.getConsultationChannel() != null ? request.getConsultationChannel() : schedule.getConsultationChannel(),
                request.getScheduledAt() != null ? request.getScheduledAt() : schedule.getScheduledAt(),
                request.getTitle() != null ? request.getTitle() : schedule.getTitle(),
                request.getMemo() != null ? request.getMemo() : schedule.getMemo(),
                request.getScheduleStatus() != null ? request.getScheduleStatus() : schedule.getScheduleStatus()
        );

        return ConsultationScheduleCreateResponse.builder()
                .scheduleId(schedule.getId())
                .scheduledAt(schedule.getScheduledAt())
                .status(schedule.getScheduleStatus())
                .build();
    }

    @Override
    @Transactional
    public void deleteSchedule(UUID fpId, UUID scheduleId) {
        ConsultationSchedule schedule = consultationScheduleRepository.findByIdAndDeletedAtIsNull(scheduleId)
                .orElseThrow(() -> new BusinessException(ScheduleErrorCode.SCHEDULE_NOT_FOUND));

        validateOwner(schedule, fpId);

        schedule.delete(fpId);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleCalendarResponse getMonthlyCalendar(
            int year,
            int month,
            User fp
    ) {
        if (month < 1 || month > 12) {
            throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "월은 1부터 12 사이여야 합니다.");
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<ConsultationSchedule> schedules =
                consultationScheduleRepository.findAllByFp_IdAndScheduledAtBetweenAndDeletedAtIsNull(
                        fp.getId(),
                        start,
                        end
                );

        Map<LocalDate, List<ConsultationSchedule>> schedulesByDate = schedules.stream()
                .collect(Collectors.groupingBy(schedule -> schedule.getScheduledAt().toLocalDate()));

        List<ScheduleCalendarDayResponse> days = schedulesByDate.entrySet().stream()
                .map(entry -> {
                    long consultationCount = entry.getValue().stream()
                            .filter(schedule -> schedule.getScheduleType() == ScheduleType.CONSULTATION)
                            .count();

                    long contractExpiryCount = entry.getValue().stream()
                            .filter(schedule -> schedule.getScheduleType() == ScheduleType.CONTRACT_EXPIRY)
                            .count();

                    return ScheduleCalendarDayResponse.builder()
                            .date(entry.getKey())
                            .consultationCount(consultationCount)
                            .contractExpiryCount(contractExpiryCount)
                            .build();
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .toList();

        return ScheduleCalendarResponse.builder()
                .year(year)
                .month(month)
                .days(days)
                .build();
    }

    private void validateOwner(ConsultationSchedule schedule, UUID fpId) {
        if (!schedule.getFp().getId().equals(fpId)) {
            throw new BusinessException(ScheduleErrorCode.SCHEDULE_ACCESS_DENIED);
        }
    }

    private ConsultationScheduleItemResponse toItemResponse(ConsultationSchedule schedule) {
        Customer customer = schedule.getCustomer();

        return ConsultationScheduleItemResponse.builder()
                .scheduleId(schedule.getId())
                .scheduleType(schedule.getScheduleType())
                .customerId(customer != null ? customer.getId() : null)
                .customerName(customer != null ? customer.getCustomerName() : null)
                .contractId(schedule.getContract() != null ? schedule.getContract().getId() : null)
                .consultationId(schedule.getConsultation() != null ? schedule.getConsultation().getId() : null)
                .consultationType(schedule.getConsultationType())
                .consultationChannel(schedule.getConsultationChannel())
                .title(schedule.getTitle())
                .memo(schedule.getMemo())
                .scheduledAt(schedule.getScheduledAt())
                .status(schedule.getScheduleStatus())
                .build();
    }
}