package com.example.spoconnection;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.AsyncTask;
import android.view.Window;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import java.nio.charset.Charset;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public String authCookie;

    public JSONArray studentLessons;
    public JSONObject exercises;
    public JSONObject exercisesVisits;
    public JSONObject teachers;

    public JSONArray todayExercises; // не обязательно today, лол
    public JSONObject todayExercisesVisits; // не обязательно today, лол

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // убрать шторку сверху
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        String[] loginParams = {
                "https://ifspo.ifmo.ru/",
                "iskhakovgermantimurovich",
                "passwordToChange"
        };
        sendLoginRequest(loginParams);

    }

    // бэкенд

    /*
      Отправить фоорму для входа в аккаунт
      String[] params = {url, name, password}

      Нельзя изменять функцию
    */
    private void sendLoginRequest(String[] params) {
        loginRequest request = new loginRequest();
        request.execute(params);
    }

    /*
        Отправить запрос на получение основных данных
        String[] params = {url, cookie}

        Нельзя изменять функцию
    */
    private void sendGetStudentMainDataRequest(String[] params) {
        getStudentMainDataRequest request = new getStudentMainDataRequest();
        request.execute(params);
    }

    /*
        Отпраить завпрос на получение данных о парах за определенный день
        String[] params = {url, data, cookie}

        Нельзя изменять функцию
    */
    private void sendGetExercisesByDayRequest(String[] params) {
        getExercisesByDayRequest request = new getExercisesByDayRequest();
        request.execute(params);
    }

    /*
        Callback, когда запрос на фход в аккаунт завершен
        Если cookie = "", то вход не удался
        Иначе вызываем sendLoginRequest

        Можно изменять
    */
    public void onLoginRequestCompleted(String cookie) {
        if (cookie != "") {
            System.out.println("Login success!");
            authCookie = cookie;
            System.out.println("AuthCookie: " + authCookie);

            String[] requestParams = {
                    "https://ifspo.ifmo.ru/profile/getStudentLessonsVisits",
                    authCookie
            };
            sendGetStudentMainDataRequest(requestParams);
        } else {
            System.out.println("Login failure!");
        }
    }

    /*
        Callback, когда запрос на получение основных данных завершился
        Если  responseBody == "", то запрос не удался (не зависит от пользователя)
        Иначе парсим responseBody и записываем из него значения в переменные
        studentLessons, exercises, exercisesVisits, teachers

        Можно изменять
    */
    public void onGetStudentMainDataRequestCompleted(String responseBody) {
        if (responseBody != "") {
            System.out.println("GetStudentMainData Success!");
            JSONObject jsonData;
            try {
                jsonData = new JSONObject(responseBody);

                studentLessons = jsonData.getJSONArray("userlessons");
                exercises = jsonData.getJSONObject("Exercises");
                exercisesVisits = jsonData.getJSONObject("ExercisesVisits");
                teachers = jsonData.getJSONObject("lessonteachers");

                System.out.println("StudentLessons: " + studentLessons.toString());
                System.out.println("Exercises: " + exercises.toString());
                System.out.println("ExercisesVisits: " + exercisesVisits.toString());
                System.out.println("Teachers: " + teachers.toString());

                String[] params = {
                        "https://ifspo.ifmo.ru//journal/getStudentExercisesByDay",
                        "2020-02-22",
                        authCookie
                };
                sendGetExercisesByDayRequest(params);

            } catch (JSONException e) {

            }
        } else {
            System.out.println("GetStudentMainData Failure!");
        }
    }

    /*
        Callback, когда запрос на получение данных по парам за определенный день завершился
    */
    public void onGetExercisesByDayRequestCompleted (String responseBody) {
        if (responseBody != "") {
            System.out.println("GetExercisesByDay Success!");
            JSONObject jsonData;
            try {
                jsonData = new JSONObject(responseBody);

                todayExercises = jsonData.getJSONArray("todayExercises");
                todayExercisesVisits = jsonData.getJSONObject("todayExercisesVisits");

                System.out.println("TodayExercises: " + todayExercises.toString());
                System.out.println("TodayExercisesVisits: " + todayExercisesVisits.toString());
            } catch (JSONException e) {

            }

            // создание фронтенда по полученным данным

            buildFrontend();



        } else {
            System.out.println("GetExercisesByDay Failure!");
        }
    }

    /*
        Ассинхронный класс, кторый отпраляет login запрос
        В нем менять ничего не нужно
    */
    class loginRequest extends AsyncTask<String[], Void, String> {
        @Override
        protected String doInBackground(String[]... params) {
            URL url;
            HttpURLConnection urlConnection = null;
            String authCookie = "";
            try {
                String url_address = params[0][0];
                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                String urlParameters = "User[login]=" + params[0][1] + "&User[password]=" + params[0][2];
                byte[] postData = urlParameters.getBytes(Charset.forName("UTF-8"));
                int postDataLength = postData.length;

                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36 OPR/66.0.3515.95");
                urlConnection.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
                urlConnection.setDoOutput(true);
                urlConnection.setUseCaches(false);
                urlConnection.setInstanceFollowRedirects(false);

                try (DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream())) {
                    wr.write(postData);
                }

                List<String> cookies = urlConnection.getHeaderFields().get("Set-cookie");
                Integer cookies_count = cookies.size();

                if (cookies_count > 1) {
                    authCookie = cookies.get(cookies_count - 1);
                }
            } catch (Exception e) {
                System.out.println("Problems with login request");
                System.out.println(e.toString());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return authCookie;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            onLoginRequestCompleted(result);
        }
    }

    /*
        Еще один ассинхронный класс
        В нем менять ничего не нужно
    */
    class getStudentMainDataRequest extends AsyncTask<String[], Void, String> {
        protected String doInBackground(String[]... params) {
            URL url;
            HttpURLConnection urlConnection = null;
            String responseBody = "";

            String[] decoded_cookie = URLDecoder.decode(params[0][1]).split("s:");
            String userIdDirty = decoded_cookie[decoded_cookie.length - 5].split(":")[1];
            String studentId = userIdDirty.substring(1, userIdDirty.length() - 2);

            String month = new SimpleDateFormat("MM", Locale.getDefault()).format(new Date());
            String year = new SimpleDateFormat("YYYY", Locale.getDefault()).format(new Date());

            try {
                String url_address = params[0][0] + "?stud=" + studentId + "&dateyear=" + year + "&datemonth=" + month;
                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36 OPR/66.0.3515.95");
                urlConnection.setRequestProperty("Cookie", params[0][1]);
                urlConnection.setUseCaches(false);
                urlConnection.setInstanceFollowRedirects(false);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String currentLine;

                try {
                    while ((currentLine = in.readLine()) != null)
                        response.append(currentLine);

                    in.close();
                } catch (IOException e) {
                    System.out.println(e.toString());
                }
                responseBody = response.toString();
            } catch (Exception e) {
                System.out.println("Problems with getStudentMainData request");
                System.out.println(e.toString());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return responseBody;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            onGetStudentMainDataRequestCompleted(result);
        }
    }

    /*
        Еще один ассинхронный класс
        В нем менять ничего не нужно
    */
    class getExercisesByDayRequest extends AsyncTask <String[], Void, String> {
        protected String doInBackground(String[]... params) {
            URL url;
            HttpURLConnection urlConnection = null;
            String responseBody = "";

            String[] decoded_cookie = URLDecoder.decode(params[0][2]).split("s:");
            String userIdDirty = decoded_cookie[decoded_cookie.length - 5].split(":")[1];
            String studentId = userIdDirty.substring(1, userIdDirty.length() - 2);

            try {
                String url_address = params[0][0] + "?student=" + studentId + "&day=" + params[0][1];
                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36 OPR/66.0.3515.95");
                urlConnection.setRequestProperty("Cookie", params[0][2]);
                urlConnection.setUseCaches(false);
                urlConnection.setInstanceFollowRedirects(false);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String currentLine;

                try {
                    while ((currentLine = in.readLine()) != null)
                        response.append(currentLine);

                    in.close();
                } catch (IOException e) {
                    System.out.println(e.toString());
                }
                responseBody = response.toString();
            } catch (Exception e) {
                System.out.println("Problems with getExercisesByDay request");
                System.out.println(e.toString());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return responseBody;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            onGetExercisesByDayRequestCompleted(result);
        }
    }













    // ну собсна фронтендыч

    // studentLessons - JSONArray предметов с их name, semester, id
    // exercises - JSONObject где id предмета - массив с объектами (парами) с их id, topic, type, time, day, timeday (дата yyyy-mm-dd)
    // exercisesVisits - JSONObject где id предмета - объект, содержащий id пар, которые являются массивом с одним объектом внутри с id (какое-то свое, бесполезное), presence, point, delay, performance, visit_need, mark_need
    // teachers - JSONObject где id предмета - объект с их id, lastname, firstname, middlename
    // todayExercises - JSONArray (пары по дате) объектов с их id (пары), topic, name (предмета), shortname (для приложения), time, lid (id предмета)
    // studentLessons - JSONObject пар за выбранную дату, где каждое поле - массив, содержащий один объект с их id (какое-то свое, бесполезное), presence, point, delay, performance, visit_need, mark_need, review (массив), daypast

    void buildFrontend() {
        String test = null;
        try {
            test = exercisesVisits.getJSONObject("120").getJSONArray("132682").getJSONObject(0).getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println("test: " + test);
    }
}
