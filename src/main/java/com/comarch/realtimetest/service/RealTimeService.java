package com.comarch.realtimetest.service;

import com.google.transit.realtime.GtfsRealtime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeLibrary;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeMutableProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class RealTimeService {

    private static final Logger LOG = LoggerFactory.getLogger(RealTimeService.class);

    private ScheduledExecutorService executor;
    public GtfsRealtimeMutableProvider gtfsRealtimeProvider;
    private URL url = new URL("http://www3.septa.org/hackathon/TrainView/");

    /**
     * How often vehicle data will be downloaded, in seconds.
     */
    private int refreshInterval = 30;

    public RealTimeService() throws MalformedURLException {
    }

    @Autowired
    public void setGtfsRealtimeProvider(GtfsRealtimeMutableProvider gtfsRealtimeProvider) {
        this.gtfsRealtimeProvider = gtfsRealtimeProvider;
    }

    /**
     * @param url the URL for the SEPTA vehicle data API.
     */
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
     * @param refreshInterval how often vehicle data will be downloaded, in
     *                        seconds.
     */
    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    /**
     * The start method automatically starts up a recurring task that periodically
     * downloads the latest vehicle data from the SEPTA vehicle stream and
     * processes them.
     */
    @PostConstruct
    public void start() {
        LOG.info("starting GTFS-realtime service");
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(new VehiclesRefreshTask(), 0,
                refreshInterval, TimeUnit.SECONDS);
    }

    /**
     * The stop method cancels the recurring vehicle data downloader task.
     */
    @PreDestroy
    public void stop() {
        LOG.info("stopping GTFS-realtime service");
        executor.shutdownNow();
    }

    /****
     * Private Methods - Here is where the real work happens
     ****/

    /**
     * Task that will download new vehicle data from the remote data source when
     * executed.
     */
    private class VehiclesRefreshTask implements Runnable {

        @Override
        public void run() {
            try {
                LOG.info("refreshing vehicles");
                refreshVehicles();
            } catch (Exception ex) {
                LOG.warn("Error in vehicle refresh task", ex);
            }
        }

        /**
         * This method downloads the latest vehicle data, processes each vehicle in
         * turn, and create a GTFS-realtime feed of trip updates and vehicle positions
         * as a result.
         */
        private void refreshVehicles() throws IOException, JSONException {

            /**
             * We download the vehicle details as an array of JSON objects.
             */
            JSONArray vehicleArray = downloadVehicleDetails();

            /**
             * The FeedMessage.Builder is what we will use to build up our GTFS-realtime
             * feeds. We create a feed for both trip updates and vehicle positions.
             */
            GtfsRealtime.FeedMessage.Builder tripUpdates = GtfsRealtimeLibrary.createFeedMessageBuilder();
            GtfsRealtime.FeedMessage.Builder vehiclePositions = GtfsRealtimeLibrary.createFeedMessageBuilder();

            /**
             * We iterate over every JSON vehicle object.
             */
            for (int i = 0; i < vehicleArray.length(); ++i) {

                JSONObject obj = vehicleArray.getJSONObject(i);
                String trainNumber = obj.getString("trainno");
                String route = obj.getString("dest");
                String stopId = obj.getString("nextstop");
                double lat = obj.getDouble("lat");
                double lon = obj.getDouble("lon");
                int delay = obj.getInt("late");

                /**
                 * We construct a TripDescriptor and VehicleDescriptor, which will be used
                 * in both trip updates and vehicle positions to identify the trip and
                 * vehicle. Ideally, we would have a trip id to use for the trip
                 * descriptor, but the SEPTA api doesn't include it, so we settle for a
                 * route id instead.
                 */
                GtfsRealtime.TripDescriptor.Builder tripDescriptor = GtfsRealtime.TripDescriptor.newBuilder();
                tripDescriptor.setRouteId(route);

                GtfsRealtime.VehicleDescriptor.Builder vehicleDescriptor = GtfsRealtime.VehicleDescriptor.newBuilder();
                vehicleDescriptor.setId(trainNumber);

                /**
                 * To construct our TripUpdate, we create a stop-time arrival event for
                 * the next stop for the vehicle, with the specified arrival delay. We add
                 * the stop-time update to a TripUpdate builder, along with the trip and
                 * vehicle descriptors.
                 */
                GtfsRealtime.TripUpdate.StopTimeEvent.Builder arrival = GtfsRealtime.TripUpdate.StopTimeEvent.newBuilder();
                arrival.setDelay(delay * 60);

                GtfsRealtime.TripUpdate.StopTimeUpdate.Builder stopTimeUpdate = GtfsRealtime.TripUpdate.StopTimeUpdate.newBuilder();
                stopTimeUpdate.setArrival(arrival);
                stopTimeUpdate.setStopId(stopId);

                GtfsRealtime.TripUpdate.Builder tripUpdate = GtfsRealtime.TripUpdate.newBuilder();
                tripUpdate.addStopTimeUpdate(stopTimeUpdate);
                tripUpdate.setTrip(tripDescriptor);
                tripUpdate.setVehicle(vehicleDescriptor);

                /**
                 * Create a new feed entity to wrap the trip update and add it to the
                 * GTFS-realtime trip updates feed.
                 */
                GtfsRealtime.FeedEntity.Builder tripUpdateEntity = GtfsRealtime.FeedEntity.newBuilder();
                tripUpdateEntity.setId(trainNumber);
                tripUpdateEntity.setTripUpdate(tripUpdate);
                tripUpdates.addEntity(tripUpdateEntity);

                /**
                 * To construct our VehiclePosition, we create a position for the vehicle.
                 * We add the position to a VehiclePosition builder, along with the trip
                 * and vehicle descriptors.
                 */

                GtfsRealtime.Position.Builder position = GtfsRealtime.Position.newBuilder();
                position.setLatitude((float) lat);
                position.setLongitude((float) lon);

                GtfsRealtime.VehiclePosition.Builder vehiclePosition = GtfsRealtime.VehiclePosition.newBuilder();
                vehiclePosition.setPosition(position);
                vehiclePosition.setTrip(tripDescriptor);
                vehiclePosition.setVehicle(vehicleDescriptor);

                /**
                 * Create a new feed entity to wrap the vehicle position and add it to the
                 * GTFS-realtime vehicle positions feed.
                 */
                GtfsRealtime.FeedEntity.Builder vehiclePositionEntity = GtfsRealtime.FeedEntity.newBuilder();
                vehiclePositionEntity.setId(trainNumber);
                vehiclePositionEntity.setVehicle(vehiclePosition);
                vehiclePositions.addEntity(vehiclePositionEntity);
            }

            /**
             * Build out the final GTFS-realtime feed messagse and save them.
             */
            gtfsRealtimeProvider.setTripUpdates(tripUpdates.build());
            gtfsRealtimeProvider.setVehiclePositions(vehiclePositions.build());

            LOG.info("vehicles extracted: " + tripUpdates.getEntityCount());
        }

        /**
         * @return a JSON array parsed from the data pulled from the SEPTA vehicle
         * data API.
         */
        private JSONArray downloadVehicleDetails() throws IOException, JSONException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    url.openStream()));
            JSONTokener tokener = new JSONTokener(reader);

            return new JSONArray(tokener);
        }
    }

}
