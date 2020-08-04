import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Recommendations {

    private static final HashMap<String, Integer> usernameMappingValue = new HashMap<>();
    private static final HashMap<Integer, String> usernameMappingKey = new HashMap<>();
    private static final HashMap<String, Integer> nutritionEntryMappingValue = new HashMap<>();
    private static final HashMap<Integer, String> nutritionEntryMappingKey = new HashMap<>();
    private static final HashMap<String, Integer> exerciseEntryMappingValue = new HashMap<>();
    private static final HashMap<Integer, String> exerciseEntryMappingKey = new HashMap<>();
    private static final ArrayList<Integer> allRecommendationIds = new ArrayList<>();
    private static final HashSet<Integer> existingNutritionRecommendationIds = new HashSet<>();
    private static final HashSet<Integer> existingExerciseRecommendationIds = new HashSet<>();
    private static Connection con;
    private static int minValueEntryCountNutrition;
    private static int maxValueEntryCountNutrition;
    private static int minValueEntryCountExercise;
    private static int maxValueEntryCountExercise;


    public static void main(String[] args) {


        try {
            // Fetching JDBC Driver
            Class.forName("com.mysql.jdbc.Driver");
            // Entering database login credentials
            final String DB_NAME = "FitnessDB";
            final String USERNAME = args[0];
            final String PASSWORD = args[1];
            final String HOST_NAME = "127.0.0.1";
            final String PORT = args[2];

            // Url for AWS database connection
            String jdbcUrl = "jdbc:mysql://" + HOST_NAME + ":" + PORT + "/" + DB_NAME + "?user=" + USERNAME + "&password="
                    + PASSWORD;

            // Acquiring Database Connection
            con = DriverManager.getConnection(jdbcUrl);
            System.out.println("Connection to database was successful");

            //-----------------------------------------------------------------------------------------------
            executeRecommendationSystem();

        } catch (ClassNotFoundException | SQLException | IOException e) {
            System.out.println(e.toString());
        }
    }


    private static void executeRecommendationSystem() throws SQLException, IOException {


        // Creating SQL Query to get username and their recommendation ids
        String usernameQuery = "SELECT * FROM user_recommendation_ids";
        Statement usernameQueryStatement = con.createStatement();
        ResultSet usernameQueryResult = usernameQueryStatement.executeQuery(usernameQuery);

        // Storing username and recommendation within a HashMap
        while (usernameQueryResult.next()) {
            usernameMappingValue.put(usernameQueryResult.getString("username")
                    , usernameQueryResult.getInt("recommendationids"));
            usernameMappingKey.put(usernameQueryResult.getInt("recommendationids")
                    , usernameQueryResult.getString("username"));
            allRecommendationIds.add(usernameQueryResult.getInt("recommendationids"));
        }

        // Entry queries
        // Query for NutritionEntry counts for csv file
        String nutritionEntryCountQuery = "SELECT * FROM diary_nutrition_info_quantities ORDER BY nutritionentrycount";
        // Query for NutritionEntries
        String nutritionEntryQuery = "SELECT * FROM diary_nutrition_info_quantities ORDER BY nutritionentrycount";
        // Query for ExerciseEntry counts for csv file
        String exerciseEntryCountQuery = "SELECT * FROM diary_exercise_info_quantities ORDER BY exercise_entrycount";
        // Query for ExerciseEntries
        String exerciseEntryQuery = "SELECT *  FROM diary_exercise_info_quantities ORDER BY exercise_entrycount";

        // Creating Statements
        Statement nutritionEntryCountStatement = con.createStatement();
        Statement nutritionEntryStatement = con.createStatement();
        Statement exerciseEntryCountStatement = con.createStatement();
        Statement exerciseEntryStatement = con.createStatement();


        // Results Sets
        ResultSet nutritionEntryQueryResult = nutritionEntryStatement.executeQuery(nutritionEntryQuery);
        ResultSet nutritionEntryCountQueryResult = nutritionEntryCountStatement.
                executeQuery(nutritionEntryCountQuery);
        ResultSet exerciseEntryQueryResult = exerciseEntryStatement.executeQuery(exerciseEntryQuery);
        ResultSet exerciseEntryCountQueryResult = exerciseEntryCountStatement.
                executeQuery(exerciseEntryCountQuery);


        findMinMaxEntryCounts(nutritionEntryCountQueryResult, exerciseEntryCountQueryResult);

        writeToNutritionCSV(nutritionEntryQueryResult);

        writeToExerciseCSV(exerciseEntryQueryResult);


        processUserRecommendations(existingNutritionRecommendationIds, "src/main/resources/nutritionRecomData.csv",
                true);

        System.out.println("-----------------------------------------------------------------------");
        processUserRecommendations(existingExerciseRecommendationIds,
                "src/main/resources/exerciseRecomData.csv",
                false);

    }

    /**
     * Finding minimum and maximum for diary entries for both nutrition and exercise entries
     *
     * @param nutritionEntryCountQueryResult is the result from the SQL query to fetch number of entries per exercise
     *                                       entry
     * @param exerciseEntryCountQueryResult  is the result from the SQL query to fetch number of entries per exercise
     *                                       entry
     * @throws SQLException SQL exception is thrown if query fails
     */
    private static void findMinMaxEntryCounts(ResultSet nutritionEntryCountQueryResult,
                                              ResultSet exerciseEntryCountQueryResult) throws SQLException {
        int i = 0;
        boolean firstLineRead = false;

        // Searching for Min nutrition entry count and Max exercise entry count
        while (nutritionEntryCountQueryResult.next()) {
            if (!firstLineRead) {
                minValueEntryCountNutrition = nutritionEntryCountQueryResult.
                        getInt("nutritionentrycount");
                firstLineRead = true;
            }
            maxValueEntryCountNutrition = nutritionEntryCountQueryResult.getInt("nutritionentrycount");
            i++;
        }
        firstLineRead = false;

        // Searching for Min exercise entry count and Max exercise entry count
        while (exerciseEntryCountQueryResult.next()) {
            if (!firstLineRead) {
                minValueEntryCountExercise = exerciseEntryCountQueryResult.
                        getInt("exercise_entrycount");
                firstLineRead = true;
            }
            maxValueEntryCountExercise = exerciseEntryCountQueryResult.getInt("exercise_entrycount");
            i++;
        }
    }

    /**
     * @param nutritionEntryQueryResult result set from query made to SQL database to fetch all nutrition entries made
     *                                  by users
     * @throws IOException
     * @throws SQLException
     */
    private static void writeToNutritionCSV(ResultSet nutritionEntryQueryResult) throws IOException, SQLException {

        // Writing to Nutrition CSV setup
        FileWriter fileWriterNutritionCSV;
        fileWriterNutritionCSV = new FileWriter("src/main/resources/nutritionRecomData.csv");

        // Mapping of both types of entries (Nutrition and Exercise)
        int i = 0;

        while (nutritionEntryQueryResult.next()) {

            // Checking if mapping of entry has already been done
            if (!nutritionEntryMappingValue.containsKey(
                    nutritionEntryQueryResult.getString("nutritionentry"))) {
                nutritionEntryMappingValue.put(
                        nutritionEntryQueryResult.getString("nutritionentry"), i);
                nutritionEntryMappingKey.put(i,
                        nutritionEntryQueryResult.getString("nutritionentry"));
            }
            existingNutritionRecommendationIds.add(usernameMappingValue
                    .get(nutritionEntryQueryResult.getString("username")));
            // Writing username ids,nutritionEntryIds and nutritionEntryCount to csv file
            fileWriterNutritionCSV.write(String.valueOf(usernameMappingValue.
                    get(nutritionEntryQueryResult.getString("username"))));
            fileWriterNutritionCSV.write(',');
            fileWriterNutritionCSV.write(String.valueOf(nutritionEntryMappingValue.get(nutritionEntryQueryResult
                    .getString("nutritionentry"))));
            fileWriterNutritionCSV.write(',');

            //Normalize nutritionentrycount
            fileWriterNutritionCSV.write(String.format("%.1f", normaliseNutritionCount(nutritionEntryQueryResult
                    .getInt("nutritionentrycount"))));
            fileWriterNutritionCSV.write('\n');
            i++;
        }
        fileWriterNutritionCSV.flush();
        fileWriterNutritionCSV.close();
    }

    /**
     * @param exerciseEntryQueryResult result set from query made to SQL database to fetch all exercise entries made
     *                                 by users
     * @throws IOException
     * @throws SQLException
     */
    private static void writeToExerciseCSV(ResultSet exerciseEntryQueryResult) throws IOException, SQLException {

        // Writing to CSV
        FileWriter fileWriterExerciseCSV;
        fileWriterExerciseCSV = new FileWriter("src/main/resources/exerciseRecomData.csv");
        int i = 0;

        while (exerciseEntryQueryResult.next()) {
            // Checking if mapping of entry has already been done
            if (!exerciseEntryMappingValue.containsKey(
                    exerciseEntryQueryResult.getString("exercise_entry"))) {
                exerciseEntryMappingValue.put(exerciseEntryQueryResult.getString("exercise_entry"), i);
                exerciseEntryMappingKey.put(i, exerciseEntryQueryResult.getString("exercise_entry"));
            }
            existingExerciseRecommendationIds.add(usernameMappingValue
                    .get(exerciseEntryQueryResult.getString("username")));

            // Writing username ids,exerciseEntryIds and exerciseEntryCount to csv file
            fileWriterExerciseCSV.write(String.valueOf(usernameMappingValue.
                    get(exerciseEntryQueryResult.getString("username"))));
            fileWriterExerciseCSV.write(',');
            fileWriterExerciseCSV.write(String.valueOf(exerciseEntryMappingValue.
                    get(exerciseEntryQueryResult.getString("exercise_entry"))));
            fileWriterExerciseCSV.write(',');
            fileWriterExerciseCSV.write(String.format("%.1f", normaliseExerciseCount(exerciseEntryQueryResult
                    .getInt("exercise_entrycount"))));
            fileWriterExerciseCSV.write('\n');
            i++;
        }
        fileWriterExerciseCSV.flush();
        fileWriterExerciseCSV.close();
    }

    /**
     * Updates the SQL rows for user's with their new recommendation
     *
     * @param username        is the username of the user that is getting their nutrition recommendation
     * @param itemRecommended is new nutrition item that has been recommended
     */
    private static void sendRecommendationNutrition(String username, String itemRecommended) {
        // Query for SQL
        String insertRecommendedNutritionQuery = "UPDATE user_recommendation_ids " +
                "SET nutrition_recommendation = '" + itemRecommended + "'" +
                " WHERE username = '" + username + "'";
        try {
            Statement insertRecommendedNutritionStatement = con.createStatement();
            insertRecommendedNutritionStatement.executeUpdate(insertRecommendedNutritionQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the SQL rows for user's with their new recommendation
     *
     * @param username        is the username of the user that is getting their exercise recommendation
     * @param itemRecommended is new nutrition item that has been recommended
     */
    private static void sendRecommendationExercise(String username, String itemRecommended) {
        // Query for SQL
        String insertRecommendedExerciseQuery = "UPDATE user_recommendation_ids " +
                "SET exercise_recommendation = '" + itemRecommended + "'" +
                " WHERE username = '" + username + "'";
        try {
            Statement insertRecommendedNutritionStatement = con.createStatement();
            insertRecommendedNutritionStatement.executeUpdate(insertRecommendedExerciseQuery);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Normalises the nutrition entry counts fetched from SQL to allow cleaner recommendations
     * The formula used is the Min Max normalisation
     *
     * @param valueToNormalise
     * @return the normalised nutrition entry count value
     */
    private static double normaliseNutritionCount(double valueToNormalise) {
        return (valueToNormalise - minValueEntryCountNutrition) / (maxValueEntryCountNutrition -
                minValueEntryCountNutrition);
    }

    /**
     * Normalises the exercise entry counts fetched from SQL to allow cleaner recommendations
     * The formula is the Min Max normalisation
     *
     * @param valueToNormalise
     * @return the normalised exercise entry count value
     */
    private static double normaliseExerciseCount(double valueToNormalise) {
        return (valueToNormalise - minValueEntryCountExercise) / (maxValueEntryCountExercise -
                minValueEntryCountExercise);
    }

    /**
     * Processes nutrition and exercise recommendations for the users
     *
     * @param existingRecommendationIds current ids of recommendations
     * @param filePath
     * @param chosenRecommendation
     */
    private static void processUserRecommendations(HashSet<Integer> existingRecommendationIds, String filePath,
                                                   boolean chosenRecommendation) {

        DataModel model = null;
        try {
            model = new FileDataModel(new File(filePath));
        } catch (IOException e) {
        }
        {
            try {
                // Type of user similarity
                UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
                // Type of user neighborhood
                UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
                // Type of recommender
                UserBasedRecommender recommender = new GenericUserBasedRecommender(model, neighborhood, similarity);
                // List of recommended objects
                ArrayList<RecommendationObject> recommendationObjects = new ArrayList<>();

                ArrayList<Integer> recommendationSizes = new ArrayList<>();
                for (Integer existingRecommendationId : existingRecommendationIds) {
                    List<RecommendedItem> recommendations = recommender.recommend(
                            usernameMappingValue.get(
                                    usernameMappingKey.get(existingRecommendationId)), 5);

                    if (recommendations.size() != 0) {
                        recommendationSizes.add(recommendations.size());
                    }
                    for (RecommendedItem recommendation : recommendations) {
                        RecommendationObject recommendationObject = new RecommendationObject();
                        recommendationObject.setUsernameId(usernameMappingValue.get(
                                usernameMappingKey.get(existingRecommendationId)));
                        recommendationObject.setRecommendedItems(recommendation);
                        recommendationObjects.add(recommendationObject);
                    }
                }

                int count = 0;
                int j;
                // Searching through all the recommendations
                for (int i = 0; i < recommendationSizes.size(); i++) {
                    // Setting Math.random bound
                    int bound = recommendationSizes.get(i) + count - 1;
                    // If the count is equal to the bound then the current count index is returned for
                    // chosen recommendation
                    if (count == bound) {
                        j = count;
                    } else {
                        // Random number between bound is chosen to randomly select recommendation item
                        j = ThreadLocalRandom.current().nextInt(count, bound);
                    }
                    int recommendedItemId = (int) recommendationObjects.get(j).getRecommendedItems().getItemID();
                    // Send recommendation to be stored into SQL database
                    // If chosenRecommendation == true then recommendations for nutrition entry are sent
                    if (chosenRecommendation) {
                        sendRecommendationNutrition(usernameMappingKey.
                                        get(recommendationObjects.get(j).getUsernameId()),
                                nutritionEntryMappingKey.get(recommendedItemId));
                    }
                    // else recommendations for exercise entries are sent
                    else {
                        sendRecommendationExercise(usernameMappingKey.
                                        get(recommendationObjects.get(j).getUsernameId()),
                                exerciseEntryMappingKey.get(recommendedItemId));
                    }
                    count += recommendationSizes.get(i);
                }
            } catch (TasteException e) {
                System.out.println("User Does not Exist");
            }
        }
    }
}





