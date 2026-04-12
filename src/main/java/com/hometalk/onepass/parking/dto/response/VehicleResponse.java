package com.hometalk.onepass.parking.dto.response;

import com.hometalk.onepass.parking.entity.Vehicle;
import lombok.Getter;

@Getter
public class VehicleResponse {

    private Long vehicleId;
    private String vehicleNumber;
    private String model;
    private String vehicleType;
    private String status;
    private String userName;
    private String household;

    public VehicleResponse(Vehicle vehicle) {
        this.vehicleId = vehicle.getVehicleId();
        this.vehicleNumber = vehicle.getVehicleNumber();
        this.model = vehicle.getModel();
        this.vehicleType = vehicle.getVehicleType();
        this.status = vehicle.getStatus().name();
        this.userName = vehicle.getUser() != null ? vehicle.getUser().getName() : "";
        this.household = vehicle.getHousehold() != null
                ? vehicle.getHousehold().getDong() + " " + vehicle.getHousehold().getHo()
                : "";
    }
}