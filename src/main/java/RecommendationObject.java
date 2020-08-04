import org.apache.mahout.cf.taste.recommender.RecommendedItem;

public class RecommendationObject {
    private int usernameId;
    private RecommendedItem recommendedItems;

    public int getUsernameId() {
        return usernameId;
    }

    public void setUsernameId(int usernameId) {
        this.usernameId = usernameId;
    }

    public RecommendedItem getRecommendedItems() {
        return recommendedItems;
    }

    public void setRecommendedItems(RecommendedItem recommendedItems) {
        this.recommendedItems = recommendedItems;
    }
}
