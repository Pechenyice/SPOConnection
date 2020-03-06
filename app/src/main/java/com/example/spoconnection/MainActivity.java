package com.example.spoconnection;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import android.content.Context;
import android.content.SharedPreferences;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import android.os.Bundle;
import android.os.AsyncTask;

import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import org.w3c.dom.Text;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;
import java.util.Locale;

//import static com.example.spoconnection.Functions.setupPOSTAuthRequest;
//import com.example.spoconnection.Functions;


public class MainActivity extends AppCompatActivity {

    final long COOKIE_LIFETIME = 90; // в минутах. На самом деле 120 минут.


    final Integer STATS_REQUEST_CONNECT_TIMEOUT                 = 5;  // в секундах
    final Integer LOGIN_REQUEST_CONNECT_TIMEOUT                 = 5;
    final Integer MAIN_DATA_REQUEST_CONNECT_TIMEOUT             = 5;
    final Integer STUDENT_PROFILE_REQUEST_CONNECT_TIMEOUT       = 5;
    final Integer SCHEDULE_REQUEST_CONNECT_TIMEOUT              = 5;
    final Integer EXERCISES_BY_DAY_REQUEST_CONNECT_TIMEOUT      = 5;
    final Integer EXERCISES_BY_LESSON_REQUEST_CONNECT_TIMEOUT   = 5;
    final Integer VK_POSTS_REQUEST_CONNECT_TIMEOUT              = 5;

    final Integer STATS_REQUEST_READ_TIMEOUT                 = 5;  // в секундах
    final Integer LOGIN_REQUEST_READ_TIMEOUT                 = 5;
    final Integer MAIN_DATA_REQUEST_READ_TIMEOUT             = 5;
    final Integer STUDENT_PROFILE_REQUEST_READ_TIMEOUT       = 5;
    final Integer SCHEDULE_REQUEST_READ_TIMEOUT              = 5;
    final Integer EXERCISES_BY_DAY_REQUEST_READ_TIMEOUT      = 5;
    final Integer EXERCISES_BY_LESSON_REQUEST_READ_TIMEOUT   = 5;
    final Integer VK_POSTS_REQUEST_READ_TIMEOUT              = 5;

    // Переменные, получаемые с запросов

    // by loginRequest
    public String authCookie;
    public String studentId;

    // by getStudentMainDataRequest
    public JSONArray studentLessons;
    public JSONObject teachers;

    // by getExercisesByDay
    public JSONArray exercisesByDay;
    public JSONObject exercisesVisitsByDay;

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
    enum RequestStatus {NOT_CALLED, CALLED, COMPLETED, FAILED, TIMEOUT, EMPTY_RESPONSE}

    RequestStatus loginRequestStatus;
    RequestStatus getStudentMainDataRequestStatus;
    RequestStatus getExercisesByDayRequestStatus;
    RequestStatus getExercisesByLessonRequestStatus;
    RequestStatus getVKWallPostsRequestStatus;
    RequestStatus getStudentProfileDataRequestStatus;
    RequestStatus getScheduleRequestStatus;
    RequestStatus getStudentStatsRequestStatus;


    // Переменная, чтобы buildFrontend не вызвался дважды (и после getMainData и после getByDay)
//    Boolean buildFrontendCalled = false;

    Boolean nowWeekScheduleCalled = false;
    Boolean nextWeekScheduleCalled = false;

    Boolean appFirstRun = false;


    // контейнеры

    public RelativeLayout main;
//    public RelativeLayout profileScreen;
//    public LinearLayout profileScreen;
    public ScrollView profileScreen;
    public RelativeLayout loginForm;
    public LinearLayout navigation;
    public RelativeLayout homeScreen;
    public RelativeLayout scheduleScreen;
    public RelativeLayout lessonsScreen;
    public RelativeLayout lessonsInformationScreen;
    public LinearLayout userHelpScreen;
    public LinearLayout notificationListScreen;
    public RelativeLayout loadingScreen;


    // массив расписания
    JSONObject scheduleLessons = new JSONObject();

    String studentGroup; // Y2234
    String studentFIO;
    String studentAvatarSrc;

    String statsMidMark; // 4.74
    String statsDebtsCount; // 0
    String statsPercentageOfVisits; // 91%

    SharedPreferences preferences;
    SharedPreferences.Editor preferencesEditor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println('\n');

        // убрать шторку сверху
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

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
        loadingScreen = findViewById(R.id.loadingScreen);

        // локальные кнопки экранов

//        scheduleChanges = findViewById(R.id.notificationSchedule);
        scheduleNow = findViewById(R.id.now);
        scheduleNext = findViewById(R.id.next);

        // инициаизируем переменные для очистки их при выходе (важно)
        profileUserName = findViewById(R.id.profileUserName);
        profileUserGroup = findViewById(R.id.profileUserGroup);
        profileUserCalendar = findViewById(R.id.profileUserCalendar);
        profileUserBalls = findViewById(R.id.profileUserBalls);
        profileUserBills = findViewById(R.id.profileUserBills);

        // локальные кнопки экранов
//        scheduleChanges = findViewById(R.id.notificationSchedule);

        // запросы для расписания отправляются только 1 раз

        scheduleNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!nowWeekScheduleCalled) {
                    sendGetScheduleParsingRequest("now");
                    setLoadingToList(ContainerName.SCHEDULE);
                    nowWeekScheduleCalled = true;
                } else {
                    onGetScheduleRequestCompleted("now");
                }
            }
        });

        scheduleNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!nextWeekScheduleCalled) {
                    sendGetScheduleParsingRequest("next");
                    setLoadingToList(ContainerName.SCHEDULE);
                    nextWeekScheduleCalled = true;
                } else {
                    onGetScheduleRequestCompleted("next");
                }
            }
        });


        // в начале убираем все экраны

        main.removeView(profileScreen);
        main.removeView(navigation);
        main.removeView(homeScreen);
        main.removeView(scheduleScreen);
        main.removeView(lessonsScreen);
        main.removeView(lessonsInformationScreen);
        main.removeView(userHelpScreen);
        main.removeView(notificationListScreen);
        main.removeView(loginForm);
        activeContainer = ContainerName.LOADING;

        resetRequestsStatuses();

        preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);

        appFirstRun = preferences.getBoolean("appFirstRun", true);
//        System.out.println(preferences.getAll());

        // первый запуск
        if (appFirstRun) {

            setContainer(ContainerName.LOGIN);

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

            System.out.println("App first run");
//            preferencesEditor = preferences.edit();

//            preferencesEditor.putBoolean("appFirstRun", false);
//            preferencesEditor.apply();
        } else {
            System.out.println("Not app first run");
            String lastLoginRequestTime = preferences.getString("lastLoginRequest", "");

            // есть дата последнего входа в аккаунт
            if (!lastLoginRequestTime.isEmpty()) {
                Date loginRequestDate = null;
                Date currentDate;

                try { loginRequestDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse(lastLoginRequestTime); }
                catch (ParseException e) {}
                currentDate = new Date();

                long minutesBetweenDates = ((currentDate.getTime() / 60000) - (loginRequestDate.getTime() / 60000));
                System.out.println("Last login request was " + minutesBetweenDates + " minutes ago");

                // вход был выполнен более COOKIE_LIFETIME минут назад, тогда нужно сделать запрос заново
                if ( minutesBetweenDates >= COOKIE_LIFETIME) {

                    setContainer(ContainerName.LOGIN);

                    System.out.println("Cookie lifetime is more then " + COOKIE_LIFETIME + " minutes. Sending new login request");

                    String name = preferences.getString("studentName", "");
                    String password = preferences.getString("studentPassword", "");

                    studentGroup = preferences.getString("studentGroup", "");
                    studentFIO = preferences.getString("studentFIO", "");
                    studentAvatarSrc = preferences.getString("studentAvatarSrc", "");
                    getStudentProfileDataRequestStatus = RequestStatus.COMPLETED;

                    sendLoginRequest(new String[] { name, password });
                    // иначе пропускаем вход в аккаунт
                } else {
                    System.out.println("Cookie lifetime is less then " + COOKIE_LIFETIME + " minutes. Continue");
                    authCookie = preferences.getString("authCookie", "");
                    studentId = Functions.getStudentIdFromCookie(authCookie);

                    studentGroup = preferences.getString("studentGroup", "");
                    studentFIO = preferences.getString("studentFIO", "");
                    studentAvatarSrc = preferences.getString("studentAvatarSrc", "");

                    statsMidMark = preferences.getString("studentStatsMidMark", "");
                    statsDebtsCount = preferences.getString("studentStatsDebtsCount", "");
                    statsPercentageOfVisits = preferences.getString("studentStatsPercentageOfVisits", "");
                    getStudentProfileDataRequestStatus = RequestStatus.COMPLETED;

                    Date date = new Date();
                    String year = new SimpleDateFormat("yyyy").format(date);
                    String month = new SimpleDateFormat("MM").format(date);
                    String day = new SimpleDateFormat("dd").format(date);

                    sendGetStudentStatsRequest();
                    sendGetStudentMainDataRequest(new String[]{ year, month });
                    sendGetExercisesByDayRequest(new String[] { year + "-" + month + "-" + day }); // 2020-02-26
                }

            } else {
                setContainer(ContainerName.LOGIN);

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

                System.out.println("App first login");
            }
        }


    }

    /* -------------------------------------------- BackEnd -------------------------------------------- */

    public void resetRequestsStatuses() {
        loginRequestStatus                    = RequestStatus.NOT_CALLED;
        getStudentMainDataRequestStatus       = RequestStatus.NOT_CALLED;
        getExercisesByDayRequestStatus        = RequestStatus.NOT_CALLED;
        getExercisesByLessonRequestStatus     = RequestStatus.NOT_CALLED;
        getVKWallPostsRequestStatus           = RequestStatus.NOT_CALLED;
        getStudentProfileDataRequestStatus    = RequestStatus.NOT_CALLED;
        getScheduleRequestStatus              = RequestStatus.NOT_CALLED;
        getStudentStatsRequestStatus          = RequestStatus.NOT_CALLED;
    }


    public void resetApp() {

        preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        preferencesEditor = preferences.edit();
        preferencesEditor.clear();
        preferencesEditor.apply();

        nowWeekScheduleCalled = false;
        nextWeekScheduleCalled = false;
        readyExercisesByLesson = new JSONObject();

        resetRequestsStatuses();

    }

    public void setLoginFormContainer(String nameText, String passwordText) {
        setContainer(ContainerName.LOGIN);
        Button submit = findViewById(R.id.loginFormSubmit);

        final TextInputEditText login = findViewById(R.id.loginFormLogin);
        final TextInputEditText password = findViewById(R.id.loginFormPassword);

        login.setText(nameText, TextView.BufferType.EDITABLE);
        password.setText(passwordText, TextView.BufferType.EDITABLE);

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

    public void setLoginFormContainer() {
        setContainer(ContainerName.LOGIN);
        Button submit = findViewById(R.id.loginFormSubmit);

        final TextInputEditText login = findViewById(R.id.loginFormLogin);
        final TextInputEditText password = findViewById(R.id.loginFormPassword);

        login.setText("");
        password.setText("");

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

    // Когда отпраили все запросы для входа в акаунт
    public void onAuthCompleted() {

        System.out.println("---------");
        System.out.println("Stats request: " + getStudentStatsRequestStatus);
        System.out.println("Main data request: " + getStudentMainDataRequestStatus);
        System.out.println("Student profile data request: " + getStudentProfileDataRequestStatus);
        System.out.println("Exercises by day request: " + getExercisesByDayRequestStatus);
        System.out.println("---------");

        if (getStudentStatsRequestStatus == RequestStatus.COMPLETED
                && getStudentMainDataRequestStatus == RequestStatus.COMPLETED
                && getExercisesByDayRequestStatus  == RequestStatus.COMPLETED
        ) {
            preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
            preferencesEditor = preferences.edit();
            preferencesEditor.putBoolean("appFirstRun", false);
            preferencesEditor.apply();

            buildFrontend();
        } else {
            preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
            String name = preferences.getString("studentName", "");
            String password = preferences.getString("studentPassword", "");

            setLoginFormContainer(name, password);
            resetApp();
        }
    }

    // Функции по отправке запроса. Их нужно вызывать при жедании сделать запрос

    private void sendLoginRequest(String[] params) {
        setContainer(ContainerName.LOADING);
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

    private void sendGetStudentProfileDataRequest() {
        getStudentProfileDataRequest request = new getStudentProfileDataRequest();
        getStudentProfileDataRequestStatus = RequestStatus.CALLED;
        request.execute();
    }

    private void sendGetScheduleParsingRequest(String param) {
        getScheduleRequest request = new getScheduleRequest();
        getScheduleRequestStatus = RequestStatus.CALLED;
        request.execute(new String[] {param});
    }

    private void sendGetStudentStatsRequest() {
        getStudentStatsRequest request = new getStudentStatsRequest();
        getStudentStatsRequestStatus = RequestStatus.CALLED;
        request.execute();
    }


    // Колбеки, которые вызываются при завершении определенного запроса

    public void onLoginRequestCompleted(String[] response) {
        String cookie = response[0];
        String studentName = response[1];
        String studentPassword = response[2];

        if (loginRequestStatus == RequestStatus.TIMEOUT) {
            setLoginFormContainer(studentName, studentPassword);
            resetApp();
        }
        else if (loginRequestStatus == RequestStatus.FAILED) {
            setLoginFormContainer(studentName, studentPassword);
            resetApp();
        }
        else if (!cookie.isEmpty()) {
            loginRequestStatus = RequestStatus.COMPLETED;

            authCookie = cookie;
            studentId = Functions.getStudentIdFromCookie(authCookie);

            System.out.println("Login success!");
            System.out.println("AuthCookie: " + authCookie);

            preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
            preferencesEditor = preferences.edit();

            String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
            preferencesEditor.putString("lastLoginRequest", currentDate);
            preferencesEditor.putString("authCookie", authCookie);
            preferencesEditor.putString("studentName", studentName);
            preferencesEditor.putString("studentPassword", studentPassword);
            preferencesEditor.apply();

            // После входа в акк загружаем предметы, учителей и пары за сегодня

            Date date = new Date();
            String year = new SimpleDateFormat("yyyy").format(date);
            String month = new SimpleDateFormat("MM").format(date);
            String day = new SimpleDateFormat("dd").format(date);

            sendGetStudentStatsRequest();
            sendGetStudentMainDataRequest(new String[]{ year, month });
            if (getStudentProfileDataRequestStatus != RequestStatus.COMPLETED) sendGetStudentProfileDataRequest();
            sendGetExercisesByDayRequest(new String[] { year + "-" + month + "-" + day }); // 2020-02-26

        } else {
            loginRequestStatus = RequestStatus.EMPTY_RESPONSE;
            System.out.println("Login request empty response!");

            setLoginFormContainer(studentName, studentPassword);
            resetApp();
        }
    }

    public void onGetStudentMainDataRequestCompleted(String responseBody) {

        if (getStudentMainDataRequestStatus == RequestStatus.TIMEOUT) {

        }
        else if (getStudentMainDataRequestStatus == RequestStatus.FAILED) {

        }
        else if (!responseBody.isEmpty()) {
            getStudentMainDataRequestStatus = RequestStatus.COMPLETED;

            System.out.println("GetStudentMainData Success!");
            JSONObject jsonData;
            try {
                jsonData = new JSONObject(responseBody);

                studentLessons = jsonData.getJSONArray("userlessons");
                teachers = jsonData.getJSONObject("lessonteachers");

                System.out.println("StudentLessons: " + studentLessons.toString());
                System.out.println("Teachers: " + teachers.toString());

//                if (getExercisesByDayRequestStatus == RequestStatus.COMPLETED
//                        && getStudentProfileDataRequestStatus == RequestStatus.COMPLETED
//                        && !buildFrontendCalled)
//                {
//                    buildFrontendCalled = true;
//                    buildFrontend();
//                }

            } catch (JSONException e) {

            }
        } else {
            getStudentMainDataRequestStatus = RequestStatus.EMPTY_RESPONSE;
            System.out.println("Student main data request empty response!");
        }
    }

    public void onGetExercisesByDayRequestCompleted (String responseBody) {

//        System.out.println(getExercisesByDayRequestStatus);

        if (getExercisesByDayRequestStatus == RequestStatus.TIMEOUT) {

        }
        else if (getExercisesByDayRequestStatus == RequestStatus.FAILED) {

        }
        else if (!responseBody.isEmpty()) {
            getExercisesByDayRequestStatus = RequestStatus.COMPLETED;

            System.out.println("GetExercisesByDay Success!");
            JSONObject jsonData;
            try {
                jsonData = new JSONObject(responseBody);

                exercisesByDay = jsonData.getJSONArray("todayExercises");

//                System.out.println(jsonData.get("todayExercisesVisits"));
//                System.out.println(jsonData.toString());

                if (!jsonData.get("todayExercisesVisits").equals(null))
                    exercisesVisitsByDay = jsonData.getJSONObject("todayExercisesVisits");
                else
                    exercisesVisitsByDay = new JSONObject();

//                System.out.println(exercisesByDay.toString(4));
//                System.out.println(exercisesVisitsByDay.toString(4));

            } catch (JSONException e) {
                System.out.println(e.toString());
            }
        } else {
            getExercisesByDayRequestStatus = RequestStatus.EMPTY_RESPONSE;
            System.out.println("Exercises by day request empty response!");
        }

        onAuthCompleted();
    }

    public void onGetExercisesByLessonRequestCompleted (String[] response) {

        String responseBody = response[0];
        String lessonId = response[1];

        if (getExercisesByLessonRequestStatus == RequestStatus.TIMEOUT) {

        }
        else if (getExercisesByLessonRequestStatus == RequestStatus.FAILED) {

        }
        else if (!responseBody.isEmpty() && activeContainer == ContainerName.LESSONS_INFORMATION) {
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

//                System.out.println("exercisesByLesson: " + exercisesByLesson.toString());
//                System.out.println("exercisesByLessonVisits: " + exercisesByLessonVisits.toString());
//                System.out.println("exercisesByLessonTeacher: " + exercisesByLessonTeacher.toString());
//                System.out.println("exercisesByLessonAmount: " + exercisesByLessonAmount.toString());
//                System.out.println("exercisesByLessonVisitsAmount: " + exercisesByLessonVisitsAmount.toString());
            } catch (JSONException e) {

            }

            // берем нужный предмет
            JSONArray buffer = exercisesByLesson;
            LinearLayout lessonsInformationList = findViewById(R.id.lessonsInformationList);
            lessonsInformationList.removeAllViews();

            int dp = (int) getResources().getDisplayMetrics().density;

            Typeface light = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_light);
            Typeface medium = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_medium);
            Typeface semibold = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_semibold);
            Typeface regular = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_regular);


            // выкидываем информацию о паре
            for (int k = 0; k < buffer.length(); k++) {
                JSONObject value;
                try {

                    value = buffer.getJSONObject(k);
//                    TextView temp = new TextView(getApplicationContext());
//                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150);
//                    lp.setMargins(0,0,0, 50);
//                    temp.setLayoutParams(lp);
//                    temp.setText(value.getString("topic") + " и эта пара была " + value.getString("day"));
//                    temp.setBackgroundColor(167);


                    TextView allLessonsInformation = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams allLessonsInformationLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    allLessonsInformationLP.setMargins(20*dp, 7*dp, 0, 2*dp);
                    allLessonsInformation.setLayoutParams(allLessonsInformationLP);
                    allLessonsInformation.setText(value.getString("day"));
                    allLessonsInformation.setTextSize(12);
                    allLessonsInformation.setTextColor(getResources().getColor(R.color.pinkColor));
                    allLessonsInformation.setTypeface(medium);
                    lessonsInformationList.addView(allLessonsInformation);

                    LinearLayout allLessonsInformationAllInfoBox = new LinearLayout(getApplicationContext());
                    LinearLayout.LayoutParams allLessonsInformationAllInfoBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    allLessonsInformationAllInfoBox.setLayoutParams(allLessonsInformationAllInfoBoxLP);
                    allLessonsInformationAllInfoBox.setBackgroundResource(R.drawable.forms_example);
                    allLessonsInformationAllInfoBox.setOrientation(LinearLayout.VERTICAL);
                    lessonsInformationList.addView(allLessonsInformationAllInfoBox);

                    TextView lessonsAllInformationTheme = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams lessonsAllInformationThemeLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lessonsAllInformationThemeLP.setMargins(30*dp, 10*dp, 30*dp, 0);
                    lessonsAllInformationTheme.setLayoutParams(lessonsAllInformationThemeLP);
                    lessonsAllInformationTheme.setText(value.getString("day"));
                    lessonsAllInformationTheme.setGravity(Gravity.CENTER_VERTICAL);
                    lessonsAllInformationTheme.setTextSize(14);
                    lessonsAllInformationTheme.setText(value.getString("topic"));
                    lessonsAllInformationTheme.setTextColor(getResources().getColor(R.color.white));
                    lessonsAllInformationTheme.setTypeface(medium);
                    allLessonsInformationAllInfoBox.addView(lessonsAllInformationTheme);


                    LinearLayout todayLessonsForUserInformationBox = new LinearLayout(getApplicationContext());
                    LinearLayout.LayoutParams todayLessonsForUserInformationBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    todayLessonsForUserInformationBoxLP.setMargins(5*dp, 5*dp, 5*dp, 0);
                    todayLessonsForUserInformationBox.setLayoutParams(todayLessonsForUserInformationBoxLP);
                    allLessonsInformationAllInfoBox.addView(todayLessonsForUserInformationBox);

                    TextView todayLessonTmpBoxPris = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams todayLessonTmpBoxPrisLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    todayLessonTmpBoxPrisLP.weight = 1;
                    todayLessonTmpBoxPris.setLayoutParams(todayLessonTmpBoxPrisLP);
                    todayLessonTmpBoxPris.setText("присутствие");
                    todayLessonTmpBoxPris.setTextSize(10);
                    todayLessonTmpBoxPris.setGravity(Gravity.CENTER);
                    todayLessonTmpBoxPris.setTextColor(getResources().getColor(R.color.greyColor));
                    todayLessonTmpBoxPris.setTypeface(light);
                    todayLessonsForUserInformationBox.addView(todayLessonTmpBoxPris);

                    TextView todayLessonTmpBoxMark = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams todayLessonTmpBoxMarkLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    todayLessonTmpBoxMarkLP.weight = 1;
                    todayLessonTmpBoxMark.setLayoutParams(todayLessonTmpBoxMarkLP);
                    todayLessonTmpBoxMark.setText("оценка");
                    todayLessonTmpBoxMark.setTextSize(10);
                    todayLessonTmpBoxMark.setGravity(Gravity.CENTER);
                    todayLessonTmpBoxMark.setBackgroundResource(R.drawable.today_lessons_border);
                    todayLessonTmpBoxMark.setTextColor(getResources().getColor(R.color.greyColor));
                    todayLessonTmpBoxMark.setTypeface(light);
                    todayLessonsForUserInformationBox.addView(todayLessonTmpBoxMark);

                    TextView todayLessonTmpBoxAct = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams todayLessonTmpBoxActLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    todayLessonTmpBoxActLP.weight = 1;
                    todayLessonTmpBoxAct.setLayoutParams(todayLessonTmpBoxActLP);
                    todayLessonTmpBoxAct.setText("активность");
                    todayLessonTmpBoxAct.setTextSize(10);
                    todayLessonTmpBoxAct.setGravity(Gravity.CENTER);
                    todayLessonTmpBoxAct.setBackgroundResource(R.drawable.today_lessons_border_right_only);
                    todayLessonTmpBoxAct.setTextColor(getResources().getColor(R.color.greyColor));
                    todayLessonTmpBoxAct.setTypeface(light);
                    todayLessonsForUserInformationBox.addView(todayLessonTmpBoxAct);

                    TextView todayLessonTmpBoxLate = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams todayLessonTmpBoxLateLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    todayLessonTmpBoxLateLP.weight = 1;
                    todayLessonTmpBoxLate.setLayoutParams(todayLessonTmpBoxLateLP);
                    todayLessonTmpBoxLate.setText("опоздание");
                    todayLessonTmpBoxLate.setTextSize(10);
                    todayLessonTmpBoxLate.setGravity(Gravity.CENTER);
                    todayLessonTmpBoxLate.setTextColor(getResources().getColor(R.color.greyColor));
                    todayLessonTmpBoxLate.setTypeface(light);
                    todayLessonsForUserInformationBox.addView(todayLessonTmpBoxLate);

                    LinearLayout todayLessonsAboutUserInformationBox = new LinearLayout(getApplicationContext());
                    LinearLayout.LayoutParams todayLessonsAboutUserInformationBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    todayLessonsAboutUserInformationBoxLP.setMargins(5*dp,0,5*dp,10*dp);
                    todayLessonsAboutUserInformationBox.setLayoutParams(todayLessonsAboutUserInformationBoxLP);
                    allLessonsInformationAllInfoBox.addView(todayLessonsAboutUserInformationBox);


                    // получаем подробную информацию о паре

                    JSONObject valueInfo;
                    try {
                        valueInfo = exercisesByLessonVisits.getJSONArray(value.getString("id")).getJSONObject(0);

                        String presence = valueInfo.getString("presence").equals("0") ? "нет" : "да";
                        String point = valueInfo.getString("point").toString().equals("null")  ? "нет" : valueInfo.getString("point");
                        if (point.equals("1")) point = "зачет";
                        String delay = valueInfo.getString("delay").toString().equals("null")  ? "нет" : "да";
                        String performance = valueInfo.getString("performance").equals("null") ? "нет" : "да";



                        TextView todayLessonTmpBoxPrisInfo = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams todayLessonTmpBoxPrisInfoLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        todayLessonTmpBoxPrisInfoLP.weight = 1;
                        todayLessonTmpBoxPrisInfo.setLayoutParams(todayLessonTmpBoxPrisInfoLP);
                        todayLessonTmpBoxPrisInfo.setText(presence);
                        todayLessonTmpBoxPrisInfo.setTextSize(12);
                        todayLessonTmpBoxPrisInfo.setGravity(Gravity.CENTER);
                        todayLessonTmpBoxPrisInfo.setTextColor(getResources().getColor(R.color.pinkColor));
                        todayLessonTmpBoxPrisInfo.setTypeface(semibold);
                        todayLessonsAboutUserInformationBox.addView(todayLessonTmpBoxPrisInfo);

                        TextView todayLessonTmpBoxMarkInfo = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams todayLessonTmpBoxMarkInfoLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        todayLessonTmpBoxMarkInfoLP.weight = 1;
                        todayLessonTmpBoxMarkInfo.setLayoutParams(todayLessonTmpBoxMarkInfoLP);
                        todayLessonTmpBoxMarkInfo.setText(point);
                        todayLessonTmpBoxMarkInfo.setTextSize(12);
                        todayLessonTmpBoxMarkInfo.setGravity(Gravity.CENTER);
                        todayLessonTmpBoxMarkInfo.setTextColor(getResources().getColor(R.color.pinkColor));
                        todayLessonTmpBoxMarkInfo.setTypeface(semibold);
                        todayLessonsAboutUserInformationBox.addView(todayLessonTmpBoxMarkInfo);

                        TextView todayLessonTmpBoxActInfo = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams todayLessonTmpBoxActInfoLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        todayLessonTmpBoxActInfoLP.weight = 1;
                        todayLessonTmpBoxActInfo.setLayoutParams(todayLessonTmpBoxActInfoLP);
                        todayLessonTmpBoxActInfo.setText(performance);
                        todayLessonTmpBoxActInfo.setTextSize(12);
                        todayLessonTmpBoxActInfo.setGravity(Gravity.CENTER);
                        todayLessonTmpBoxActInfo.setTextColor(getResources().getColor(R.color.pinkColor));
                        todayLessonTmpBoxActInfo.setTypeface(semibold);
                        todayLessonsAboutUserInformationBox.addView(todayLessonTmpBoxActInfo);

                        TextView todayLessonTmpBoxLateInfo = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams todayLessonTmpBoxLateInfoLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        todayLessonTmpBoxLateInfoLP.weight = 1;
                        todayLessonTmpBoxLateInfo.setLayoutParams(todayLessonTmpBoxLateInfoLP);
                        todayLessonTmpBoxLateInfo.setText(delay);
                        todayLessonTmpBoxLateInfo.setTextSize(12);
                        todayLessonTmpBoxLateInfo.setGravity(Gravity.CENTER);
                        todayLessonTmpBoxLateInfo.setTextColor(getResources().getColor(R.color.pinkColor));
                        todayLessonTmpBoxLateInfo.setTypeface(semibold);
                        todayLessonsAboutUserInformationBox.addView(todayLessonTmpBoxLateInfo);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }




                    // опять же id - ключ для следующего массива

//                    temp.setId(IntegeFnList.addView(temp);

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
            System.out.println("Exercises by lesson request empty response!");
        }
    }

    public void onGetVKWallPostsRequestCompleted (String responseBody) {

        LinearLayout notificationList = findViewById(R.id.notificationList);
        notificationList.removeAllViews();

        if (getVKWallPostsRequestStatus == RequestStatus.TIMEOUT) {

        }
        else if (getVKWallPostsRequestStatus == RequestStatus.FAILED) {

        }
        else if (!responseBody.isEmpty() && activeContainer == ContainerName.NOTIFICATION) {
            getVKWallPostsRequestStatus = RequestStatus.COMPLETED;

            System.out.println("GetVKWallPosts Success!");
            JSONObject jsonData;
            try {
                jsonData = new JSONObject(responseBody);

                vkWallPosts = jsonData.getJSONObject("response");

                System.out.println("vkWallPosts: " + vkWallPosts.toString());

            } catch (JSONException e) {

            }

            int dp = (int) getResources().getDisplayMetrics().density;

            Typeface light = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_light);
            Typeface medium = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_medium);
            Typeface semibold = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_semibold);
            Typeface regular = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_regular);


            JSONArray value;
            try {
                value = vkWallPosts.getJSONArray("items");
//                LinearLayout notificationList = findViewById(R.id.notificationList);
//                notificationList.removeAllViews();

                for (int i = 0; i < value.length(); i++) {

                    // берем каждый пост

                    JSONObject tmp;
                    try {
                        tmp = value.getJSONObject(i);

                        //и выкидывем его на форму
                        long stamp = System.currentTimeMillis()/1000;
                        System.out.println("current time: " + stamp);

                        //и выкидывем его на форму если он моложе двух дней
//                        if (stamp - Long.parseLong(tmp.getString("date")) <= 2*24*3600) {
//
//                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//                            lp.setMargins(25, 25, 25, 50);
//                            TextView note = new TextView(getApplicationContext());
//                            note.setLayoutParams(lp);
//                            note.setText( (i+1) + " пост (" + new Date(Long.parseLong(tmp.getString("date"))*1000) + "):    " + tmp.getString("text"));
//                            notificationList.addView(note);

                        if (stamp - Long.parseLong(tmp.getString("date")) <= 7*24*3600) {

//                            Date date = new Date(Long.parseLong(tmp.getString("date"))*1000);
//                            String data = new SimpleDateFormat("y-M-d H:m:s.S").parse(date);
//                            System.out.println(data);

                            Date date = new Date(Long.parseLong(tmp.getString("date")) * 1000);
                            String dateText = new SimpleDateFormat("dd MMMM yy, HH:mm", Locale.getDefault()).format(date);

                            TextView vkPostCurrentInformationTime = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams vkPostCurrentInformationTimeLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            vkPostCurrentInformationTimeLP.setMargins(10*dp, 20*dp, 0, 3*dp);
                            vkPostCurrentInformationTime.setLayoutParams(vkPostCurrentInformationTimeLP);
                            vkPostCurrentInformationTime.setTextSize(12);
                            vkPostCurrentInformationTime.setText(dateText);
//                            vkPostCurrentInformationTime.setText(new Date(Long.parseLong(tmp.getString("date"))*1000).toString());
                            vkPostCurrentInformationTime.setTextColor(getResources().getColor(R.color.pinkColor));
                            vkPostCurrentInformationTime.setTypeface(medium);
                            notificationList.addView(vkPostCurrentInformationTime);

                            TextView vkPostCurrentInformation = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams vkPostCurrentInformationLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            vkPostCurrentInformation.setLayoutParams(vkPostCurrentInformationLP);
                            vkPostCurrentInformation.setPadding(15*dp,15*dp,15*dp,15*dp);
                            vkPostCurrentInformation.setText(tmp.getString("text"));
                            vkPostCurrentInformation.setTextSize(12);
                            vkPostCurrentInformation.setTextColor(getResources().getColor(R.color.greyColor));
                            vkPostCurrentInformation.setBackgroundResource(R.drawable.forms_example);
                            vkPostCurrentInformation.setTypeface(regular);
                            notificationList.addView(vkPostCurrentInformation);

//                            note.setText( (i+1) + " пост (" + new Date(Long.parseLong(tmp.getString("date"))*1000) + "):    " + tmp.getString("text"));
//                            notificationList.addView(note);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        else {
            getVKWallPostsRequestStatus = RequestStatus.EMPTY_RESPONSE;
            System.out.println("VK wall posts request empty response!");
        }
    }

    public void onGetStudentProfileDataRequestCompleted (String[] response){

        String studentFIO = response[0];
        String studentGroup = response[1];
        String studentAvatarSrc = response[2];

        if (getStudentProfileDataRequestStatus == RequestStatus.TIMEOUT) {

        }
        else if (getStudentProfileDataRequestStatus == RequestStatus.FAILED) {

        }
        else if ( !(studentFIO.isEmpty() || studentGroup.isEmpty() || studentAvatarSrc.isEmpty()) ) {
            getStudentProfileDataRequestStatus = RequestStatus.COMPLETED;

            preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
            preferencesEditor = preferences.edit();

            preferencesEditor.putString("studentFIO", studentFIO);
            preferencesEditor.putString("studentGroup", studentGroup);
            preferencesEditor.putString("studentAvatarSrc", studentAvatarSrc);

            preferencesEditor.putString("studentStatsMidMark", statsMidMark);
            preferencesEditor.putString("studentStatsDebtsCount", statsDebtsCount);
            preferencesEditor.putString("studentStatsPercentageOfVisits", statsPercentageOfVisits);

            preferencesEditor.apply();

            this.studentFIO = studentFIO;
            this.studentGroup = studentGroup;
            this.studentAvatarSrc = studentAvatarSrc;

//            if (getExercisesByDayRequestStatus == RequestStatus.COMPLETED && getStudentMainDataRequestStatus == RequestStatus.COMPLETED && !buildFrontendCalled) {
//                buildFrontendCalled = true;
//                buildFrontend();
//            }
        } else {
            getStudentProfileDataRequestStatus = RequestStatus.EMPTY_RESPONSE;
            System.out.println("Student profile's data request empty response!");
        }
    }

    public void onGetScheduleRequestCompleted(String param) {

        LinearLayout box = findViewById(R.id.scheduleList);
        box.removeAllViews();

        TextView text = new TextView(getApplicationContext());
        text.setText(text.getText() + "Problems with schedule request");

        if (getScheduleRequestStatus == RequestStatus.TIMEOUT) {

        }
        else if (getScheduleRequestStatus == RequestStatus.FAILED) {

        }
        else if (getScheduleRequestStatus == RequestStatus.COMPLETED && activeContainer == ContainerName.SCHEDULE) {

            int dp = (int) getResources().getDisplayMetrics().density;

            Typeface light = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_light);
            Typeface medium = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_medium);
            Typeface semibold = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_semibold);
            Typeface regular = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_regular);


            JSONArray value = new JSONArray();

            try {
                value = scheduleLessons.getJSONArray(param);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < value.length(); i++) {

                TextView dayOfWeekName = new TextView(getApplicationContext());
                LinearLayout.LayoutParams dayOfWeekLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                dayOfWeekLP.setMargins(15*dp,0,0,3*dp);
                dayOfWeekName.setLayoutParams(dayOfWeekLP);
                dayOfWeekName.setTextSize(12);
                dayOfWeekName.setTextColor(getResources().getColor(R.color.pinkColor));
                dayOfWeekName.setTypeface(medium);

                switch (i) {
                    case 0: {
//                        text.setText(text.getText() + "Понедельник:\n\n");
                        dayOfWeekName.setText("Понедельник");
                        break;
                    }
                    case 1: {
//                        text.setText(text.getText() + "Вторник:\n\n");
                        dayOfWeekName.setText("Вторник");
                        break;
                    }
                    case 2: {
//                        text.setText(text.getText() + "Среда:\n\n");
                        dayOfWeekName.setText("Среда");
                        break;
                    }
                    case 3: {
//                        text.setText(text.getText() + "Четверг:\n\n");
                        dayOfWeekName.setText("Четверг");
                        break;
                    }
                    case 4: {
//                        text.setText(text.getText() + "Пятница:\n\n");
                        dayOfWeekName.setText("Пятница");
                        break;
                    }
                    case 5: {
//                        text.setText(text.getText() + "Суббота:\n\n");
                        dayOfWeekName.setText("Суббота");
                        break;
                    }
                }

                box.addView(dayOfWeekName);


                LinearLayout forLessonsOfTheDayBox = new LinearLayout(getApplicationContext());
                LinearLayout.LayoutParams forLessonsOfTheDayBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                forLessonsOfTheDayBoxLP.setMargins(0,0,0,10*dp);
                forLessonsOfTheDayBox.setLayoutParams(forLessonsOfTheDayBoxLP);
                forLessonsOfTheDayBox.setBackgroundResource(R.drawable.forms_example);
                forLessonsOfTheDayBox.setPadding(0,0,0,5*dp);
                forLessonsOfTheDayBox.setOrientation(LinearLayout.VERTICAL);
                box.addView(forLessonsOfTheDayBox);

                JSONArray temp = new JSONArray();
                try {
                    temp = value.getJSONArray(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for (int j = 0; j < temp.length(); j++) {

                    JSONObject tmp = new JSONObject();
                    try {
                        tmp = temp.getJSONObject(j);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
//                        text.setText(text.getText() + tmp.getString("position") + " (" + tmp.getString("start") + tmp.getString("end") + ") " + tmp.getString("name") + " (" + tmp.getString("teacher") + ")\n");

                        LinearLayout aboutLessonsOfTheDayBox = new LinearLayout(getApplicationContext());
                        LinearLayout.LayoutParams  aboutLessonsOfTheDayBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        aboutLessonsOfTheDayBoxLP.setMargins(0,5*dp,0,5*dp);
                        aboutLessonsOfTheDayBox.setLayoutParams(aboutLessonsOfTheDayBoxLP);
                        aboutLessonsOfTheDayBox.setBackgroundResource(R.drawable.forms_example);
                        aboutLessonsOfTheDayBox.setOrientation(LinearLayout.VERTICAL);
                        forLessonsOfTheDayBox.addView(aboutLessonsOfTheDayBox);

                        String pos = "";

                        switch (tmp.getString("position")) {
                            case "I": {
                                pos = "1";
                                break;
                            }
                            case "II": {
                                pos = "2";
                                break;
                            }
                            case "III": {
                                pos = "3";
                                break;
                            }
                            case "IV": {
                                pos = "4";
                                break;
                            }
                            case "V": {
                                pos = "5";
                                break;
                            }
                            case "VI": {
                                pos = "6";
                                break;
                            }
                        }

                        TextView theDayLessonsCounter = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams theDayLessonsCounterLP = new LinearLayout.LayoutParams(20*dp, 20*dp);
                        theDayLessonsCounterLP.setMargins(12*dp,10*dp,0,0);
                        theDayLessonsCounter.setLayoutParams(theDayLessonsCounterLP);
                        theDayLessonsCounter.setBackgroundResource(R.drawable.lesson_number);
                        theDayLessonsCounter.setText(pos);
                        theDayLessonsCounter.setTypeface(light);
                        theDayLessonsCounter.setTextColor(getResources().getColor(R.color.white));
                        theDayLessonsCounter.setGravity(Gravity.CENTER);
                        theDayLessonsCounter.setTextSize(14);
                        aboutLessonsOfTheDayBox.addView(theDayLessonsCounter);
//
//
                        TextView theDayLessonsCab = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams theDayLessonsCabLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20*dp);
                        theDayLessonsCabLP.setMargins(0,-20*dp,12*dp,0);
                        theDayLessonsCab.setLayoutParams(theDayLessonsCabLP);
                        theDayLessonsCab.setText(tmp.getString("room"));
                        theDayLessonsCab.setTypeface(medium);
                        theDayLessonsCab.setTextColor(getResources().getColor(R.color.white));
                        theDayLessonsCab.setGravity(Gravity.RIGHT);
                        theDayLessonsCab.setTextSize(12);
                        aboutLessonsOfTheDayBox.addView(theDayLessonsCab);

                        TextView theDayLessonsTime = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams theDayLessonsTimeLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 20*dp);
                        theDayLessonsTimeLP.setMargins(40*dp,-21*dp,0,0);
                        theDayLessonsTime.setLayoutParams(theDayLessonsTimeLP);
                        theDayLessonsTime.setText(tmp.getString("start") + " - " + tmp.getString("end"));
                        theDayLessonsTime.setTypeface(light);
                        theDayLessonsTime.setTextColor(getResources().getColor(R.color.greyColor));
                        theDayLessonsTime.setGravity(Gravity.CENTER_VERTICAL);
                        theDayLessonsTime.setTextSize(12);
                        aboutLessonsOfTheDayBox.addView(theDayLessonsTime);

                        TextView theDayLessonsNameOfLesson = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams theDayLessonsNameOfLessonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        theDayLessonsNameOfLessonLP.setMargins(40*dp,5*dp,40*dp,0);
                        theDayLessonsNameOfLesson.setLayoutParams(theDayLessonsNameOfLessonLP);
                        theDayLessonsNameOfLesson.setText(tmp.getString("name"));
                        theDayLessonsNameOfLesson.setTypeface(medium);
                        theDayLessonsNameOfLesson.setTextColor(getResources().getColor(R.color.white));
                        theDayLessonsNameOfLesson.setGravity(Gravity.CENTER_VERTICAL);
                        theDayLessonsNameOfLesson.setTextSize(13);
                        aboutLessonsOfTheDayBox.addView(theDayLessonsNameOfLesson);

                        TextView theDayLessonsTeacher = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams theDayLessonsTeacherLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        theDayLessonsTeacherLP.setMargins(40*dp,0,40*dp,5*dp);
                        theDayLessonsTeacher.setLayoutParams(theDayLessonsTeacherLP);
                        theDayLessonsTeacher.setText(tmp.getString("teacher"));
                        theDayLessonsTeacher.setTypeface(light);
                        theDayLessonsTeacher.setTextColor(getResources().getColor(R.color.greyColor));
                        theDayLessonsTeacher.setGravity(Gravity.CENTER_VERTICAL);
                        theDayLessonsTeacher.setTextSize(12);
                        aboutLessonsOfTheDayBox.addView(theDayLessonsTeacher);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
//                    text.setText(text.getText() + "\n\n");

                }
            }
        } else {
            getScheduleRequestStatus = RequestStatus.EMPTY_RESPONSE;
            System.out.println("Schedule request empty response!");
        }

//        box.addView(text);
    }

    public void onGetStudentStatsRequestCompleted(String[] response) {

        String statsMidMark = response[0];
        String statsDebtsCount = response[1];
        String statsPercentageOfVisits = response[2];

        if (getStudentStatsRequestStatus == RequestStatus.TIMEOUT) {

        }
        else if (getStudentStatsRequestStatus == RequestStatus.FAILED) {

        }
        else if ( !(statsDebtsCount.isEmpty() || statsDebtsCount.isEmpty() || statsPercentageOfVisits.isEmpty()) ) {
            getStudentStatsRequestStatus = RequestStatus.COMPLETED;

            System.out.println("StudentStats Success!");

            this.statsMidMark = statsMidMark;
            this.statsDebtsCount = statsDebtsCount;
            this.statsPercentageOfVisits = statsPercentageOfVisits;
        } else {
            getStudentStatsRequestStatus = RequestStatus.EMPTY_RESPONSE;
            System.out.println("Student stats request empty response!");
        }
    }

    // Сами асинхронные запросы

    // [name, password]
    class loginRequest extends AsyncTask<String[], Void, String[]> {

        @Override
        protected String[] doInBackground(String[]... params) { // params[0][0] - name, params[0][1] - password
            HttpURLConnection urlConnection = null;
            String cookie = "";

            try {
                String url_address = "https://ifspo.ifmo.ru/";
                String urlParameters = "User[login]=" + params[0][0] + "&User[password]=" + params[0][1];

                urlConnection = Functions.setupPOSTAuthRequest(url_address, urlParameters, "", LOGIN_REQUEST_CONNECT_TIMEOUT, LOGIN_REQUEST_READ_TIMEOUT);

                List<String> cookies = urlConnection.getHeaderFields().get("Set-cookie");
                Integer cookies_count = cookies.size();

                if (cookies_count > 1) {
                    cookie = cookies.get(cookies_count - 1);
                }
            } catch (SocketTimeoutException e) {
                System.out.println("Login request timeout!");
                loginRequestStatus = RequestStatus.TIMEOUT;
                return new String[] {"", params[0][0], params[0][1]};
            } catch (Exception e) {
                System.out.println("Problems with login request");
                System.out.println(e.toString());
                loginRequestStatus = RequestStatus.FAILED;
                return new String[] {"", params[0][0], params[0][1]};
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return new String[] { cookie, params[0][0], params[0][1] };
        }

        @Override
        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            onLoginRequestCompleted(result);
        }
    }

    // [year, month]
    class getStudentMainDataRequest extends AsyncTask<String[], Void, String> {

        protected String doInBackground(String[]... params) { // params[0][0] - year, params[0][1] - month

            System.out.println("Student main data request called");

            HttpURLConnection urlConnection = null;
            String responseBody = "";


            // создаем мап для картинки
            if (getStudentProfileDataRequestStatus == RequestStatus.COMPLETED) {
                try {
                    bitmap = BitmapFactory.decodeStream((InputStream) new URL("https://ifspo.ifmo.ru" + studentAvatarSrc).getContent());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                String url_address = "https://ifspo.ifmo.ru/profile/getStudentLessonsVisits"
                        + "?stud=" + studentId
                        + "&dateyear=" + params[0][0]
                        + "&datemonth=" + params[0][1];

                urlConnection = Functions.setupGETAuthRequest(url_address, authCookie, MAIN_DATA_REQUEST_CONNECT_TIMEOUT, MAIN_DATA_REQUEST_READ_TIMEOUT);
                responseBody = Functions.getResponseFromGetRequest(urlConnection);

            } catch (SocketTimeoutException e) {
                System.out.println("Main data request timeout!");
                getStudentMainDataRequestStatus = RequestStatus.TIMEOUT;
                return "";
            } catch (Exception e) {
                System.out.println("Problems with main data request request");
                System.out.println(e.toString());
                getStudentMainDataRequestStatus = RequestStatus.FAILED;
                return "";
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
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
            HttpURLConnection urlConnection = null;
            String responseBody = "";

            try {
                String url_address = "https://ifspo.ifmo.ru/journal/getStudentExercisesByLesson"
                        + "?lesson=" + params[0][0]
                        + "&student=" + studentId;

                urlConnection = Functions.setupGETAuthRequest(url_address, authCookie, EXERCISES_BY_LESSON_REQUEST_CONNECT_TIMEOUT, EXERCISES_BY_LESSON_REQUEST_READ_TIMEOUT);
                responseBody = Functions.getResponseFromGetRequest(urlConnection);

            } catch (SocketTimeoutException e) {
                System.out.println("Exercises by lesson request timeout!");
                getExercisesByLessonRequestStatus = RequestStatus.TIMEOUT;
                return new String[] {"", ""};
            } catch (Exception e) {
                System.out.println("Problems with exercises by lesson request");
                System.out.println(e.toString());
                getExercisesByLessonRequestStatus = RequestStatus.FAILED;
                return new String[] {"", ""};
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
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
            HttpURLConnection urlConnection = null;
            String responseBody = "";

            try {
                String url_address = "https://ifspo.ifmo.ru//journal/getStudentExercisesByDay"
                        + "?student=" + studentId
                        + "&day=" + params[0][0];

                urlConnection = Functions.setupGETAuthRequest(url_address, authCookie, EXERCISES_BY_DAY_REQUEST_CONNECT_TIMEOUT, EXERCISES_BY_DAY_REQUEST_READ_TIMEOUT);
                responseBody = Functions.getResponseFromGetRequest(urlConnection);

            } catch (SocketTimeoutException e) {
                System.out.println("Exercises by day request timeout!");
                getExercisesByDayRequestStatus = RequestStatus.TIMEOUT;
                return "";
            } catch (Exception e) {
                System.out.println("Problems with exercises by day request");
                getExercisesByDayRequestStatus = RequestStatus.FAILED;
                return "";
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
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
            HttpURLConnection urlConnection = null;
            String responseBody = "";

            try {
                String url_address = "https://api.vk.com/method/wall.get?domain=raspfspo"
                        + "&count=" + params[0][0]
                        + "&filter=owner&access_token=c2cb19e3c2cb19e3c2cb19e339c2a4f3d6cc2cbc2cb19e39c9fe125dc37c9d4bb7994cd&v=5.103";

                urlConnection = Functions.setupGETAuthRequest(url_address, authCookie, VK_POSTS_REQUEST_CONNECT_TIMEOUT, VK_POSTS_REQUEST_READ_TIMEOUT);
                responseBody = Functions.getResponseFromGetRequest(urlConnection);

            } catch (SocketTimeoutException e) {
                System.out.println("VK posts request timeout!");
                getVKWallPostsRequestStatus = RequestStatus.TIMEOUT;
                return "";
            } catch (Exception e) {
                System.out.println("Problems with vk wall posts request");
                System.out.println(e.toString());
                getVKWallPostsRequestStatus = RequestStatus.FAILED;
                return "";
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
            return responseBody;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            onGetVKWallPostsRequestCompleted(result);
        }
    }

    Bitmap bitmap; // картинка профиля

    class getStudentProfileDataRequest extends AsyncTask<Void, Void, String[]> { //FIO, GROUP

        protected String[] doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            String responseBody = "";
            Document html = new Document(responseBody);

            String studentFIO = "";
            String studentGroup = "";
            String avatarSrc = "";

            Element row = null;

            try {
                String url_address = "https://ifspo.ifmo.ru/profile";

                urlConnection = Functions.setupGETAuthRequest(url_address, authCookie, STUDENT_PROFILE_REQUEST_CONNECT_TIMEOUT, STUDENT_PROFILE_REQUEST_READ_TIMEOUT);
                responseBody = Functions.getResponseFromGetRequest(urlConnection);

                html = Jsoup.parse(responseBody);

                Elements rows = html.body().getElementsByClass("container").get(0).getElementsByClass("row");
                for (Element el : rows) {
                    if (el.childrenSize() > 1) {
                        row = el;
                        break;
                    }
                }

                studentFIO = row.getElementsByClass("span9").select("h3").get(0).text();
                studentGroup = row.getElementsByClass("span9").get(0)
                        .getElementsByClass("row").get(0)
                        .getElementsByClass("span3").select("ul").select("li").last().text();
                avatarSrc = row.getElementsByClass("span3").get(0)
                        .getElementsByClass("showchange").get(0)
                        .getElementsByTag("img").get(0).attr("src");

                studentGroup = studentGroup.split(" ")[0];

            } catch (SocketTimeoutException e) {
                System.out.println("Student's profile data request timeout!");
                getStudentProfileDataRequestStatus = RequestStatus.TIMEOUT;
                return new String[] {"", "", ""};
            } catch (Exception e) {
                System.out.println("Problems with student's profile data request");
                System.out.println(e.toString());
                getStudentProfileDataRequestStatus = RequestStatus.FAILED;
                return new String[] {"", "", ""};
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }

            System.out.println("GetProfileParsing Success!");

            // создаем мап для картинки

            try {
                bitmap = BitmapFactory.decodeStream((InputStream)new URL("https://ifspo.ifmo.ru" + avatarSrc).getContent());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return new String[] { studentFIO, studentGroup, avatarSrc };
        }

        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            onGetStudentProfileDataRequestCompleted(result);

        }
    }

    class getScheduleRequest extends AsyncTask<String[], Void, String> { // Void на самом деле

        protected String doInBackground(String[]... params) { // now next
            URL url;
            HttpURLConnection urlConnection = null;
            String responseBody = "";
            Document html = new Document(responseBody);

            try {
                String url_address = "https://ifspo.ifmo.ru/schedule/get?num=" + Functions.getGroupIdByName(studentGroup) + "&week=" + params[0][0];

                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(SCHEDULE_REQUEST_CONNECT_TIMEOUT * 1000);
                urlConnection.setReadTimeout(SCHEDULE_REQUEST_READ_TIMEOUT * 1000);

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
            } catch (SocketTimeoutException e) {
                System.out.println("Schedule request timeout!");
                getScheduleRequestStatus = RequestStatus.TIMEOUT;
                return "";
            } catch (Exception e) {
                System.out.println("Problems with schedule request");
                System.out.println(e.toString());
                getScheduleRequestStatus = RequestStatus.FAILED;
                return "";
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

            JSONArray lessonsCopy = studentLessons;

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
//                    String idOfLesson = "-1";
//                    System.out.println(nameOfLesson);
//
//                    for (int j = 0; j < lessonsCopy.length(); j++) {
//                        try {
//                            if (nameOfLesson.equals(lessonsCopy.getJSONObject(j).getString("name"))) {
//                                idOfLesson = lessonsCopy.getJSONObject(j).getString("id");
//                                System.out.println(idOfLesson + ": " + nameOfLesson);
//                                lessonsCopy.remove(j);
//                                break;
//                            }
//                            System.out.println(nameOfLesson);
//                            System.out.println(lessonsCopy.getJSONObject(i).getString("name"));
//                        } catch (Exception e) {}
//                    }
//
//                    System.out.println(studentLessons.length());
//                    System.out.println(lessonsCopy.length());


                    String teacherOfLesson = elem.getElementsByClass("lesson_td").get(0).select("div").get(1).text();
                    String roomOfLesson = elem.getElementsByClass("place_td").get(0).select("div").get(0).text();

                    JSONObject scheduleRootLessonInformation = new JSONObject();
                    try {
                        scheduleRootLessonInformation = scheduleRootLesson.getJSONObject(scheduleRootLesson.length() - 1);
                        scheduleRootLessonInformation.put("position", numberOfLesson);
                        scheduleRootLessonInformation.put("start", timeOfLessonStart);
                        scheduleRootLessonInformation.put("end", timeOfLessonEnd);
                        scheduleRootLessonInformation.put("name", nameOfLesson);
                        scheduleRootLessonInformation.put("teacher", teacherOfLesson);
                        scheduleRootLessonInformation.put("room", roomOfLesson);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

            }

            System.out.println(scheduleLessons.toString());

            return params[0][0];
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            getScheduleRequestStatus = RequestStatus.COMPLETED;
            onGetScheduleRequestCompleted(result);
        }
    }

    class getStudentStatsRequest extends AsyncTask<Void, Void, String[]> { // Void на самом деле

        protected String[] doInBackground(Void ...params) {
            String statsMidMark = "";
            String statsDebtsCount = "";
            String statsPercentageOfVisits = "";

            String responseBody = "";
            Document html;

            HttpURLConnection urlConnection = null;

            try {
                String url_address = "https://ifspo.ifmo.ru/profile/getStudentStatistics";
                URL url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                String urlParameters = "student_id=" + studentId;
                urlConnection = Functions.setupPOSTAuthRequest(url_address, urlParameters, authCookie, STATS_REQUEST_CONNECT_TIMEOUT, STATS_REQUEST_READ_TIMEOUT);


                responseBody = Functions.getResponseFromGetRequest(urlConnection);
                html = Jsoup.parse(responseBody);

                Elements stats = html.body().getElementsByClass("stat-block");

                statsMidMark = stats.get(0).getElementsByClass("stat-value").get(0).text();
                statsDebtsCount = stats.get(1).getElementsByClass("stat-value").get(0).text();
                statsPercentageOfVisits = stats.get(2).getElementsByClass("stat-value").get(0).text();

                statsMidMark = statsMidMark.substring(0, statsMidMark.length() - 1);

//                System.out.println(statsMidMark + " " + statsDebtsCount + " " + statsPercentageOfVisits);

            } catch (SocketTimeoutException e) {
                System.out.println("Student stats request timeout!");
                getStudentStatsRequestStatus = RequestStatus.TIMEOUT;
                return new String[] {"", "", ""};
            } catch (Exception e) {
                System.out.println("Problems with statistics request");
                System.out.println(e.toString());
                getStudentStatsRequestStatus = RequestStatus.FAILED;
                return new String[] {"", "", ""};
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return new String[] { statsMidMark, statsDebtsCount, statsPercentageOfVisits };
        }

        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            onGetStudentStatsRequestCompleted(result);

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

    ImageView home;
    ImageView schedule;
    ImageView profile;
    ImageView lessons;
    ImageView exit;

    ImageView userHelp;
    TextView scheduleNow;
    TextView scheduleNext;


    // ПОД ОЧИСТКУ ОЧЕНЬ ОЧЕНЬ ВАЖНО

    TextView profileUserName;
    TextView profileUserGroup;
    TextView profileUserCalendar;
    TextView profileUserBalls;
    TextView profileUserBills;
    LinearLayout todayLessonsView;



    // переменная для мониторинга активного контейнера

    enum ContainerName { PROFILE, HOME, SCHEDULE, LESSONS, LESSONS_INFORMATION, NOTIFICATION, LOADING, LOGIN }
    ContainerName activeContainer;


    public void setContainer(ContainerName newContainer) { // функция обновления активного контейнера
        switch (activeContainer) {
            case PROFILE: {
                main.removeView(profileScreen);
                break;
            }
            case HOME: {
                main.removeView(homeScreen);
                break;
            }
            case SCHEDULE: {
                main.removeView(scheduleScreen);
                break;
            }
            case LESSONS: {
                main.removeView(lessonsScreen);
                break;
            }
            case LESSONS_INFORMATION: {
                main.removeView(lessonsInformationScreen);
                break;
            }
            case NOTIFICATION: {
                main.removeView(notificationListScreen);
                break;
            }
            case LOGIN: {
                main.removeView(loginForm);
                break;
            }
            case LOADING: {
                main.removeView(loadingScreen);
            }
        }

        switch (newContainer) {
            case PROFILE: {
                main.addView(profileScreen);
                activeContainer = ContainerName.PROFILE;
                break;
            }
            case HOME: {
                main.addView(homeScreen);
                activeContainer = ContainerName.HOME;
                break;
            }
            case SCHEDULE: {
                main.addView(scheduleScreen);
                activeContainer = ContainerName.SCHEDULE;
                break;
            }
            case LESSONS: {
                main.addView(lessonsScreen);
                activeContainer = ContainerName.LESSONS;
                break;
            }
            case LESSONS_INFORMATION: {
                main.addView(lessonsInformationScreen);
                activeContainer = ContainerName.LESSONS_INFORMATION;
                break;
            }
            case NOTIFICATION: {
                main.addView(notificationListScreen);
                activeContainer = ContainerName.NOTIFICATION;
                break;
            }
            case LOGIN: {
                main.removeAllViews();
                main.addView(loginForm);
                activeContainer = ContainerName.LOGIN;
                break;
            }
            case LOADING: {
                main.addView(loadingScreen);
                activeContainer = ContainerName.LOADING;
            }
        }
    }

    public void setLoadingToList(ContainerName neededContainer) { // функция обновления активного контейнера
        switch (neededContainer) {
            case SCHEDULE: {
                LinearLayout box = findViewById(R.id.scheduleList);
                box.removeAllViews();
                box.addView(loadingScreen);
                break;
            }
            case LESSONS_INFORMATION: {
                LinearLayout box = findViewById(R.id.lessonsInformationList);
                box.removeAllViews();
                box.addView(loadingScreen);
                break;
            }
            case NOTIFICATION: {
                LinearLayout box = findViewById(R.id.notificationList);
                box.removeAllViews();
                box.addView(loadingScreen);
                break;
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
//
//            };


    void buildFrontend() {

        int dp = (int) getResources().getDisplayMetrics().density;

        Typeface light = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_light);
        Typeface medium = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_medium);
        Typeface semibold = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_semibold);
        Typeface regular = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_regular);

        //заранее высираем контент в lessonsScreen
        main.addView(lessonsScreen);

        LinearLayout lessonsList = findViewById(R.id.lessonsList);

//        System.out.println(statsMidMark + " " + statsDebtsCount + " " + statsPercentageOfVisits);

        for(int i = 0; i < studentLessons.length(); i++){
            JSONObject value;
            try {
                value = studentLessons.getJSONObject(i);


                if (i == 0) {
                    TextView semestrCounter = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams semestrCounterLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    semestrCounterLP.setMargins(20 * dp, 15 * dp, 0, 0);
                    semestrCounter.setLayoutParams(semestrCounterLP);
                    semestrCounter.setText(value.getString("semester") + " семестр");
                    semestrCounter.setTextSize(12);
                    semestrCounter.setTextColor(getResources().getColor(R.color.pinkColor));
                    semestrCounter.setTypeface(medium);
                    lessonsList.addView(semestrCounter);
                } else if (!studentLessons.getJSONObject(i-1).getString("semester").equals(value.getString("semester"))) {
                    TextView semestrCounter = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams semestrCounterLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    semestrCounterLP.setMargins(20*dp, 30*dp, 0, 0);
                    semestrCounter.setLayoutParams(semestrCounterLP);
                    semestrCounter.setText(value.getString("semester") + " семестр");
                    semestrCounter.setTextSize(12);
                    semestrCounter.setTextColor(getResources().getColor(R.color.pinkColor));
                    semestrCounter.setTypeface(medium);
                    lessonsList.addView(semestrCounter);
                }

                RelativeLayout studentLessonsFullInfoBox = new RelativeLayout(getApplicationContext());
                RelativeLayout.LayoutParams studentLessonsFullInfoBoxLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                studentLessonsFullInfoBoxLP.setMargins(0,5*dp,0,0);
                studentLessonsFullInfoBox.setLayoutParams(studentLessonsFullInfoBoxLP);
                studentLessonsFullInfoBox.setBackgroundResource(R.drawable.forms_example);
                lessonsList.addView(studentLessonsFullInfoBox);

                TextView nameOfLessonForFullInfo = new TextView(getApplicationContext());
                RelativeLayout.LayoutParams nameOfLessonForFullInfoLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                nameOfLessonForFullInfoLP.setMargins(20*dp, 15*dp, 40 * dp, 15*dp);
                nameOfLessonForFullInfo.setLayoutParams(nameOfLessonForFullInfoLP);
                nameOfLessonForFullInfo.setGravity(Gravity.CENTER_VERTICAL);
                nameOfLessonForFullInfo.setText(value.getString("name"));
                nameOfLessonForFullInfo.setTextSize(11);
                nameOfLessonForFullInfo.setTextColor(getResources().getColor(R.color.white));
                nameOfLessonForFullInfo.setTypeface(medium);
                studentLessonsFullInfoBox.addView(nameOfLessonForFullInfo);

                TextView nameOfLessonForFullInfoDec = new TextView(getApplicationContext());
                RelativeLayout.LayoutParams nameOfLessonForFullInfoDecLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                nameOfLessonForFullInfoDecLP.setMargins(0, 0, 20*dp, 0);
                nameOfLessonForFullInfoDecLP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                nameOfLessonForFullInfoDecLP.addRule(RelativeLayout.CENTER_VERTICAL);
                nameOfLessonForFullInfoDec.setLayoutParams(nameOfLessonForFullInfoDecLP);
                nameOfLessonForFullInfoDec.setGravity(Gravity.CENTER);
                nameOfLessonForFullInfoDec.setText(">");
                nameOfLessonForFullInfoDec.setTextSize(16);
                nameOfLessonForFullInfoDec.setTextColor(getResources().getColor(R.color.greyColor));
                nameOfLessonForFullInfoDec.setTypeface(medium);
                studentLessonsFullInfoBox.addView(nameOfLessonForFullInfoDec);



                // самая важная вещь - id temp'а это id для JSONObject

                studentLessonsFullInfoBox.setId(Integer.parseInt(value.getString("id")));


                // вешаем универсальный обработчик кликов для каждого предмета

                lessonInformationClickListener needMoreInfo = new lessonInformationClickListener();
                studentLessonsFullInfoBox.setOnClickListener(needMoreInfo);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        //скрываем lessonsScreen
        main.removeView(lessonsScreen);

        // убираем регистрацию и подрубаем стартовый экран
        main.removeView(loadingScreen);
        main.removeView(loginForm);
        setContainer(ContainerName.PROFILE);
        main.addView(navigation);
        main.addView(userHelpScreen);

        // инициализируем картинку

        ImageView img = (ImageView) findViewById(R.id.profileImage);
        img.setImageBitmap(bitmap);
        img.setScaleType(ImageView.ScaleType.FIT_XY);


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

//        scheduleChanges.setOnClickListener(wasClicked);


        profileUserName.setText(studentFIO.split(" ")[0] + " "+ studentFIO.split(" ")[1]);
        profileUserGroup.setText(studentGroup);
        profileUserCalendar.setText(statsPercentageOfVisits);
        profileUserBalls.setText(statsMidMark);
        profileUserBills.setText(statsDebtsCount);

        todayLessonsView = findViewById(R.id.todayLessonsView);





        // высираем сегодняшние пары перебором

        for(int i = 0; i < exercisesByDay.length(); i++){
            JSONObject value;
            try {
                value = exercisesByDay.getJSONObject(i);

                LinearLayout mainTodayLessonsTmpBox = new LinearLayout(getApplicationContext());
                LinearLayout.LayoutParams mainTodayLessonsTmpBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                mainTodayLessonsTmpBox.setLayoutParams(mainTodayLessonsTmpBoxLP);
                mainTodayLessonsTmpBoxLP.setMargins(0,dp*15, 0,7*dp);
                mainTodayLessonsTmpBox.setOrientation(LinearLayout.VERTICAL);
                todayLessonsView.addView(mainTodayLessonsTmpBox);

                TextView todayLessonTmpBoxNumber = new TextView(getApplicationContext());
                LinearLayout.LayoutParams todayLessonTmpBoxNumberLP = new LinearLayout.LayoutParams((int)dp*20, (int)dp*20);
                todayLessonTmpBoxNumberLP.setMargins(12*dp,0,0,0);
                todayLessonTmpBoxNumber.setLayoutParams(todayLessonTmpBoxNumberLP);
                todayLessonTmpBoxNumber.setBackgroundResource(R.drawable.lesson_number);
                todayLessonTmpBoxNumber.setText(value.getString("time"));
                todayLessonTmpBoxNumber.setTextSize(14);
                todayLessonTmpBoxNumber.setGravity(Gravity.CENTER);
                todayLessonTmpBoxNumber.setTextColor(getResources().getColor(R.color.white));
                todayLessonTmpBoxNumber.setTypeface(light);
                mainTodayLessonsTmpBox.addView(todayLessonTmpBoxNumber);

//                время для пар

                String todayLessonDuration = "";

                switch (value.getString("time")) {
                    case "1": {
                        todayLessonDuration = "8:20 - 9:50";
                        break;
                    }
                    case "2": {
                        todayLessonDuration = "10:00 - 11:30";
                        break;
                    }
                    case "3": {
                        todayLessonDuration = "11:40 - 13:10";
                        break;
                    }
                    case "4": {
                        todayLessonDuration = "13:30 - 15:00";
                        break;
                    }
                    case "5": {
                        todayLessonDuration = "15:20 - 16:50";
                        break;
                    }
                    case "6": {
                        todayLessonDuration = "17:00 - 18:30";
                        break;
                    }
                }

                TextView todayLessonTmpBoxTime = new TextView(getApplicationContext());
                LinearLayout.LayoutParams todayLessonTmpBoxTimeLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 20*dp);
                todayLessonTmpBoxTimeLP.setMargins(40*dp, -21*dp, 0, 0);
                todayLessonTmpBoxTime.setLayoutParams(todayLessonTmpBoxTimeLP);
                todayLessonTmpBoxTime.setText(todayLessonDuration);
                todayLessonTmpBoxTime.setTextSize(12);
                todayLessonTmpBoxTime.setGravity(Gravity.CENTER_VERTICAL);
                todayLessonTmpBoxTime.setTextColor(getResources().getColor(R.color.greyColor));
                todayLessonTmpBoxTime.setTypeface(light);
                mainTodayLessonsTmpBox.addView(todayLessonTmpBoxTime);


                TextView todayLessonTmpBoxName = new TextView(getApplicationContext());
                LinearLayout.LayoutParams todayLessonTmpBoxNameLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                todayLessonTmpBoxNameLP.setMargins(40*dp, 5*dp, 40*dp, 0);
                todayLessonTmpBoxName.setLayoutParams(todayLessonTmpBoxNameLP);
                todayLessonTmpBoxName.setText(value.getString("name"));
                todayLessonTmpBoxName.setTextSize(13);
                todayLessonTmpBoxName.setGravity(Gravity.CENTER_VERTICAL);
                todayLessonTmpBoxName.setTextColor(getResources().getColor(R.color.white));
                todayLessonTmpBoxName.setTypeface(medium);
                mainTodayLessonsTmpBox.addView(todayLessonTmpBoxName);

                TextView todayLessonTmpBoxPrepod = new TextView(getApplicationContext());
                LinearLayout.LayoutParams todayLessonTmpBoxPrepodLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                todayLessonTmpBoxPrepodLP.setMargins(40*dp, 0, 40*dp, 0);
                todayLessonTmpBoxPrepod.setLayoutParams(todayLessonTmpBoxPrepodLP);
                JSONObject teacher = teachers.getJSONObject(value.getString("lid"));
                todayLessonTmpBoxPrepod.setText(teacher.getString("lastname") + " " + teacher.getString("firstname") + " " + teacher.getString("middlename"));
                todayLessonTmpBoxPrepod.setTextSize(12);
                todayLessonTmpBoxPrepod.setGravity(Gravity.CENTER_VERTICAL);
                todayLessonTmpBoxPrepod.setTextColor(getResources().getColor(R.color.greyColor));
                todayLessonTmpBoxPrepod.setTypeface(light);
                mainTodayLessonsTmpBox.addView(todayLessonTmpBoxPrepod);

                LinearLayout todayLessonsForUserInformationBox = new LinearLayout(getApplicationContext());
                LinearLayout.LayoutParams todayLessonsForUserInformationBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                todayLessonsForUserInformationBoxLP.setMargins(5*dp, 5*dp, 5*dp, 0);
                todayLessonsForUserInformationBox.setLayoutParams(todayLessonsForUserInformationBoxLP);
                mainTodayLessonsTmpBox.addView(todayLessonsForUserInformationBox);

                TextView todayLessonTmpBoxPris = new TextView(getApplicationContext());
                LinearLayout.LayoutParams todayLessonTmpBoxPrisLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                todayLessonTmpBoxPrisLP.weight = 1;
                todayLessonTmpBoxPris.setLayoutParams(todayLessonTmpBoxPrisLP);
                todayLessonTmpBoxPris.setText("присутствие");
                todayLessonTmpBoxPris.setTextSize(10);
                todayLessonTmpBoxPris.setGravity(Gravity.CENTER);
                todayLessonTmpBoxPris.setTextColor(getResources().getColor(R.color.greyColor));
                todayLessonTmpBoxPris.setTypeface(light);
                todayLessonsForUserInformationBox.addView(todayLessonTmpBoxPris);

                TextView todayLessonTmpBoxMark = new TextView(getApplicationContext());
                LinearLayout.LayoutParams todayLessonTmpBoxMarkLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                todayLessonTmpBoxMarkLP.weight = 1;
                todayLessonTmpBoxMark.setLayoutParams(todayLessonTmpBoxMarkLP);
                todayLessonTmpBoxMark.setText("оценка");
                todayLessonTmpBoxMark.setTextSize(10);
                todayLessonTmpBoxMark.setGravity(Gravity.CENTER);
                todayLessonTmpBoxMark.setBackgroundResource(R.drawable.today_lessons_border);
                todayLessonTmpBoxMark.setTextColor(getResources().getColor(R.color.greyColor));
                todayLessonTmpBoxMark.setTypeface(light);
                todayLessonsForUserInformationBox.addView(todayLessonTmpBoxMark);

                TextView todayLessonTmpBoxAct = new TextView(getApplicationContext());
                LinearLayout.LayoutParams todayLessonTmpBoxActLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                todayLessonTmpBoxActLP.weight = 1;
                todayLessonTmpBoxAct.setLayoutParams(todayLessonTmpBoxActLP);
                todayLessonTmpBoxAct.setText("активность");
                todayLessonTmpBoxAct.setTextSize(10);
                todayLessonTmpBoxAct.setGravity(Gravity.CENTER);
                todayLessonTmpBoxAct.setBackgroundResource(R.drawable.today_lessons_border_right_only);
                todayLessonTmpBoxAct.setTextColor(getResources().getColor(R.color.greyColor));
                todayLessonTmpBoxAct.setTypeface(light);
                todayLessonsForUserInformationBox.addView(todayLessonTmpBoxAct);

                TextView todayLessonTmpBoxLate = new TextView(getApplicationContext());
                LinearLayout.LayoutParams todayLessonTmpBoxLateLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                todayLessonTmpBoxLateLP.weight = 1;
                todayLessonTmpBoxLate.setLayoutParams(todayLessonTmpBoxLateLP);
                todayLessonTmpBoxLate.setText("опоздание");
                todayLessonTmpBoxLate.setTextSize(10);
                todayLessonTmpBoxLate.setGravity(Gravity.CENTER);
                todayLessonTmpBoxLate.setTextColor(getResources().getColor(R.color.greyColor));
                todayLessonTmpBoxLate.setTypeface(light);
                todayLessonsForUserInformationBox.addView(todayLessonTmpBoxLate);

                LinearLayout todayLessonsAboutUserInformationBox = new LinearLayout(getApplicationContext());
                LinearLayout.LayoutParams todayLessonsAboutUserInformationBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                todayLessonsAboutUserInformationBoxLP.setMargins(5*dp,0,5*dp,0);
                todayLessonsAboutUserInformationBox.setLayoutParams(todayLessonsAboutUserInformationBoxLP);
                mainTodayLessonsTmpBox.addView(todayLessonsAboutUserInformationBox);



                // узнаем подробную информацию о паре

                JSONObject valueInfo;
                try {
                    valueInfo = exercisesVisitsByDay.getJSONArray(value.getString("id")).getJSONObject(0);

                    String presence = valueInfo.getString("presence").equals("0") ? "нет" : "да";
                    String point = valueInfo.getString("point").toString().equals("null")  ? "нет" : valueInfo.getString("point");
                    if (point.equals("1")) point = "зачет";
                    String delay = valueInfo.getString("delay").toString().equals("null")  ? "нет" : "да";
                    String performance = valueInfo.getString("performance").equals("null") ? "нет" : "да";



                    TextView todayLessonTmpBoxPrisInfo = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams todayLessonTmpBoxPrisInfoLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    todayLessonTmpBoxPrisInfoLP.weight = 1;
                    todayLessonTmpBoxPrisInfo.setLayoutParams(todayLessonTmpBoxPrisInfoLP);
                    todayLessonTmpBoxPrisInfo.setText(presence);
                    todayLessonTmpBoxPrisInfo.setTextSize(12);
                    todayLessonTmpBoxPrisInfo.setGravity(Gravity.CENTER);
                    todayLessonTmpBoxPrisInfo.setTextColor(getResources().getColor(R.color.pinkColor));
                    todayLessonTmpBoxPrisInfo.setTypeface(semibold);
                    todayLessonsAboutUserInformationBox.addView(todayLessonTmpBoxPrisInfo);

                    TextView todayLessonTmpBoxMarkInfo = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams todayLessonTmpBoxMarkInfoLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    todayLessonTmpBoxMarkInfoLP.weight = 1;
                    todayLessonTmpBoxMarkInfo.setLayoutParams(todayLessonTmpBoxMarkInfoLP);
                    todayLessonTmpBoxMarkInfo.setText(point);
                    todayLessonTmpBoxMarkInfo.setTextSize(12);
                    todayLessonTmpBoxMarkInfo.setGravity(Gravity.CENTER);
                    todayLessonTmpBoxMarkInfo.setTextColor(getResources().getColor(R.color.pinkColor));
                    todayLessonTmpBoxMarkInfo.setTypeface(semibold);
                    todayLessonsAboutUserInformationBox.addView(todayLessonTmpBoxMarkInfo);

                    TextView todayLessonTmpBoxActInfo = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams todayLessonTmpBoxActInfoLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    todayLessonTmpBoxActInfoLP.weight = 1;
                    todayLessonTmpBoxActInfo.setLayoutParams(todayLessonTmpBoxActInfoLP);
                    todayLessonTmpBoxActInfo.setText(performance);
                    todayLessonTmpBoxActInfo.setTextSize(12);
                    todayLessonTmpBoxActInfo.setGravity(Gravity.CENTER);
                    todayLessonTmpBoxActInfo.setTextColor(getResources().getColor(R.color.pinkColor));
                    todayLessonTmpBoxActInfo.setTypeface(semibold);
                    todayLessonsAboutUserInformationBox.addView(todayLessonTmpBoxActInfo);

                    TextView todayLessonTmpBoxLateInfo = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams todayLessonTmpBoxLateInfoLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    todayLessonTmpBoxLateInfoLP.weight = 1;
                    todayLessonTmpBoxLateInfo.setLayoutParams(todayLessonTmpBoxLateInfoLP);
                    todayLessonTmpBoxLateInfo.setText(delay);
                    todayLessonTmpBoxLateInfo.setTextSize(12);
                    todayLessonTmpBoxLateInfo.setGravity(Gravity.CENTER);
                    todayLessonTmpBoxLateInfo.setTextColor(getResources().getColor(R.color.pinkColor));
                    todayLessonTmpBoxLateInfo.setTypeface(semibold);
                    todayLessonsAboutUserInformationBox.addView(todayLessonTmpBoxLateInfo);




                } catch (JSONException e) {
                    e.printStackTrace();
                }


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
                    if (v.getId() == profile.getId()) return;
                    main.removeView(profileScreen);
                    break;
                }
                case HOME: {
                    if (v.getId() == home.getId()) return;
                    main.removeView(homeScreen);
                    break;
                }
                case SCHEDULE: {
                    if (v.getId() == schedule.getId()) return;
                    main.removeView(scheduleScreen);
                    break;
                }
                case LESSONS: {
                    if (v.getId() == lessons.getId()) return;
                    main.removeView(lessonsScreen);
                    break;
                }
                case LESSONS_INFORMATION: {
                    main.removeView(lessonsInformationScreen);
                    break;
                }
                case NOTIFICATION: {
                    if (v.getId() == userHelp.getId()) return;
                    main.removeView(notificationListScreen);
                    break;
                }
            }

            // и добавляем новый

            if (v.getId() == home.getId()) {
                System.out.println("You clicked home");
                setContainer(ContainerName.HOME);
            }

            if (v.getId() == schedule.getId()) {
                System.out.println("You clicked schedule");
                setContainer(ContainerName.SCHEDULE);
            }

            if (v.getId() == profile.getId()) {
                System.out.println("You clicked profile");
                setContainer(ContainerName.PROFILE);
            }

            if (v.getId() == lessons.getId()) {
                System.out.println("You clicked lessons");
                setContainer(ContainerName.LESSONS);
            }

            if (v.getId() == userHelp.getId()) {
                System.out.println("You clicked notifications");
                setContainer(ContainerName.NOTIFICATION);
            }

            if (v.getId() == exit.getId()) {
                resetApp();
                setLoginFormContainer();
            }

            // но если кликнута кнопка изменений в расписании, нужно еще выкинуть контент от вк



            if (activeContainer == ContainerName.NOTIFICATION) {

                sendGetVKWallPostsRequest(new String[] {"40"});
                setLoadingToList(ContainerName.NOTIFICATION);

            }



        }

    }



    // обработчик нажатий на предметы в lessons

    class lessonInformationClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v)
        {

            int dp = (int) getResources().getDisplayMetrics().density;

            Typeface light = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_light);
            Typeface medium = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_medium);
            Typeface semibold = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_semibold);
            Typeface regular = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_regular);


            // обновляем активный экран

            main.removeView(lessonsScreen);
            activeContainer = ContainerName.LESSONS_INFORMATION;
            main.addView(lessonsInformationScreen);

            JSONArray checkArray = studentLessons;
            for (int i = 0; i < checkArray.length(); i++) {
                try {
                    JSONObject tmp = checkArray.getJSONObject(i);
                    if (tmp.getString("id").equals(v.getId()+"")) {
                        TextView nameView = findViewById(R.id.lessonsInformationName);
                        nameView.setText(tmp.getString("name"));
                        TextView semesterView = findViewById(R.id.lessonsInformationSemester);
                        semesterView.setText(tmp.getString("semester") + " семестр");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }


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
                setLoadingToList(ContainerName.LESSONS_INFORMATION);
                sendGetExercisesByLessonRequest(new String[] {v.getId()+""});
            } else {


                for (int k = 0; k < buffer.length(); k++) {
                    JSONObject value;
                    try {

                        value = buffer.getJSONObject(k);
//                        TextView temp = new TextView(getApplicationContext());
//                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150);
//                        lp.setMargins(0,0,0, 50);
//                        temp.setLayoutParams(lp);
//                        temp.setText(value.getString("topic") + " и эта пара была " + value.getString("day"));
//                        temp.setBackgroundColor(167);


                        TextView allLessonsInformation = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams allLessonsInformationLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        allLessonsInformationLP.setMargins(20*dp, 7*dp, 0, 2*dp);
                        allLessonsInformation.setLayoutParams(allLessonsInformationLP);
                        allLessonsInformation.setText(value.getString("day"));
                        allLessonsInformation.setTextSize(12);
                        allLessonsInformation.setTextColor(getResources().getColor(R.color.pinkColor));
                        allLessonsInformation.setTypeface(medium);
                        lessonsInformationList.addView(allLessonsInformation);

                        LinearLayout allLessonsInformationAllInfoBox = new LinearLayout(getApplicationContext());
                        LinearLayout.LayoutParams allLessonsInformationAllInfoBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        allLessonsInformationAllInfoBox.setLayoutParams(allLessonsInformationAllInfoBoxLP);
                        allLessonsInformationAllInfoBox.setBackgroundResource(R.drawable.forms_example);
                        allLessonsInformationAllInfoBox.setOrientation(LinearLayout.VERTICAL);
                        lessonsInformationList.addView(allLessonsInformationAllInfoBox);

                        TextView lessonsAllInformationTheme = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams lessonsAllInformationThemeLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        lessonsAllInformationThemeLP.setMargins(30*dp, 10*dp, 30*dp, 0);
                        lessonsAllInformationTheme.setLayoutParams(lessonsAllInformationThemeLP);
                        lessonsAllInformationTheme.setText(value.getString("day"));
                        lessonsAllInformationTheme.setGravity(Gravity.CENTER_VERTICAL);
                        lessonsAllInformationTheme.setTextSize(14);
                        lessonsAllInformationTheme.setText(value.getString("topic"));
                        lessonsAllInformationTheme.setTextColor(getResources().getColor(R.color.white));
                        lessonsAllInformationTheme.setTypeface(medium);
                        allLessonsInformationAllInfoBox.addView(lessonsAllInformationTheme);


                        LinearLayout todayLessonsForUserInformationBox = new LinearLayout(getApplicationContext());
                        LinearLayout.LayoutParams todayLessonsForUserInformationBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        todayLessonsForUserInformationBoxLP.setMargins(5*dp, 5*dp, 5*dp, 0);
                        todayLessonsForUserInformationBox.setLayoutParams(todayLessonsForUserInformationBoxLP);
                        allLessonsInformationAllInfoBox.addView(todayLessonsForUserInformationBox);

                        TextView todayLessonTmpBoxPris = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams todayLessonTmpBoxPrisLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        todayLessonTmpBoxPrisLP.weight = 1;
                        todayLessonTmpBoxPris.setLayoutParams(todayLessonTmpBoxPrisLP);
                        todayLessonTmpBoxPris.setText("присутствие");
                        todayLessonTmpBoxPris.setTextSize(10);
                        todayLessonTmpBoxPris.setGravity(Gravity.CENTER);
                        todayLessonTmpBoxPris.setTextColor(getResources().getColor(R.color.greyColor));
                        todayLessonTmpBoxPris.setTypeface(light);
                        todayLessonsForUserInformationBox.addView(todayLessonTmpBoxPris);

                        TextView todayLessonTmpBoxMark = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams todayLessonTmpBoxMarkLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        todayLessonTmpBoxMarkLP.weight = 1;
                        todayLessonTmpBoxMark.setLayoutParams(todayLessonTmpBoxMarkLP);
                        todayLessonTmpBoxMark.setText("оценка");
                        todayLessonTmpBoxMark.setTextSize(10);
                        todayLessonTmpBoxMark.setGravity(Gravity.CENTER);
                        todayLessonTmpBoxMark.setBackgroundResource(R.drawable.today_lessons_border);
                        todayLessonTmpBoxMark.setTextColor(getResources().getColor(R.color.greyColor));
                        todayLessonTmpBoxMark.setTypeface(light);
                        todayLessonsForUserInformationBox.addView(todayLessonTmpBoxMark);

                        TextView todayLessonTmpBoxAct = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams todayLessonTmpBoxActLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        todayLessonTmpBoxActLP.weight = 1;
                        todayLessonTmpBoxAct.setLayoutParams(todayLessonTmpBoxActLP);
                        todayLessonTmpBoxAct.setText("активность");
                        todayLessonTmpBoxAct.setTextSize(10);
                        todayLessonTmpBoxAct.setGravity(Gravity.CENTER);
                        todayLessonTmpBoxAct.setBackgroundResource(R.drawable.today_lessons_border_right_only);
                        todayLessonTmpBoxAct.setTextColor(getResources().getColor(R.color.greyColor));
                        todayLessonTmpBoxAct.setTypeface(light);
                        todayLessonsForUserInformationBox.addView(todayLessonTmpBoxAct);

                        TextView todayLessonTmpBoxLate = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams todayLessonTmpBoxLateLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        todayLessonTmpBoxLateLP.weight = 1;
                        todayLessonTmpBoxLate.setLayoutParams(todayLessonTmpBoxLateLP);
                        todayLessonTmpBoxLate.setText("опоздание");
                        todayLessonTmpBoxLate.setTextSize(10);
                        todayLessonTmpBoxLate.setGravity(Gravity.CENTER);
                        todayLessonTmpBoxLate.setTextColor(getResources().getColor(R.color.greyColor));
                        todayLessonTmpBoxLate.setTypeface(light);
                        todayLessonsForUserInformationBox.addView(todayLessonTmpBoxLate);

                        LinearLayout todayLessonsAboutUserInformationBox = new LinearLayout(getApplicationContext());
                        LinearLayout.LayoutParams todayLessonsAboutUserInformationBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        todayLessonsAboutUserInformationBoxLP.setMargins(5*dp,0,5*dp,10*dp);
                        todayLessonsAboutUserInformationBox.setLayoutParams(todayLessonsAboutUserInformationBoxLP);
                        allLessonsInformationAllInfoBox.addView(todayLessonsAboutUserInformationBox);

                        // получаем подробную информацию о паре

                        System.out.println("1: " +  readyExercisesByLesson);
                        System.out.println("2: " +  readyExercisesByLessonVisits);


                        JSONObject valueInfo;
                        try {
                            valueInfo = readyExercisesByLessonVisits.getJSONObject(v.getId()+"").getJSONArray(value.getString("id")).getJSONObject(0);
                            String presence = valueInfo.getString("presence").equals("0") ? "нет" : "да";
                            String point = valueInfo.getString("point").toString().equals("null")  ? "нет" : valueInfo.getString("point");
                            if (point.equals("1")) point = "зачет";
                            String delay = valueInfo.getString("delay").toString().equals("null")  ? "нет" : "да";
                            String performance = valueInfo.getString("performance").equals("null") ? "нет" : "да";



                            TextView todayLessonTmpBoxPrisInfo = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams todayLessonTmpBoxPrisInfoLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            todayLessonTmpBoxPrisInfoLP.weight = 1;
                            todayLessonTmpBoxPrisInfo.setLayoutParams(todayLessonTmpBoxPrisInfoLP);
                            todayLessonTmpBoxPrisInfo.setText(presence);
                            todayLessonTmpBoxPrisInfo.setTextSize(12);
                            todayLessonTmpBoxPrisInfo.setGravity(Gravity.CENTER);
                            todayLessonTmpBoxPrisInfo.setTextColor(getResources().getColor(R.color.pinkColor));
                            todayLessonTmpBoxPrisInfo.setTypeface(semibold);
                            todayLessonsAboutUserInformationBox.addView(todayLessonTmpBoxPrisInfo);

                            TextView todayLessonTmpBoxMarkInfo = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams todayLessonTmpBoxMarkInfoLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            todayLessonTmpBoxMarkInfoLP.weight = 1;
                            todayLessonTmpBoxMarkInfo.setLayoutParams(todayLessonTmpBoxMarkInfoLP);
                            todayLessonTmpBoxMarkInfo.setText(point);
                            todayLessonTmpBoxMarkInfo.setTextSize(12);
                            todayLessonTmpBoxMarkInfo.setGravity(Gravity.CENTER);
                            todayLessonTmpBoxMarkInfo.setTextColor(getResources().getColor(R.color.pinkColor));
                            todayLessonTmpBoxMarkInfo.setTypeface(semibold);
                            todayLessonsAboutUserInformationBox.addView(todayLessonTmpBoxMarkInfo);

                            TextView todayLessonTmpBoxActInfo = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams todayLessonTmpBoxActInfoLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            todayLessonTmpBoxActInfoLP.weight = 1;
                            todayLessonTmpBoxActInfo.setLayoutParams(todayLessonTmpBoxActInfoLP);
                            todayLessonTmpBoxActInfo.setText(performance);
                            todayLessonTmpBoxActInfo.setTextSize(12);
                            todayLessonTmpBoxActInfo.setGravity(Gravity.CENTER);
                            todayLessonTmpBoxActInfo.setTextColor(getResources().getColor(R.color.pinkColor));
                            todayLessonTmpBoxActInfo.setTypeface(semibold);
                            todayLessonsAboutUserInformationBox.addView(todayLessonTmpBoxActInfo);

                            TextView todayLessonTmpBoxLateInfo = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams todayLessonTmpBoxLateInfoLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            todayLessonTmpBoxLateInfoLP.weight = 1;
                            todayLessonTmpBoxLateInfo.setLayoutParams(todayLessonTmpBoxLateInfoLP);
                            todayLessonTmpBoxLateInfo.setText(delay);
                            todayLessonTmpBoxLateInfo.setTextSize(12);
                            todayLessonTmpBoxLateInfo.setGravity(Gravity.CENTER);
                            todayLessonTmpBoxLateInfo.setTextColor(getResources().getColor(R.color.pinkColor));
                            todayLessonTmpBoxLateInfo.setTypeface(semibold);
                            todayLessonsAboutUserInformationBox.addView(todayLessonTmpBoxLateInfo);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }



                        // опять же id - ключ для следующего массива



                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }

        }

    }

}