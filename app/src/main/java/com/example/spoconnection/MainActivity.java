package com.example.spoconnection;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.AsyncTask;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
import java.util.Map;

/*
    TODO
    1. Дата сегодня - сделано
    2. Построить праильно вызовы - сделано
    3. Handlers - сделано
    4. Обьект, где будут пары по кажому предмету
    5. При копировании кода будут ошибки из-за переменных закоменченых - сделано
    6. Main и ByDay одновременно - сделано
    7. Сейчас при нажатии на какой-то предмет ничего не происходит, как и при нажатии на уведомления - сделано
*/

public class MainActivity extends AppCompatActivity {

    // Переменные, получаемые с запросов

    // by loginRequest
    public String authCookie;
    public String studentId;

    // by getStudentMainDataRequest
    public JSONArray studentLessons;
    public JSONObject teachers;

    // Пары за месяц пока не используем
//    public JSONObject exercises;
//    public JSONObject exercisesVisits;

    // by getExercisesByDay
    public JSONArray exercisesByDay;
    public JSONObject exercisesVisitsByDay;

    // Переменные выше заменяют эти переменные
//    public JSONArray todayExercises;
//    public JSONObject todayExercisesVisits;


    // by getExercisesByLesson
    public JSONArray exercisesByLesson;
    public JSONObject exercisesByLessonVisits;
    public JSONObject exercisesByLessonTeacher;
    public Integer exercisesByLessonAmount;
    public Integer exercisesByLessonVisitsAmount;

    public JSONObject readyExercisesByLesson = new JSONObject();
    public JSONObject readyExercisesByLessonVisits = new JSONObject();

    // by vk api
    public JSONObject vkWallPosts;



    // Handlers для проверки на выполнение запроса

    /*
        Изначально запросы не вызваны - NOT_CALLED
        При вызове метода get...Request - CALLED
        Если внутри запроса ошибка - FAILED (пока не сделал)
        Если возвращает пустой response - EMPTY_RESPONSE        // Эти два значения задаются в функции колбеке on...RequestCompleted()
        Если возвращает тело response - COMPLETED               //
    */
    enum RequestStatus {NOT_CALLED, CALLED, COMPLETED, FAILED, EMPTY_RESPONSE}


    RequestStatus loginRequestStatus = RequestStatus.NOT_CALLED;
    RequestStatus getStudentMainDataRequestStatus = RequestStatus.NOT_CALLED;
    RequestStatus getExercisesByDayRequestStatus = RequestStatus.NOT_CALLED;
    RequestStatus getExercisesByLessonRequestStatus = RequestStatus.NOT_CALLED;
    RequestStatus getVKWallPostsRequestStatus = RequestStatus.NOT_CALLED;
    RequestStatus getProfileParsingRequestStatus = RequestStatus.NOT_CALLED;
    RequestStatus getScheduleParsingRequestStatus = RequestStatus.NOT_CALLED;


    // Переменная, чтобы buildFrontend не вызвался дважды (и после getMainData и после getByDay)
    Boolean buildFrontendCalled = false;


    // контейнеры

    public RelativeLayout main;
    public RelativeLayout profileScreen;
    public RelativeLayout loginForm;
    public LinearLayout navigation;
    public RelativeLayout homeScreen;
    public RelativeLayout scheduleScreen;
    public RelativeLayout lessonsScreen;
    public RelativeLayout lessonsInformationScreen;
    public LinearLayout userHelpScreen;
    public LinearLayout notificationListScreen;


    // массив расписания
    JSONObject scheduleLessons = new JSONObject();

    // номер группы пользователя
    String groupNumber;

    // номер группы для запроса

    private String getGroupURLAddressByName(String name) { // "Y2234"
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // убрать шторку сверху
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // инициализируем экраны

        main = findViewById(R.id.main);
        profileScreen = findViewById(R.id.profileScreen);
        loginForm = findViewById(R.id.loginForm);
        navigation = findViewById(R.id.navigation);
        homeScreen = findViewById(R.id.homeScreen);
        scheduleScreen = findViewById(R.id.scheduleScreen);
        lessonsScreen = findViewById(R.id.lessonsScreen);
        lessonsInformationScreen = findViewById(R.id.lessonsInformationScreen);
        userHelpScreen = findViewById(R.id.userHelp);
        notificationListScreen = findViewById(R.id.notificationListScreen);


        // в начале убираем все экраны

        main.removeView(profileScreen);
        main.removeView(navigation);
        main.removeView(homeScreen);
        main.removeView(scheduleScreen);
        main.removeView(lessonsScreen);
        main.removeView(lessonsInformationScreen);
        main.removeView(userHelpScreen);
        main.removeView(notificationListScreen);

        // получаем данные для отправки запроса

        final TextInputEditText login = findViewById(R.id.loginFormLogin);
        final TextInputEditText password = findViewById(R.id.loginFormPassword);
        final Button submit = findViewById(R.id.loginFormSubmit);


        submit.setOnClickListener(new View.OnClickListener() {

            // отправляем запрос
            @Override
            public void onClick(View v) {
                sendLoginRequest(new String[] {
                        login.getText().toString(),
                        password.getText().toString()
                });
            }
        });

    }

    /* -------------------------------------------- BackEnd -------------------------------------------- */

    // Функции по отправке запроса. Их нужно вызывать при жедании сделать запрос

    private void sendLoginRequest(String[] params) {
        loginRequest request = new loginRequest();
        loginRequestStatus = RequestStatus.CALLED;
        request.execute(params);
    }

    private void sendGetStudentMainDataRequest(String[] params) {
        getStudentMainDataRequest request = new getStudentMainDataRequest();
        getStudentMainDataRequestStatus = RequestStatus.CALLED;
        request.execute(params);
    }

    private void sendGetExercisesByDayRequest(String[] params) {
        getExercisesByDayRequest request = new getExercisesByDayRequest();
        getExercisesByDayRequestStatus = RequestStatus.CALLED;
        request.execute(params);
    }

    private void sendGetExercisesByLessonRequest(String[] params) {
        getExercisesByLessonRequest request = new getExercisesByLessonRequest();
        getExercisesByLessonRequestStatus = RequestStatus.CALLED;
        request.execute(params);
    }

    private void sendGetVKWallPostsRequest(String[] params) {
        getVKWallPostsRequest request = new getVKWallPostsRequest();
        getVKWallPostsRequestStatus = RequestStatus.CALLED;
        request.execute(params);
    }

    private void sendGetProfileParsingRequest() {
        getProfileParsingRequest request = new getProfileParsingRequest();
        getProfileParsingRequestStatus = RequestStatus.CALLED;
        request.execute();
    }

    private void sendGetScheduleParsingRequest(String param) {
        getScheduleParsingRequest request = new getScheduleParsingRequest();
        getScheduleParsingRequestStatus = RequestStatus.CALLED;
        request.execute(new String[] {param});
    }


    // Колбеки, которые вызываются при завершении определенного запроса

    public void onLoginRequestCompleted(String cookie) {
        if (cookie != "") {
            loginRequestStatus = RequestStatus.COMPLETED;

            authCookie = cookie;

            System.out.println("Login success!");
            System.out.println("AuthCookie: " + authCookie);

            // После входа в акк загружаем предметы, учителей и пары за сегодня

            Date date = new Date();
            String year = new SimpleDateFormat("yyyy").format(date);
            String month = new SimpleDateFormat("MM").format(date);
            String day = new SimpleDateFormat("dd").format(date);

            sendGetStudentMainDataRequest(new String[]{ year, month });
            sendGetExercisesByDayRequest(new String[] { year + "-" + month + "-" + day }); // 2020-02-26
            sendGetProfileParsingRequest();

        } else {
            loginRequestStatus = RequestStatus.EMPTY_RESPONSE;
            System.out.println("Login failure!");
        }
    }

    public void onGetStudentMainDataRequestCompleted(String responseBody) {
        if (responseBody != "") {
            getStudentMainDataRequestStatus = RequestStatus.COMPLETED;

            System.out.println("GetStudentMainData Success!");
            JSONObject jsonData;
            try {
                jsonData = new JSONObject(responseBody);

                studentLessons = jsonData.getJSONArray("userlessons");
//                exercises = jsonData.getJSONObject("Exercises");
//                exercisesVisits = jsonData.getJSONObject("ExercisesVisits");
                teachers = jsonData.getJSONObject("lessonteachers");

                System.out.println("StudentLessons: " + studentLessons.toString());
//                System.out.println("Exercises: " + exercises.toString());
//                System.out.println("ExercisesVisits: " + exercisesVisits.toString());
                System.out.println("Teachers: " + teachers.toString());

                if (getExercisesByDayRequestStatus == RequestStatus.COMPLETED && getProfileParsingRequestStatus == RequestStatus.COMPLETED && !buildFrontendCalled) {
                    buildFrontendCalled = true;
                    buildFrontend();
                }

            } catch (JSONException e) {

            }
        } else {
            getStudentMainDataRequestStatus = RequestStatus.EMPTY_RESPONSE;
            System.out.println("GetStudentMainData Failure!");
        }
    }

    public void onGetExercisesByDayRequestCompleted (String responseBody) {
        if (responseBody != "") {
            getExercisesByDayRequestStatus = RequestStatus.COMPLETED;

            System.out.println("GetExercisesByDay Success!");
            JSONObject jsonData;
            try {
                jsonData = new JSONObject(responseBody);

                exercisesByDay = jsonData.getJSONArray("todayExercises");
                exercisesVisitsByDay = jsonData.getJSONObject("todayExercisesVisits");

                System.out.println("TodayExercises: " + exercisesByDay.toString());
                System.out.println("TodayExercisesVisits: " + exercisesVisitsByDay.toString());

                if (getStudentMainDataRequestStatus == RequestStatus.COMPLETED && getProfileParsingRequestStatus == RequestStatus.COMPLETED && !buildFrontendCalled) {
                    buildFrontendCalled = true;
                    buildFrontend();
                }

            } catch (JSONException e) {

            }
        } else {
            getExercisesByDayRequestStatus = RequestStatus.EMPTY_RESPONSE;
            System.out.println("GetExercisesByDay Failure!");
        }
    }

    public void onGetExercisesByLessonRequestCompleted (String[] response) {
        String responseBody = response[0];
        String lessonId = response[1];

        if (responseBody != "" && activeContainer == ContainerName.LESSONS_INFORMATION) {
            getExercisesByLessonRequestStatus = RequestStatus.COMPLETED;

            System.out.println("GetExercisesByLesson Success!");
            JSONObject jsonData;
            try {
                jsonData = new JSONObject(responseBody);

                exercisesByLesson = jsonData.getJSONArray("Exercises");
                exercisesByLessonVisits = jsonData.getJSONObject("todayExercisesVisits");
                exercisesByLessonTeacher = jsonData.getJSONObject("teacher");
                exercisesByLessonAmount = jsonData.getInt("all");
                exercisesByLessonVisitsAmount = jsonData.getInt("was");

                System.out.println("exercisesByLesson: " + exercisesByLesson.toString());
                System.out.println("exercisesByLessonVisits: " + exercisesByLessonVisits.toString());
                System.out.println("exercisesByLessonTeacher: " + exercisesByLessonTeacher.toString());
                System.out.println("exercisesByLessonAmount: " + exercisesByLessonAmount.toString());
                System.out.println("exercisesByLessonVisitsAmount: " + exercisesByLessonVisitsAmount.toString());
            } catch (JSONException e) {

            }

            // берем нужный предмет

            JSONArray buffer = exercisesByLesson;


//             выкидываем информацию о паре

            for (int k = 0; k < buffer.length(); k++) {
                JSONObject value;
                try {

                    value = buffer.getJSONObject(k);
                    TextView temp = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150);
                    lp.setMargins(0,0,0, 50);
                    temp.setLayoutParams(lp);
                    temp.setText(value.getString("topic") + " и эта пара была " + value.getString("day"));
                    temp.setBackgroundColor(167);


                    // получаем подробную информацию о паре

                    JSONObject valueInfo;
                    try {
                        valueInfo = exercisesByLessonVisits.getJSONArray(value.getString("id")).getJSONObject(0);

                        String presence = valueInfo.getString("presence").equals("0") ? " присутствие: нет " : " присутствие: да ";
                        String point = valueInfo.getString("point").toString().equals("null")  ? " оценка: нет " : " оценка: да ";
                        switch (valueInfo.getString("point")) {
                            case "2": {
                                point = " оценка: 2";
                                break;
                            }
                            case "3": {
                                point = " оценка: 3";
                                break;
                            }
                            case "4": {
                                point = " оценка: 4";
                                break;
                            }
                            case "5": {
                                point = " оценка: 5";
                                break;
                            }
                        }
                        String delay = valueInfo.getString("delay").toString().equals("null")  ? " опоздание: нет " : " опоздание: да ";
                        String performance = valueInfo.getString("performance").toString().equals("null") ? " активность: нет " : " активность: да ";

                        temp.setText(temp.getText() + presence + point + delay + performance);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    LinearLayout lessonsInformationList = findViewById(R.id.lessonsInformationList);

                    // опять же id - ключ для следующего массива

                    temp.setId(Integer.parseInt(value.getString("id")));
                    lessonsInformationList.addView(temp);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            try {
                readyExercisesByLesson.put(lessonId, exercisesByLesson);
                readyExercisesByLessonVisits.put(lessonId, exercisesByLessonVisits);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            System.out.println("readyExercisesByLesson: "  + readyExercisesByLesson.toString());
            System.out.println("readyExercisesByLessonVisits: "  + readyExercisesByLessonVisits.toString());

        } else {
            getExercisesByLessonRequestStatus = RequestStatus.EMPTY_RESPONSE;
            System.out.println("GetExercisesByLesson Failure!");
        }
    }

    public void onGetVKWallPostsRequestCompleted (String responseBody) {
        if (responseBody != "" && activeContainer == ContainerName.NOTIFICATION) {
            getVKWallPostsRequestStatus = RequestStatus.COMPLETED;

            System.out.println("GetVKWallPosts Success!");
            JSONObject jsonData;
            try {
                jsonData = new JSONObject(responseBody);

                vkWallPosts = jsonData.getJSONObject("response");

                System.out.println("vkWallPosts: " + vkWallPosts.toString());

            } catch (JSONException e) {

            }

            JSONArray value;
            try {
                value = vkWallPosts.getJSONArray("items");

                for (int i = 0; i < value.length(); i++) {

                    // берем каждый пост

                    JSONObject tmp;
                    try {
                        tmp = value.getJSONObject(i);

                        //и выкидывем его на форму
                        long stamp = System.currentTimeMillis()/1000;
                        System.out.println("current time: " + stamp);

                        //и выкидывем его на форму если он моложе двух дней
                        if (stamp - Long.parseLong(tmp.getString("date")) <= 2*24*3600) {

                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            lp.setMargins(25, 25, 25, 50);
                            TextView note = new TextView(getApplicationContext());
                            note.setLayoutParams(lp);
                            note.setText( (i+1) + " пост (" + new Date(Long.parseLong(tmp.getString("date"))*1000 + 3*3600*1000) + "):    " + tmp.getString("text"));
                            LinearLayout notificationList = findViewById(R.id.notificationList);
                            notificationList.addView(note);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            getVKWallPostsRequestStatus = RequestStatus.EMPTY_RESPONSE;
            System.out.println("GetVKWallPosts Failure!");
        }
    }

    public void onGetProfileParsingRequestCompleted (String[] response){

        // ошибку пока хз как ловить, лень разбираться

//        if (responseBody != "") {
        getProfileParsingRequestStatus = RequestStatus.COMPLETED;


        System.out.println(response[0]); // fio
        System.out.println(response[1]); // group


        if (getExercisesByDayRequestStatus == RequestStatus.COMPLETED && getStudentMainDataRequestStatus == RequestStatus.COMPLETED && !buildFrontendCalled) {
            buildFrontendCalled = true;
            buildFrontend();
        }

//        } else {
//            getExercisesByDayRequestStatus = RequestStatus.EMPTY_RESPONSE;
//            System.out.println("GetExercisesByDay Failure!");
//        }
    }

    public void onGetScheduleParsingRequestCompleted() {

    }


    // Сами асинхронные запросы

    // [name, password]
    class loginRequest extends AsyncTask<String[], Void, String> {

        @Override
        protected String doInBackground(String[]... params) { // params[0][0] - name, params[0][1] - password
            URL url;
            HttpURLConnection urlConnection = null;
            String authCookie = "";

            try {
                String url_address = "https://ifspo.ifmo.ru/";
                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                String urlParameters = "User[login]=" + params[0][0] + "&User[password]=" + params[0][1];
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

                    String[] decoded_cookie = URLDecoder.decode(authCookie).split("s:");
                    String userIdDirty = decoded_cookie[decoded_cookie.length - 5].split(":")[1];
                    studentId = userIdDirty.substring(1, userIdDirty.length() - 2);
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




    // [year, month]
    class getStudentMainDataRequest extends AsyncTask<String[], Void, String> {

        protected String doInBackground(String[]... params) { // params[0][0] - year, params[0][1] - month
            URL url;
            HttpURLConnection urlConnection = null;
            String responseBody = "";

            try {
                String url_address = "https://ifspo.ifmo.ru/profile/getStudentLessonsVisits"
                        + "?stud=" + studentId
                        + "&dateyear=" + params[0][0]
                        + "&datemonth=" + params[0][1];

                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36 OPR/66.0.3515.95");
                urlConnection.setRequestProperty("Cookie", authCookie);
                urlConnection.setUseCaches(false);
                urlConnection.setInstanceFollowRedirects(false);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String currentLine;

                try {
                    while ((currentLine = in.readLine()) != null) response.append(currentLine);
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

    // [lessonId]
    class getExercisesByLessonRequest extends AsyncTask <String[], Void, String[]> {
        protected String[] doInBackground(String[]... params) { // params[0][0] - lesson_id (String)
            URL url;
            HttpURLConnection urlConnection = null;
            String responseBody = "";

            try {
                String url_address = "https://ifspo.ifmo.ru/journal/getStudentExercisesByLesson"
                        + "?lesson=" + params[0][0]
                        + "&student=" + studentId;
                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36 OPR/66.0.3515.95");
                urlConnection.setRequestProperty("Cookie", authCookie);
                urlConnection.setUseCaches(false);
                urlConnection.setInstanceFollowRedirects(false);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String currentLine;

                try {
                    while ((currentLine = in.readLine()) != null) response.append(currentLine);
                    in.close();
                } catch (IOException e) {
                    System.out.println(e.toString());
                }

                responseBody = response.toString();
            } catch (Exception e) {
                System.out.println("Problems with getExercisesByLesson request");
                System.out.println(e.toString());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return new String[] {responseBody, params[0][0]};
        }

        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            onGetExercisesByLessonRequestCompleted(result);
        }
    }


    // [date <yyyy-mm-dd>]
    class getExercisesByDayRequest extends AsyncTask <String[], Void, String> {
        protected String doInBackground(String[]... params) { // params[0][0] - date <yyyy-mm-dd>
            URL url;
            HttpURLConnection urlConnection = null;
            String responseBody = "";

            try {
                String url_address = "https://ifspo.ifmo.ru//journal/getStudentExercisesByDay"
                        + "?student=" + studentId
                        + "&day=" + params[0][0];

                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36 OPR/66.0.3515.95");
                urlConnection.setRequestProperty("Cookie", authCookie);
                urlConnection.setUseCaches(false);
                urlConnection.setInstanceFollowRedirects(false);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String currentLine;

                try {
                    while ((currentLine = in.readLine()) != null) response.append(currentLine);
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

    // [postsCount]
    class getVKWallPostsRequest extends AsyncTask <String[], Void, String> { // params[0][0] - posts count
        protected String doInBackground(String[]... params) {
            URL url;
            HttpURLConnection urlConnection = null;
            String responseBody = "";

            try {
                String url_address = "https://api.vk.com/method/wall.get?domain=raspfspo"
                        + "&count=" + params[0][0]
                        + "&filter=owner&access_token=c2cb19e3c2cb19e3c2cb19e339c2a4f3d6cc2cbc2cb19e39c9fe125dc37c9d4bb7994cd&v=5.103";

                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setUseCaches(false);
                urlConnection.setInstanceFollowRedirects(false);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String currentLine;

                try {
                    while ((currentLine = in.readLine()) != null) response.append(currentLine);
                    in.close();
                } catch (IOException e) {
                    System.out.println(e.toString());
                }

                responseBody = response.toString();
            } catch (Exception e) {
                System.out.println("Problems with vk request");
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
            onGetVKWallPostsRequestCompleted(result);
        }
    }



    class getProfileParsingRequest extends AsyncTask<Void, Void, String[]> { //FIO, GROUP

        protected String[] doInBackground(Void... params) {
            URL url;
            HttpURLConnection urlConnection = null;
            String responseBody = "";
            Document html = new Document(responseBody);

            try {
                String url_address = "https://ifspo.ifmo.ru/profile";

                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36 OPR/66.0.3515.95");
                urlConnection.setRequestProperty("Cookie", authCookie);
                urlConnection.setUseCaches(false);
                urlConnection.setInstanceFollowRedirects(false);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String currentLine;

                try {
                    while ((currentLine = in.readLine()) != null) response.append(currentLine);
                    in.close();
                } catch (IOException e) {
                    System.out.println(e.toString());
                }

                responseBody = response.toString();
                html = Jsoup.parse(responseBody);
            } catch (Exception e) {
                System.out.println("Problems with profileParsing request");
                System.out.println(e.toString());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            System.out.println("GetProfileParsing Success!");

            String FIO = html.body().getElementsByClass("row").get(0).getElementsByClass("span9").select("h3").get(0).text();
            String GROUP = html.body().getElementsByClass("row").get(0).getElementsByClass("span9").get(0).getElementsByClass("row").get(0).getElementsByClass("span3").select("ul").select("li").last().text();
            String[] groupContent = GROUP.split(" ");
            groupNumber = getGroupURLAddressByName(groupContent[0]);

//            System.out.println(groupNumber);
//            JSONObject week = {"now": [{{"1", "10-11 30", "matan"}, {"2", "11 30 - 13 00", "matan"}}, { {} {} }]};

            return new String[] {FIO, groupContent[0]};
        }

        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            onGetProfileParsingRequestCompleted(result);


            /////////
            sendGetScheduleParsingRequest("now");
            sendGetScheduleParsingRequest("next");
        }
    }




    class getScheduleParsingRequest extends AsyncTask<String[], Void, String> { // Void на самом деле

        protected String doInBackground(String[]... params) { // now next
            URL url;
            HttpURLConnection urlConnection = null;
            String responseBody = "";
            Document html = new Document(responseBody);

            try {
                String url_address = "https://ifspo.ifmo.ru/schedule/get?num=" + groupNumber + "&week=" + params[0][0];

                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36 OPR/66.0.3515.95");
                urlConnection.setRequestProperty("Cookie", authCookie);
                urlConnection.setUseCaches(false);
                urlConnection.setInstanceFollowRedirects(false);

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream())
                );

                StringBuilder response = new StringBuilder();
                String currentLine;

                try {
                    while ((currentLine = in.readLine()) != null) response.append(currentLine);
                    in.close();
                } catch (IOException e) {
                    System.out.println(e.toString());
                }

                responseBody = response.toString();
                html = Jsoup.parse(responseBody);
            } catch (Exception e) {
                System.out.println("Problems with sheduleParsing request");
                System.out.println(e.toString());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            System.out.println("GetScheduleParsing Success!");

            JSONArray scheduleRoot = new JSONArray();

            try {
                scheduleLessons.put(params[0][0], new JSONArray());
//                System.out.println(scheduleLessons.toString());
                scheduleRoot = scheduleLessons.getJSONArray(params[0][0]);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < 6; i++) {

                scheduleRoot.put(new JSONArray());

                JSONArray scheduleRootLesson = new JSONArray();
                try {
                    scheduleRootLesson = scheduleRoot.getJSONArray(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Element root = html.body().getElementsByClass("schedule-row-table").get(0).getElementsByClass("weekday-div").get(i);
                Elements lessonRoot = root.getElementsByClass("period-tr");
                for (Element elem : lessonRoot) {

                    scheduleRootLesson.put(new JSONObject());

                    String numberOfLesson = elem.select("div").get(0).text();
                    numberOfLesson = numberOfLesson.split(" ")[0];

                    String timeOfLessonStart = "";
                    String timeOfLessonEnd = "";

                    switch (numberOfLesson) {
                        case "I": {
                            timeOfLessonStart = "8:20";
                            timeOfLessonEnd = "9:50";
                            break;
                        }
                        case "II": {
                            timeOfLessonStart = "10:00";
                            timeOfLessonEnd = "11:30";
                            break;
                        }
                        case "III": {
                            timeOfLessonStart = "11:40";
                            timeOfLessonEnd = "13:10";
                            break;
                        }
                        case "IV": {
                            timeOfLessonStart = "13:30";
                            timeOfLessonEnd = "15:00";
                            break;
                        }
                        case "V": {
                            timeOfLessonStart = "15:20";
                            timeOfLessonEnd = "16:50";
                            break;
                        }
                        case "VI": {
                            timeOfLessonStart = "17:00";
                            timeOfLessonEnd = "18:30";
                            break;
                        }
                    }
                    String nameOfLesson = elem.getElementsByClass("lesson_td").get(0).select("div").get(0).text();
                    String teacherOfLesson = elem.getElementsByClass("lesson_td").get(0).select("div").get(1).text();

                    JSONObject scheduleRootLessonInformation = new JSONObject();
                    try {
                        scheduleRootLessonInformation = scheduleRootLesson.getJSONObject(scheduleRootLesson.length() - 1);
                        scheduleRootLessonInformation.put("position", numberOfLesson);
                        scheduleRootLessonInformation.put("start", timeOfLessonStart);
                        scheduleRootLessonInformation.put("end", timeOfLessonEnd);
                        scheduleRootLessonInformation.put("name", nameOfLesson);
                        scheduleRootLessonInformation.put("teacher", teacherOfLesson);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

            }

//            JSONObject schedule = {

//              "now": [ // week

//                  [ // day
//                      {position: "I", start: "10", end: "11", name: "name", teacher: "teacher"}, // lesson
//                      {position: "II", start: "11", end: "12", name: "name", teacher: "teacher"}
//                  ],
//                  [ // day
//                      {position: "I", start: "10", end: "11", name: "name", teacher: "teacher"}, // lesson
//                      {position: "II", start: "11", end: "12", name: "name", teacher: "teacher"}
//                  ]
//
//              ]

//            };

            System.out.println(scheduleLessons.toString());

            return " ";
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            onGetScheduleParsingRequestCompleted();
        }
    }








    /* --------------------------------- FrontEnd ------------------------------------------------------- */





    // studentLessons - JSONArray предметов с их name, semester, id
    // exercises - JSONObject где id предмета - массив с объектами (парами) с их id, topic, type, time, day, timeday (дата yyyy-mm-dd)
    // exercisesVisits - JSONObject где id предмета - объект, содержащий id пар, которые являются массивом с одним объектом внутри с id (какое-то свое, бесполезное), presence, point, delay, performance, visit_need, mark_need
    // teachers - JSONObject где id предмета - объект с их id, lastname, firstname, middlename
    // todayExercises - JSONArray (пары по дате) объектов с их id (пары), topic, name (предмета), shortname (для приложения), time, lid (id предмета)
    // studentLessons - JSONObject пар за выбранную дату, где каждое поле - массив, содержащий один объект с их id (какое-то свое, бесполезное), presence, point, delay, performance, visit_need, mark_need, review (массив), daypast


    // кнопки нужны глобально

    Button home;
    Button schedule;
    Button profile;
    Button lessons;
    Button exit;

    Button userHelp;

    // переменная для мониторинга активного контейнера

    enum ContainerName { PROFILE, HOME, SCHEDULE, LESSONS, LESSONS_INFORMATION, NOTIFICATION }
    ContainerName activeContainer;

    /*
    Какие запросы, для каких сцен:
        PROFILE
            GetExercisesByDaY (сегодня) вернет пары, которые были сегодня, на данный момент
            переменные: todayExercisesVisits, todayExercises нужно заменить на exercisesByDay, exercisesVisitsByDay
        LESSONS
            GetStudentMainData (этот или прошлый год) главное, что вернет все предметы, которые и будут выводится в списке
        LESSONS_INFORMATION
            GetExercisesByLesson (айди предмета) вернет все пары по этому предмету
        В общем, все нормально, кроме вывода информации по парам для каждого предмета
        Потому что использу.тся переменные exercises, exercisesVisits (с getStudentMainDataRequest) кторые показывают пары только за текущий месяц.
    */


    void buildFrontend() {

        //заранее высираем контент в lessonsScreen
        main.addView(lessonsScreen);

        LinearLayout lessonsList = findViewById(R.id.lessonsList);

        for(int i = 0; i < studentLessons.length(); i++){
            JSONObject value;
            try {
                value = studentLessons.getJSONObject(i);
                TextView temp = new TextView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150);
                lp.setMargins(0,0,0, 50);
                temp.setLayoutParams(lp);
                temp.setText(value.getString("name") + " семестр " + value.getString("semester"));
                temp.setBackgroundColor(127);

                // самая важная вещь - id temp'а это id для JSONObject

                temp.setId(Integer.parseInt(value.getString("id")));


                // вешаем универсальный обработчик кликов для каждого предмета

                lessonInformationClickListener needMoreInfo = new lessonInformationClickListener();
                temp.setOnClickListener(needMoreInfo);
                lessonsList.addView(temp);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        //скрываем lessonsScreen
        main.removeView(lessonsScreen);

        // убираем регистрацию и подрубаем стартовый экран

        main.removeView(loginForm);
        main.addView(profileScreen);
        main.addView(navigation);
        main.addView(userHelpScreen);

        // делаем активным контейнер profile

        activeContainer = ContainerName.PROFILE;

        // создаем слушатели для кнопок

        home = findViewById(R.id.home);
        schedule = findViewById(R.id.schedule);
        profile = findViewById(R.id.profile);
        lessons = findViewById(R.id.lessons);
        exit = findViewById(R.id.exit);
        userHelp = findViewById(R.id.notification);


        // наш обработчик кликов

        navigationButtonClickListener wasClicked = new navigationButtonClickListener();

        userHelp.setOnClickListener(wasClicked);

        home.setOnClickListener(wasClicked);
        schedule.setOnClickListener(wasClicked);
        profile.setOnClickListener(wasClicked);
        lessons.setOnClickListener(wasClicked);
        exit.setOnClickListener(wasClicked);


        final LinearLayout todayLessonsView = (LinearLayout) findViewById(R.id.todayLessonsView);


        // высираем сегодняшние пары перебором

        for(int i = 0; i < exercisesByDay.length(); i++){
            JSONObject value;
            try {
                value = exercisesByDay.getJSONObject(i);

                TextView temp = new TextView(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 50);
                temp.setLayoutParams(lp);
                temp.setText(value.getString("name") + " " + value.getString("time") + " пара ");

                // узнаем подробную информацию о паре

                JSONObject valueInfo;
                try {
                    valueInfo = exercisesVisitsByDay.getJSONArray(value.getString("id")).getJSONObject(0);

                    String presence = valueInfo.getString("presence").equals("0") ? " присутствие: нет " : " присутствие: да ";
                    String point = valueInfo.getString("point").toString().equals("null")  ? " оценка: нет " : " оценка: да ";
                    switch (valueInfo.getString("point")) {
                        case "2": {
                            point = " оценка: 2";
                            break;
                        }
                        case "3": {
                            point = " оценка: 3";
                            break;
                        }
                        case "4": {
                            point = " оценка: 4";
                            break;
                        }
                        case "5": {
                            point = " оценка: 5";
                            break;
                        }
                    }
                    String delay = valueInfo.getString("delay").toString().equals("null")  ? " опоздание: нет " : " опоздание: да ";
                    String performance = valueInfo.getString("performance").equals("null") ? " активность: нет " : " активность: да ";

                    temp.setText(temp.getText() + presence + point + delay + performance);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                todayLessonsView.addView(temp);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    // обработчик нажатий на кнопки навигации

    class navigationButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v)
        {

            // убираем старый контейнер

            switch (activeContainer) {
                case PROFILE: {
                    if (v.getId() == profile.getId()) {
                        return;
                    }
                    main.removeView(profileScreen);
                    break;
                }
                case HOME: {
                    if (v.getId() == home.getId()) {
                        return;
                    }
                    main.removeView(homeScreen);
                    break;
                }
                case SCHEDULE: {
                    if (v.getId() == schedule.getId()) {
                        return;
                    }
                    main.removeView(scheduleScreen);
                    break;
                }
                case LESSONS: {
                    if (v.getId() == lessons.getId()) {
                        return;
                    }
                    main.removeView(lessonsScreen);
                    break;
                }
                case LESSONS_INFORMATION: {
                    main.removeView(lessonsInformationScreen);
                    break;
                }

                case NOTIFICATION: {
                    if (v.getId() == userHelp.getId()) {
                        return;
                    }
                    main.removeView(notificationListScreen);
                    break;
                }
            }

            // и добавляем новый

            if (v.getId() == home.getId()) {
                System.out.println("You clicked home");
                activeContainer = ContainerName.HOME;
                main.addView(homeScreen);
            }

            if (v.getId() == schedule.getId()) {
                System.out.println("You clicked schedule");
                activeContainer = ContainerName.SCHEDULE;
                main.addView(scheduleScreen);
            }

            if (v.getId() == profile.getId()) {
                System.out.println("You clicked profile");
                activeContainer = ContainerName.PROFILE;
                main.addView(profileScreen);
            }

            if (v.getId() == lessons.getId()) {
                System.out.println("You clicked lessons");
                activeContainer = ContainerName.LESSONS;
                main.addView(lessonsScreen);
            }

            if (v.getId() == userHelp.getId()) {
                System.out.println("You clicked notifications");
                activeContainer = ContainerName.NOTIFICATION;
                main.addView(notificationListScreen);
            }

            if (v.getId() == exit.getId()) {
                System.out.println("You clicked exit");
            }

            // но если кликнута кнопка изменений в расписании, нужно еще выкинуть контент от вк



            if (activeContainer == ContainerName.NOTIFICATION) {

                sendGetVKWallPostsRequest(new String[] {"10"});

            }



        }

    }

    // обработчик нажатий на предметы в lessons

    class lessonInformationClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v)
        {

            // обновляем активный экран

            main.removeView(lessonsScreen);
            activeContainer = ContainerName.LESSONS_INFORMATION;
            main.addView(lessonsInformationScreen);

            LinearLayout lessonsInformationList = findViewById(R.id.lessonsInformationList);


            // очищаем scrollview

            lessonsInformationList.removeAllViews();

            JSONArray buffer = null;
            try {
                buffer = readyExercisesByLesson.getJSONArray(v.getId()+"");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            System.out.println(buffer);
            if (buffer == null) {
                sendGetExercisesByLessonRequest(new String[] {v.getId()+""});
            } else {


                for (int k = 0; k < buffer.length(); k++) {
                    JSONObject value;
                    try {

                        value = buffer.getJSONObject(k);
                        TextView temp = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150);
                        lp.setMargins(0,0,0, 50);
                        temp.setLayoutParams(lp);
                        temp.setText(value.getString("topic") + " и эта пара была " + value.getString("day"));
                        temp.setBackgroundColor(167);


                        // получаем подробную информацию о паре

                        JSONObject valueInfo;
                        try {
                            valueInfo = readyExercisesByLessonVisits.getJSONArray(value.getString("id")).getJSONObject(0);

                            String presence = valueInfo.getString("presence").equals("0") ? " присутствие: нет " : " присутствие: да ";
                            String point = valueInfo.getString("point").toString().equals("null")  ? " оценка: нет " : " оценка: да ";
                            switch (valueInfo.getString("point")) {
                                case "2": {
                                    point = " оценка: 2";
                                    break;
                                }
                                case "3": {
                                    point = " оценка: 3";
                                    break;
                                }
                                case "4": {
                                    point = " оценка: 4";
                                    break;
                                }
                                case "5": {
                                    point = " оценка: 5";
                                    break;
                                }
                            }
                            String delay = valueInfo.getString("delay").toString().equals("null")  ? " опоздание: нет " : " опоздание: да ";
                            String performance = valueInfo.getString("performance").toString().equals("null") ? " активность: нет " : " активность: да ";

                            temp.setText(temp.getText() + presence + point + delay + performance);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }



                        // опять же id - ключ для следующего массива

                        temp.setId(Integer.parseInt(value.getString("id")));
                        lessonsInformationList.addView(temp);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }

        }

    }



}