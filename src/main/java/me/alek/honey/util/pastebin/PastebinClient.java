package me.alek.honey.util.pastebin;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PastebinClient {

    private static final String URL_STRING = "https://pastebin.com/api/api_post.php";

    public static String post(String developerKey, PasteRequest request) {
        StringBuilder postBody = new StringBuilder();

        for (Map.Entry<String, String> entry : request.getParameters().entrySet()) {
            postBody.append("&");

            try {
                postBody.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
            } catch (UnsupportedEncodingException var22) {
                throw new IllegalStateException("Could not find UTF-8 encoding (this should never happen)");
            }
        }
        postBody.append("&api_dev_key=").append(developerKey);

        try {
            URL url = new URL(URL_STRING);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            DataOutputStream parametersStream = new DataOutputStream(connection.getOutputStream());
            parametersStream.writeBytes(postBody.toString());
            parametersStream.flush();
            parametersStream.close();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            return reader.readLine();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
