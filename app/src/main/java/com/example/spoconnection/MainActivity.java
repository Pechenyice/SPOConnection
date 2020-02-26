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

                if (getExercisesByDayRequestStatus == RequestStatus.COMPLETED && !buildFrontendCalled) {
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

                if (getStudentMainDataRequestStatus == RequestStatus.COMPLETED && !buildFrontendCalled) {
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

        if (responseBody != "") {
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
        if (responseBody != "") {
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