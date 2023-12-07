package main.utils;

import com.google.gson.Gson;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class Utils {

    public static void writeJsonToResponse(Object object, Gson gson, HttpServletResponse resp) throws IOException {
        String responseMessageStr = gson.toJson(object);
        PrintWriter writer = resp.getWriter();
        writer.print(responseMessageStr);
        writer.flush();
    }
}
