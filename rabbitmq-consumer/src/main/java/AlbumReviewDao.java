import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AlbumReviewDao {
    private static final String REVIEW = "review";

    public static int updateReview(String albumID, String likeOrNot) {
        PreparedStatement statement = null;
        int count = 0;
        try (Connection remoteConnection = DataSource.getConnection()) {
            try {
                String query = String.format("UPDATE %s SET %s = %s + 1 WHERE album_id = %s",
                        REVIEW, likeOrNot, likeOrNot, albumID);
                statement = remoteConnection.prepareStatement(query);
                count = statement.executeUpdate();
                if (count < 1) {
                    System.err.println("Failed to write into the review table.");
                }
            } catch (SQLException e) {
                System.err.println("Has SQLException error when inserting into album_review: " + e);
            } finally {
                try {
                    if (statement != null) {
                        statement.close();
                    }
                } catch (SQLException e) {
                    System.err.println("Has SQLException error when closing statement: " + e);
                }
            }
        } catch (SQLException e) {
            System.err.println("Has SQLException error when getting Database Connection Pooling: " + e);
        }
        return count;
    }
}
