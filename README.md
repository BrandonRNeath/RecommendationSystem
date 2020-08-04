# Recommendation System 

The recommendation system was developed to work along side the Fitness Android application that was developed for my Final year project. The developed Fitness application consisted of a diary feature where users could enter their food and drink they have had throughout their day as well as the exercises they completed. This recommendation system carries out user based recommendations using Pearson Correlation Similarity and provides daily recommendations to the users of the application for both a nutrition recommendation and a exercise recommendation. The system determines its recommendations off users that are similiar to one another.

## Recommendation System Instructions
1: Install Gradle Version 4.10.2 or above (If Gradle is not already installed)
2: SQL Workbench Version 8.0.19 or above is needed to be installed if not already
3: Create localhost connection to localhost server
4: Open a SQL connection on SQL workbench 
5: Select Server tab and select Data Import
6: Select Import from Self-Contained File and select FitMorph_Data.sql which is located within the GitHub repo
7: For Default Target Schema click New… and name the Schema fitnessdb
8: Select the drop down menu for Default Target Schema and select fitnessdb
9: Select Start Import
10: Open terminal and cd into RecommendationSystem/
11: Run the command gradle build 
12: Run the command gradle run --args='USERNAME PASSWORD PORT' 
USERNAME = your localhost username 
PASSWORD = your localhost password 
PORT = your localhost port


The following is the external recommender engine used to develop this system:

* [Apache Mahout](https://mahout.apache.org/users/recommender/recommender-documentation.html)