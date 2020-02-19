package de.tum.bgu.msm.longDistance.tripGeneration;

import com.pb.common.datafile.TableDataSet;
import de.tum.bgu.msm.JsonUtilMto;
import de.tum.bgu.msm.longDistance.DataSet;
import de.tum.bgu.msm.longDistance.LDModel;
import de.tum.bgu.msm.longDistance.data.*;
import de.tum.bgu.msm.longDistance.data.sp.Person;
import de.tum.bgu.msm.Util;

import org.json.simple.JSONObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Created by Carlos on 7/19/2016.
 * Based on number of trips and increased later with travel parties.
 */
public class InternationalTripGeneration implements TripGenerationModule {

    private static Logger logger = LogManager.getLogger(InternationalTripGeneration.class);

    //private ResourceBundle rb;
    private JSONObject prop;
    private Map<Purpose, Map<Type, Double>> sumProbabilities;
    private int[] personIds;
    private Map<Purpose, Map<Type, Double[]>> probabilityMatrix;
    //private SyntheticPopulation synPop;
    private TableDataSet travelPartyProbabilities;
    private TableDataSet internationalTripRates;
    private TableDataSet tripGenerationCoefficients;

    private TableDataSet originCombinedZones;

    private DataSet dataSet;

    private AtomicInteger atomicInteger;


    public InternationalTripGeneration(JSONObject prop) {
//        this.synPop = synPop;
//        this.rb = rb;
        this.prop = prop;


        //String internationalTriprates = rb.getString("int.trips");
        internationalTripRates = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"trip_generation.international.rates_file"));
        internationalTripRates.buildIndex(internationalTripRates.getColumnPosition("tripState"));

        //String intTravelPartyProbabilitiesFilename = rb.getString("int.parties");;
        travelPartyProbabilities = Util.readCSVfile(JsonUtilMto.getStringProp(prop,"trip_generation.international.party_file"));
        travelPartyProbabilities.buildIndex(travelPartyProbabilities.getColumnPosition("travelParty"));



    }


    public void load(DataSet dataSet){

        this.dataSet = dataSet;
        //method to calculate the accessibility to US as a measure of the probability of starting and international trip
//        List<String> fromZones;
//        List<String> toZones;
//        fromZones = Arrays.asList("ONTARIO");
//        float alpha = (float) ResourceUtil.getDoubleProperty(rb, "int.access.alpha");
//        float beta = (float) ResourceUtil.getDoubleProperty(rb, "int.access.beta");
//        toZones = Arrays.asList("EXTUS");
//        zonalData.calculateAccessibility(zonalData.getZoneList(), fromZones, toZones, alpha , beta);

        originCombinedZones = dataSet.getDcIntOutbound().getOrigCombinedZones();


    }

    //method to run the trip generation
    public ArrayList<LongDistanceTrip> run() {

      atomicInteger = new AtomicInteger(dataSet.getAllTrips().size() + 1);



        ArrayList<LongDistanceTrip> trips = new ArrayList<>();

        //initialize probMatrices
        for (Purpose purpose : PurposeOntario.values()){
            sumProbabilities.put(purpose, new HashMap<>());
            probabilityMatrix.put(purpose, new HashMap<>());
            for (Type type : TypeOntario.values()){
                sumProbabilities.get(purpose).put(type, 0.);
                probabilityMatrix.get(purpose).put(type, new Double[dataSet.getPersons().size()]);
            }
        }
        personIds = new int[dataSet.getPersons().size()];

        //normalize p(travel) per purpose/state by sum of the probability for each person
        sumProbs();

        //run trip generation
        for (Purpose tripPurpose : PurposeOntario.values()) {
            for (TypeOntario tripState : TypeOntario.values()) {
                int tripCount = 0;
                //get the total number of trips to generate
                int numberOfTrips = (int)(internationalTripRates.getIndexedValueAt(TypeOntario.getIndex(tripState), tripPurpose.toString().toLowerCase())*personIds.length);
                //select the travellers - repeat more than once because the two random numbers can be in the interval of 1 person
                for (int iteration = 0; iteration < 5; iteration++){
                    int n = numberOfTrips - tripCount;
                    double[] randomChoice = new double[n];
                    for (int k = 0; k < randomChoice.length; k++) {
                        randomChoice[k] = LDModel.rand.nextDouble()*sumProbabilities.get(tripPurpose).get(tripState);
                    }
                    //sort the matrix for faster lookup
                    Arrays.sort(randomChoice);
                    //look up for the n travellers
                    int p = 0;
                    double cumulative = probabilityMatrix.get(tripPurpose).get(tripState)[p];

                    for (double randomNumber : randomChoice){
                        while (randomNumber > cumulative && p < personIds.length - 1) {
                            p++;
                            cumulative += probabilityMatrix.get(tripPurpose).get(tripState)[p];
                        }
                        Person pers = dataSet.getPersonFromId(personIds[p]);
                        if (!pers.isDaytrip() && !pers.isAway() && !pers.isInOutTrip() && pers.getAge() > 17 && tripCount < numberOfTrips) {

                            LongDistanceTrip trip = createIntLongDistanceTrip(pers, tripPurpose,tripState, travelPartyProbabilities);
                            trips.add(trip);
                            tripCount++;
                        }
                    }
                    if (numberOfTrips - tripCount == 0){
                        //logger.info("Number of iterations: " + iteration);
                        break;
                    }
                }
                //logger.info(tripCount + " international trips generated in Ontario, with purpose " + tripPurpose + " and state " + tripState);
            }
        }
        return trips;
    }


    private LongDistanceTrip createIntLongDistanceTrip(Person pers, Purpose tripPurpose, Type tripState, TableDataSet travelPartyProbabilities ){

        TypeOntario type = (TypeOntario) tripState;

        switch (type) {
            case AWAY :
                pers.setAway(true);
            case DAYTRIP:
                pers.setDaytrip(true);
            case INOUT:
                pers.setInOutTrip(true);
        }

        ArrayList<Person> adultsHhTravelParty = DomesticTripGeneration.addAdultsHhTravelParty(pers, tripPurpose.toString(), travelPartyProbabilities);
        ArrayList<Person> kidsHhTravelParty = DomesticTripGeneration.addKidsHhTravelParty(pers, tripPurpose.toString(), travelPartyProbabilities);
        ArrayList<Person> hhTravelParty = new ArrayList<>();
        hhTravelParty.addAll(adultsHhTravelParty);
        hhTravelParty.addAll(kidsHhTravelParty);
        int nonHhTravelPartySize = DomesticTripGeneration.addNonHhTravelPartySize(tripPurpose.toString(), travelPartyProbabilities);

        int tripDuration;
        if (pers.isDaytrip()) tripDuration = 0;
        else {
            tripDuration = 1;
        }

        LongDistanceTrip trip =  new LongDistanceTrip(atomicInteger.getAndIncrement(), pers, true, tripPurpose, tripState, pers.getHousehold().getZone(), tripDuration,
                nonHhTravelPartySize);
        trip.setHhTravelParty(hhTravelParty);

        return trip;


    }


    public void sumProbs(){
        List <Person> persons = new ArrayList<>(dataSet.getPersons().values());

        //make random list of persons
        Collections.shuffle(persons, LDModel.rand);
        double exponent = 2;

        int p = 0;
        for (Person pers : persons) {
            //IntStream.range(0, synPop.getPersons().size()).parallel().forEach(p -> {
            //Person pers = synPop.getPersonFromId(p);
            personIds[p] = pers.getPersonId();
            if (pers.getTravelProbabilities() != null) {
                for (Purpose tripPurpose : PurposeOntario.values()) {
                    for (Type tripState : TypeOntario.values()) {

                        if (pers.isAway() || pers.isDaytrip() || pers.isInOutTrip() || pers.getAge() < 18) {
                            probabilityMatrix.get(tripPurpose).get(tripState)[p] = 0.;
                            //cannot be an adult travelling
                        } else {
                            //probabilityMatrix[i][j][p] = pers.getTravelProbabilities()[i][j];
                            //correct here the probability by the accessibility to US - using access at level 2 zone
                            probabilityMatrix.get(tripPurpose).get(tripState)[p] = pers.getTravelProbabilities().get(tripPurpose).get(tripState) * Math.pow(originCombinedZones.getIndexedValueAt(pers.getHousehold().getZone().getCombinedZoneId(), "usAccess"),exponent);
                        }
                        double newValue = sumProbabilities.get(tripPurpose).get(tripState) + probabilityMatrix.get(tripPurpose).get(tripState)[p];
                        sumProbabilities.get(tripPurpose).put(tripState, newValue);
                    }
                }
            }
            p++;

        }

        //});
        //logger.info("  sum of probabilities done for " + p + " persons");

    }

}
