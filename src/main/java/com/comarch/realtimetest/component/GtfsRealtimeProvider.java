package com.comarch.realtimetest.component;

import com.google.transit.realtime.GtfsRealtime;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeListener;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeMutableProvider;
import org.springframework.stereotype.Component;

@Component
public class GtfsRealtimeProvider implements GtfsRealtimeMutableProvider {

    private GtfsRealtime.FeedMessage tripUpdates;
    private GtfsRealtime.FeedMessage vehiclePositions;

    @Override
    public void setTripUpdates(GtfsRealtime.FeedMessage tripUpdates) {
        this.tripUpdates = tripUpdates;
    }

    @Override
    public void setTripUpdates(GtfsRealtime.FeedMessage tripUpdates, boolean fireUpdate) {

    }

    @Override
    public void setVehiclePositions(GtfsRealtime.FeedMessage vehiclePositions) {
        this.vehiclePositions = vehiclePositions;
    }

    @Override
    public void setVehiclePositions(GtfsRealtime.FeedMessage vehiclePositions, boolean fireUpdate) {

    }

    @Override
    public void setAlerts(GtfsRealtime.FeedMessage alerts) {

    }

    @Override
    public void setAlerts(GtfsRealtime.FeedMessage alerts, boolean fireUpdate) {

    }

    @Override
    public void fireUpdate() {

    }

    @Override
    public GtfsRealtime.FeedMessage getTripUpdates() {
        return tripUpdates;
    }

    @Override
    public GtfsRealtime.FeedMessage getVehiclePositions() {
        return vehiclePositions;
    }

    @Override
    public GtfsRealtime.FeedMessage getAlerts() {
        return null;
    }

    @Override
    public void addGtfsRealtimeListener(GtfsRealtimeListener listener) {

    }

    @Override
    public void removeGtfsRealtimeListener(GtfsRealtimeListener listener) {

    }
}
