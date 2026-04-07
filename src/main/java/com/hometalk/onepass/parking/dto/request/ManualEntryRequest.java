package com.hometalk.onepass.parking.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ManualEntryRequest {

    private String vehicleNumber;
    private String purposeType;
    private String dong;
    private String ho;
}