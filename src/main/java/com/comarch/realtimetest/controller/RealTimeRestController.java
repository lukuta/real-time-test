package com.comarch.realtimetest.controller;

import com.comarch.realtimetest.Dto.VehicleInfoDto;
import com.comarch.realtimetest.Dto.VehiclePositionDto;
import com.comarch.realtimetest.service.RealTimeService;
import com.google.transit.realtime.GtfsRealtime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/")
public class RealTimeRestController {

    private RealTimeService realTimeService;

    @Autowired
    public RealTimeRestController(RealTimeService realTimeService) {
        this.realTimeService = realTimeService;
    }

    @GetMapping("vehicles")
    public VehicleInfoDto getVehicles() {
        GtfsRealtime.FeedMessage feedMessage = realTimeService.getVehiclePositions();

        List<VehiclePositionDto> vehiclePositionDtoList = new ArrayList<>();
        feedMessage.getEntityList().forEach(
                feedEntity -> vehiclePositionDtoList.add(VehiclePositionDto.builder()
                        .id(feedEntity.getVehicle().getVehicle().getId())
                        .latitude(feedEntity.getVehicle().getPosition().getLatitude())
                        .longitude(feedEntity.getVehicle().getPosition().getLongitude())
                        .build()
                )
        );

        return VehicleInfoDto.builder()
                .timestamp(feedMessage.getHeader().getTimestamp())
                .vehicles(vehiclePositionDtoList)
                .build();
    }

    @GetMapping("trips")
    public GtfsRealtime.FeedMessage getTrips() {
        return realTimeService.getTripUpdates();
    }
}
