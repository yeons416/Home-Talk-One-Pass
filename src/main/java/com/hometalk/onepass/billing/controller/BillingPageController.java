package com.hometalk.onepass.billing.controller;

import com.hometalk.onepass.billing.dto.ResidentBillingResponse;
import com.hometalk.onepass.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/billing")
@RequiredArgsConstructor
public class BillingPageController {

    private final BillingService billingService;

    /**
     * 입주민 관리비 페이지
     * GET /billing
     * TODO: Security 완성 후 householdId를 CustomUserDetails에서 추출하도록 변경
     */
    @GetMapping
    public String billingPage(
            @RequestParam Long householdId,  // TODO: @AuthenticationPrincipal로 교체
            Model model
    ) {
        ResidentBillingResponse response = billingService.getResidentBillingPage(householdId);
        model.addAttribute("billing", response);
        return "billing/billing";  // templates/billing/billing.html
    }

    /**
     * 관리자 고지서 업로드 페이지
     * GET /billing/admin/upload
     */
    @GetMapping("/admin/upload")
    public String uploadPage() {
        return "billing/admin-upload";  // templates/billing/admin-upload.html
    }

    /**
     * 관리자 미납 세대 관리 페이지
     * GET /billing/admin/unpaid
     */
    @GetMapping("/admin/unpaid")
    public String unpaidPage() {
        return "billing/admin-unpaid";  // templates/billing/admin-unpaid.html
    }
}