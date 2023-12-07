package main.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class AlbumProfile {
    private AlbumInfo albumInfo;
    private InputStream image;

    public AlbumProfile(AlbumInfo albumInfo, InputStream image) {
        this.albumInfo = albumInfo;
        this.image = image;
    }

    public AlbumInfo getAlbumInfo() {
        return albumInfo;
    }

    public InputStream getImage() {
        return image;
    }

    public void setAlbumInfo(AlbumInfo albumInfo) {
        this.albumInfo = albumInfo;
    }

    public void setImage(InputStream image) {
        this.image = image;
    }

    @Override
    public String toString() {
        try {
            return "AlbumProfile{" +
                    "albumInfo=" + albumInfo +
                    ", image=" + Arrays.toString(image.readAllBytes()) +
                    '}';
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
