package de.tum.bgu.msm.longDistance.trafficAssignment;


import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import java.util.ArrayList;
import java.util.List;

/**
 * This class shows how to generate Network Change Events based on the speeds measured in an existing simulation.
 * These change events may be useful to simulate only some of the traffic of a simulation (e.g., fleets of taxis).
 * <br/>
 * The code is meant to be copied and modified according to needs; it is <i>not</i> meant to be used as a library code.
 * Copied and adapted from https://github.com/matsim-org/matsim-code-examples/blob/13.x/src/main/java/org/matsim/codeexamples/network/RunCreateNetworkChangeEventsFromExistingSimulationExample.java
 */


public class RunCreateNetworkChangeEvents {

    private static final int ENDTIME = 36 * 3600;
    private static final int TIMESTEP = 15 * 60;
    private static final double MAXFREESPEED = 140 / 3.6;
    private static String NETWORKFILE = "";
    private static String SIMULATION_EVENTS_FILE = "";
    private static String CHANGE_EVENTS_FILE = "";
    private static final double MINIMUMFREESPEED = 3;


    public static void main(String[] args) {

        NETWORKFILE = "eu_germany_network_w_connector_trucks.xml.gz";
        SIMULATION_EVENTS_FILE = "output/sd_passenger/mito_sd.output_events.xml";
        CHANGE_EVENTS_FILE = "externalDemand/networkChangeEvents.xml.gz";

        RunCreateNetworkChangeEvents ncg = new RunCreateNetworkChangeEvents();
        ncg.run();
    }

    private void run() {
        Network network = NetworkUtils.createNetwork();
        new MatsimNetworkReader(network).readFile(NETWORKFILE);
        TravelTimeCalculator tcc = readEventsIntoTravelTimeCalculator(network);
        List<NetworkChangeEvent> networkChangeEvents = createNetworkChangeEvents(network, tcc);
        new NetworkChangeEventsWriter().write(CHANGE_EVENTS_FILE, networkChangeEvents);
    }

    public static List<NetworkChangeEvent> createNetworkChangeEvents(Network network, TravelTimeCalculator tcc) {
        List<NetworkChangeEvent> networkChangeEvents = new ArrayList<>();

        for (Link l : network.getLinks().values()) {

//			if (l.getId().toString().startsWith("pt")) continue;

            double length = l.getLength();
            double previousTravelTime = l.getLength() / l.getFreespeed();

            for (double time = 0; time < ENDTIME; time = time + TIMESTEP) {

                double newTravelTime = tcc.getLinkTravelTimes().getLinkTravelTime(l, time, null, null);
                if (newTravelTime != previousTravelTime) {

                    NetworkChangeEvent nce = new NetworkChangeEvent(time);
                    nce.addLink(l);
                    double newFreespeed = length / newTravelTime;
                    if (newFreespeed < MINIMUMFREESPEED) newFreespeed = MINIMUMFREESPEED;
                    if (newFreespeed > MAXFREESPEED || newFreespeed == Double.POSITIVE_INFINITY) newFreespeed = MAXFREESPEED;
                    NetworkChangeEvent.ChangeValue freespeedChange = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, newFreespeed);
                    nce.setFreespeedChange(freespeedChange);

                    networkChangeEvents.add(nce);
                    previousTravelTime = newTravelTime;
                }
            }
        }
        return networkChangeEvents;
    }

    public static TravelTimeCalculator readEventsIntoTravelTimeCalculator(Network network) {
        EventsManager manager = EventsUtils.createEventsManager();
        TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
        TravelTimeCalculator tcc = builder.build();
        manager.addHandler(tcc);
        manager.initProcessing();
        new MatsimEventsReader(manager).readFile(SIMULATION_EVENTS_FILE);
        manager.finishProcessing();
        return tcc;
    }
}
