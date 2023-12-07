package main;

import com.google.gson.Gson;
import main.model.AlbumInfo;
import main.model.AlbumProfile;
import main.model.ErrorMsg;
import main.model.ImageMetaData;
import main.dao.AlbumInfoDao;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static main.dao.AlbumInfoDao.insertOneReviewToDB;
import static main.utils.Utils.writeJsonToResponse;


@WebServlet(name = "AlbumServlet", value = "/albums/*")
@MultipartConfig
public class AlbumServlet extends HttpServlet {
    private static final String KEY_NAME_ALBUM_IMAGE = "image";
    private static final String KEY_NAME_ALBUM_PROFILE = "profile";
    private static final int ID_PARAM_INDEX = 1;

    private final Gson gson = new Gson();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        String urlPath = req.getPathInfo();

        if (urlPath == null || urlPath.isEmpty()) { // URL doesn't exists, invalid request
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJsonToResponse(new ErrorMsg("invalid request"), gson, resp);
            return;
        }

        String[] urlParts = urlPath.split("/");

        if (!isValidURL(urlParts)) { // invalid
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJsonToResponse(new ErrorMsg("invalid request"), gson, resp);
            return;
        }

        AlbumInfoDao.GetAlbumResult oneAlbumFromDBResult = AlbumInfoDao.getOneAlbumFromDB(urlParts[ID_PARAM_INDEX]);
        if (!oneAlbumFromDBResult.isSuccessful()) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJsonToResponse(new ErrorMsg("Failed to get album info from DB"), gson, resp);
            return;
        }
        AlbumInfo oneAlbum = oneAlbumFromDBResult.getAlbumInfo();
        if (oneAlbum != null) { // exist
            resp.setStatus(HttpServletResponse.SC_OK);
            writeJsonToResponse(oneAlbum, gson, resp);
        } else { // doesn't exist
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeJsonToResponse(new ErrorMsg("Key not found"), gson, resp);
        }
    }

    private boolean isValidURL(String[] urlParts) {
        // example urlParts: [, 5]
        if (urlParts == null || urlParts.length != 2) {
            return false;
        }
        String idNumberRegex = "[0-9]+";
        Pattern idNumberPattern = Pattern.compile(idNumberRegex);
        return urlParts[0].isEmpty() && idNumberPattern.matcher(urlParts[1]).matches();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        String urlPath = req.getPathInfo();

        if (urlPath != null) { // should be null as we match to /albums
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJsonToResponse(new ErrorMsg("invalid request"), gson, resp);
            return;
        }

        // Read the file data from body
        AlbumInfo postedAlbumInfo = null;
        InputStream imageInputStream = null;
        long size = 0;
        for (Part part : req.getParts()) {
            if (part.getName().equals(KEY_NAME_ALBUM_IMAGE)) {
                size += part.getSize();
                imageInputStream = part.getInputStream();
            } else if (part.getName().equals(KEY_NAME_ALBUM_PROFILE)) {
                String jsonData = readPartContent(part);
                postedAlbumInfo = gson.fromJson(jsonData, AlbumInfo.class);
            }
        }

        // Create the new album object
        String imageSize = String.valueOf(size);

        // insert to DB
        AlbumProfile albumProfile = new AlbumProfile(postedAlbumInfo, imageInputStream);
        int autoIncKey = AlbumInfoDao.insertOneAlbumToDB(albumProfile);
        if (autoIncKey < 0) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJsonToResponse(new ErrorMsg("Failed to get auto increment key from DB"), gson, resp);
            return;
        }
        if (imageInputStream != null) {
            imageInputStream.close();
        }
        // write to the review table with 0 value
        int result = insertOneReviewToDB(autoIncKey);
        if (result < 0) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJsonToResponse(new ErrorMsg("Failed to write into review DB"), gson, resp);
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        ImageMetaData createdMetaData = new ImageMetaData(String.valueOf(autoIncKey), imageSize);
        writeJsonToResponse(createdMetaData, gson, resp);
    }

    private String readPartContent(Part part) throws IOException {
        try (InputStream inputStream = part.getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(reader)) {

            return bufferedReader.lines().collect(Collectors.joining());
        }
    }



}
