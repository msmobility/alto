package de.tum.bgu.msm.longDistance.emissions;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.ModelComponent;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.*;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;


public class Emissions implements ModelComponent {

    static Logger logger = Logger.getLogger(Emissions.class);
    private TableDataSet coefficients;

    @Override
    public void setup(JSONObject prop, String inputFolder, String outputFolder) {
        coefficients = Util.readCSVfile(inputFolder+JsonUtilMto.getStringProp(prop, "emissions.coef_file"));
        coefficients.buildStringIndex(2);
        logger.info("Domestic DC set up");
    }

    @Override
    public void load(DataSet dataSet) {

    }

    @Override
    public void run(DataSet dataSet, int nThreads) {

        ArrayList<LongDistanceTrip> trips = dataSet.getAllTrips();
        logger.info("Running emission calculator for " + trips.size() + " trips");

        trips.parallelStream().forEach(tripFromArray -> {
            LongDistanceTripGermany trip = (LongDistanceTripGermany) tripFromArray;
            calculateEmissions(trip);
        });
        logger.info("Finished emission calculator");

    }

    private void calculateEmissions(LongDistanceTripGermany t) {
        ModeGermany mode = (ModeGermany) t.getMode();
        float distance = t.getTravelDistance() / 1000; // convert to km
        HashMap<Pollutant, Float> emissions = new HashMap<>();
        for (Pollutant pollutant : Pollutant.values()){
            float emissionPerTrip = 0;
            if (t.getTripState().equals(TypeGermany.AWAY)) {
                emissionPerTrip = 0; // have not travelled
            } else {
                String columnModePollutant = mode.toString() + "." + pollutant.toString();
                float emissionFactor = (float) (coefficients.getStringIndexedValueAt("alpha", columnModePollutant) *
                        Math.pow(distance,coefficients.getStringIndexedValueAt("beta", columnModePollutant)));
                if (t.getTripState().equals(TypeGermany.OVERNIGHT)) {
                    emissionPerTrip = emissionFactor * distance;
                } else if (t.getTripState().equals(TypeGermany.DAYTRIP)) {
                    emissionPerTrip = emissionFactor * distance * 2; //also account for the emissions of the return trip
                }
            }
            emissions.put(pollutant, emissionPerTrip);
        }
        t.setEmissions(emissions);
    }


}
