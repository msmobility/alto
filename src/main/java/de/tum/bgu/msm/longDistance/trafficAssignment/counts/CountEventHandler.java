package de.tum.bgu.msm.longDistance.trafficAssignment.counts;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;



public class CountEventHandler implements LinkEnterEventHandler {

    private enum CountVehicleType {
        car_sd,car_ld, truck;
    }

    private final int LAST_HOUR = 49;

    private static Logger logger = Logger.getLogger(CountEventHandler.class);

    private int thisIteration;

    private Map<Id, Map<Integer, Map<CountVehicleType, Integer>>> listOfSelectedLinks = new HashMap<>();

    private int getHourFromTime(double time_s) {
        return (int) (time_s / 3600) > (LAST_HOUR - 1) ? LAST_HOUR : (int) Math.floor(time_s / 3600);
    }

    public void addLinkById(Id linkId) {
        Map<Integer, Map<CountVehicleType, Integer>> countsByHour = new HashMap<>();
        for (int i = 0; i < LAST_HOUR + 1; i++) {
            Map<CountVehicleType, Integer> countsByType = new HashMap<>();
            for (CountVehicleType countVehicleType : CountVehicleType.values()){
                countsByType.put(countVehicleType, 0);
            }
            countsByHour.put(i, countsByType);
        }
        listOfSelectedLinks.put(linkId, countsByHour);
    }


    @Override
    public void handleEvent(LinkEnterEvent linkEnterEvent) {
        Id id = linkEnterEvent.getLinkId();
        int hour = getHourFromTime(linkEnterEvent.getTime());
        CountVehicleType type = getTypeFromId(linkEnterEvent.getVehicleId().toString());
        if (listOfSelectedLinks.containsKey(id)) {
            listOfSelectedLinks.get(id).get(hour).put(type, listOfSelectedLinks.get(id).get(hour).get(type) + 1);
        }
    }

    @Override
    public void reset(int iteration) {
        this.thisIteration = iteration;
        for (Id id : listOfSelectedLinks.keySet()) {
            addLinkById(id);
        }
        logger.info("Reset event handler at iteration " + thisIteration);
    }


    public void printOutCounts(String countsFile, String networkFile) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(countsFile));

        Network network = NetworkUtils.readNetwork(networkFile);

        pw.print("link,hour,length,type");
        for (CountVehicleType type : CountVehicleType.values()) {
            pw.print(",");
            pw.print(type);
        }
        pw.println();

        for (Id id : listOfSelectedLinks.keySet()) {
            Map<Integer, Map<CountVehicleType, Integer>> countsByHour = listOfSelectedLinks.get(id);
            for (int hour : countsByHour.keySet()) {
                pw.print(id.toString());
                pw.print(",");
                pw.print(hour);
                pw.print(",");
                Link link = network.getLinks().get(id);
                pw.print(link.getLength());
                pw.print(",");
                pw.print(link.getAttributes().getAttribute("type"));
                Map<CountVehicleType, Integer> countsByType = countsByHour.get(hour);
                for (CountVehicleType type : CountVehicleType.values()){
                    pw.print(",");
                    pw.print(countsByType.get(type));
                }
                pw.println();
            }
        }
        pw.close();

    }

    private static CountVehicleType getTypeFromId(String vehicleId){
        //todo review this
        if(vehicleId.contains("truck")){
            return CountVehicleType.truck;
        } else if (vehicleId.contains("ld")) {
            return CountVehicleType.car_ld;
        } else {
            return CountVehicleType.car_sd;
        }
    }

}
