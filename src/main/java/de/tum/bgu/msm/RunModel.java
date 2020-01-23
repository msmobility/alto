package de.tum.bgu.msm;

import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LDModel;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;


import java.util.ResourceBundle;

/**
 * Ontario Provincial Model
 * Module to simulate long-distance travel
 * Author: Rolf Moeckel, Technische Universität München (TUM), rolf.moeckel@tum.de
 * Date: 11 December 2015
 * Version 1
 */

public class RunModel {
    // main class
    private static Logger logger = Logger.getLogger(RunModel.class);
    private JSONObject prop;

    private RunModel(JSONObject prop) {
        // constructor
        this.prop = prop;
    }


    public static void main(String[] args) {
        // main model run method

        logger.info("MITO Long distance model");
        // Check how many arguments were passed in
        if (args.length != 3) {
            logger.error("Error: Please provide three arguments, 1. the model resources as rb, 2. the model resources as json, 3. the start year");
            System.exit(0);
        }
        long startTime = System.currentTimeMillis();
        JsonUtilMto jsonUtilMto = new JsonUtilMto(args[1]);
        JSONObject prop = jsonUtilMto.getJsonProperties();


        RunModel model = new RunModel(prop);
        model.runLongDistModel();
        float endTime = Util.rounder(((System.currentTimeMillis() - startTime) / 60000), 1);
        int hours = (int) (endTime / 60);
        int min = (int) (endTime - 60 * hours);
        int seconds = (int) ((endTime - 60 * hours - min) * 60);
        logger.info("Runtime: " + hours + " hours and " + min + " minutes and " + seconds + " seconds.");
    }


    private void runLongDistModel() {
        // main method to run long-distance model
        logger.info("Started runLongDistModel for the year " + JsonUtilMto.getIntProp(prop, "year"));
        DataSet dataSet = new DataSet();
        String inputFolder = "";
        String outputFolder = "./output/";
        LDModel ldModel = new LDModel();
        ldModel.setup(prop, inputFolder, outputFolder);
        ldModel.load(dataSet);
        ldModel.run(dataSet, -1);
        logger.info("Module runLongDistModel completed.");

    }
}
