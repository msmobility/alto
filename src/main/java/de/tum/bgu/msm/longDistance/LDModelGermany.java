package de.tum.bgu.msm.longDistance;

import de.tum.bgu.msm.Util;
import de.tum.bgu.msm.longDistance.airportAnalysis.AirTripsGeneration;
import de.tum.bgu.msm.longDistance.data.DataSet;
import de.tum.bgu.msm.longDistance.destinationChoice.DestinationChoice;
import de.tum.bgu.msm.longDistance.emissions.Emissions;
import de.tum.bgu.msm.longDistance.io.reader.*;
import de.tum.bgu.msm.longDistance.io.writer.OutputWriter;
import de.tum.bgu.msm.longDistance.io.writer.TripsToPlans;
import de.tum.bgu.msm.longDistance.modeChoice.ModeChoice;
import de.tum.bgu.msm.longDistance.scaling.PotentialTravelersSelectionGermany;
import de.tum.bgu.msm.longDistance.scenarioAnalysis.ScenarioAnalysis;
import de.tum.bgu.msm.longDistance.timeOfDay.TimeOfDayChoice;
import de.tum.bgu.msm.longDistance.tripGeneration.TripGeneration;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import java.util.Random;

/**
 * Germany Model
 * Module to simulate long-distance travel
 * Author: Ana Moreno, Technische Universität München (TUM), ana.moreno@tum.de
 * Date: 17 December 2020
 * Version 1
 * Adapted from Ontario Provincial Model
 */

public class LDModelGermany implements ModelComponent, LDModel {

    public static Random rand;
    static Logger logger = Logger.getLogger(LDModelGermany.class);

    //modules
    private ZoneReader zoneReader;
    private GridReader gridReader;
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
    private PotentialTravelersSelectionGermany potentialTravelersSelection;
    private ScenarioAnalysis scenarioAnalysis;
    private AirTripsGeneration airTripsGeneration;
    private TripsToPlans trips2Plans;

    public LDModelGermany(ZoneReader zoneReader, GridReader gridReader, SkimsReader skimsReader,
                          SyntheticPopulationReader syntheticPopulationReader,
                          EconomicStatusReader economicStatusReader,
                          TripGeneration tripGenModel,
                          DestinationChoice destinationChoice,
                          AirTripsGeneration airTripsGeneration,
                          ModeChoice mcModel,
                          TimeOfDayChoice timeOfDayChoice,
                          Emissions emissions,
                          OutputWriter outputWriter,
                          CalibrationGermany calibrationGermany,
                          PotentialTravelersSelectionGermany potentialTravelersSelection,
                          ScenarioAnalysis scenarioAnalysis,
                          TripsToPlans trips2Plans) {
        this.zoneReader = zoneReader;
        this.gridReader = gridReader;
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
        this.potentialTravelersSelection = potentialTravelersSelection;
        this.scenarioAnalysis = scenarioAnalysis;
        this.airTripsGeneration = airTripsGeneration;
        this.trips2Plans = trips2Plans;
    }

    public void setup(JSONObject prop, String inputFolder, String outputFolder) {

        Util.initializeRandomNumber(prop);


        //options
        zoneReader.setup(prop, inputFolder, outputFolder);
        gridReader.setup(prop, inputFolder, outputFolder);
        skimsReader.setup(prop, inputFolder, outputFolder);
        syntheticPopulationReader.setup(prop, inputFolder, outputFolder);
        economicStatusReader.setup(prop, inputFolder, outputFolder);
        potentialTravelersSelection.setup(prop, inputFolder, outputFolder);
        scenarioAnalysis.setup(prop, inputFolder, outputFolder);
        tripGenModel.setup(prop, inputFolder, outputFolder);
        destinationChoice.setup(prop, inputFolder, outputFolder);
        airTripsGeneration.setup(prop, inputFolder, outputFolder);
        mcModel.setup(prop, inputFolder, outputFolder);
        timeOfDayChoice.setup(prop, inputFolder, outputFolder);
        emissions.setup(prop, inputFolder, outputFolder);
        outputWriter.setup(prop, inputFolder, outputFolder);
        calibrationGermany.setup(prop, inputFolder, outputFolder);
        trips2Plans.setup(prop, inputFolder, outputFolder);
        logger.info("---------------------ALL MODULES SET UP---------------------");
    }

    public void load(DataSet dataSet) {

        zoneReader.load(dataSet);
        gridReader.load(dataSet);
        skimsReader.load(dataSet);
        syntheticPopulationReader.load(dataSet);
        economicStatusReader.load(dataSet);
        potentialTravelersSelection.load(dataSet);
        scenarioAnalysis.load(dataSet);
        mcModel.load(dataSet);
        destinationChoice.load(dataSet);
        tripGenModel.load(dataSet);
        calibrationGermany.load(dataSet);
        timeOfDayChoice.load(dataSet);
        emissions.load(dataSet);
        outputWriter.load(dataSet);
        airTripsGeneration.load(dataSet);
        trips2Plans.load(dataSet);
        logger.info("---------------------ALL MODULES LOADED---------------------");

    }

    public void run(DataSet dataSet, int nThreads) {

        //for (int populationSection = 1; populationSection <= dataSet.getNumberOfSubpopulations(); populationSection++) {
        potentialTravelersSelection.run(dataSet, -1);
        tripGenModel.run(dataSet, -1);
        destinationChoice.run(dataSet, -1);
        airTripsGeneration.run(dataSet, -1);
        for (int policyScenario = 1; policyScenario <= dataSet.getNumberOfScenarios(); policyScenario++) {
            dataSet.setScenario(policyScenario);
            mcModel.run(dataSet, -1);
            timeOfDayChoice.run(dataSet, -1);
            calibrationGermany.run(dataSet, -1);
            emissions.run(dataSet, -1);
            outputWriter.run(dataSet, -1);
            //scenarioAnalysis.run(dataSet, -1);
        //}
        }
        scenarioAnalysis.run(dataSet, -1);
        trips2Plans.run(dataSet,-1);



    }





}
