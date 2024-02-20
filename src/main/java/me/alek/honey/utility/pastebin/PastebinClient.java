package me.alek.honey.utility.pastebin;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

public class PastebinClient {

    public static String post(String developerKey, PasteRequest request) {
        StringBuilder postBody = new StringBuilder();
        boolean first = true;

        for(Iterator<Map.Entry<String, String>> var5 = request.getParameters().entrySet().iterator(); var5.hasNext(); first = false) {
            Map.Entry<String, String> entry = var5.next();
            if (!first) {
                postBody.append("&");
            }

            try {
                postBody.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
            } catch (UnsupportedEncodingException var22) {
                throw new IllegalStateException("Could not find UTF-8 encoding (this should never happen)");
            }
        }
        postBody.append("&api_dev_key=").append(developerKey);

        try {
            String urlString = "https://pastebin.com/api/api_post.php";
            URL url = new URL(urlString);

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
