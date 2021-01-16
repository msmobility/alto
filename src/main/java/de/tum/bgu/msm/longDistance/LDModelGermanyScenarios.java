package de.tum.bgu.msm.longDistance;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTrip;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTripGermany;
import de.tum.bgu.msm.longDistance.data.trips.LongDistanceTripOntario;
import de.tum.bgu.msm.longDistance.data.trips.TypeOntario;
import de.tum.bgu.msm.longDistance.destinationChoice.DestinationChoice;
import de.tum.bgu.msm.longDistance.emissions.Emissions;
import de.tum.bgu.msm.longDistance.io.reader.EconomicStatusReader;
import de.tum.bgu.msm.longDistance.io.reader.SkimsReader;
import de.tum.bgu.msm.longDistance.io.reader.SyntheticPopulationReader;
import de.tum.bgu.msm.longDistance.io.reader.ZoneReader;
import de.tum.bgu.msm.longDistance.io.writer.OutputWriter;
import de.tum.bgu.msm.longDistance.io.writer.OutputWriterGermanScenario;
import de.tum.bgu.msm.longDistance.modeChoice.ModeChoice;
import de.tum.bgu.msm.longDistance.modeChoice.ModeChoiceGermanyScenario;
import de.tum.bgu.msm.longDistance.timeOfDay.TimeOfDayChoice;
import de.tum.bgu.msm.longDistance.tripGeneration.TripGeneration;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Random;

/**
 * Germany Model
 * Module to simulate long-distance travel
 * Author: Ana Moreno, Technische Universität München (TUM), ana.moreno@tum.de
 * Date: 17 December 2020
 * Version 1
 * Adapted from Ontario Provincial Model
 */

public class LDModelGermanyScenarios implements ModelComponent, LDModel {

    public static Random rand;
    static Logger logger = Logger.getLogger(LDModelGermanyScenarios.class);

    //modules
    private ZoneReader zoneReader;
    private SkimsReader skimsReader;
    private SyntheticPopulationReader syntheticPopulationReader;
    private TripGeneration tripGenModel;
    private DestinationChoice destinationChoice;
    private ModeChoice mcModel;
    private TimeOfDayChoice timeOfDayChoice;
    private OutputWriter outputWriter;
    private EconomicStatusReader economicStatusReader;
    private Emissions emissions;
    private CalibrationGermany calibrationGermany;

    private TableDataSet scenarioVariables;

    public LDModelGermanyScenarios(ZoneReader zoneReader, SkimsReader skimsReader,
                                   SyntheticPopulationReader syntheticPopulationReader,
                                   EconomicStatusReader economicStatusReader,
                                   TripGeneration tripGenModel,
                                   DestinationChoice destinationChoice,
                                   ModeChoice mcModel,
                                   TimeOfDayChoice timeOfDayChoice,
                                   Emissions emissions,
                                   OutputWriter outputWriter, CalibrationGermany calibrationGermany) {
        this.zoneReader = zoneReader;
        this.skimsReader = skimsReader;
        this.syntheticPopulationReader = syntheticPopulationReader;
        this.economicStatusReader = economicStatusReader;
        this.tripGenModel = tripGenModel;
        this.destinationChoice = destinationChoice;
        this.mcModel = mcModel;
        this.timeOfDayChoice = timeOfDayChoice;
        this.emissions = emissions;
        this.outputWriter = outputWriter;
        this.calibrationGermany = calibrationGermany;
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        Util.initializeRandomNumber(prop);

        scenarioVariables = Util.readCSVfile(inputFolder + JsonUtilMto.getStringProp(prop,"scenarioPolicy.scenarios"));
        //options
        zoneReader.setup(prop, inputFolder, outputFolder);
        skimsReader.setup(prop, inputFolder, outputFolder);
        syntheticPopulationReader.setup(prop, inputFolder, outputFolder);
        economicStatusReader.setup(prop, inputFolder, outputFolder);
        tripGenModel.setup(prop, inputFolder, outputFolder);
        destinationChoice.setup(prop, inputFolder, outputFolder);
        mcModel.setup(prop, inputFolder, outputFolder);
        timeOfDayChoice.setup(prop, inputFolder, outputFolder);
        emissions.setup(prop, inputFolder, outputFolder);
        outputWriter.setup(prop, inputFolder, outputFolder);
        calibrationGermany.setup(prop, inputFolder, outputFolder);
        logger.info("---------------------ALL MODULES SET UP---------------------");
    }

    public void load(DataSet dataSet) {

        zoneReader.load(dataSet);
        skimsReader.load(dataSet);
        syntheticPopulationReader.load(dataSet);
        economicStatusReader.load(dataSet);
        mcModel.load(dataSet);
        destinationChoice.load(dataSet);
        tripGenModel.load(dataSet);
        calibrationGermany.load(dataSet);
        emissions.load(dataSet);
        outputWriter.load(dataSet);
        logger.info("---------------------ALL MODULES LOADED---------------------");

    }

    public void run(DataSet dataSet, int nThreads) {

        //property change to avoid parallelization
        //System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "0");
        tripGenModel.run(dataSet, -1);
        destinationChoice.run(dataSet, -1);
        for (int scenario = 1; scenario <= scenarioVariables.getRowCount(); scenario++) {
            setScenarios(dataSet, scenario);
            mcModel.run(dataSet, -1);
            //timeOfDayChoice.run(dataSet, -1);
            calibrationGermany.run(dataSet, -1);
            emissions.run(dataSet, -1);
            outputWriter.run(dataSet, -1);
        }
        //print outputs



    }


    private void setScenarios(DataSet dataSet, int scenario){
        for (LongDistanceTrip t : dataSet.getAllTrips()) {
            LongDistanceTripGermany trip = (LongDistanceTripGermany) t;
            ((LongDistanceTripGermany) trip).setScenario(scenario);
        }
    }


}