package com.example.inimuws.controller.admin;

import com.example.inimuws.dto.AdminInquirySearchCondition;
import com.example.inimuws.dto.InquiryStatusUpdateRequest;
import com.example.inimuws.entity.Inquiry;
import com.example.inimuws.enums.InquiryStatus;
import com.example.inimuws.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.util.StringUtils;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/inquiries")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    public String list(@ModelAttribute("condition") AdminInquirySearchCondition condition, Model model) {
        normalize(condition);
        model.addAttribute("inquiries", inquiryService.findAll(condition));
        return "admin/inquiries";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Inquiry inquiry = inquiryService.findById(id);
        InquiryStatusUpdateRequest updateRequest = new InquiryStatusUpdateRequest();
        updateRequest.setStatus(inquiry.getStatus());
        updateRequest.setAdminMemo(inquiry.getAdminMemo());
        model.addAttribute("inquiry", inquiry);
        model.addAttribute("updateRequest", updateRequest);
        model.addAttribute("inquiryStatuses", InquiryStatus.values());
        return "admin/inquiry-detail";
    }

    @PostMapping("/{id}/status")
    public String update(@PathVariable Long id, @ModelAttribute InquiryStatusUpdateRequest request) {
        inquiryService.updateStatus(id, request);
        return "redirect:/admin/inquiries/" + id;
    }

    private void normalize(AdminInquirySearchCondition condition) {
        if (!StringUtils.hasText(condition.getSort())) {
            condition.setSort("createdAt");
        }
        if (!StringUtils.hasText(condition.getDirection())) {
            condition.setDirection("desc");
        }
    }
}
