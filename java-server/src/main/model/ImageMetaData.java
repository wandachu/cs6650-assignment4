package main.model;

public class ImageMetaData {
    private String albumID;
    private String imageSize;

    public ImageMetaData(String albumID, String imageSize) {
        this.albumID = albumID;
        this.imageSize = imageSize;
    }

    public String getAlbumID() {
        return albumID;
    }

    public void setAlbumID(String albumID) {
        this.albumID = albumID;
    }

    public String getImageSize() {
        return imageSize;
    }

    public void setImageSize(String imageSize) {
        this.imageSize = imageSize;
    }
}
