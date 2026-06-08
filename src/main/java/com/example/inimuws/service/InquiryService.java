package com.example.inimuws.service;

import com.example.inimuws.dto.AdminInquirySearchCondition;
import com.example.inimuws.dto.InquiryStatusUpdateRequest;
import com.example.inimuws.dto.ReservationRequest;
import com.example.inimuws.entity.Inquiry;
import com.example.inimuws.enums.InquiryStatus;
import com.example.inimuws.exception.BusinessException;
import com.example.inimuws.repository.InquiryRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");

    private final InquiryRepository inquiryRepository;

    @Transactional
    public Inquiry createInquiry(ReservationRequest request) {
        if (!StringUtils.hasText(request.getCustomerMessage())) {
            throw new BusinessException("お問い合わせ内容を入力してください");
        }

        LocalDate inquiryDate = LocalDate.now();
        Inquiry inquiry = Inquiry.builder()
                .inquiryCode(generateInquiryCode(inquiryDate))
                .customerFamilyName(request.getCustomerFamilyName())
                .customerGivenName(request.getCustomerGivenName())
                .customerFamilyKana(request.getCustomerFamilyKana())
                .customerGivenKana(request.getCustomerGivenKana())
                .customerEmail(request.getCustomerEmail())
                .customerTel(request.getCustomerTel())
                .customerMessage(request.getCustomerMessage())
                .status(InquiryStatus.OPEN)
                .build();
        return inquiryRepository.save(inquiry);
    }

    @Transactional(readOnly = true)
    public List<Inquiry> findAll() {
        return findAll((AdminInquirySearchCondition) null);
    }

    @Transactional(readOnly = true)
    public List<Inquiry> findAll(AdminInquirySearchCondition condition) {
        return inquiryRepository.findAll(buildSort(condition));
    }

    @Transactional(readOnly = true)
    public Inquiry findById(Long id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("問い合わせが見つかりません"));
    }

    @Transactional
    public Inquiry updateStatus(Long id, InquiryStatusUpdateRequest request) {
        Inquiry inquiry = findById(id);
        inquiry.setStatus(request.getStatus());
        inquiry.setAdminMemo(request.getAdminMemo());
        return inquiry;
    }

    @Transactional(readOnly = true)
    public long countOpen() {
        return inquiryRepository.countByStatus(InquiryStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public long countInProgress() {
        return inquiryRepository.countByStatus(InquiryStatus.IN_PROGRESS);
    }

    private Sort buildSort(AdminInquirySearchCondition condition) {
        if (condition == null || !StringUtils.hasText(condition.getSort())) {
            return Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(condition.getDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return switch (condition.getSort()) {
            case "createdAt" -> Sort.by(new Sort.Order(direction, "createdAt"), new Sort.Order(Sort.Direction.DESC, "id"));
            case "status" -> Sort.by(
                    new Sort.Order(direction, "status"),
                    new Sort.Order(Sort.Direction.DESC, "createdAt"),
                    new Sort.Order(Sort.Direction.DESC, "id")
            );
            default -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        };
    }

    private String generateInquiryCode(LocalDate inquiryDate) {
        long sequence = inquiryRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                inquiryDate.atStartOfDay(),
                inquiryDate.plusDays(1).atStartOfDay()
        ) + 1;
        String prefix = "INQ-" + inquiryDate.format(CODE_DATE_FORMAT) + "-";
        String code;
        do {
            code = prefix + String.format("%02d", sequence++);
        } while (inquiryRepository.existsByInquiryCode(code));
        return code;
    }
}
