package com.hometalk.onepass.parking.controller;

import com.hometalk.onepass.parking.dto.request.VehicleApprovalRequest;
import com.hometalk.onepass.parking.dto.request.VehicleRegisterRequest;
import com.hometalk.onepass.parking.dto.response.VehicleApprovalResponse;
import com.hometalk.onepass.parking.dto.response.VehicleResponse;
import com.hometalk.onepass.parking.entity.Vehicle;
import com.hometalk.onepass.parking.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/parking")
public class VehicleController {

    private final VehicleService vehicleService;

    // ========== 입주자 페이지 ==========

    // 세대 차량 목록 조회 페이지
    @GetMapping("/vehicle")
    public String vehicleList(Model model) {
        Long householdId = null; // TODO: JWT 연동 후 추출
        List<VehicleResponse> vehicles = vehicleService.getHouseholdVehicles(householdId);
        model.addAttribute("vehicles", vehicles);
        //model.addAttribute("menu", "parking");
        return "parking/vehicle-status";
    }

    // 차량 등록 페이지
    @GetMapping("/vehicle/register")
    public String vehicleRegisterPage(Model model) {

        //model.addAttribute("menu", "parking");
        return "parking/vehicle-register";
    }

    // 차량 등록 처리
    @PostMapping("/vehicle/register")
    public String vehicleRegister(
            @ModelAttribute VehicleRegisterRequest request,
            @RequestParam(value = "documents") List<MultipartFile> documents) {
        Long userId = null; // TODO: JWT 연동 후 추출
        vehicleService.register(userId, request, documents);
        return "redirect:/parking/vehicle";
    }

    // 반려 사유 조회 (JSON)
    @GetMapping("/vehicle/reject-reason/{vehicleId}")
    @ResponseBody
    public ResponseEntity<String> getRejectReason(@PathVariable Long vehicleId) {
        String rejectReason = vehicleService.getRejectReason(vehicleId);
        return ResponseEntity.ok(rejectReason != null ? rejectReason : "");
    }

    // 차량 재신청 페이지
    @GetMapping("/vehicle/reapply/{vehicleId}")
    public String vehicleReapplyPage(@PathVariable Long vehicleId, Model model) {
        VehicleResponse vehicle = vehicleService.getVehicle(vehicleId);
        String rejectReason = vehicleService.getRejectReason(vehicleId);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("rejectReason", rejectReason);
        return "parking/vehicle-reapply";
    }

    // 차량 재신청 처리
    @PostMapping("/vehicle/reapply/{vehicleId}")
    public String vehicleReapply(
            @PathVariable Long vehicleId,
            @RequestParam(value = "documents") List<MultipartFile> documents) {
        vehicleService.reapply(vehicleId, documents);
        return "redirect:/parking/vehicle";
    }

    // ========== 관리자 페이지 ==========

    // 관리자 차량 승인 목록 페이지
    @GetMapping("/vehicle/approval")
    public String approvalPage() {
        return "parking/admin-vehicle-approval";
    }

    // 관리자 차량 목록 조회 (JSON)
    @GetMapping("/vehicle/approval/list")
    @ResponseBody
    public ResponseEntity<List<VehicleApprovalResponse>> getApprovalList(
            @RequestParam Vehicle.VehicleStatus status) {
        return ResponseEntity.ok(vehicleService.getApprovalList(status));
    }

    // 관리자 차량 승인 처리
    @PostMapping("/vehicle/approval/approve")
    @ResponseBody
    public ResponseEntity<Void> approve(@RequestBody VehicleApprovalRequest request) {
        Long userId = null; // TODO: JWT 연동 후 추출
        vehicleService.approve(userId, request.getApprovalId());
        return ResponseEntity.ok().build();
    }

    // 관리자 차량 반려 처리
    @PostMapping("/vehicle/approval/reject")
    @ResponseBody
    public ResponseEntity<Void> reject(@RequestBody VehicleApprovalRequest request) {
        Long userId = null; // TODO: JWT 연동 후 추출
        vehicleService.reject(userId, request.getApprovalId(), request.getRejectReason());
        return ResponseEntity.ok().build();
    }
}