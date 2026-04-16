package com.hometalk.onepass.parking.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VehicleRegisterRequest {

    private String vehicleNumber;
    private String model;
    private String vehicleType;
}