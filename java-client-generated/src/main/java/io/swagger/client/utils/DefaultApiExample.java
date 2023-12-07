package io.swagger.client.utils;

import io.swagger.client.utils.model.AlbumInfo;
import io.swagger.client.utils.model.AlbumsProfile;
import io.swagger.client.utils.model.ImageMetaData;
import io.swagger.client.utils.api.DefaultApi;

import java.io.File;


public class DefaultApiExample {

    public static void main(String[] args) {

        DefaultApi apiInstance = new DefaultApi();

        // get request
        String albumID = "123"; // String | path  parameter is album key to retrieve
        try {
            AlbumInfo result = apiInstance.getAlbumByKey(albumID);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling DefaultApi#getAlbumByKey");
            e.printStackTrace();
        }

        // post request
        File image = new File("nmtb.png"); // File |
        AlbumsProfile profile = new AlbumsProfile(); // AlbumsProfile |
        profile.setArtist("Jay Chow");
        profile.setYear("2002");
        profile.setTitle("Fantasy");
        try {
            ImageMetaData result = apiInstance.newAlbum(image, profile);
            System.out.println(result.toString());
        } catch (ApiException e) {
            System.err.println("Exception when calling DefaultApi#newAlbum");
            e.printStackTrace();
        }
    }
}