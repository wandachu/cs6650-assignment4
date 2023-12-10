package main;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import main.dao.AlbumInfoDao;
import main.dao.ReviewDao;
import main.model.AlbumInfo;
import main.model.ErrorMsg;
import main.model.ReviewInfo;
import main.rabbitmqpool.RMQChannelPool;
import main.rabbitmqpool.RabbitMQProducer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

import static main.utils.Utils.writeJsonToResponse;

@WebServlet(name = "ReviewServlet", value = "/review/*")
public class ReviewServlet extends HttpServlet {
    private final Gson gson = new Gson();
    public static final String LIKE = "like";
    public static final String DISLIKE = "dislike";
    private static final int LIKE_OR_DISLIKE_INDEX = 1;
    private static final int ID_PARAM_INDEX = 2;
    private static final int REVIEW_ID_PARAM_INDEX = 1;
    // RMQ broker machine
    private static final String RABBITMQ_SERVER = "52.41.101.185";
    // test queue name
    private final static String QUEUE_NAME = "likeornot";
    private RMQChannelPool pool;

    @Override
    public void init() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_SERVER);
        factory.setUsername("test");
        factory.setPassword("test");

        try {
            final Connection connection = factory.newConnection();
            System.out.println("INFO: RabbitMQ connection established");
            pool = RabbitMQProducer.createChannelPool(connection);
        } catch (Exception e) {
            System.err.println("RabbitMQ connection channel pool failed to establish: " + e);
        }
    }

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

        if (!isValidReviewGetURL(urlParts)) { // invalid
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJsonToResponse(new ErrorMsg("Invalid inputs"), gson, resp);
            return;
        }

        ReviewDao.GetReviewResult oneReviewFromDB = ReviewDao.getOneReviewFromDB(urlParts[REVIEW_ID_PARAM_INDEX]);
        if (!oneReviewFromDB.isSuccessful()) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJsonToResponse(new ErrorMsg("Failed to get album info from DB"), gson, resp);
            return;
        }
        ReviewInfo reviewInfo = oneReviewFromDB.getReviewInfo();
        if (reviewInfo != null) { // exist
            resp.setStatus(HttpServletResponse.SC_OK);
            writeJsonToResponse(reviewInfo, gson, resp);
        } else { // doesn't exist
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeJsonToResponse(new ErrorMsg("Album not found"), gson, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
            writeJsonToResponse(new ErrorMsg("Invalid inputs"), gson, resp);
            return;
        }

        String likeOrDislike = urlParts[LIKE_OR_DISLIKE_INDEX];
        String albumID = urlParts[ID_PARAM_INDEX];

        // Asynchronous process
        try {
            // borrow channel object
            Channel channel = this.pool.borrowObject();

            // declare queue
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);

            // publish a message
            String payload = albumID + ":" + likeOrDislike;
            channel.basicPublish("", QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, payload.getBytes());

            // return channel object
            pool.returnObject(channel);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            writeJsonToResponse(new ErrorMsg("Album not found"), gson, resp);
            return;
        }
        resp.setStatus(HttpServletResponse.SC_CREATED);
    }

    private boolean isValidURL(String[] urlParts) {
        // example urlParts: [, like, 1]
        if (urlParts == null || urlParts.length != 3) {
            return false;
        }
        String idNumberRegex = "[0-9]+";
        Pattern idNumberPattern = Pattern.compile(idNumberRegex);
        return urlParts[0].isEmpty()
                && (urlParts[1].equals(LIKE) || urlParts[1].equals(DISLIKE))
                && idNumberPattern.matcher(urlParts[2]).matches();
    }

    private boolean isValidReviewGetURL(String[] urlParts) {
        // example urlParts: [, 5]
        if (urlParts == null || urlParts.length != 2) {
            return false;
        }
        String idNumberRegex = "[0-9]+";
        Pattern idNumberPattern = Pattern.compile(idNumberRegex);
        return urlParts[0].isEmpty() && idNumberPattern.matcher(urlParts[1]).matches();
    }
}
