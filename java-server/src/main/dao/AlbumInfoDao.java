package main.dao;

import main.DataSource;
import main.model.AlbumInfo;
import main.model.AlbumProfile;

import java.sql.*;

public class AlbumInfoDao {
    private static final String ALBUM_PROFILE = "album_profile";
    private static final String REVIEW = "review";
    private static final String SELECT_QUERY = "SELECT * FROM " + ALBUM_PROFILE + " WHERE albumID = ";
    private static final String INSERT_PROFILE_QUERY = "INSERT INTO " + ALBUM_PROFILE
            + " (artist, title, year, image) VALUES (?, ?, ?, ?)";
    private static final String INSERT_REVIEW_QUERY = "INSERT INTO " + REVIEW + " (album_id) VALUES (?)";
    public static final int ALBUMID_COLUMN_INDEX = 1;

    public static GetAlbumResult getOneAlbumFromDB(String albumID) {
        Statement statement = null;
        ResultSet resultSet = null;
        try (Connection remoteConnection = DataSource.getConnection()) {
            try {
                statement = remoteConnection.createStatement();
                resultSet = statement.executeQuery(SELECT_QUERY + albumID);
                if (resultSet.next()) {
                    String artist = resultSet.getString("artist");
                    String title = resultSet.getString("title");
                    String year = resultSet.getString("year");
                    return new GetAlbumResult(new AlbumInfo(artist, title, year), true);
                } else {
                    return new GetAlbumResult(null, true); // the item doesn't exist
                }
            } catch (SQLException e) {
                System.err.println("Has SQLException error when retrieving record: " + e);
                return new GetAlbumResult(null, false);
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
            return new GetAlbumResult(null, false);
        }
    }

    public static int insertOneReviewToDB(int albumID) {
        PreparedStatement statement = null;
        try (Connection remoteConnection = DataSource.getConnection()) {
            try {
                statement = remoteConnection.prepareStatement(INSERT_REVIEW_QUERY);
                statement.setString(1, String.valueOf(albumID));
                return statement.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Has SQLException error when inserting into album_review: " + e);
                return -1;
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
            return -1;
        }
    }

    public static int insertOneAlbumToDB(AlbumProfile albumProfile) {
        ResultSet rs = null;
        PreparedStatement statement = null;
        try (Connection remoteConnection = DataSource.getConnection()) {
            try {
                statement = remoteConnection.prepareStatement(INSERT_PROFILE_QUERY, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, albumProfile.getAlbumInfo().getArtist());
                statement.setString(2, albumProfile.getAlbumInfo().getTitle());
                statement.setString(3, albumProfile.getAlbumInfo().getYear());
                statement.setBinaryStream(4, albumProfile.getImage());
                statement.executeUpdate();

                int autoIncKeyFromApi = -1;
                rs = statement.getGeneratedKeys();
                if (rs.next()) {
                    autoIncKeyFromApi = rs.getInt(ALBUMID_COLUMN_INDEX);
                } else {
                    System.err.println("Failed when getting autoIncKeyFromApi");
                }
                return autoIncKeyFromApi;
            } catch (SQLException e) {
                System.err.println("Has SQLException error when retrieving record: " + e);
                return -1;
            } finally {
                try {
                    if (rs != null) {
                        rs.close();
                    }
                    if (statement != null) {
                        statement.close();
                    }
                } catch (SQLException e) {
                    System.err.println("Has SQLException error when closing statement / resultSet: " + e);
                }
            }
        } catch (SQLException e) {
            System.err.println("Has SQLException error when getting Database Connection Pooling: " + e);
            return -1;
        }
    }

    public static class GetAlbumResult {
        private AlbumInfo albumInfo;
        private boolean isSuccessful;

        public GetAlbumResult(AlbumInfo albumInfo, boolean isSuccessful) {
            this.albumInfo = albumInfo;
            this.isSuccessful = isSuccessful;
        }

        public AlbumInfo getAlbumInfo() {
            return albumInfo;
        }

        public void setAlbumInfo(AlbumInfo albumInfo) {
            this.albumInfo = albumInfo;
        }

        public boolean isSuccessful() {
            return isSuccessful;
        }

        public void setSuccessful(boolean successful) {
            isSuccessful = successful;
        }
    }
}
