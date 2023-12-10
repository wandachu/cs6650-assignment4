package main.dao;

import main.ReadReplicaDataSource;
import main.model.ReviewInfo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ReviewDao {
    private static final String ALBUM_ID = "album_id";
    private static final String REVIEW = "review";
    private static final String LIKE = "likes";
    private static final String DISLIKE = "dislikes";

    private static final String SELECT_QUERY = "SELECT * FROM " + REVIEW + " WHERE " + ALBUM_ID + " = ";

    public static GetReviewResult getOneReviewFromDB(String albumID) {
        Statement statement = null;
        ResultSet resultSet = null;
        try (Connection remoteConnection = ReadReplicaDataSource.getConnection()) { // TODO
            try {
                statement = remoteConnection.createStatement();
                resultSet = statement.executeQuery(SELECT_QUERY + albumID);
                if (resultSet.next()) {
                    String likes = resultSet.getString(LIKE);
                    String dislikes = resultSet.getString(DISLIKE);
                    return new GetReviewResult(new ReviewInfo(likes, dislikes), true);
                } else {
                    return new GetReviewResult(null, true); // the item doesn't exist
                }
            } catch (SQLException e) {
                System.err.println("Has SQLException error when retrieving record: " + e);
                return new GetReviewResult(null, false);
            } finally {
                try {
                    if (statement != null) {
                        statement.close();
                    }
                    if (resultSet != null) {
                        resultSet.close();
                    }
                } catch (SQLException e) {
                    System.err.println("Has SQLException error when closing statement / resultSet: " + e);
                }
            }
        }  catch (SQLException e) {
            System.err.println("Has SQLException error when getting Database Connection Pooling: " + e);
            return new GetReviewResult(null, false);
        }
    }

    public static class GetReviewResult {
        private ReviewInfo reviewInfo;
        private boolean isSuccessful;

        public GetReviewResult(ReviewInfo reviewInfo, boolean isSuccessful) {
            this.reviewInfo = reviewInfo;
            this.isSuccessful = isSuccessful;
        }

        public ReviewInfo getReviewInfo() {
            return reviewInfo;
        }

        public void setReviewInfo(ReviewInfo reviewInfo) {
            this.reviewInfo = reviewInfo;
        }

        public boolean isSuccessful() {
            return isSuccessful;
        }

        public void setSuccessful(boolean successful) {
            isSuccessful = successful;
        }
    }
}
