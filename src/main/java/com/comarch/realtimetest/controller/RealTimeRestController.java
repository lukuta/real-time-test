package com.comarch.realtimetest.controller;

import com.comarch.realtimetest.service.RealTimeService;
import com.google.transit.realtime.GtfsRealtime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/")
public class RealTimeRestController {

    private RealTimeService realTimeService;

    @Autowired
    public RealTimeRestController(RealTimeService realTimeService) {
        this.realTimeService = realTimeService;
    }

    @GetMapping("vehicles")
    public GtfsRealtime.FeedMessage getVehicles() {
        return realTimeService.gtfsRealtimeProvider.getVehiclePositions();
    }

    @GetMapping("trips")
    public GtfsRealtime.FeedMessage getTrips() {
        return realTimeService.gtfsRealtimeProvider.getTripUpdates();
    }
}
