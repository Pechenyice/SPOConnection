package com.example.spoconnection;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;

public class Functions {

    public static String getGroupIdByName(String name) { // "Y2234"
        switch (name) {
            case "Y2231": return "g6"; case "Y2232": return "g7";
            case "Y2233": return "g8"; case "Y2234": return "g9";
            case "Y2235": return "g10"; case "Y2236": return "g66";
            case "Y2237": return "g67"; case "Y2238": return "g68";

            case "Y2331": return "g11"; case "Y2333": return "g13";
            case "Y2334": return "g14"; case "Y2335": return "g15";
            case "Y2336": return "g70"; case "Y2337": return "g71";
            case "Y2338": return "g72"; case "Y2339": return "g2760";

            case "Y2431": return "g16"; case "Y2432": return "g17";
            case "Y2433": return "g18"; case "Y2434": return "g19";
            case "Y2435": return "g20"; case "Y2436": return "g31";
            case "Y2437": return "g74"; case "Y2438": return "g75";
        }
        return "";
    }

    public static HttpURLConnection setupGETAuthRequest(String address, String authCookie, Integer connectTimeout, Integer readTimeout) throws IOException {

        URL url = new URL(address);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        con.setConnectTimeout(connectTimeout * 1000);
        con.setReadTimeout(readTimeout * 1000);

        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36 OPR/66.0.3515.95");
        con.setRequestProperty("Cookie", authCookie);
        con.setUseCaches(false);
        con.setInstanceFollowRedirects(false);

        return con;
    }

    public static String getResponseFromGetRequest(HttpURLConnection con) throws IOException{

        StringBuilder response = new StringBuilder();
        String currentLine;

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream())
        );

        while ((currentLine = in.readLine()) != null) response.append(currentLine);
        in.close();

        return response.toString();
    }

    public static String getStudentIdFromCookie(String cookie) {

        String[] decoded_cookie = URLDecoder.decode(cookie).split("s:");
        String userIdDirty = decoded_cookie[decoded_cookie.length - 5].split(":")[1];
        return userIdDirty.substring(1, userIdDirty.length() - 2);
    }

    public static HttpURLConnection setupPOSTAuthRequest(String address, String urlParameters, String authCookie, Integer connectTimeout, Integer readTimeout) throws IOException {

        URL url = new URL(address);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        byte[] postData = urlParameters.getBytes(Charset.forName("UTF-8"));
        int postDataLength = postData.length;

        urlConnection.setRequestMethod("POST");
        urlConnection.setConnectTimeout(connectTimeout * 1000);
        urlConnection.setReadTimeout(readTimeout * 1000);

        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36 OPR/66.0.3515.95");
        urlConnection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
        urlConnection.setRequestProperty("Cookie", authCookie);
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        urlConnection.setInstanceFollowRedirects(false);

        try (DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream())) {
            wr.write(postData);
        }

        return urlConnection;
    }

}
