package de.tum.bgu.msm.longDistance.tripGeneration;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LDModel;
import de.tum.bgu.msm.longDistance.data.*;
import de.tum.bgu.msm.longDistance.data.sp.Person;
import de.tum.bgu.msm.longDistance.zoneSystem.Zone;
import de.tum.bgu.msm.longDistance.zoneSystem.ZoneType;
import de.tum.bgu.msm.Util;

import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by carlloga on 8/31/2016.
 */
public class VisitorsTripGeneration implements TripGenerationModule {

    private TableDataSet visitorPartyProbabilities;
    private TableDataSet visitorsRatePerZone;


    private TableDataSet externalCanIntRates;

    private DataSet dataSet;
    private ArrayList<Zone> externalZoneList;

    static Logger logger = LogManager.getLogger(VisitorsTripGeneration.class);
    private AtomicInteger atomicInteger;
    private AtomicInteger atomicIntegerVisitors;

    private JSONObject prop;

    public VisitorsTripGeneration(JSONObject prop) {

        this.prop = prop;

        //this.rb = rb;

        //String visitorPartyProbabilitiesFilename = rb.getString("visitor.parties");
        visitorPartyProbabilities = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"trip_generation.visitors.party_file"));
        visitorPartyProbabilities.buildIndex(visitorPartyProbabilities.getColumnPosition("travelParty"));

        //String visitorsRatePerZoneFilename = rb.getString("visitor.zone.rates");
        visitorsRatePerZone = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"trip_generation.visitors.rates_file"));
        visitorsRatePerZone.buildIndex(visitorsRatePerZone.getColumnPosition("zone"));

        //String externalCanIntRatesName = rb.getString("ext.can.int.zone.rates");
        externalCanIntRates = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"trip_generation.visitors.external_can_int_rates_file"));
        externalCanIntRates.buildIndex(externalCanIntRates.getColumnPosition("zone"));



    }


    public void load(DataSet dataSet){
        this.dataSet  = dataSet;
        externalZoneList = dataSet.getExternalZones();
    }


    //method to run the trip generation
    public ArrayList<LongDistanceTrip> run() {

        atomicInteger = new AtomicInteger(dataSet.getAllTrips().size() + 1);
        atomicIntegerVisitors = new AtomicInteger(90000000);

        ArrayList<LongDistanceTrip> visitorTrips = new ArrayList<>();

        int tripCount = 0;
        int tripCount2 = 0;
        for (Zone zone : externalZoneList) {
            if (zone.getZoneType().equals(ZoneType.EXTCANADA)) {
                for (Purpose tripPurpose : PurposeOntario.values()) {
                    for (Type tripState : TypeOntario.values()) {

                        String column = tripState.toString() + "." + tripPurpose.toString();
                        double tripRate;
                        //generates all travellers and apply later destination choice
                        tripRate = externalCanIntRates.getIndexedValueAt(zone.getId(), column);

                        int numberOfTrips = (int) Math.round(tripRate * zone.getPopulation());
                        for (int i = 0; i < numberOfTrips; i++) {
                            LongDistanceTrip trip = createExtCanIntLongDistanceTrip(tripPurpose, tripState, zone, visitorPartyProbabilities);
                            tripCount2++;
                            visitorTrips.add(trip);
                        }
                    }
                }
            }

            for (Purpose tripPurpose : PurposeOntario.values()) {
                for (Type tripState : TypeOntario.values()) {

                    String column = tripState.toString() + "." + tripPurpose.toString();
                    double tripRate;
                    tripRate = visitorsRatePerZone.getIndexedValueAt(zone.getId(), column);
                    int numberOfTrips = (int) (tripRate * zone.getPopulation());
                    for (int i = 0; i < numberOfTrips; i++) {
                        LongDistanceTrip trip = createVisitorLongDistanceTrip(tripPurpose, tripState, visitorPartyProbabilities, zone);
                        tripCount++;
                        visitorTrips.add(trip);
                    }
                }
            }
        }
        logger.info("  " + tripCount + " international trips from visitors to Canada + domestic trips from external zones Canada generated");
        logger.info("  " + tripCount2 + " international trips from External Zones in Canada generated");

        return visitorTrips;
    }

    private LongDistanceTrip createVisitorLongDistanceTrip(Purpose tripPurpose, Type tripState, TableDataSet visitorPartyProbabilities, Zone zone) {
        boolean international;
        int adultsHh;
        int kidsHh;
        int nonHh;
        if (zone.getZoneType().equals(ZoneType.EXTCANADA)) international = false;
        else international = true;

        //generation of trip parties (no assignment of person, only sizes)
        adultsHh = 1;
        kidsHh = 0;
        nonHh = 0;
        String column = "adults." + tripPurpose;
        double randomChoice = LDModel.rand.nextDouble();
        while (adultsHh < 9 & randomChoice < visitorPartyProbabilities.getIndexedValueAt(Math.min(adultsHh, 5), column))
            adultsHh++;

        column = "kids." + tripPurpose;
        randomChoice = LDModel.rand.nextDouble();
        while (kidsHh < 9 & randomChoice < visitorPartyProbabilities.getIndexedValueAt(Math.min(kidsHh + 1, 9), column))
            kidsHh++;

        column = "nonHh." + tripPurpose;
        randomChoice = LDModel.rand.nextDouble();
        while (nonHh < 9 & randomChoice < visitorPartyProbabilities.getIndexedValueAt(Math.min(nonHh + 1, 9), column))
            nonHh++;

        int duration = tripState.equals("daytrip")? 0:1;

        Person visitor = new Person(atomicIntegerVisitors.getAndIncrement(), 99999999, -1, 'f',-1, -1, -1, null );

        LongDistanceTrip trip = new LongDistanceTrip(atomicInteger.get(),visitor, international, tripPurpose, tripState, zone, duration, nonHh);
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        for (int v = 0; v < adultsHh + kidsHh - 1; v++){
            Person newVisitor = new Person(atomicIntegerVisitors.getAndIncrement(), 99999999, -1, 'f',-1, -1, -1, null );
            hhTravelParty.add(newVisitor);
        }
        trip.setHhTravelParty(hhTravelParty);

        return trip;


    }

    private LongDistanceTrip createExtCanIntLongDistanceTrip(Purpose tripPurpose, Type tripState, Zone zone, TableDataSet travelPartyProbabilities) {

        boolean international = true;
        int adultsHh;
        int kidsHh;
        int nonHh;
        //generation of trip parties (no assignment of person, only sizes)
        adultsHh = 1;
        kidsHh = 0;
        nonHh = 0;
        String column = "adults." + tripPurpose;
        double randomChoice = LDModel.rand.nextDouble();
        while (adultsHh < 9 && randomChoice < travelPartyProbabilities.getIndexedValueAt(Math.min(adultsHh, 5), column))
            adultsHh++;

        column = "kids." + tripPurpose;
        randomChoice = LDModel.rand.nextDouble();
        while (kidsHh < 9 && randomChoice < travelPartyProbabilities.getIndexedValueAt(Math.min(kidsHh + 1, 9), column))
            kidsHh++;

        column = "nonHh." + tripPurpose;
        randomChoice = LDModel.rand.nextDouble();
        while (nonHh < 9 && randomChoice < travelPartyProbabilities.getIndexedValueAt(Math.min(nonHh + 1, 9), column))
            nonHh++;

        int duration = tripState.equals("daytrip")? 0:1;

        Person visitor = new Person(atomicIntegerVisitors.getAndIncrement(), 99999999, -1, 'f',-1, -1, -1, null );
        LongDistanceTrip trip = new LongDistanceTrip(atomicInteger.get(),visitor, true, tripPurpose, tripState, zone, duration, nonHh);
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        for (int v = 0; v < adultsHh + kidsHh - 1; v++){
            Person newVisitor = new Person(atomicIntegerVisitors.getAndIncrement(), 99999999, -1, 'f',-1, -1, -1, null );
            hhTravelParty.add(newVisitor);
        }
        trip.setHhTravelParty(hhTravelParty);

        return trip;

    }


}
