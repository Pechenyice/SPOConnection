package com.example.spoconnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;

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

    public static HttpURLConnection setupGetAuthRequest(String address, String authCookie) throws IOException {
        URL url = new URL(address);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();

        con.setRequestMethod("GET");
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

//
//    public static String getResponseFromGetRequest(HttpURLConnection con, Integer responseLength) throws IOException{
//        StringBuilder response = new StringBuilder();
//        String currentLine;
//
//        BufferedReader in = new BufferedReader(
//                new InputStreamReader(con.getInputStream())
//        );
////        int i = 0;
//        while (((currentLine = in.readLine()) != null) && responseLength > 0) {
//            response.append(currentLine);
//            responseLength -= currentLine.length();
////            i += currentLine.length();
//        }
////        System.out.println(i);
//        in.close();
//
//        return response.toString();
//    }
}
