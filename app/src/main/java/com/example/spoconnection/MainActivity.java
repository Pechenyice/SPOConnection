package com.example.spoconnection;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.DialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.GradientDrawable;

import android.os.Bundle;
import android.os.AsyncTask;

import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

//import static com.example.spoconnection.Functions.setupPOSTAuthRequest;
//import com.example.spoconnection.Functions;


public class MainActivity extends AppCompatActivity {

    final long COOKIE_LIFETIME = 100; // в минутах. На самом деле 120 минут.
    final long AUTH_SYNC_PERIOD = 7 * 24 * 60; // в минутах (7 дней)


    final Integer STATS_REQUEST_CONNECT_TIMEOUT                 = 5;  // в секундах
    final Integer LOGIN_REQUEST_CONNECT_TIMEOUT                 = 5;
    final Integer MAIN_DATA_REQUEST_CONNECT_TIMEOUT             = 5;
    final Integer STUDENT_PROFILE_REQUEST_CONNECT_TIMEOUT       = 8;
    final Integer SCHEDULE_OF_GROUP_REQUEST_CONNECT_TIMEOUT     = 5;
    final Integer SCHEDULE_OF_TEACHER_REQUEST_CONNECT_TIMEOUT   = 5;
    final Integer EXERCISES_BY_DAY_REQUEST_CONNECT_TIMEOUT      = 5;
    final Integer EXERCISES_BY_LESSON_REQUEST_CONNECT_TIMEOUT   = 5;
    final Integer VK_POSTS_REQUEST_CONNECT_TIMEOUT              = 5;
    final Integer FINAL_MARKS_REQUEST_CONNECT_TIMEOUT           = 5;
    final Integer ALL_FINAL_MARKS_REQUEST_CONNECT_TIMEOUT       = 5;
    final Integer RATING_REQUEST_CONNECT_TIMEOUT                = 7;
    final Integer SCHEDULE_INFO_REQUEST_CONNECT_TIMEOUT         = 5;

    final Integer STATS_REQUEST_READ_TIMEOUT                 = 5;  // в секундах
    final Integer LOGIN_REQUEST_READ_TIMEOUT                 = 5;
    final Integer MAIN_DATA_REQUEST_READ_TIMEOUT             = 5;
    final Integer STUDENT_PROFILE_REQUEST_READ_TIMEOUT       = 8;
    final Integer SCHEDULE_OF_GROUP_REQUEST_READ_TIMEOUT     = 5;
    final Integer SCHEDULE_OF_TEACHER_REQUEST_READ_TIMEOUT   = 5;
    final Integer EXERCISES_BY_DAY_REQUEST_READ_TIMEOUT      = 5;
    final Integer EXERCISES_BY_LESSON_REQUEST_READ_TIMEOUT   = 5;
    final Integer VK_POSTS_REQUEST_READ_TIMEOUT              = 5;
    final Integer FINAL_MARKS_REQUEST_READ_TIMEOUT           = 5;
    final Integer ALL_FINAL_MARKS_REQUEST_READ_TIMEOUT       = 5;
    final Integer RATING_REQUEST_READ_TIMEOUT                = 7;
    final Integer SCHEDULE_INFO_REQUEST_READ_TIMEOUT         = 5;

    // Хэндлер диалогов

    int dialogCounter = 1;

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

    public JSONArray studentFinalMarks = new JSONArray();
    public JSONArray studentAllFinalMarks = new JSONArray();
    public String finalMarksSemestr;

    // by vk api
    public JSONObject vkWallPosts;

    public JSONObject ratingInfo;

    public Bitmap studentAvatarBitmap;


    LoginRequest loginRequest;
    GetStudentMainDataRequest mainDataRequest;
    GetExercisesByDayRequest exercisesByDayRequest;
    GetExercisesByLessonRequest exercisesByLessonRequest;
    GetVKWallPostsRequest vkPostsRequest;
    GetStudentProfileDataRequest studentProfileRequest;
    GetStudentStatsRequest studentStatsRequest;
    RatingRequest ratingRequest;
    GetFinalMarksRequest finalMarksRequest;
    GetAllFinalMarksRequest allFinalMarksRequest;

    GetScheduleOfGroupRequest scheduleOfGroupRequest;
    GetScheduleOfTeacherRequest scheduleOfTeacherRequest;
    GetScheduleInfoRequest scheduleInfoRequest;
    GetStudentAchievementsRequest achievementsRequest;

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
    RequestStatus getStudentStatsRequestStatus;
    RequestStatus getFinalMarksRequestStatus;
    RequestStatus getAllFinalMarksRequestStatus;
    RequestStatus ratingRequestStatus;

    RequestStatus getScheduleOfGroupRequestStatus;
    RequestStatus getScheduleOfTeacherRequestStatus;
    RequestStatus getScheduleInfoRequestStatus;
    RequestStatus getStudentAchievementsRequestStatus;


    // Переменная, чтобы buildFrontend не вызвался дважды (и после getMainData и после getByDay)
//    Boolean buildFrontendCalled = false;

    int activeScheduleWeek = 0; // для мониторинга текущей недели при вызове сортировки по дню 0 - тек, 1 - след

    Boolean nowWeekScheduleCalled = false;
    Boolean nextWeekScheduleCalled = false;
    Boolean itogMarksAreReady = false;
//    Boolean teachersScheduleIsReady = false;

    Boolean appFirstRun = false;
    Boolean isAuth = false;

    // расписание преподавателей

    Boolean teacherNowWeek = false;
    Boolean teacherNextWeek = false;

    // рейтинг

    TextView ratingPlace;
    TextView ratingCount;


    // контейнеры

    public RelativeLayout main;
//    public RelativeLayout profileScreen;
//    public LinearLayout profileScreen;
    public ScrollView profileScreen;
    public RelativeLayout loginForm;
    public LinearLayout navigation;
    public RelativeLayout homeScreen;
    public RelativeLayout scheduleScreen;
    public RelativeLayout teacherScheduleScreen;
    public RelativeLayout lessonsScreen;
    public RelativeLayout lessonsInformationScreen;
    public LinearLayout userHelpScreen;
    public LinearLayout notificationListScreen;
    public RelativeLayout loadingScreen;
    public RelativeLayout settingsScreen;
    public RelativeLayout itogScreen;
    public RelativeLayout errorScreen;
    public RelativeLayout backConnectScreen;
    public RelativeLayout teachersScreen;

    public LinearLayout settingsSync;
    public LinearLayout settingsHelp;


    // массив расписания
    JSONObject groupScheduleLessons = new JSONObject();
    JSONObject teacherScheduleLessons = new JSONObject();

    String studentGroup; // Y2234
    String studentFIO;
    String studentAvatarSrc;

    String statsMidMark; // 4.74
    String statsDebtsCount; // 0
    String statsPercentageOfVisits; // 91%

    SharedPreferences preferences;
    SharedPreferences.Editor preferencesEditor;

    TextView monday;
    TextView tuesday;
    TextView wednesday;
    TextView thursday;
    TextView friday;
    TextView saturday;

    Boolean mondayIsActive = false;
    Boolean tuesdayIsActive = false;
    Boolean wednesdayIsActive = false;
    Boolean thursdayIsActive = false;
    Boolean fridayIsActive = false;
    Boolean saturdayIsActive = false;

    // массив расписания преподов

    public JSONObject teachersSchedule = new JSONObject();

    //для errorScreen

    LinearLayout scheduleListError;
    LinearLayout teacherScheduleList;
    LinearLayout lessonsInfoList;
    LinearLayout notList;
    LinearLayout itogList;
    LinearLayout teachersList;

    //навигация

    EditText navigationInput;
    String lastNavText = "";



//    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener{
//        @Override
//        public boolean onDown(MotionEvent e) {
//            return true;
//        }
//        @Override
//        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
//            System.out.println(activeContainer);
//            if (e2.getX() - e1.getX() > 100) {
//                switch (activeContainer) {
//                    case SETTINGS: {
//                        setContainer(ContainerName.LESSONS);
//                        break;
//                    }
//                    case SCHEDULE: {
//                        setContainer(ContainerName.HOME);
//                        break;
//                    }
//                    case PROFILE: {
//                        setContainer(ContainerName.SCHEDULE);
//                        break;
//                    }
//                    case LESSONS: {
//                        setContainer(ContainerName.PROFILE);
//                        break;
//                    }
//                }
//                System.out.println("left to right");
//            }
//            if (e1.getX() - e2.getX() > 100) {
//                switch (activeContainer) {
//                    case HOME: {
//                        setContainer(ContainerName.SCHEDULE);
//                        break;
//                    }
//                    case SCHEDULE: {
//                        setContainer(ContainerName.PROFILE);
//                        break;
//                    }
//                    case PROFILE: {
//                        setContainer(ContainerName.LESSONS);
//                        break;
//                    }
//                    case LESSONS: {
//                        setContainer(ContainerName.SETTINGS);
//                        break;
//                    }
//                }
//                System.out.println("right to left");
//            }
//            return false;
//        }
//    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        System.out.println('\n');

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
        teacherScheduleScreen = findViewById(R.id.teacherScheduleScreen);
        lessonsScreen = findViewById(R.id.lessonsScreen);
        lessonsInformationScreen = findViewById(R.id.lessonsInformationScreen);
        userHelpScreen = findViewById(R.id.userHelp);
        notificationListScreen = findViewById(R.id.notificationListScreen);
        loadingScreen = findViewById(R.id.loadingScreen);
        settingsScreen = findViewById(R.id.settingsScreen);
        itogScreen = findViewById(R.id.itogScreen);
        errorScreen = findViewById(R.id.errorScreen);
        backConnectScreen = findViewById(R.id.backConnectScreen);
        teachersScreen = findViewById(R.id.teachersScreen);

        settingsSync = findViewById(R.id.settingsSync);
        settingsHelp = findViewById(R.id.settingsHelp);

        scheduleListError = findViewById(R.id.scheduleList);
        teacherScheduleList = findViewById(R.id.teacherScheduleList);
        lessonsInfoList = findViewById(R.id.lessonsInformationList);
        notList = findViewById(R.id.notificationList);
        itogList = findViewById(R.id.itogList);
        teachersList = findViewById(R.id.teachersList);

        // свайпы

//        final GestureDetectorCompat lSwipeDetector = new GestureDetectorCompat(this, new MyGestureListener());
//
//        main.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//
//        profileScreen.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//
//        navigation.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//
//        homeScreen.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//
//        scheduleScreen.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//
//        teacherScheduleScreen.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//
//        lessonsScreen.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//
//        lessonsInformationScreen.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//        userHelpScreen.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//        notificationListScreen.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//        settingsScreen.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//        itogScreen.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//        backConnectScreen.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });
//        teachersScreen.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return lSwipeDetector.onTouchEvent(event);
//            }
//        });

        // webView

        WebView gif = findViewById(R.id.loadingWebView);
//        WebSettings ws = gif.getSettings();
//        ws.setJavaScriptEnabled(true);
        gif.setBackgroundColor(Color.TRANSPARENT);
        gif.loadUrl("file:android_res/drawable/preloader.gif");

        // издержки

        ratingPlace = findViewById(R.id.ratePlace);
        ratingCount = findViewById(R.id.rateCount);

        //навигация

        navigationInput = findViewById(R.id.navigationInput);

        navigationInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                navigationInput.clearFocus();
                InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                return  false;
            }
        });

        navigationInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus) {
//                    lastNavText = navigationInput.getText().toString();
                   navigationInput.setText("");
                } else {
                    switch (navigationInput.getText().toString().toLowerCase()) {
                        case "user.profile": {
                            if (activeContainer != ContainerName.PROFILE) {
                                setContainer(ContainerName.PROFILE);
                                navigationInput.setText("User.Profile");
                            }
                            break;
                        }
                        case "user.schedule": {
                            if (activeContainer != ContainerName.SCHEDULE) {
                                setContainer(ContainerName.SCHEDULE);
                                navigationInput.setText("User.Schedule");
                            }
                            break;
                        }
                        case "user.home": {
                            if (activeContainer != ContainerName.HOME) {
                                setContainer(ContainerName.HOME);
                                navigationInput.setText("User.Home");
                            }
                            break;
                        }
                        case "user.changes": {
                            if (activeContainer != ContainerName.NOTIFICATION) {
                                setContainer(ContainerName.NOTIFICATION);
                                navigationInput.setText("User.Notification");
                            }
                            break;
                        }
                        case "user.lessons": {
                            if (activeContainer != ContainerName.LESSONS) {
                                setContainer(ContainerName.LESSONS);
                                navigationInput.setText("User.Lessons");
                            }
                            break;
                        }
                        case "user.settings": {
                            if (activeContainer != ContainerName.SETTINGS) {
                                setContainer(ContainerName.SETTINGS);
                                navigationInput.setText("User.Settings");
                            }
                            break;
                        }
                        default: {
                            switch (activeContainer) {
                                case NOTIFICATION: {
                                    navigationInput.setText("User.Changes");
                                    break;
                                }
                                case HOME: {
                                    navigationInput.setText("User.Home");
                                    break;
                                }
                                case ITOG: {
                                    navigationInput.setText("User.Lessons");
                                    break;
                                }
                                case LESSONS: {
                                    navigationInput.setText("User.Lessons");
                                    break;
                                }
                                case LESSONS_INFORMATION: {
                                    navigationInput.setText("User.Lessons");
                                    break;
                                }
                                case PROFILE: {
                                    navigationInput.setText("User.Profile");
                                    break;
                                }
                                case SCHEDULE: {
                                    navigationInput.setText("User.Schedule");
                                    break;
                                }
                                case SETTINGS: {
                                    navigationInput.setText("User.Settings");
                                    break;
                                }
                                case BACKCONNECT: {
                                    navigationInput.setText("User.Settings");
                                    break;
                                }
                            }
                            break;
                        }
                    }
//                    System.out.println("NAV: " + navigationInput.getText().toString());
                }
            }
        });

        //Выход из аккаунта

        LinearLayout settingsExit = findViewById(R.id.settingsExit);
        settingsExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetApp();
//                setContainer(ContainerName.LOGIN);
                setLoginFormContainer();
            }
        });

        // Инициализируем навигацию

        homeNavImg = findViewById(R.id.homeNavImg);
        homeNavText = findViewById(R.id.homeNavText);
        scheduleNavImg = findViewById(R.id.scheduleNavImg);
        scheduleNavText = findViewById(R.id.scheduleNavText);
        profileNavImg = findViewById(R.id.profileNavImg);
        profileNavText = findViewById(R.id.profileNavText);
        lessonsNavImg = findViewById(R.id.lessonsNavImg);
        lessonsNavText = findViewById(R.id.lessonsNavText);
        settingsNavImg = findViewById(R.id.settingsNavImg);
        settingsNavText = findViewById(R.id.settingsNavText);
        notificationNavImg = findViewById(R.id.notificationNavImg);
        notificationNavText = findViewById(R.id.notificationNavText);

        // и кнопки расписания

        scheduleDaysClickListener filtered = new scheduleDaysClickListener();

        monday = findViewById(R.id.monday);
        tuesday = findViewById(R.id.tuesday);
        wednesday = findViewById(R.id.wednesday);
        thursday = findViewById(R.id.thursday);
        friday = findViewById(R.id.friday);
        saturday = findViewById(R.id.saturday);

        monday.setOnClickListener(filtered);
        tuesday.setOnClickListener(filtered);
        wednesday.setOnClickListener(filtered);
        thursday.setOnClickListener(filtered);
        friday.setOnClickListener(filtered);
        saturday.setOnClickListener(filtered);

        // локальные кнопки экранов

//        scheduleChanges = findViewById(R.id.notificationSchedule);
        scheduleNow = findViewById(R.id.now);
        scheduleNext = findViewById(R.id.next);

        teacherNow = findViewById(R.id.teacherNow);
        teacherNext = findViewById(R.id.teacherNext);



        // инициаизируем переменные для очистки их при выходе (важно)

        profileUserName = findViewById(R.id.profileUserName);
        profileUserGroup = findViewById(R.id.profileUserGroup);
        profileUserCalendar = findViewById(R.id.profileUserCalendar);
        profileUserBalls = findViewById(R.id.profileUserBalls);
        profileUserBills = findViewById(R.id.profileUserBills);
        scheduleList = findViewById(R.id.scheduleList);
        lessonsList = findViewById(R.id.lessonsList);

        // локальные кнопки экранов
//        scheduleChanges = findVieawById(R.id.notificationSchedule);

        // запросы для расписания отправляются только 1 раз

        scheduleNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mondayIsActive) {
                    mondayIsActive = false;
                    monday.setTextColor(getResources().getColor(R.color.greyColor));
                }
                if (tuesdayIsActive) {
                    tuesdayIsActive = false;
                    tuesday.setTextColor(getResources().getColor(R.color.greyColor));
                }
                if (wednesdayIsActive) {
                    wednesdayIsActive = false;
                    wednesday.setTextColor(getResources().getColor(R.color.greyColor));
                }
                if (thursdayIsActive) {
                    thursdayIsActive = false;
                    thursday.setTextColor(getResources().getColor(R.color.greyColor));
                }
                if (fridayIsActive) {
                    fridayIsActive = false;
                    friday.setTextColor(getResources().getColor(R.color.greyColor));
                }
                if (saturdayIsActive) {
                    saturdayIsActive = false;
                    saturday.setTextColor(getResources().getColor(R.color.greyColor));
                }

                if (!nowWeekScheduleCalled) {
                    activeScheduleWeek = 0;
                    TextView nextView = findViewById(R.id.next);
                    nextView.setBackgroundResource(R.drawable.passive_schedule);
                    nextView.setTextColor(getResources().getColor(R.color.pinkColor));
                    TextView nowView = findViewById(v.getId());
                    nowView.setBackgroundResource(R.drawable.active_schedule);
                    nowView.setTextColor(getResources().getColor(R.color.backgroundMainColor));
                    sendGetScheduleOfGroupRequest(new String[] {"now"});
                    setLoadingToList(ContainerName.SCHEDULE);
                    nowWeekScheduleCalled = true;
                } else {
                    activeScheduleWeek = 0;
                    TextView nextView = findViewById(R.id.next);
                    nextView.setBackgroundResource(R.drawable.passive_schedule);
                    nextView.setTextColor(getResources().getColor(R.color.pinkColor));
                    TextView nowView = findViewById(v.getId());
                    nowView.setBackgroundResource(R.drawable.active_schedule);
                    nowView.setTextColor(getResources().getColor(R.color.backgroundMainColor));
                    LinearLayout scheduleList = findViewById(R.id.scheduleList);
                    scheduleList.removeAllViews();
                    setLoadingToList(ContainerName.SCHEDULE);
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    onGetScheduleOfGroupRequestCompleted("now", "");
                                }
                            });
                        }
                    }, 200);
                }
            }
        });

        scheduleNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mondayIsActive) {
                    mondayIsActive = false;
                    monday.setTextColor(getResources().getColor(R.color.greyColor));
                }
                if (tuesdayIsActive) {
                    tuesdayIsActive = false;
                    tuesday.setTextColor(getResources().getColor(R.color.greyColor));
                }
                if (wednesdayIsActive) {
                    wednesdayIsActive = false;
                    wednesday.setTextColor(getResources().getColor(R.color.greyColor));
                }
                if (thursdayIsActive) {
                    thursdayIsActive = false;
                    thursday.setTextColor(getResources().getColor(R.color.greyColor));
                }
                if (fridayIsActive) {
                    fridayIsActive = false;
                    friday.setTextColor(getResources().getColor(R.color.greyColor));
                }
                if (saturdayIsActive) {
                    saturdayIsActive = false;
                    saturday.setTextColor(getResources().getColor(R.color.greyColor));
                }

                if (!nextWeekScheduleCalled) {
                    activeScheduleWeek = 1;
                    TextView nextView = findViewById(R.id.now);
                    nextView.setBackgroundResource(R.drawable.passive_schedule);
                    nextView.setTextColor(getResources().getColor(R.color.pinkColor));
                    TextView nowView = findViewById(v.getId());
                    nowView.setBackgroundResource(R.drawable.active_schedule);
                    nowView.setTextColor(getResources().getColor(R.color.backgroundMainColor));
                    sendGetScheduleOfGroupRequest(new String[] {"next"});
                    setLoadingToList(ContainerName.SCHEDULE);
                    nextWeekScheduleCalled = true;
                } else {
                    activeScheduleWeek = 1;
                    TextView nextView = findViewById(R.id.now);
                    nextView.setBackgroundResource(R.drawable.passive_schedule);
                    nextView.setTextColor(getResources().getColor(R.color.pinkColor));
                    TextView nowView = findViewById(v.getId());
                    nowView.setBackgroundResource(R.drawable.active_schedule);
                    nowView.setTextColor(getResources().getColor(R.color.backgroundMainColor));
                    LinearLayout scheduleList = findViewById(R.id.scheduleList);
                    scheduleList.removeAllViews();
                    setLoadingToList(ContainerName.SCHEDULE);
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    onGetScheduleOfGroupRequestCompleted("next", "");
                                }
                            });
                        }
                    }, 200);
                }
            }
        });

        teacherNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!teacherNowWeek) {
                    sendGetScheduleOfTeacherRequest(new String[] {"now", currentTeacherId});
                    teacherNextWeek = false;
                    teacherNowWeek = true;
                    setLoadingToList(ContainerName.TEACHERSCHEDULE);
                    teacherNow.setBackgroundResource(R.drawable.active_schedule);
                    teacherNow.setTextColor(getResources().getColor(R.color.backgroundMainColor));
                    teacherNext.setBackgroundResource(R.drawable.passive_schedule);
                    teacherNext.setTextColor(getResources().getColor(R.color.pinkColor));
                }
            }
        });

        teacherNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!teacherNextWeek) {
                    sendGetScheduleOfTeacherRequest(new String[] {"next", currentTeacherId});
                    teacherNowWeek = false;
                    teacherNextWeek = true;
                    setLoadingToList(ContainerName.TEACHERSCHEDULE);
                    teacherNext.setBackgroundResource(R.drawable.active_schedule);
                    teacherNext.setTextColor(getResources().getColor(R.color.backgroundMainColor));
                    teacherNow.setBackgroundResource(R.drawable.passive_schedule);
                    teacherNow.setTextColor(getResources().getColor(R.color.pinkColor));
                }
            }
        });

        // ссылки с lessons

        RelativeLayout lessonsItogLink = findViewById(R.id.lessonsScreenItogLink);

        // ссылки с настроек

        RelativeLayout settingsBackConnectLink = findViewById(R.id.settingsBackConnectLink);

        // ссылки с главной

        RelativeLayout homeProfileLink = findViewById(R.id.homeProfileLink);
        RelativeLayout homeScheduleLink = findViewById(R.id.homeScheduleLink);
        RelativeLayout homeLessonsLink = findViewById(R.id.homeLessonsLink);
        RelativeLayout homeNotificationLink = findViewById(R.id.homeNotificationLink);
        RelativeLayout homeItogLink = findViewById(R.id.homeItogLink);
        RelativeLayout homeTeachersLink = findViewById(R.id.homeTeachersLink);
//        RelativeLayout homeAchivLink = findViewById(R.id.homeAchivLink);

        homeProfileLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContainer(ContainerName.PROFILE);
            }
        });
        homeScheduleLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContainer(ContainerName.SCHEDULE);
            }
        });
        homeLessonsLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContainer(ContainerName.LESSONS);
            }
        });
        homeNotificationLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendGetVKWallPostsRequest(new String[] {"40"});
                setContainer(ContainerName.NOTIFICATION);
                if (activeContainer == ContainerName.NOTIFICATION) {
                    LinearLayout box = findViewById(R.id.notificationList);
                    box.removeAllViews();
                    setLoadingToList(ContainerName.NOTIFICATION);
                }
            }
        });
        homeTeachersLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (!teachersScheduleIsReady) {
                    sendGetScheduleInfoRequest();
//                    teachersScheduleIsReady = true;
//                } else {
//                    onGetScheduleInfoRequestCompleted();
//                }
                setContainer(ContainerName.TEACHERS);
                setLoadingToList(ContainerName.TEACHERS);
            }
        });


        homeItogLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContainer(ContainerName.ITOG);
                // оценки за последний семестр
                if (!itogMarksAreReady) {
                    LinearLayout box = findViewById(R.id.itogList);
                    box.removeAllViews();
                    LinearLayout checker = findViewById(R.id.onClickItogInfo);
                    checker.setVisibility(View.INVISIBLE);
                    setLoadingToList(ContainerName.ITOG);

                    sendGetFinalMarksRequest();
                    // оценки за все семестры
                    sendGetAllFinalMarksRequest();
                    itogMarksAreReady = true;
                } else {
                    onGetFinalMarksRequestCompleted();
                    onGetAllFinalMarksRequestCompleted();
                }
            }
        });

        settingsBackConnectLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContainer(ContainerName.BACKCONNECT);
            }
        });

        lessonsItogLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContainer(ContainerName.ITOG);
                // оценки за последний семестр
                if (!itogMarksAreReady) {
                    LinearLayout box = findViewById(R.id.itogList);
                    box.removeAllViews();
                    LinearLayout checker = findViewById(R.id.onClickItogInfo);
                    checker.setVisibility(View.INVISIBLE);
                    setLoadingToList(ContainerName.ITOG);

                    sendGetFinalMarksRequest();
                    // оценки за все семестры
                    sendGetAllFinalMarksRequest();
                    itogMarksAreReady = true;
                } else {
                    onGetFinalMarksRequestCompleted();
                    onGetAllFinalMarksRequestCompleted();
                }
            }
        });


        // в начале убираем все экраны

        main.removeView(profileScreen);
        main.removeView(navigation);
        main.removeView(homeScreen);
        main.removeView(scheduleScreen);
        main.removeView(teacherScheduleScreen);
        main.removeView(lessonsScreen);
        main.removeView(lessonsInformationScreen);
        main.removeView(userHelpScreen);
        main.removeView(notificationListScreen);
        main.removeView(loginForm);
        main.removeView(settingsScreen);
        main.removeView(itogScreen);
        main.removeView(errorScreen);
        main.removeView(backConnectScreen);
        main.removeView(teachersScreen);
        activeContainer = ContainerName.LOADING;

        initAuth();

    }

    @Override
    public void onBackPressed() {

        if (activeContainer == ContainerName.PROFILE || activeContainer == ContainerName.LOGIN || activeContainer == ContainerName.ERROR) {
            super.onBackPressed();
            return;
        }

        if (activeContainer == ContainerName.LESSONS_INFORMATION) {
            setContainer(ContainerName.LESSONS);
            return;
        } else if (activeContainer == ContainerName.BACKCONNECT) {
            setContainer(ContainerName.SETTINGS);
            return;
        } else if (activeContainer == ContainerName.TEACHERSCHEDULE) {
            setContainer(ContainerName.TEACHERS);
            return;
        } else if (activeContainer == ContainerName.ITOG) {
            setContainer(ContainerName.LESSONS);
            return;
        } else if (activeContainer == ContainerName.TEACHERS) {
            setContainer(ContainerName.HOME);
            return;
        } else {
            setContainer(ContainerName.PROFILE);
            return;
        }
    }

    // Вывод на экран лоадинга текста

    public void loadingLog(String text) {

        try {
            TextView box = findViewById(R.id.loadingInfoText);
            box.setText(text);
        } catch (Exception e) {
//            System.out.println(e);
        }
    }

    public void setError(ContainerName check) {

        switch (check) {
            case TEACHERS: {
                teachersList.addView(errorScreen);
                break;
            }
            case NOTIFICATION: {
                LinearLayout notificationList = findViewById(R.id.notificationList);
                notificationList.addView(errorScreen);
                break;
            }
            case SCHEDULE: {
                LinearLayout scheduleList = findViewById(R.id.scheduleList);
                scheduleList.addView(errorScreen);
                nowWeekScheduleCalled = false;
                nextWeekScheduleCalled = false;
                break;
            }
            case TEACHERSCHEDULE: {
                LinearLayout scheduleList = findViewById(R.id.teacherScheduleList);
                scheduleList.addView(errorScreen);
                break;
            }
            case ITOG: {
                LinearLayout itogList = findViewById(R.id.itogList);
                itogList.addView(errorScreen);
                break;
            }
            case LESSONS_INFORMATION: {
                LinearLayout lessonsInformationList = findViewById(R.id.lessonsInformationList);
                lessonsInformationList.addView(errorScreen);
                break;
            }
        }
    }

    /* -------------------------------------------- BackEnd -------------------------------------------- */

    public void initAuth() {

        resetRequestsStatuses();

        preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        preferencesEditor = preferences.edit();

        appFirstRun = preferences.getBoolean("appFirstRun", true);
        isAuth = preferences.getBoolean("isAuth", false);

//        System.out.println(preferences.getAll().toString());

        if (appFirstRun) {
            System.out.println("App first run");
            preferencesEditor.putBoolean("appFirstRun", false);
            preferencesEditor.apply();
        }

        // вход еще не выполнен
        if (!isAuth) {
            setContainer(ContainerName.LOGIN);

            // получаем данные для отправки запроса

            final TextInputEditText login = findViewById(R.id.loginFormLogin);
            final TextInputEditText password = findViewById(R.id.loginFormPassword);
            final Button submit = findViewById(R.id.loginFormSubmit);

            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendLoginRequest(new String[] {
                            login.getText().toString(),
                            password.getText().toString()
                    });
                }
            });

//            System.out.println("Student not auth");
        } else {
//            System.out.println("Student auth");

            String lastLoginRequestTime = preferences.getString("lastLoginRequest", "");

            Date loginRequestDate = null;
            Date currentDate = new Date();

            try { loginRequestDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse(lastLoginRequestTime); }
            catch (ParseException e) {}

            long minutesBetweenDates = ((currentDate.getTime() / 60000) - (loginRequestDate.getTime() / 60000));
//            System.out.println("Last login request was " + minutesBetweenDates + " minutes ago");

            long lastSyncDate = preferences.getLong("lastSyncDate", 0);

            // требуется ли синхронизация
            if ((currentDate.getTime() - lastSyncDate) > (AUTH_SYNC_PERIOD * 60 * 1000)) {
//                System.out.println("Need to sync");
                String name = preferences.getString("studentName", "");
                String password = preferences.getString("studentPassword", "");
                sendLoginRequest(new String[] { name, password });
            }

            // вход был выполнен более COOKIE_LIFETIME минут назад, тогда нужно сделать запрос заново
            else if ( minutesBetweenDates >= COOKIE_LIFETIME) {
                setContainer(ContainerName.LOGIN);

//                System.out.println("Cookie lifetime is more then " + COOKIE_LIFETIME + " minutes. Sending new login request");

                String name = preferences.getString("studentName", "");
                String password = preferences.getString("studentPassword", "");

                // получение данных из профиля т.к. запрос на профиль отправлятся не будет
                studentGroup = preferences.getString("studentGroup", "");
                studentFIO = preferences.getString("studentFIO", "");
                studentAvatarSrc = preferences.getString("studentAvatarSrc", "");
                getStudentProfileDataRequestStatus = RequestStatus.COMPLETED;

                sendLoginRequest(new String[] { name, password });

            }
            // иначе пропускаем вход в аккаунт, вместо этого берем данные из хранилища
            else {
//                System.out.println("Cookie lifetime is less then " + COOKIE_LIFETIME + " minutes. Continue");
                authCookie = preferences.getString("authCookie", "");
                studentId = Functions.getStudentIdFromCookie(authCookie);

                studentGroup = preferences.getString("studentGroup", "");
                studentFIO = preferences.getString("studentFIO", "");
                studentAvatarSrc = preferences.getString("studentAvatarSrc", "");
                getStudentProfileDataRequestStatus = RequestStatus.COMPLETED;

//                    statsMidMark = preferences.getString("studentStatsMidMark", "");
//                    statsDebtsCount = preferences.getString("studentStatsDebtsCount", "");
//                    statsPercentageOfVisits = preferences.getString("studentStatsPercentageOfVisits", "");

                afterLoginRequest();
            }
        }
    }

    public void clearPreferences() {

        preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        preferencesEditor = preferences.edit();

        Boolean buf = preferences.getBoolean("appFirstRun", true);
        preferencesEditor.clear();
        preferencesEditor.putBoolean("appFirstRun", buf);
        preferencesEditor.apply();

    }

    public void afterLoginRequest() {

        Date date = new Date();
        String year = new SimpleDateFormat("yyyy").format(date);
        String month = new SimpleDateFormat("MM").format(date);
        String day = new SimpleDateFormat("dd").format(date);

        preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);

        final String name = preferences.getString("studentName", "");
        final String password = preferences.getString("studentPassword", "");

        settingsSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                main.removeAllViews();
                resetApp();
                sendLoginRequest(new String[] { name, password });
            }
        });

        settingsHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helpShow();
            }
        });

//        sendGetScheduleOfTeacherRequest(new String[] {"now", "t55"});


        // долги, посещения, средний балл
        sendGetStudentStatsRequest();

        // достижения
//        sendGetStudentAchievementsRequest();

        // если необходимо, парсим страницу с профилем
        if (getStudentProfileDataRequestStatus != RequestStatus.COMPLETED)
            sendGetStudentProfileDataRequest();

        // получение рейтинга
        sendRatingRequest(new String[] { name, password });

        // учителя, предметы
        sendGetStudentMainDataRequest(new String[]{ year, month });

        // пары за сегодня
        sendGetExercisesByDayRequest(new String[] { year + "-" + month + "-" + day }); // 2020-02-26
    }

    // Когда отпраили все запросы для входа в акаунт
    public void afterFirstRequests() {

        System.out.println("+--------");
        System.out.println("| Stats request: " + getStudentStatsRequestStatus);
        System.out.println("| Final marks request: " + getFinalMarksRequestStatus);
        System.out.println("| All final marks request: " + getAllFinalMarksRequestStatus);
        System.out.println("| Main data request: " + getStudentMainDataRequestStatus);
        System.out.println("| Student profile data request: " + getStudentProfileDataRequestStatus);
        System.out.println("| Rating request: " + ratingRequestStatus);
        System.out.println("| Exercises by day request: " + getExercisesByDayRequestStatus);
        System.out.println("+--------");

        preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        preferencesEditor = preferences.edit();

        if (
                getStudentStatsRequestStatus == RequestStatus.COMPLETED
                && getStudentMainDataRequestStatus    == RequestStatus.COMPLETED
                && getExercisesByDayRequestStatus     == RequestStatus.COMPLETED
//                && getFinalMarksRequestStatus         == RequestStatus.COMPLETED
//                && getAllFinalMarksRequestStatus      == RequestStatus.COMPLETED
//                && ratingRequestStatus                == RequestStatus.COMPLETED
                && getStudentProfileDataRequestStatus == RequestStatus.COMPLETED
        ) {
            isAuth = true;

            preferencesEditor.putBoolean("isAuth", true);
            preferencesEditor.putLong("lastSyncDate", (new Date()).getTime());
            preferencesEditor.apply();

            buildFrontend();
        }
        else if (isAuth) {
            setContainer(ContainerName.ERROR);
        }
        else if (!isAuth) {
            String name = preferences.getString("studentName", "");
            String password = preferences.getString("studentPassword", "");

            resetRequestsStatuses();
            clearPreferences();

            setLoginFormContainer(name, password);
        }
    }

    public void resetRequestsStatuses() {

        loginRequestStatus                    = RequestStatus.NOT_CALLED;
        getStudentMainDataRequestStatus       = RequestStatus.NOT_CALLED;
        getExercisesByDayRequestStatus        = RequestStatus.NOT_CALLED;
        getExercisesByLessonRequestStatus     = RequestStatus.NOT_CALLED;
        getVKWallPostsRequestStatus           = RequestStatus.NOT_CALLED;
        getStudentProfileDataRequestStatus    = RequestStatus.NOT_CALLED;
        getScheduleOfGroupRequestStatus       = RequestStatus.NOT_CALLED;
        getScheduleOfTeacherRequestStatus     = RequestStatus.NOT_CALLED;
        getScheduleInfoRequestStatus          = RequestStatus.NOT_CALLED;
        getStudentStatsRequestStatus          = RequestStatus.NOT_CALLED;
        getFinalMarksRequestStatus            = RequestStatus.NOT_CALLED;
        getAllFinalMarksRequestStatus         = RequestStatus.NOT_CALLED;
        ratingRequestStatus                   = RequestStatus.NOT_CALLED;
        getStudentAchievementsRequestStatus   = RequestStatus.NOT_CALLED;
    }

    public void resetApp() {

        profileUserName.setText("");
        profileUserGroup.setText("");
        profileUserCalendar.setText("");
        profileUserBalls.setText("");
        profileUserBills.setText("");
//        todayLessonsView.removeAllViews();
        todayLessonsView.removeAllViews();
        scheduleList.removeAllViews();
        lessonsList.removeAllViews();

        nowWeekScheduleCalled = false;
        nextWeekScheduleCalled = false;
        readyExercisesByLesson = new JSONObject();

        resetRequestsStatuses();
        clearPreferences();
        isAuth = false;

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

    // Функции по отправке запроса. Их нужно вызывать при жедании сделать запрос

    private void sendLoginRequest(String[] params) {

        setContainer(ContainerName.LOADING);
        main.removeView(errorScreen);
        main.removeView(itogScreen);
//        loginRequest request = new loginRequest();
//        loginRequestStatus = RequestStatus.CALLED;
//        request.execute(params);

        loginRequest = new LoginRequest();
        loginRequestStatus = RequestStatus.CALLED;
        loginRequest.execute(params);

    }

    private void sendGetStudentMainDataRequest(String[] params) {
//        getStudentMainDataRequest request = new getStudentMainDataRequest();
//        getStudentMainDataRequestStatus = RequestStatus.CALLED;
//        request.execute(params);

        getStudentMainDataRequestStatus = RequestStatus.CALLED;
        mainDataRequest = new GetStudentMainDataRequest();
        mainDataRequest.execute(params);
    }

    private void sendGetExercisesByDayRequest(String[] params) {
//        getExercisesByDayRequest request = new getExercisesByDayRequest();
//        getExercisesByDayRequestStatus = RequestStatus.CALLED;
//        request.execute(params);

        getExercisesByDayRequestStatus = RequestStatus.CALLED;
        exercisesByDayRequest = new GetExercisesByDayRequest();
        exercisesByDayRequest.execute(params);
    }

    private void sendGetExercisesByLessonRequest(String[] params) {
//        getExercisesByLessonRequest request = new getExercisesByLessonRequest();
//        getExercisesByLessonRequestStatus = RequestStatus.CALLED;
//        request.execute(params);

        getExercisesByLessonRequestStatus = RequestStatus.CALLED;
        exercisesByLessonRequest = new GetExercisesByLessonRequest();
        exercisesByLessonRequest.execute(params);
    }

    private void sendGetVKWallPostsRequest(String[] params) {
//        getVKWallPostsRequest request = new getVKWallPostsRequest();
//        getVKWallPostsRequestStatus = RequestStatus.CALLED;
//        request.execute(params);

        getVKWallPostsRequestStatus = RequestStatus.CALLED;
        vkPostsRequest = new GetVKWallPostsRequest();
        vkPostsRequest.execute(params);
    }

    private void sendGetStudentProfileDataRequest() {
//        getStudentProfileDataRequest request = new getStudentProfileDataRequest();
//        getStudentProfileDataRequestStatus = RequestStatus.CALLED;
//        request.execute();

        getStudentProfileDataRequestStatus = RequestStatus.CALLED;
        studentProfileRequest = new GetStudentProfileDataRequest();
        studentProfileRequest.execute();
    }

    private void sendGetStudentStatsRequest() {
//        getStudentStatsRequest request = new getStudentStatsRequest();
//        getStudentStatsRequestStatus = RequestStatus.CALLED;
//        request.execute();

        getStudentStatsRequestStatus = RequestStatus.CALLED;
        studentStatsRequest = new GetStudentStatsRequest();
        studentStatsRequest.execute();
    }

    private void sendGetFinalMarksRequest() {
//        getFinalMarksRequest request = new getFinalMarksRequest();
//        getFinalMarksRequestStatus = RequestStatus.CALLED;
//        request.execute();

        getFinalMarksRequestStatus = RequestStatus.CALLED;
        finalMarksRequest = new GetFinalMarksRequest();
        finalMarksRequest.execute();
    }

    private void sendGetAllFinalMarksRequest() {
//        getAllFinalMarksRequest request = new getAllFinalMarksRequest();
//        getAllFinalMarksRequestStatus = RequestStatus.CALLED;
//        request.execute();

        getAllFinalMarksRequestStatus = RequestStatus.CALLED;
        allFinalMarksRequest = new GetAllFinalMarksRequest();
        allFinalMarksRequest.execute();
    }

    private void sendRatingRequest(String[] params) {
//        RatingRequest request = new RatingRequest();
//        ratingRequestStatus = RequestStatus.CALLED;
//        request.execute(params);

        ratingRequestStatus = RequestStatus.CALLED;
        ratingRequest = new RatingRequest();
        ratingRequest.execute(params);
    }

    private void sendGetScheduleOfGroupRequest(String[] params) {
//        getScheduleOfGroupRequest request = new getScheduleOfGroupRequest();
//        getScheduleOfGroupRequestStatus = RequestStatus.CALLED;
//        request.execute(params);

        getScheduleOfGroupRequestStatus = RequestStatus.CALLED;
        scheduleOfGroupRequest = new GetScheduleOfGroupRequest();
        scheduleOfGroupRequest.execute(params);

    }

    private void sendGetScheduleOfTeacherRequest(String[] params) {
//        getScheduleOfTeacherRequest request = new getScheduleOfTeacherRequest();
//        getScheduleOfTeacherRequestStatus = RequestStatus.CALLED;
//        request.execute(params);

        getScheduleOfTeacherRequestStatus = RequestStatus.CALLED;
        scheduleOfTeacherRequest = new GetScheduleOfTeacherRequest();
        scheduleOfTeacherRequest.execute(params);
    }

    private void sendGetScheduleInfoRequest() {
//        getScheduleInfoRequest request = new getScheduleInfoRequest();
//        getScheduleInfoRequestStatus = RequestStatus.CALLED;
//        request.execute();

        getScheduleInfoRequestStatus = RequestStatus.CALLED;
        scheduleInfoRequest = new GetScheduleInfoRequest();
        scheduleInfoRequest.execute();
    }

    private void sendGetStudentAchievementsRequest() {

        getStudentAchievementsRequestStatus = RequestStatus.CALLED;
        achievementsRequest = new GetStudentAchievementsRequest();
        achievementsRequest.execute();
    }

    // Колбеки, которые вызываются при завершении определенного запроса

    public void onLoginRequestCompleted(String[] response) {

        String cookie = response[0];
        String studentName = response[1];
        String studentPassword = response[2];

        if (!cookie.isEmpty()) {
            loginRequestStatus = RequestStatus.COMPLETED;

            authCookie = cookie;
            studentId = Functions.getStudentIdFromCookie(authCookie);

//            System.out.println("Login success!");
//            System.out.println("AuthCookie: " + authCookie);

            preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
            preferencesEditor = preferences.edit();

            String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
            preferencesEditor.putString("lastLoginRequest", currentDate);
            preferencesEditor.putString("authCookie", authCookie);
            preferencesEditor.putString("studentName", studentName);
            preferencesEditor.putString("studentPassword", studentPassword);
            preferencesEditor.apply();

            afterLoginRequest();

        } else if ((loginRequestStatus != RequestStatus.FAILED) && (loginRequestStatus != RequestStatus.TIMEOUT)) {
            clearPreferences();
            resetRequestsStatuses();
            setLoginFormContainer(studentName, "");

        }
        else {
//            loginRequestStatus = RequestStatus.EMPTY_RESPONSE;
//            System.out.println("Login request failed (timeout or failed)!");

            if (isAuth) {
                setContainer(ContainerName.ERROR);
            } else {
                clearPreferences();
                resetRequestsStatuses();
                setLoginFormContainer(studentName, studentPassword);
            }
        }
    }

    public void onGetStudentMainDataRequestCompleted(String responseBody) {

        if (getStudentMainDataRequestStatus == RequestStatus.TIMEOUT) {

        }
        else if (getStudentMainDataRequestStatus == RequestStatus.FAILED) {

        }
        else if (!responseBody.isEmpty()) {
            getStudentMainDataRequestStatus = RequestStatus.COMPLETED;

//            System.out.println("GetStudentMainData Success!");
            JSONObject jsonData;
            try {
                jsonData = new JSONObject(responseBody);

                studentLessons = jsonData.getJSONArray("userlessons");
                teachers = jsonData.getJSONObject("lessonteachers");

//                System.out.println("StudentLessons: " + studentLessons.toString());
//                System.out.println("Teachers: " + teachers.toString());

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
//            System.out.println("Student main data request empty response!");
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

//            System.out.println("GetExercisesByDay Success!");
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
//                System.out.println(e.toString());
            }
        } else {
            getExercisesByDayRequestStatus = RequestStatus.EMPTY_RESPONSE;
//            System.out.println("Exercises by day request empty response!");
        }

        afterFirstRequests();
    }

    public void onGetExercisesByLessonRequestCompleted (String[] response) {

        String responseBody = response[0];
        String lessonId = response[1];

        if (getExercisesByLessonRequestStatus == RequestStatus.TIMEOUT) {
            LinearLayout lessonsInformationList = findViewById(R.id.lessonsInformationList);
            lessonsInformationList.removeAllViews();
            setError(ContainerName.LESSONS_INFORMATION);
        }
        else if (getExercisesByLessonRequestStatus == RequestStatus.FAILED) {
            LinearLayout lessonsInformationList = findViewById(R.id.lessonsInformationList);
            lessonsInformationList.removeAllViews();
            setError(ContainerName.LESSONS_INFORMATION);
        }
        else if (!responseBody.isEmpty() && activeContainer == ContainerName.LESSONS_INFORMATION) {
            getExercisesByLessonRequestStatus = RequestStatus.COMPLETED;

//            System.out.println("GetExercisesByLesson Success!");
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
                        String performance = valueInfo.getString("performance").equals("null") ? "нет" : "▲";
                        if (valueInfo.getString("performance").equals("2")) performance = "▼";



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

//            System.out.println("readyExercisesByLesson: "  + readyExercisesByLesson.toString());
//            System.out.println("readyExercisesByLessonVisits: "  + readyExercisesByLessonVisits.toString());

        } else {
            getExercisesByLessonRequestStatus = RequestStatus.EMPTY_RESPONSE;
//            System.out.println("Exercises by lesson request empty response!");
        }
    }


    public void onGetVKWallPostsRequestCompleted (String responseBody) {

        if (activeContainer != ContainerName.NOTIFICATION) return;

        if (getVKWallPostsRequestStatus == RequestStatus.TIMEOUT) {
            LinearLayout notificationList = findViewById(R.id.notificationList);
            notificationList.removeAllViews();
            setError(ContainerName.NOTIFICATION);
        }
        else if (getVKWallPostsRequestStatus == RequestStatus.FAILED) {
            LinearLayout notificationList = findViewById(R.id.notificationList);
            notificationList.removeAllViews();
            setError(ContainerName.NOTIFICATION);
        }
        else if (!responseBody.isEmpty() && activeContainer == ContainerName.NOTIFICATION) {

            LinearLayout notificationList = findViewById(R.id.notificationList);
            notificationList.removeAllViews();
            getVKWallPostsRequestStatus = RequestStatus.COMPLETED;

//            System.out.println("GetVKWallPosts Success!");
            JSONObject jsonData;
            try {
                jsonData = new JSONObject(responseBody);

                vkWallPosts = jsonData.getJSONObject("response");

//                System.out.println("vkWallPosts: " + vkWallPosts.toString());

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
//                        System.out.println("current time: " + stamp);

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
                            String dateText = new SimpleDateFormat("dd MMMM, HH:mm", Locale.getDefault()).format(date);

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
//            System.out.println("VK wall posts request empty response!");
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

//            preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
//            preferencesEditor = preferences.edit();
//
//            preferencesEditor.putString("studentFIO", studentFIO);
//            preferencesEditor.putString("studentGroup", studentGroup);
//            preferencesEditor.putString("studentAvatarSrc", studentAvatarSrc);
//
//            preferencesEditor.putString("studentStatsMidMark", statsMidMark);
//            preferencesEditor.putString("studentStatsDebtsCount", statsDebtsCount);
//            preferencesEditor.putString("studentStatsPercentageOfVisits", statsPercentageOfVisits);
//
//            preferencesEditor.apply();
//
//            this.studentFIO = studentFIO;
//            this.studentGroup = studentGroup;
//            this.studentAvatarSrc = studentAvatarSrc;

//            if (getExercisesByDayRequestStatus == RequestStatus.COMPLETED && getStudentMainDataRequestStatus == RequestStatus.COMPLETED && !buildFrontendCalled) {
//                buildFrontendCalled = true;
//                buildFrontend();
//            }
        } else {
            getStudentProfileDataRequestStatus = RequestStatus.EMPTY_RESPONSE;
//            System.out.println("Student profile's data request empty response!");
        }
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

//            System.out.println("StudentStats Success!");

            this.statsMidMark = statsMidMark;
            this.statsDebtsCount = statsDebtsCount;
            this.statsPercentageOfVisits = statsPercentageOfVisits;

            preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
            preferencesEditor = preferences.edit();

            preferencesEditor.putString("studentStatsMidMark", statsMidMark);
            preferencesEditor.putString("studentStatsDebtsCount", statsDebtsCount);
            preferencesEditor.putString("studentStatsPercentageOfVisits", statsPercentageOfVisits);

            preferencesEditor.apply();

        } else {
            getStudentStatsRequestStatus = RequestStatus.EMPTY_RESPONSE;
//            System.out.println("Student stats request empty response!");
        }
    }

    public void onGetFinalMarksRequestCompleted() {

        if (getFinalMarksRequestStatus == RequestStatus.CALLED) {
            getFinalMarksRequestStatus = RequestStatus.COMPLETED;

            if (activeContainer != ContainerName.ITOG) {
                itogMarksAreReady = false;

                return;
            }

            if (getFinalMarksRequestStatus == RequestStatus.COMPLETED && getAllFinalMarksRequestStatus == RequestStatus.COMPLETED) {
                LinearLayout checker = findViewById(R.id.onClickItogInfo);
                checker.setVisibility(View.VISIBLE);
            TextView itogLessonsSem = findViewById(R.id.itogLessonsSem);
            itogLessonsSem.setText(finalMarksSemestr);

            int dp = (int) getResources().getDisplayMetrics().density;
            Typeface light = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_light);
            Typeface medium = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_medium);
            Typeface semibold = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_semibold);
            Typeface regular = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_regular);

            LinearLayout itogList = findViewById(R.id.itogList);
            itogList.removeAllViews();

            for (int i = 0; i < studentFinalMarks.length(); i++) {

                RelativeLayout tmp = new RelativeLayout(getApplicationContext());
                RelativeLayout.LayoutParams tmpLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                tmpLP.setMargins(0, 5 * dp, 0, 0);
                tmp.setLayoutParams(tmpLP);
                tmp.setBackgroundResource(R.drawable.forms_example);
                itogList.addView(tmp);

                LinearLayout tempBox = new LinearLayout(getApplicationContext());
                LinearLayout.LayoutParams tempBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                tempBoxLP.setMargins(5 * dp, 15 * dp, 5 * dp, 15 * dp);
                tempBox.setLayoutParams(tempBoxLP);
                tempBox.setOrientation(LinearLayout.HORIZONTAL);
                tmp.addView(tempBox);

                TextView tempName = new TextView(getApplicationContext());
                LinearLayout.LayoutParams tempNameLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2);
                tempName.setLayoutParams(tempNameLP);
                tempName.setTextSize(12);
                tempName.setTextColor(getResources().getColor(R.color.white));
                tempName.setTypeface(medium);
                try {
                    tempName.setText(studentFinalMarks.getJSONObject(i).getString("name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                tempBox.addView(tempName);

                TextView tempAbs = new TextView(getApplicationContext());
                LinearLayout.LayoutParams tempAbsLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 3);
                tempAbs.setLayoutParams(tempAbsLP);
                tempAbs.setTextSize(12);
                tempAbs.setTextColor(getResources().getColor(R.color.pinkColor));
                tempAbs.setTypeface(semibold);
                tempAbs.setGravity(Gravity.CENTER);
                try {
                    String was = studentFinalMarks.getJSONObject(i).getString("was");
                    was = was.split(" ")[0];
                    String all = studentFinalMarks.getJSONObject(i).getString("all");
                    all = all.split(" ")[0];
                    tempAbs.setText(was + "/" + all);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                tempBox.addView(tempAbs);

                TextView tempMark = new TextView(getApplicationContext());
                LinearLayout.LayoutParams tempMarkLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 3);
                tempMark.setLayoutParams(tempMarkLP);
                tempMark.setTextSize(12);
                tempMark.setTextColor(getResources().getColor(R.color.pinkColor));
                tempMark.setTypeface(semibold);
                tempMark.setGravity(Gravity.CENTER);
                try {
                    tempMark.setText(studentFinalMarks.getJSONObject(i).getString("mark"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                tempBox.addView(tempMark);
            }
            for (int j = studentAllFinalMarks.length() - 3; j >= 0; j--) {

                TextView itogSem = new TextView(getApplicationContext());
                LinearLayout.LayoutParams itogSemLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                itogSemLP.setMargins(5 * dp, 20 * dp, 0, 0);
                itogSem.setLayoutParams(itogSemLP);
                itogSem.setTextColor(getResources().getColor(R.color.pinkColor));
                itogSem.setTypeface(medium);
                itogSem.setTextSize(12);
                try {
                    itogSem.setText(studentAllFinalMarks.getJSONObject(j).getString("semester"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                itogList.addView(itogSem);

                int len = 0;
                try {
                    len = studentAllFinalMarks.getJSONObject(j).getJSONArray("lessons").length();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                for (int i = 0; i < len; i++) {

                    RelativeLayout tmp = new RelativeLayout(getApplicationContext());
                    RelativeLayout.LayoutParams tmpLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    tmpLP.setMargins(0, 5 * dp, 0, 0);
                    tmp.setLayoutParams(tmpLP);
                    tmp.setBackgroundResource(R.drawable.forms_example);
                    itogList.addView(tmp);

                    LinearLayout tempBox = new LinearLayout(getApplicationContext());
                    LinearLayout.LayoutParams tempBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    tempBoxLP.setMargins(5 * dp, 15 * dp, 5 * dp, 15 * dp);
                    tempBox.setLayoutParams(tempBoxLP);
                    tempBox.setOrientation(LinearLayout.HORIZONTAL);
                    tmp.addView(tempBox);

                    TextView tempName = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams tempNameLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2);
                    tempName.setLayoutParams(tempNameLP);
                    tempName.setTextSize(12);
                    tempName.setTextColor(getResources().getColor(R.color.white));
                    tempName.setTypeface(medium);
                    try {
                        tempName.setText(studentAllFinalMarks.getJSONObject(j).getJSONArray("lessons").getJSONObject(i).getString("name"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    tempBox.addView(tempName);

                    TextView tempAbs = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams tempAbsLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 3);
                    tempAbs.setLayoutParams(tempAbsLP);
                    tempAbs.setTextSize(12);
                    tempAbs.setTextColor(getResources().getColor(R.color.pinkColor));
                    tempAbs.setTypeface(semibold);
                    tempAbs.setGravity(Gravity.CENTER);
                    tempAbs.setText("");
                    tempBox.addView(tempAbs);

                    TextView tempMark = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams tempMarkLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 3);
                    tempMark.setLayoutParams(tempMarkLP);
                    tempMark.setTextSize(12);
                    tempMark.setTextColor(getResources().getColor(R.color.pinkColor));
                    tempMark.setTypeface(semibold);
                    tempMark.setGravity(Gravity.CENTER);
                    try {
                        tempMark.setText(studentAllFinalMarks.getJSONObject(j).getJSONArray("lessons").getJSONObject(i).getString("mark"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    tempBox.addView(tempMark);
                }
            }
        }
        }
        else if (getFinalMarksRequestStatus == RequestStatus.FAILED) {
            LinearLayout box = findViewById(R.id.itogList);
            box.removeAllViews();
            setError(ContainerName.ITOG);
        }
        else if (getFinalMarksRequestStatus == RequestStatus.TIMEOUT) {
            LinearLayout box = findViewById(R.id.itogList);
            box.removeAllViews();
            setError(ContainerName.ITOG);
        }
        else {

        }
    }

    public void onGetAllFinalMarksRequestCompleted() {

        if (getAllFinalMarksRequestStatus == RequestStatus.CALLED) {
            getAllFinalMarksRequestStatus = RequestStatus.COMPLETED;

            if (activeContainer != ContainerName.ITOG) {
                itogMarksAreReady = false;
                return;
            }

            if (getFinalMarksRequestStatus == RequestStatus.COMPLETED && getAllFinalMarksRequestStatus == RequestStatus.COMPLETED) {
                LinearLayout checker = findViewById(R.id.onClickItogInfo);
                checker.setVisibility(View.VISIBLE);

                TextView itogLessonsSem = findViewById(R.id.itogLessonsSem);
                itogLessonsSem.setText(finalMarksSemestr);

                int dp = (int) getResources().getDisplayMetrics().density;
                Typeface light = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_light);
                Typeface medium = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_medium);
                Typeface semibold = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_semibold);
                Typeface regular = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_regular);

                LinearLayout itogList = findViewById(R.id.itogList);
                itogList.removeAllViews();

                for (int i = 0; i < studentFinalMarks.length(); i++) {

                    RelativeLayout tmp = new RelativeLayout(getApplicationContext());
                    RelativeLayout.LayoutParams tmpLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    tmpLP.setMargins(0, 5 * dp, 0, 0);
                    tmp.setLayoutParams(tmpLP);
                    tmp.setBackgroundResource(R.drawable.forms_example);
                    itogList.addView(tmp);

                    LinearLayout tempBox = new LinearLayout(getApplicationContext());
                    LinearLayout.LayoutParams tempBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    tempBoxLP.setMargins(5 * dp, 15 * dp, 5 * dp, 15 * dp);
                    tempBox.setLayoutParams(tempBoxLP);
                    tempBox.setOrientation(LinearLayout.HORIZONTAL);
                    tmp.addView(tempBox);

                    TextView tempName = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams tempNameLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2);
                    tempName.setLayoutParams(tempNameLP);
                    tempName.setTextSize(12);
                    tempName.setTextColor(getResources().getColor(R.color.white));
                    tempName.setTypeface(medium);
                    try {
                        tempName.setText(studentFinalMarks.getJSONObject(i).getString("name"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    tempBox.addView(tempName);

                    TextView tempAbs = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams tempAbsLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 3);
                    tempAbs.setLayoutParams(tempAbsLP);
                    tempAbs.setTextSize(12);
                    tempAbs.setTextColor(getResources().getColor(R.color.pinkColor));
                    tempAbs.setTypeface(semibold);
                    tempAbs.setGravity(Gravity.CENTER);
                    try {
                        String was = studentFinalMarks.getJSONObject(i).getString("was");
                        was = was.split(" ")[0];
                        String all = studentFinalMarks.getJSONObject(i).getString("all");
                        all = all.split(" ")[0];
                        tempAbs.setText(was + "/" + all);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    tempBox.addView(tempAbs);

                    TextView tempMark = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams tempMarkLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 3);
                    tempMark.setLayoutParams(tempMarkLP);
                    tempMark.setTextSize(12);
                    tempMark.setTextColor(getResources().getColor(R.color.pinkColor));
                    tempMark.setTypeface(semibold);
                    tempMark.setGravity(Gravity.CENTER);
                    try {
                        tempMark.setText(studentFinalMarks.getJSONObject(i).getString("mark"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    tempBox.addView(tempMark);
                }
                for (int j = studentAllFinalMarks.length() - 3; j >= 0; j--) {

                    TextView itogSem = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams itogSemLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    itogSemLP.setMargins(5 * dp, 20 * dp, 0, 0);
                    itogSem.setLayoutParams(itogSemLP);
                    itogSem.setTextColor(getResources().getColor(R.color.pinkColor));
                    itogSem.setTypeface(medium);
                    itogSem.setTextSize(12);
                    try {
                        itogSem.setText(studentAllFinalMarks.getJSONObject(j).getString("semester"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    itogList.addView(itogSem);

                    int len = 0;
                    try {
                        len = studentAllFinalMarks.getJSONObject(j).getJSONArray("lessons").length();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    for (int i = 0; i < len; i++) {

                        RelativeLayout tmp = new RelativeLayout(getApplicationContext());
                        RelativeLayout.LayoutParams tmpLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        tmpLP.setMargins(0, 5 * dp, 0, 0);
                        tmp.setLayoutParams(tmpLP);
                        tmp.setBackgroundResource(R.drawable.forms_example);
                        itogList.addView(tmp);

                        LinearLayout tempBox = new LinearLayout(getApplicationContext());
                        LinearLayout.LayoutParams tempBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        tempBoxLP.setMargins(5 * dp, 15 * dp, 5 * dp, 15 * dp);
                        tempBox.setLayoutParams(tempBoxLP);
                        tempBox.setOrientation(LinearLayout.HORIZONTAL);
                        tmp.addView(tempBox);

                        TextView tempName = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams tempNameLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2);
                        tempName.setLayoutParams(tempNameLP);
                        tempName.setTextSize(12);
                        tempName.setTextColor(getResources().getColor(R.color.white));
                        tempName.setTypeface(medium);
                        try {
                            tempName.setText(studentAllFinalMarks.getJSONObject(j).getJSONArray("lessons").getJSONObject(i).getString("name"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        tempBox.addView(tempName);

                        TextView tempAbs = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams tempAbsLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 3);
                        tempAbs.setLayoutParams(tempAbsLP);
                        tempAbs.setTextSize(12);
                        tempAbs.setTextColor(getResources().getColor(R.color.pinkColor));
                        tempAbs.setTypeface(semibold);
                        tempAbs.setGravity(Gravity.CENTER);
                        tempAbs.setText("");
                        tempBox.addView(tempAbs);

                        TextView tempMark = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams tempMarkLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 3);
                        tempMark.setLayoutParams(tempMarkLP);
                        tempMark.setTextSize(12);
                        tempMark.setTextColor(getResources().getColor(R.color.pinkColor));
                        tempMark.setTypeface(semibold);
                        tempMark.setGravity(Gravity.CENTER);
                        try {
                            tempMark.setText(studentAllFinalMarks.getJSONObject(j).getJSONArray("lessons").getJSONObject(i).getString("mark"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        tempBox.addView(tempMark);
                    }
                }
            }
        }
        else if (getAllFinalMarksRequestStatus == RequestStatus.FAILED) {
            LinearLayout box = findViewById(R.id.itogList);
            box.removeAllViews();
            setError(ContainerName.ITOG);
            itogMarksAreReady = false;
        }
        else if (getAllFinalMarksRequestStatus == RequestStatus.TIMEOUT) {
            LinearLayout box = findViewById(R.id.itogList);
            box.removeAllViews();
            setError(ContainerName.ITOG);
            itogMarksAreReady = false;
        }
        else {

        }
    }

    public void onRatingRequestCompleted(String response) {
        if (!response.isEmpty()) {
            ratingRequestStatus = RequestStatus.COMPLETED;

            try {
                 ratingInfo = new JSONObject(response);
//                 System.out.println(ratingInfo);

                 ratingPlace.setText(ratingInfo.getString("studentPosition"));
                 ratingCount.setText(ratingInfo.getString("studentsCount"));
            } catch (JSONException e) {

            }
        }
        else if (ratingRequestStatus == RequestStatus.FAILED) {
            ratingPlace.setText("F");
        }
        else if (ratingRequestStatus == RequestStatus.TIMEOUT) {
            ratingPlace.setText("T");
        }
        else {
            ratingRequestStatus = RequestStatus.EMPTY_RESPONSE;
            ratingPlace.setText("E");
        }
    }


    public void onGetScheduleOfGroupRequestCompleted(String param, String filter) {

        if (activeContainer != ContainerName.SCHEDULE) {
            if (param == "now") nowWeekScheduleCalled = false;
            if (param == "false") nextWeekScheduleCalled = false;
            return;
        }

        if (getScheduleOfGroupRequestStatus == RequestStatus.TIMEOUT) {
            LinearLayout box = findViewById(R.id.scheduleList);
            box.removeAllViews();
            setError(ContainerName.SCHEDULE);
        }
        else if (getScheduleOfGroupRequestStatus == RequestStatus.FAILED) {
            LinearLayout box = findViewById(R.id.scheduleList);
            box.removeAllViews();
            setError(ContainerName.SCHEDULE);
        }
        else if (getScheduleOfGroupRequestStatus == RequestStatus.COMPLETED && activeContainer == ContainerName.SCHEDULE) {
            LinearLayout box = findViewById(R.id.scheduleList);
            box.removeAllViews();

            int dp = (int) getResources().getDisplayMetrics().density;

            Typeface light = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_light);
            Typeface medium = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_medium);
            Typeface semibold = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_semibold);
            Typeface regular = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_regular);


            JSONArray value = new JSONArray();

            try {
                value = groupScheduleLessons.getJSONArray(param);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (filter.equals("")) {
                for (int i = 0; i < value.length(); i++) {

                    TextView dayOfWeekName = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams dayOfWeekLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    dayOfWeekLP.setMargins(15 * dp, 0, 0, 3 * dp);
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
                    forLessonsOfTheDayBoxLP.setMargins(0, 0, 0, 10 * dp);
                    forLessonsOfTheDayBox.setLayoutParams(forLessonsOfTheDayBoxLP);
                    forLessonsOfTheDayBox.setBackgroundResource(R.drawable.forms_example);
                    forLessonsOfTheDayBox.setPadding(0, 0, 0, 5 * dp);
                    forLessonsOfTheDayBox.setOrientation(LinearLayout.VERTICAL);
                    box.addView(forLessonsOfTheDayBox);

                    JSONArray temp = new JSONArray();
                    try {
                        temp = value.getJSONArray(i);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (temp.length() == 0) {
                        TextView nullText = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams nullTextLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        nullTextLP.setMargins(0,25*dp,0,25*dp);
                        nullText.setLayoutParams(nullTextLP);
                        nullText.setGravity(Gravity.CENTER);
                        nullText.setTextSize(12);
                        nullText.setTypeface(light);
                        nullText.setText("Занятий нет");
                        nullText.setTextColor(getResources().getColor(R.color.greyColor));
                        forLessonsOfTheDayBox.addView(nullText);
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
                            LinearLayout.LayoutParams aboutLessonsOfTheDayBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            aboutLessonsOfTheDayBoxLP.setMargins(0, 5 * dp, 0, 5 * dp);
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
                                case "VII": {
                                    pos = "7";
                                    break;
                                }
                            }

                            TextView theDayLessonsCounter = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams theDayLessonsCounterLP = new LinearLayout.LayoutParams(20 * dp, 20 * dp);
                            theDayLessonsCounterLP.setMargins(12 * dp, 10 * dp, 0, 0);
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
                            LinearLayout.LayoutParams theDayLessonsCabLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20 * dp);
                            theDayLessonsCabLP.setMargins(0, -20 * dp, 12 * dp, 0);
                            theDayLessonsCab.setLayoutParams(theDayLessonsCabLP);
                            theDayLessonsCab.setText(tmp.getString("room"));
                            theDayLessonsCab.setTypeface(medium);
                            theDayLessonsCab.setTextColor(getResources().getColor(R.color.white));
                            theDayLessonsCab.setGravity(Gravity.RIGHT);
                            theDayLessonsCab.setTextSize(12);
                            aboutLessonsOfTheDayBox.addView(theDayLessonsCab);

                            TextView theDayLessonsTime = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams theDayLessonsTimeLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 20 * dp);
                            theDayLessonsTimeLP.setMargins(40 * dp, -21 * dp, 0, 0);
                            theDayLessonsTime.setLayoutParams(theDayLessonsTimeLP);
                            theDayLessonsTime.setText(tmp.getString("start") + " - " + tmp.getString("end"));
                            theDayLessonsTime.setTypeface(light);
                            theDayLessonsTime.setTextColor(getResources().getColor(R.color.greyColor));
                            theDayLessonsTime.setGravity(Gravity.CENTER_VERTICAL);
                            theDayLessonsTime.setTextSize(12);
                            aboutLessonsOfTheDayBox.addView(theDayLessonsTime);

                            TextView theDayLessonsNameOfLesson = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams theDayLessonsNameOfLessonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            theDayLessonsNameOfLessonLP.setMargins(40 * dp, 5 * dp, 40 * dp, 0);
                            theDayLessonsNameOfLesson.setLayoutParams(theDayLessonsNameOfLessonLP);
                            theDayLessonsNameOfLesson.setText(tmp.getString("name"));
                            theDayLessonsNameOfLesson.setTypeface(medium);
                            theDayLessonsNameOfLesson.setTextColor(getResources().getColor(R.color.white));
                            theDayLessonsNameOfLesson.setGravity(Gravity.CENTER_VERTICAL);
                            theDayLessonsNameOfLesson.setTextSize(13);
                            aboutLessonsOfTheDayBox.addView(theDayLessonsNameOfLesson);

                            TextView theDayLessonsTeacher = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams theDayLessonsTeacherLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            theDayLessonsTeacherLP.setMargins(40 * dp, 0, 40 * dp, 5 * dp);
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
                for (int i = Integer.parseInt(filter); i < Integer.parseInt(filter)+1; i++) {

                    TextView dayOfWeekName = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams dayOfWeekLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    dayOfWeekLP.setMargins(15 * dp, 0, 0, 3 * dp);
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
                    forLessonsOfTheDayBoxLP.setMargins(0, 0, 0, 10 * dp);
                    forLessonsOfTheDayBox.setLayoutParams(forLessonsOfTheDayBoxLP);
                    forLessonsOfTheDayBox.setBackgroundResource(R.drawable.forms_example);
                    forLessonsOfTheDayBox.setPadding(0, 0, 0, 5 * dp);
                    forLessonsOfTheDayBox.setOrientation(LinearLayout.VERTICAL);
                    box.addView(forLessonsOfTheDayBox);

                    JSONArray temp = new JSONArray();
                    try {
                        temp = value.getJSONArray(i);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    
                    if (temp.length() == 0) {
                        TextView nullText = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams nullTextLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        nullTextLP.setMargins(0,25*dp,0,25*dp);
                        nullText.setLayoutParams(nullTextLP);
                        nullText.setGravity(Gravity.CENTER);
                        nullText.setTextSize(12);
                        nullText.setTypeface(light);
                        nullText.setText("Занятий нет");
                        nullText.setTextColor(getResources().getColor(R.color.greyColor));
                        forLessonsOfTheDayBox.addView(nullText);
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
                            LinearLayout.LayoutParams aboutLessonsOfTheDayBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            aboutLessonsOfTheDayBoxLP.setMargins(0, 5 * dp, 0, 5 * dp);
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
                                case "VII": {
                                    pos = "7";
                                    break;
                                }
                            }

                            TextView theDayLessonsCounter = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams theDayLessonsCounterLP = new LinearLayout.LayoutParams(20 * dp, 20 * dp);
                            theDayLessonsCounterLP.setMargins(12 * dp, 10 * dp, 0, 0);
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
                            LinearLayout.LayoutParams theDayLessonsCabLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20 * dp);
                            theDayLessonsCabLP.setMargins(0, -20 * dp, 12 * dp, 0);
                            theDayLessonsCab.setLayoutParams(theDayLessonsCabLP);
                            theDayLessonsCab.setText(tmp.getString("room"));
                            theDayLessonsCab.setTypeface(medium);
                            theDayLessonsCab.setTextColor(getResources().getColor(R.color.white));
                            theDayLessonsCab.setGravity(Gravity.RIGHT);
                            theDayLessonsCab.setTextSize(12);
                            aboutLessonsOfTheDayBox.addView(theDayLessonsCab);

                            TextView theDayLessonsTime = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams theDayLessonsTimeLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 20 * dp);
                            theDayLessonsTimeLP.setMargins(40 * dp, -21 * dp, 0, 0);
                            theDayLessonsTime.setLayoutParams(theDayLessonsTimeLP);
                            theDayLessonsTime.setText(tmp.getString("start") + " - " + tmp.getString("end"));
                            theDayLessonsTime.setTypeface(light);
                            theDayLessonsTime.setTextColor(getResources().getColor(R.color.greyColor));
                            theDayLessonsTime.setGravity(Gravity.CENTER_VERTICAL);
                            theDayLessonsTime.setTextSize(12);
                            aboutLessonsOfTheDayBox.addView(theDayLessonsTime);

                            TextView theDayLessonsNameOfLesson = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams theDayLessonsNameOfLessonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            theDayLessonsNameOfLessonLP.setMargins(40 * dp, 5 * dp, 40 * dp, 0);
                            theDayLessonsNameOfLesson.setLayoutParams(theDayLessonsNameOfLessonLP);
                            theDayLessonsNameOfLesson.setText(tmp.getString("name"));
                            theDayLessonsNameOfLesson.setTypeface(medium);
                            theDayLessonsNameOfLesson.setTextColor(getResources().getColor(R.color.white));
                            theDayLessonsNameOfLesson.setGravity(Gravity.CENTER_VERTICAL);
                            theDayLessonsNameOfLesson.setTextSize(13);
                            aboutLessonsOfTheDayBox.addView(theDayLessonsNameOfLesson);

                            TextView theDayLessonsTeacher = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams theDayLessonsTeacherLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            theDayLessonsTeacherLP.setMargins(40 * dp, 0, 40 * dp, 5 * dp);
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
            }
        } else {
            getScheduleOfGroupRequestStatus = RequestStatus.EMPTY_RESPONSE;
//            System.out.println("Schedule of group request empty response!");
        }

//        box.addView(text);
    }

    public void onGetScheduleOfTeacherRequestCompleted(JSONArray arr) {

        if (getScheduleOfTeacherRequestStatus == RequestStatus.COMPLETED) {

            int dp = (int) getResources().getDisplayMetrics().density;

            Typeface light = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_light);
            Typeface medium = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_medium);
            Typeface semibold = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_semibold);
            Typeface regular = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_regular);

            TextView teacherName = findViewById(R.id.teacherName);
            teacherName.setText(currentTeacherName);
            teacherScheduleList.removeAllViews();
            LinearLayout box = teacherScheduleList;
//            System.out.println(arr.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONArray value = arr;

                TextView dayOfWeekName = new TextView(getApplicationContext());
                LinearLayout.LayoutParams dayOfWeekLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                dayOfWeekLP.setMargins(15 * dp, 0, 0, 3 * dp);
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
                forLessonsOfTheDayBoxLP.setMargins(0, 0, 0, 10 * dp);
                forLessonsOfTheDayBox.setLayoutParams(forLessonsOfTheDayBoxLP);
                forLessonsOfTheDayBox.setBackgroundResource(R.drawable.forms_example);
                forLessonsOfTheDayBox.setPadding(0, 0, 0, 5 * dp);
                forLessonsOfTheDayBox.setOrientation(LinearLayout.VERTICAL);
                box.addView(forLessonsOfTheDayBox);

                JSONArray temp = new JSONArray();
                try {
                    temp = value.getJSONArray(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (temp.length() == 0) {
                    TextView nullText = new TextView(getApplicationContext());
                    LinearLayout.LayoutParams nullTextLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    nullTextLP.setMargins(0,25*dp,0,25*dp);
                    nullText.setLayoutParams(nullTextLP);
                    nullText.setGravity(Gravity.CENTER);
                    nullText.setTextSize(12);
                    nullText.setTypeface(light);
                    nullText.setText("Занятий нет");
                    nullText.setTextColor(getResources().getColor(R.color.greyColor));
                    forLessonsOfTheDayBox.addView(nullText);
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
                        LinearLayout.LayoutParams aboutLessonsOfTheDayBoxLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        aboutLessonsOfTheDayBoxLP.setMargins(0, 5 * dp, 0, 5 * dp);
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
                            case "VII": {
                                pos = "7";
                                break;
                            }
                        }

                        TextView theDayLessonsCounter = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams theDayLessonsCounterLP = new LinearLayout.LayoutParams(20 * dp, 20 * dp);
                        theDayLessonsCounterLP.setMargins(12 * dp, 10 * dp, 0, 0);
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
                        LinearLayout.LayoutParams theDayLessonsCabLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20 * dp);
                        theDayLessonsCabLP.setMargins(0, -20 * dp, 12 * dp, 0);
                        theDayLessonsCab.setLayoutParams(theDayLessonsCabLP);
                        theDayLessonsCab.setText(tmp.getString("room"));
                        theDayLessonsCab.setTypeface(medium);
                        theDayLessonsCab.setTextColor(getResources().getColor(R.color.white));
                        theDayLessonsCab.setGravity(Gravity.RIGHT);
                        theDayLessonsCab.setTextSize(12);
                        aboutLessonsOfTheDayBox.addView(theDayLessonsCab);

                        TextView theDayLessonsTime = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams theDayLessonsTimeLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 20 * dp);
                        theDayLessonsTimeLP.setMargins(40 * dp, -21 * dp, 0, 0);
                        theDayLessonsTime.setLayoutParams(theDayLessonsTimeLP);
                        theDayLessonsTime.setText(tmp.getString("start") + " - " + tmp.getString("end"));
                        theDayLessonsTime.setTypeface(light);
                        theDayLessonsTime.setTextColor(getResources().getColor(R.color.greyColor));
                        theDayLessonsTime.setGravity(Gravity.CENTER_VERTICAL);
                        theDayLessonsTime.setTextSize(12);
                        aboutLessonsOfTheDayBox.addView(theDayLessonsTime);

                        TextView theDayLessonsNameOfLesson = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams theDayLessonsNameOfLessonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        theDayLessonsNameOfLessonLP.setMargins(40 * dp, 5 * dp, 40 * dp, 0);
                        theDayLessonsNameOfLesson.setLayoutParams(theDayLessonsNameOfLessonLP);
                        theDayLessonsNameOfLesson.setText(tmp.getString("name"));
                        theDayLessonsNameOfLesson.setTypeface(medium);
                        theDayLessonsNameOfLesson.setTextColor(getResources().getColor(R.color.white));
                        theDayLessonsNameOfLesson.setGravity(Gravity.CENTER_VERTICAL);
                        theDayLessonsNameOfLesson.setTextSize(13);
                        aboutLessonsOfTheDayBox.addView(theDayLessonsNameOfLesson);

                        TextView theDayLessonsTeacher = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams theDayLessonsTeacherLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        theDayLessonsTeacherLP.setMargins(40 * dp, 0, 40 * dp, 5 * dp);
                        theDayLessonsTeacher.setLayoutParams(theDayLessonsTeacherLP);
                        theDayLessonsTeacher.setText(tmp.getString("group"));
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
//            System.out.println(activeContainer);
        }
        else if (getScheduleOfTeacherRequestStatus == RequestStatus.FAILED) {
            teacherScheduleList.removeAllViews();
            setError(ContainerName.TEACHERSCHEDULE);
        }
        else if (getScheduleOfTeacherRequestStatus == RequestStatus.TIMEOUT) {
            teacherScheduleList.removeAllViews();
            setError(ContainerName.TEACHERSCHEDULE);
        }
        else {

        }

    }

    public void onGetScheduleInfoRequestCompleted(Document html) {
        if (getScheduleInfoRequestStatus == RequestStatus.COMPLETED) {
            Element root = html.body().getElementsByClass("users").get(0);
            teachersList.removeAllViews();

            int dp = (int) getResources().getDisplayMetrics().density;

            Typeface light = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_light);
            Typeface medium = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_medium);
            Typeface semibold = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_semibold);
            Typeface regular = ResourcesCompat.getFont(getApplicationContext(), R.font.montserrat_regular);

            Boolean titled = false;

            for (Element element : root.getElementsByClass("block")) {
                String title = element.getElementsByClass("title").get(0).text();
                titled = false;
                for (Element elem : element.select("li")) {
                    String href = elem.select("a").get(0).attr("href");
                    href = href.split("=")[1];
                    href = href.split("&")[0];
                    String name = elem.getElementsByClass("user-name").get(0).text();
                    try {
                        teachersSchedule.put(href, new JSONObject());
                        JSONObject tmp = teachersSchedule.getJSONObject(href);
                        tmp.put("FIO", name);
                        tmp.put("id", href.substring(1));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        JSONObject value = teachersSchedule.getJSONObject(href);


                        if (!titled) {
                            TextView semestrCounter = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams semestrCounterLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            semestrCounterLP.setMargins(20 * dp, 15 * dp, 0, 0);
                            semestrCounter.setLayoutParams(semestrCounterLP);
                            semestrCounter.setText(title);
                            semestrCounter.setTextSize(12);
                            semestrCounter.setTextColor(getResources().getColor(R.color.pinkColor));
                            semestrCounter.setTypeface(medium);
                            teachersList.addView(semestrCounter);
                            titled = true;
                        }

                        RelativeLayout studentLessonsFullInfoBox = new RelativeLayout(getApplicationContext());
                        RelativeLayout.LayoutParams studentLessonsFullInfoBoxLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        studentLessonsFullInfoBoxLP.setMargins(0, 5 * dp, 0, 0);
                        studentLessonsFullInfoBox.setLayoutParams(studentLessonsFullInfoBoxLP);
                        studentLessonsFullInfoBox.setBackgroundResource(R.drawable.forms_example);
                        teachersList.addView(studentLessonsFullInfoBox);

                        TextView nameOfLessonForFullInfo = new TextView(getApplicationContext());
                        RelativeLayout.LayoutParams nameOfLessonForFullInfoLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        nameOfLessonForFullInfoLP.setMargins(20 * dp, 15 * dp, 40 * dp, 15 * dp);
                        nameOfLessonForFullInfo.setLayoutParams(nameOfLessonForFullInfoLP);
                        nameOfLessonForFullInfo.setGravity(Gravity.CENTER_VERTICAL);
                        nameOfLessonForFullInfo.setText(value.getString("FIO"));
                        nameOfLessonForFullInfo.setTextSize(11);
                        nameOfLessonForFullInfo.setTextColor(getResources().getColor(R.color.white));
                        nameOfLessonForFullInfo.setTypeface(medium);
                        studentLessonsFullInfoBox.addView(nameOfLessonForFullInfo);

                        TextView nameOfLessonForFullInfoDec = new TextView(getApplicationContext());
                        RelativeLayout.LayoutParams nameOfLessonForFullInfoDecLP = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        nameOfLessonForFullInfoDecLP.setMargins(0, 0, 20 * dp, 0);
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

                        teachersScheduleClickListener needMoreInfo = new teachersScheduleClickListener();
                        studentLessonsFullInfoBox.setOnClickListener(needMoreInfo);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
//            System.out.println(teachersSchedule.toString());
        }
        else if (getScheduleInfoRequestStatus == RequestStatus.TIMEOUT) {

        }
        else if (getScheduleInfoRequestStatus == RequestStatus.FAILED) {

        }
    }

    public void onGetStudentAchievementsRequestCompleted(String str) {
        if (getStudentAchievementsRequestStatus == RequestStatus.COMPLETED) {
            System.out.println(str);
            System.out.println("HERE");
        } else if (getStudentAchievementsRequestStatus == RequestStatus.TIMEOUT) {

        } else if (getStudentAchievementsRequestStatus == RequestStatus.FAILED) {

        }
    }

    // Сами асинхронные запросы

    // [name, password]
    class LoginRequest extends AsyncTask<String[], String, String[]>  {

//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            loadingLog("Вход в аккаунт");
//        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Вход в аккаунт");
        }

        @Override
        protected String[] doInBackground(String[]... params) { // params[0][0] - name, params[0][1] - password

            publishProgress("");

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
//                System.out.println("Login request timeout!");
                loginRequestStatus = RequestStatus.TIMEOUT;
                return new String[] {"", params[0][0], params[0][1]};
            } catch (Exception e) {
//                System.out.println("Problems with login request");
//                System.out.println(e.toString());
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
    class GetStudentMainDataRequest extends AsyncTask<String[], String, String> {

//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение данных о предметах и преподавателях");
        }

        protected String doInBackground(String[]... params) { // params[0][0] - year, params[0][1] - month

            publishProgress("");

            HttpURLConnection urlConnection = null;
            String responseBody = "";


            // создаем мап для картинки
            if (getStudentProfileDataRequestStatus == RequestStatus.COMPLETED) {
                try {
                    studentAvatarBitmap = BitmapFactory.decodeStream((InputStream) new URL("https://ifspo.ifmo.ru" + studentAvatarSrc).getContent());
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
//                System.out.println("Problems with main data request request");
//                System.out.println(e.toString());
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
    class GetExercisesByLessonRequest extends AsyncTask <String[], String, String[]> {

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение данных о парах");
        }

        protected String[] doInBackground(String[]... params) { // params[0][0] - lesson_id (String)

            publishProgress("");

            HttpURLConnection urlConnection = null;
            String responseBody = "";

            try {
                String url_address = "https://ifspo.ifmo.ru/journal/getStudentExercisesByLesson"
                        + "?lesson=" + params[0][0]
                        + "&student=" + studentId;

                urlConnection = Functions.setupGETAuthRequest(url_address, authCookie, EXERCISES_BY_LESSON_REQUEST_CONNECT_TIMEOUT, EXERCISES_BY_LESSON_REQUEST_READ_TIMEOUT);
                responseBody = Functions.getResponseFromGetRequest(urlConnection);

            } catch (SocketTimeoutException e) {
//                System.out.println("Exercises by lesson request timeout!");
                getExercisesByLessonRequestStatus = RequestStatus.TIMEOUT;
                return new String[] {"", ""};
            } catch (Exception e) {
//                System.out.println("Problems with exercises by lesson request");
//                System.out.println(e.toString());
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
    class GetExercisesByDayRequest extends AsyncTask <String[], String, String> {

//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение данных о парах за сегодня");
        }

        protected String doInBackground(String[]... params) { // params[0][0] - date <yyyy-mm-dd>

            publishProgress("");

            HttpURLConnection urlConnection = null;
            String responseBody = "";

            try {
                String url_address = "https://ifspo.ifmo.ru//journal/getStudentExercisesByDay"
                        + "?student=" + studentId
                        + "&day=" + params[0][0];

                urlConnection = Functions.setupGETAuthRequest(url_address, authCookie, EXERCISES_BY_DAY_REQUEST_CONNECT_TIMEOUT, EXERCISES_BY_DAY_REQUEST_READ_TIMEOUT);
                responseBody = Functions.getResponseFromGetRequest(urlConnection);

            } catch (SocketTimeoutException e) {
//                System.out.println("Exercises by day request timeout!");
                getExercisesByDayRequestStatus = RequestStatus.TIMEOUT;
                return "";
            } catch (Exception e) {
//                System.out.println("Problems with exercises by day request");
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
    class GetVKWallPostsRequest extends AsyncTask <String[], String, String> {

//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение новостей");
        }

        protected String doInBackground(String[]... params) {

            publishProgress("");

            HttpURLConnection urlConnection = null;
            String responseBody = "";

            try {
                String url_address = "https://api.vk.com/method/wall.get?domain=raspfspo"
                        + "&count=" + params[0][0]
                        + "&filter=owner&access_token=c2cb19e3c2cb19e3c2cb19e339c2a4f3d6cc2cbc2cb19e39c9fe125dc37c9d4bb7994cd&v=5.103";

                urlConnection = Functions.setupGETAuthRequest(url_address, authCookie, VK_POSTS_REQUEST_CONNECT_TIMEOUT, VK_POSTS_REQUEST_READ_TIMEOUT);
                responseBody = Functions.getResponseFromGetRequest(urlConnection);

            } catch (SocketTimeoutException e) {
//                System.out.println("VK posts request timeout!");
                getVKWallPostsRequestStatus = RequestStatus.TIMEOUT;
                return "";
            } catch (Exception e) {
//                System.out.println("Problems with vk wall posts request");
//                System.out.println(e.toString());
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

    class GetStudentProfileDataRequest extends AsyncTask<Void, String, String[]> {

//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение данных о студенте");
        }

        protected String[] doInBackground(Void... params) {

            publishProgress("");

            HttpURLConnection urlConnection = null;
            String responseBody = "";
            Document html = new Document(responseBody);

            String FIO = "";
            String group = "";
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

                FIO = row.getElementsByClass("span9").select("h3").get(0).text();
                group = row.getElementsByClass("span9").get(0)
                        .getElementsByClass("row").get(0)
                        .getElementsByClass("span3").select("ul").select("li").last().text();
                avatarSrc = row.getElementsByClass("span3").get(0)
                        .getElementsByClass("showchange").get(0)
                        .getElementsByTag("img").get(0).attr("src");

                group = group.split(" ")[0];

            } catch (SocketTimeoutException e) {
//                System.out.println("Student's profile data request timeout!");
                getStudentProfileDataRequestStatus = RequestStatus.TIMEOUT;
                return new String[] {"", "", ""};
            } catch (Exception e) {
//                System.out.println("Problems with student's profile data request");
//                System.out.println(e.toString());
                getStudentProfileDataRequestStatus = RequestStatus.FAILED;
                return new String[] {"", "", ""};
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }

//            System.out.println("GetProfileParsing Success!");

            // создаем мап для картинки

            try {
                studentAvatarBitmap = BitmapFactory.decodeStream((InputStream)new URL("https://ifspo.ifmo.ru" + avatarSrc).getContent());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            studentFIO = FIO;
            studentGroup = group;
            studentAvatarSrc = avatarSrc;

            preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
            preferencesEditor = preferences.edit();

            preferencesEditor.putString("studentFIO", studentFIO);
            preferencesEditor.putString("studentGroup", studentGroup);
            preferencesEditor.putString("studentAvatarSrc", studentAvatarSrc);

//            preferencesEditor.putString("studentStatsMidMark", statsMidMark);
//            preferencesEditor.putString("studentStatsDebtsCount", statsDebtsCount);
//            preferencesEditor.putString("studentStatsPercentageOfVisits", statsPercentageOfVisits);

            preferencesEditor.apply();

            return new String[] { studentFIO, studentGroup, avatarSrc };
        }

        protected void onPostExecute(String[] result) {
            super.onPostExecute(result);
            onGetStudentProfileDataRequestCompleted(result);

        }
    }

    class GetScheduleOfGroupRequest extends AsyncTask<String[], String, String> {

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение данных о расписании группы");
        }

        protected String doInBackground(String[]... params) { // now next

            publishProgress("");

            URL url;
            HttpURLConnection urlConnection = null;
            String responseBody = "";
            Document html = new Document(responseBody);

            try {
                String url_address = "https://ifspo.ifmo.ru/schedule/get?num=" + Functions.getGroupIdByName(studentGroup) + "&week=" + params[0][0];

                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(SCHEDULE_OF_GROUP_REQUEST_CONNECT_TIMEOUT * 1000);
                urlConnection.setReadTimeout(SCHEDULE_OF_GROUP_REQUEST_READ_TIMEOUT * 1000);

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
//                    System.out.println(e.toString());
                }

                responseBody = response.toString();
                html = Jsoup.parse(responseBody);
            } catch (SocketTimeoutException e) {
//                System.out.println("Schedule of group request timeout!");
                getScheduleOfGroupRequestStatus = RequestStatus.TIMEOUT;
                return "";
            } catch (Exception e) {
//                System.out.println("Problems with schedule of group request");
//                System.out.println(e.toString());
                getScheduleOfGroupRequestStatus = RequestStatus.FAILED;
                return "";
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

//            System.out.println("Get schedule of group request success!");
            getScheduleOfGroupRequestStatus = RequestStatus.COMPLETED;

            JSONArray scheduleRoot = new JSONArray();

            try {
                groupScheduleLessons.put(params[0][0], new JSONArray());
                scheduleRoot = groupScheduleLessons.getJSONArray(params[0][0]);
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
                        case "VII": {
                            timeOfLessonStart = "18:40";
                            timeOfLessonEnd = "20:10";
                            break;
                        }
                    }
                    String nameOfLesson = elem.getElementsByClass("lesson_td").get(0).select("div").get(0).text();


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

//            try { System.out.println("Group schedule: " + groupScheduleLessons.toString(2)); }
//            catch (Exception e) {}

            return params[0][0];
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            onGetScheduleOfGroupRequestCompleted(result, "");
        }
    }

    class GetScheduleOfTeacherRequest extends AsyncTask<String[], String, JSONArray> {

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение данных о расписании преподавателя");
        }

        protected JSONArray doInBackground(String[]... params) { // now next

            publishProgress("");

            URL url;
            HttpURLConnection urlConnection = null;
            String responseBody = "";
            Document html = new Document(responseBody);

            try {
                String url_address = "https://ifspo.ifmo.ru/schedule/get?num=" + params[0][1] + "&week=" + params[0][0];

                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(SCHEDULE_OF_TEACHER_REQUEST_CONNECT_TIMEOUT * 1000);
                urlConnection.setReadTimeout(SCHEDULE_OF_TEACHER_REQUEST_READ_TIMEOUT * 1000);

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
//                    System.out.println(e.toString());
                }

                responseBody = response.toString();
                html = Jsoup.parse(responseBody);
            } catch (SocketTimeoutException e) {
//                System.out.println("Schedule of teacher request timeout!");
                getScheduleOfTeacherRequestStatus = RequestStatus.TIMEOUT;
                return new JSONArray();
            } catch (Exception e) {
//                System.out.println("Problems with schedule of teacher request");
//                System.out.println(e.toString());
                getScheduleOfTeacherRequestStatus = RequestStatus.FAILED;
                return new JSONArray();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

//            System.out.println("Get schedule of teacher request success!");
            getScheduleOfTeacherRequestStatus = RequestStatus.COMPLETED;

            JSONArray scheduleRoot = new JSONArray();

            try {
                teacherScheduleLessons.put(params[0][0], new JSONArray());
                scheduleRoot = teacherScheduleLessons.getJSONArray(params[0][0]);
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
                        case "VII": {
                            timeOfLessonStart = "18:40";
                            timeOfLessonEnd = "20:10";
                            break;
                        }
                    }
                    String nameOfLesson = elem.getElementsByClass("lesson_td").get(0).select("div").get(0).text();


                    String group = elem.getElementsByClass("lesson_td").get(0).select("div").get(1).text();
                    String roomOfLesson = elem.getElementsByClass("place_td").get(0).select("div").get(0).text();

                    JSONObject scheduleRootLessonInformation = new JSONObject();
                    try {
                        scheduleRootLessonInformation = scheduleRootLesson.getJSONObject(scheduleRootLesson.length() - 1);
                        scheduleRootLessonInformation.put("position", numberOfLesson);
                        scheduleRootLessonInformation.put("start", timeOfLessonStart);
                        scheduleRootLessonInformation.put("end", timeOfLessonEnd);
                        scheduleRootLessonInformation.put("name", nameOfLesson);
                        scheduleRootLessonInformation.put("group", group);
                        scheduleRootLessonInformation.put("room", roomOfLesson);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

            }

//            try { System.out.println("Teacher schedule: " + teacherScheduleLessons.toString(2)); }
//            catch (Exception e) {}

            return scheduleRoot;
        }

        protected void onPostExecute(JSONArray scheduleRoot) {
            super.onPostExecute(scheduleRoot);
            onGetScheduleOfTeacherRequestCompleted(scheduleRoot);
        }
    }

    class GetStudentStatsRequest extends AsyncTask<Void, String, String[]> {

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение данных об успеваемоти студента");
        }

        protected String[] doInBackground(Void ...params) {

            publishProgress("");

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
//                System.out.println(authCookie);
//                System.out.println(studentId);
                urlConnection = Functions.setupPOSTAuthRequest(url_address, urlParameters, authCookie, STATS_REQUEST_CONNECT_TIMEOUT, STATS_REQUEST_READ_TIMEOUT);


                responseBody = Functions.getResponseFromGetRequest(urlConnection);
                html = Jsoup.parse(responseBody);
//                System.out.println(html.toString());
//                System.out.println(html);

                Elements stats = html.body().getElementsByClass("stat-block");

                statsMidMark = stats.get(0).getElementsByClass("stat-value").get(0).text();
                statsDebtsCount = stats.get(1).getElementsByClass("stat-value").get(0).text();
                statsPercentageOfVisits = stats.get(2).getElementsByClass("stat-value").get(0).text();

                statsMidMark = statsMidMark.substring(0, statsMidMark.length() - 1);

//                System.out.println(statsMidMark + " " + statsDebtsCount + " " + statsPercentageOfVisits);

            } catch (SocketTimeoutException e) {
//                System.out.println("Student stats request timeout!");
                getStudentStatsRequestStatus = RequestStatus.TIMEOUT;
                return new String[] {"", "", ""};
            } catch (Exception e) {
//                System.out.println("Problems with statistics request");
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

    class GetFinalMarksRequest extends AsyncTask<Void, String, Void> {

//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            loadingLog("Получение данных об успеваемоти студента");
//        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение данных о последних итоговых оценках");
        }

        protected Void doInBackground(Void... params) {

            publishProgress("");

            HttpURLConnection urlConnection = null;
            String responseBody = "";
            Document html = new Document(responseBody);

            try {
                String url_address = "https://ifspo.ifmo.ru/profile/statistic?user=" + studentId;

                urlConnection = Functions.setupGETAuthRequest(url_address, authCookie, FINAL_MARKS_REQUEST_CONNECT_TIMEOUT, FINAL_MARKS_REQUEST_READ_TIMEOUT);
                responseBody = Functions.getResponseFromGetRequest(urlConnection);

                html = Jsoup.parse(responseBody);

                finalMarksSemestr = html.body()
                        .getElementsByClass("container").get(0)
                        .getElementsByClass("row").get(1)
                        .getElementsByClass("span12").get(0)
                        .select("p").get(0).text();

//                System.out.println("sem: " + finalMarksSemestr);

                Element container = html.body()
                        .getElementsByClass("container").get(0)
                        .getElementsByClass("row").get(1)
                        .getElementsByClass("span12").get(1)
                        .getElementsByTag("table").get(0)
                        .getElementsByTag("tbody").get(0);

//                System.out.println(container.toString());

                String lesson;
                String count;
                String visits;
                String absences;
                String certification;
                String finalMark;

                JSONArray array = new JSONArray();

                for (Element tr : container.getElementsByTag("tr")) {
                    Elements tds = tr.getElementsByTag("td");

                    lesson          = tds.get(0).text();
                    count           = tds.get(1).text();
                    visits          = tds.get(2).text();
                    absences        = tds.get(3).text();
                    certification   = tds.get(4).text();
                    finalMark       = tds.get(5).text();

                    JSONObject temp = new JSONObject();
                    temp.put("name", lesson);
                    temp.put("all", count);
                    temp.put("was", visits);
                    temp.put("absences", absences);
                    temp.put("certification", certification);
                    temp.put("mark", finalMark);

                    array.put(temp);
                }

//                System.out.println("final marks: " + array);

                studentFinalMarks = array;

            } catch (SocketTimeoutException e) {
//                System.out.println("Final marks request timeout!");
                getFinalMarksRequestStatus = RequestStatus.TIMEOUT;
//                return;
            } catch (Exception e) {
//                System.out.println("Problems with final marks request");
//                System.out.println(e.toString());
                getFinalMarksRequestStatus = RequestStatus.FAILED;
//                return;
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }

//            return;
//            System.out.println("GetProfileParsing Success!");

            return null;
        }

        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            onGetFinalMarksRequestCompleted();

        }
    }

    class GetAllFinalMarksRequest extends AsyncTask<Void, String, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение данных о всех итоговых оценках");
        }

        protected Void doInBackground(Void... params) {

            publishProgress("");

            HttpURLConnection urlConnection = null;
            String responseBody = "";
            Document html = new Document(responseBody);

            try {
                String url_address = "https://ifspo.ifmo.ru/profile/recordBook?student=" + studentId;

                urlConnection = Functions.setupGETAuthRequest(url_address, authCookie, ALL_FINAL_MARKS_REQUEST_CONNECT_TIMEOUT, ALL_FINAL_MARKS_REQUEST_READ_TIMEOUT);
                responseBody = Functions.getResponseFromGetRequest(urlConnection);

                html = Jsoup.parse(responseBody);

                Element container = html.body()
                        .getElementsByClass("container").get(0)
                        .getElementsByClass("row").get(1)
                        .getElementsByClass("span12").get(0);

//                System.out.println(container.toString());

                String semester;
                String name;
                String hours;
                String mark;

                JSONArray allMarks = new JSONArray();

                for (Element table : container.getElementsByTag("table"))
                {
                    semester = table.previousElementSibling().text();
                    Elements trs = table.getElementsByTag("tbody").get(0).getElementsByTag("tr");

                    JSONObject temp = new JSONObject();
                    JSONArray tempArray = new JSONArray();

                    temp.put("semester", semester);

                    for (Element tr : trs)
                    {
                        if (tr != trs.get(0)) {
                            Elements tds = tr.getElementsByTag("td");

                            name = tds.get(0).text();
                            hours = tds.get(1).text();
                            mark = tds.get(2).text();

                            JSONObject lesson = new JSONObject();
                            lesson.put("name", name);
                            lesson.put("hours", hours);
                            lesson.put("mark", mark);

                            tempArray.put(lesson);
                        }
                    }

                    temp.put("lessons", tempArray);
                    allMarks.put(temp);

                }

                studentAllFinalMarks = allMarks;
//                System.out.println(allMarks.toString());

//                studentFinalMarks = array;

            } catch (SocketTimeoutException e) {
//                System.out.println("All final marks request timeout!");
                getAllFinalMarksRequestStatus = RequestStatus.TIMEOUT;
//                return;
            } catch (Exception e) {
//                System.out.println("Problems with all final marks request");
//                System.out.println(e.toString());
                getAllFinalMarksRequestStatus = RequestStatus.FAILED;
//                return;
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }

//            return;
//            System.out.println("GetProfileParsing Success!");

            return null;
        }

        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            onGetAllFinalMarksRequestCompleted();

        }
    }

    class RatingRequest extends AsyncTask<String[], String, String> {

//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение данных о рейтинге");
        }

        protected String doInBackground(String[]... params) {

//            System.out.println(params[0][0] + " " + params[0][1]);

            publishProgress("");

            HttpURLConnection urlConnection = null;
            String responseBody = "";

            String name = params[0][0];
            String password = params[0][1];
//            String fio = params[0][2];
//            String group = params[0][3];

            try {
                String url_address = "https://spoconnection-rating-server.herokuapp.com/rating"
                        + "?name=" + name
                        + "&password=" + password
                        + "&fio=" + studentFIO
                        + "&group=" + studentGroup;

                urlConnection = Functions.setupGETAuthRequest(url_address, authCookie, RATING_REQUEST_CONNECT_TIMEOUT, RATING_REQUEST_READ_TIMEOUT);
                responseBody = Functions.getResponseFromGetRequest(urlConnection);

            } catch (SocketTimeoutException e) {
//                System.out.println("Rating request timeout!");
                ratingRequestStatus = RequestStatus.TIMEOUT;
                return "";
            } catch (Exception e) {
//                System.out.println("Problems with rating request");
//                System.out.println(e.toString());
                ratingRequestStatus = RequestStatus.FAILED;
                return "";
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }

            return responseBody;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            onRatingRequestCompleted(result);

        }
    }

    class GetScheduleInfoRequest extends AsyncTask<Void, String, Document> {

//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение данных о расписании групп и преподавателей");
        }

        protected Document doInBackground(Void... params) { // now next

            publishProgress("");

            URL url;
            HttpURLConnection urlConnection = null;
            String responseBody = "";
            Document html = new Document(responseBody);

            try {
                String url_address = "https://ifspo.ifmo.ru/schedule";

                url = new URL(url_address);
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(SCHEDULE_INFO_REQUEST_CONNECT_TIMEOUT * 1000);
                urlConnection.setReadTimeout(SCHEDULE_INFO_REQUEST_READ_TIMEOUT * 1000);

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
//                    System.out.println(e.toString());
                }

                responseBody = response.toString();
                html = Jsoup.parse(responseBody);
//                Element root = html.body().getElementsByClass("users").get(0);
//
//                for (Element elem : root.getElementsByClass("block")) {
//                    String href = elem.select("a").get(0).attr("href");
//                    href = href.split("=")[1];
//                    href = href.split("&")[0];
//                    teachersSchedule.put(href, new JSONObject());
//                }
            } catch (SocketTimeoutException e) {
//                System.out.println("Schedule info request timeout!");
                getScheduleInfoRequestStatus = RequestStatus.TIMEOUT;
                return null;
            } catch (Exception e) {
//                System.out.println("Problems with schedule info request");
//                System.out.println(e.toString());
                getScheduleInfoRequestStatus = RequestStatus.FAILED;
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

//            System.out.println("Schedule info request success!");
            getScheduleInfoRequestStatus = RequestStatus.COMPLETED;


            return html;
        }

        protected void onPostExecute(Document doc) {
            super.onPostExecute(doc);
            onGetScheduleInfoRequestCompleted(doc);
        }
    }

    class GetStudentAchievementsRequest extends AsyncTask<String[], String, String> {

//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            loadingLog("Получение данных о достижениях");
        }

        protected String doInBackground(String[]... params) { // params[0][0] - year, params[0][1] - month

            publishProgress("");

            HttpURLConnection urlConnection = null;
            String responseBody = "";


            // создаем мап для картинки
//            if (getStudentProfileDataRequestStatus == RequestStatus.COMPLETED) {
//                try {
//                    studentAvatarBitmap = BitmapFactory.decodeStream((InputStream) new URL("https://ifspo.ifmo.ru" + studentAvatarSrc).getContent());
//                } catch (MalformedURLException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }

            try {
                preferences = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
                String currentAdress = preferences.getString("studentName", "");

                String url_address = "http://spoconnection-rating-server.herokuapp.com/achievements"
                        + "?name=" + currentAdress;

                urlConnection = Functions.setupGETAuthRequest(url_address, authCookie, MAIN_DATA_REQUEST_CONNECT_TIMEOUT, MAIN_DATA_REQUEST_READ_TIMEOUT);
                responseBody = Functions.getResponseFromGetRequest(urlConnection);
                getStudentAchievementsRequestStatus = RequestStatus.COMPLETED;

            } catch (SocketTimeoutException e) {
                System.out.println("Achievements request timeout!");
                getStudentAchievementsRequestStatus = RequestStatus.TIMEOUT;
                return "";
            } catch (Exception e) {
//                System.out.println("Problems with main data request request");
//                System.out.println(e.toString());
                getStudentAchievementsRequestStatus = RequestStatus.FAILED;
                return "";
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
            return responseBody;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            onGetStudentAchievementsRequestCompleted(result);
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

    LinearLayout home;
    LinearLayout schedule;
    LinearLayout profile;
    LinearLayout lessons;
    LinearLayout settings;

    LinearLayout userHelp;
    TextView scheduleNow;
    TextView scheduleNext;
    TextView teacherNow;
    TextView teacherNext;


    // ПОД ОЧИСТКУ ОЧЕНЬ ОЧЕНЬ ВАЖНО

    TextView profileUserName;
    TextView profileUserGroup;
    TextView profileUserCalendar;
    TextView profileUserBalls;
    TextView profileUserBills;
    LinearLayout todayLessonsView;
    LinearLayout scheduleList;
    LinearLayout lessonsList;

    // Все для анимации навигации

    ImageView homeNavImg;
    TextView homeNavText;
    ImageView scheduleNavImg;
    TextView scheduleNavText;
    ImageView profileNavImg;
    TextView profileNavText;
    ImageView lessonsNavImg;
    TextView lessonsNavText;
    ImageView settingsNavImg;
    TextView settingsNavText;
    ImageView notificationNavImg;
    TextView notificationNavText;



    // переменная для мониторинга активного контейнера

    enum ContainerName { PROFILE, HOME, SCHEDULE, LESSONS, LESSONS_INFORMATION, NOTIFICATION, LOADING, LOGIN, SETTINGS, ITOG, ERROR, BACKCONNECT, TEACHERS, TEACHERSCHEDULE }
    ContainerName activeContainer;

    // функция обновления активного контейнера
    public void setContainer(ContainerName newContainer) {

        switch (newContainer) {
            case TEACHERS: {
                navigationInput.setText("User.Schedule");
                break;
            }
            case NOTIFICATION: {
                navigationInput.setText("User.Changes");
                break;
            }
            case HOME: {
                navigationInput.setText("User.Home");
                break;
            }
            case ITOG: {
                navigationInput.setText("User.Lessons");
                break;
            }
            case LESSONS: {
                navigationInput.setText("User.Lessons");
                break;
            }
            case LESSONS_INFORMATION: {
                navigationInput.setText("User.Lessons");
                break;
            }
            case PROFILE: {
                navigationInput.setText("User.Profile");
                break;
            }
            case SCHEDULE: {
                navigationInput.setText("User.Schedule");
                break;
            }
            case SETTINGS: {
                navigationInput.setText("User.Settings");
                break;
            }
            case BACKCONNECT: {
                navigationInput.setText("User.Settings");
                break;
            }
        }

        switch (activeContainer) {
            case TEACHERS: {
                scheduleNavImg.setImageResource(R.drawable.schedule);
                scheduleNavText.setTextColor(getResources().getColor(R.color.greyColor));
                scheduleNavText.setShadowLayer(0,0,0,0);
                teachersList.removeView(loadingScreen);
//                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
//                anim.setDuration(200);
//                anim.setStartOffset(0);
//                teachersScreen.startAnimation(anim);
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
                                main.removeView(teachersScreen);
//                            }
//                        });
//                    }
//                }, 200);
                break;
            }
            case PROFILE: {
                profileNavImg.setImageResource(R.drawable.profile);
                profileNavText.setTextColor(getResources().getColor(R.color.greyColor));
                profileNavText.setShadowLayer(0,0,0,0);
//                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
//                anim.setDuration(200);
//                anim.setStartOffset(0);
//                profileScreen.startAnimation(anim);
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
                                main.removeView(profileScreen);
//                            }
//                        });
//                    }
//                }, 200);
                break;
            }
            case HOME: {
                homeNavImg.setImageResource(R.drawable.main);
                homeNavText.setTextColor(getResources().getColor(R.color.greyColor));
                homeNavText.setShadowLayer(0,0,0,0);
//                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
//                anim.setDuration(200);
//                anim.setStartOffset(0);
//                homeScreen.startAnimation(anim);
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
                                main.removeView(homeScreen);
//                            }
//                        });
//                    }
//                }, 200);
                break;
            }
            case TEACHERSCHEDULE: {
                teacherNowWeek = false;
                teacherNextWeek = false;
                teacherNow.setBackgroundResource(R.drawable.active_schedule);
                teacherNow.setTextColor(getResources().getColor(R.color.backgroundMainColor));
                teacherNext.setBackgroundResource(R.drawable.passive_schedule);
                teacherNext.setTextColor(getResources().getColor(R.color.pinkColor));
                scheduleNavImg.setImageResource(R.drawable.schedule);
                scheduleNavText.setTextColor(getResources().getColor(R.color.greyColor));
                scheduleNavText.setShadowLayer(0,0,0,0);
                teacherScheduleList.removeView(errorScreen);
                teacherScheduleList.removeView(loadingScreen);
//                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
//                anim.setDuration(200);
//                anim.setStartOffset(0);
//                teacherScheduleScreen.startAnimation(anim);
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
                                main.removeView(teacherScheduleScreen);
//                            }
//                        });
//                    }
//                }, 200);
                break;
            }
            case SCHEDULE: {
                scheduleNavImg.setImageResource(R.drawable.schedule);
                scheduleNavText.setTextColor(getResources().getColor(R.color.greyColor));
                scheduleNavText.setShadowLayer(0,0,0,0);
                scheduleListError.removeView(errorScreen);
                scheduleListError.removeView(loadingScreen);
//                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
//                anim.setDuration(200);
//                anim.setStartOffset(0);
//                scheduleScreen.startAnimation(anim);
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
                                main.removeView(scheduleScreen);
//                            }
//                        });
//                    }
//                }, 200);
                break;
            }
            case LESSONS: {
                lessonsNavImg.setImageResource(R.drawable.subject);
                lessonsNavText.setTextColor(getResources().getColor(R.color.greyColor));
                lessonsNavText.setShadowLayer(0,0,0,0);
//                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
//                anim.setDuration(200);
//                anim.setStartOffset(0);
//                lessonsScreen.startAnimation(anim);
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
                                main.removeView(lessonsScreen);
//                            }
//                        });
//                    }
//                }, 200);
                break;
            }
            case LESSONS_INFORMATION: {
                lessonsNavImg.setImageResource(R.drawable.subject);
                lessonsNavText.setTextColor(getResources().getColor(R.color.greyColor));
                lessonsNavText.setShadowLayer(0,0,0,0);
//                LinearLayout lessonsInfoList = findViewById(R.id.lessonsInformationList);
                lessonsInfoList.removeView(errorScreen);
//                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
//                anim.setDuration(200);
//                anim.setStartOffset(0);
//                lessonsInformationScreen.startAnimation(anim);
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
                                main.removeView(lessonsInformationScreen);
//                            }
//                        });
//                    }
//                }, 200);
                break;
            }
            case NOTIFICATION: {
                notificationNavImg.setImageResource(R.drawable.bell);
                notificationNavText.setTextColor(getResources().getColor(R.color.greyColor));
                notificationNavText.setShadowLayer(0,0,0,0);
//                LinearLayout notList = findViewById(R.id.notificationList);
                notList.removeView(errorScreen);
                notList.removeView(loadingScreen);
//                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
//                anim.setDuration(200);
//                anim.setStartOffset(0);
//                notificationListScreen.startAnimation(anim);
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
                                main.removeView(notificationListScreen);

//                            }
//                        });
//                    }
//                }, 200);
                break;
            }
            case SETTINGS: {
                settingsNavImg.setImageResource(R.drawable.settings);
                settingsNavText.setTextColor(getResources().getColor(R.color.greyColor));
                settingsNavText.setShadowLayer(0,0,0,0);
//                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
//                anim.setDuration(200);
//                anim.setStartOffset(0);
//                settingsScreen.startAnimation(anim);
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
                                main.removeView(settingsScreen);
//                            }
//                        });
//                    }
//                }, 200);
                break;
            }
            case ITOG: {
                lessonsNavImg.setImageResource(R.drawable.subject);
                lessonsNavText.setTextColor(getResources().getColor(R.color.greyColor));
                lessonsNavText.setShadowLayer(0,0,0,0);
//                LinearLayout itogList = findViewById(R.id.itogList);
                itogList.removeView(errorScreen);
                itogList.removeView(loadingScreen);
//                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
//                anim.setDuration(200);
//                anim.setStartOffset(0);
//                itogScreen.startAnimation(anim);
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
                                main.removeView(itogScreen);
//                            }
//                        });
//                    }
//                }, 200);
                break;
            }
            case BACKCONNECT: {
                settingsNavImg.setImageResource(R.drawable.settings);
                settingsNavText.setTextColor(getResources().getColor(R.color.greyColor));
                settingsNavText.setShadowLayer(0,0,0,0);
//                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
//                anim.setDuration(200);
//                anim.setStartOffset(0);
//                backConnectScreen.startAnimation(anim);
//                Timer timer = new Timer();
//                timer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        MainActivity.this.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
                                main.removeView(backConnectScreen);
//                            }
//                        });
//                    }
//                }, 200);
                break;
            }
            case LOGIN: {
                main.removeView(loginForm);
                break;
            }
            case LOADING: {
                main.removeView(loadingScreen);
                break;
            }
            case ERROR: {
                main.removeView(errorScreen);
                break;
            }
        }

        switch (newContainer) {
            case TEACHERS: {
                scheduleNavImg.setImageResource(R.drawable.schedule_active);
                scheduleNavText.setTextColor(getResources().getColor(R.color.white));
                scheduleNavText.setShadowLayer(5,0,0,getResources().getColor(R.color.white));
                main.addView(teachersScreen);
                activeContainer = ContainerName.TEACHERS;
                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(200);
                anim.setStartOffset(200);
                teachersScreen.startAnimation(anim);
                break;
            }
            case PROFILE: {
                profileNavImg.setImageResource(R.drawable.profile_active);
                profileNavText.setTextColor(getResources().getColor(R.color.white));
                profileNavText.setShadowLayer(5,0,0,getResources().getColor(R.color.white));
                main.addView(profileScreen);
                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(200);
                anim.setStartOffset(200);
                profileScreen.startAnimation(anim);
                activeContainer = ContainerName.PROFILE;
                break;
            }
            case HOME: {
                homeNavImg.setImageResource(R.drawable.main_active);
                homeNavText.setTextColor(getResources().getColor(R.color.white));
                homeNavText.setShadowLayer(5,0,0,getResources().getColor(R.color.white));
                main.addView(homeScreen);
                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(200);
                anim.setStartOffset(200);
                homeScreen.startAnimation(anim);
                activeContainer = ContainerName.HOME;
                break;
            }
            case TEACHERSCHEDULE: {
                scheduleNavImg.setImageResource(R.drawable.schedule_active);
                scheduleNavText.setTextColor(getResources().getColor(R.color.white));
                scheduleNavText.setShadowLayer(5,0,0,getResources().getColor(R.color.white));
                main.addView(teacherScheduleScreen);
                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(200);
                anim.setStartOffset(200);
                teacherScheduleScreen.startAnimation(anim);
                activeContainer = ContainerName.TEACHERSCHEDULE;
                break;
            }
            case SCHEDULE: {
                scheduleNavImg.setImageResource(R.drawable.schedule_active);
                scheduleNavText.setTextColor(getResources().getColor(R.color.white));
                scheduleNavText.setShadowLayer(5,0,0,getResources().getColor(R.color.white));
                main.addView(scheduleScreen);
                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(200);
                anim.setStartOffset(200);
                scheduleScreen.startAnimation(anim);
                activeContainer = ContainerName.SCHEDULE;
                if (!nowWeekScheduleCalled) {
                    sendGetScheduleOfGroupRequest(new String[] {"now"});
                    setLoadingToList(ContainerName.SCHEDULE);
                    nowWeekScheduleCalled = true;
                }
                break;
            }
            case LESSONS: {
                lessonsNavImg.setImageResource(R.drawable.subject_active);
                lessonsNavText.setTextColor(getResources().getColor(R.color.white));
                lessonsNavText.setShadowLayer(5,0,0,getResources().getColor(R.color.white));
                main.addView(lessonsScreen);
                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(200);
                anim.setStartOffset(200);
                lessonsScreen.startAnimation(anim);
                activeContainer = ContainerName.LESSONS;
                break;
            }
            case LESSONS_INFORMATION: {
                lessonsNavImg.setImageResource(R.drawable.subject_active);
                lessonsNavText.setTextColor(getResources().getColor(R.color.white));
                lessonsNavText.setShadowLayer(5,0,0,getResources().getColor(R.color.white));
                main.addView(lessonsInformationScreen);
                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(200);
                anim.setStartOffset(200);
                lessonsInformationScreen.startAnimation(anim);
                activeContainer = ContainerName.LESSONS_INFORMATION;
                break;
            }
            case NOTIFICATION: {
                notificationNavImg.setImageResource(R.drawable.bell_active);
                notificationNavText.setTextColor(getResources().getColor(R.color.white));
                notificationNavText.setShadowLayer(5,0,0,getResources().getColor(R.color.white));
                main.addView(notificationListScreen);
                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(200);
                anim.setStartOffset(200);
                notificationListScreen.startAnimation(anim);
                activeContainer = ContainerName.NOTIFICATION;
                break;
            }
            case SETTINGS: {
                settingsNavImg.setImageResource(R.drawable.settings_active);
                settingsNavText.setTextColor(getResources().getColor(R.color.white));
                settingsNavText.setShadowLayer(5,0,0,getResources().getColor(R.color.white));
                main.addView(settingsScreen);
                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(200);
                anim.setStartOffset(200);
                settingsScreen.startAnimation(anim);
                activeContainer = ContainerName.SETTINGS;
                break;
            }
            case BACKCONNECT: {
                settingsNavImg.setImageResource(R.drawable.settings_active);
                settingsNavText.setTextColor(getResources().getColor(R.color.white));
                settingsNavText.setShadowLayer(5,0,0,getResources().getColor(R.color.white));
                main.addView(backConnectScreen);
                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(200);
                anim.setStartOffset(200);
                backConnectScreen.startAnimation(anim);
                activeContainer = ContainerName.BACKCONNECT;
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
                break;
            }
            case ERROR: {
                main.addView(errorScreen);
                activeContainer = ContainerName.ERROR;
                break;
            }
            case ITOG: {
                lessonsNavImg.setImageResource(R.drawable.subject_active);
                lessonsNavText.setTextColor(getResources().getColor(R.color.white));
                lessonsNavText.setShadowLayer(5,0,0,getResources().getColor(R.color.white));
                main.addView(itogScreen);
                AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(200);
                anim.setStartOffset(200);
                itogScreen.startAnimation(anim);
                activeContainer = ContainerName.ITOG;
                break;
            }
        }
//        System.out.println("To: " + activeContainer);
    }

    public void setLoadingToList(ContainerName neededContainer) { // функция обновления активного контейнера

        switch (neededContainer) {
            case SCHEDULE: {
                LinearLayout box = findViewById(R.id.scheduleList);
                box.removeAllViews();
                box.addView(loadingScreen);
                break;
            }
            case TEACHERSCHEDULE: {
                LinearLayout box = findViewById(R.id.teacherScheduleList);
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
            case ITOG: {
                LinearLayout box = findViewById(R.id.itogList);
                box.addView(loadingScreen);
                break;
            }
            case TEACHERS: {
                teachersList.removeAllViews();
                teachersList.addView(loadingScreen);
                break;
            }
        }
    }

    //    {
//          "t55": {
//            "now": [
//               [ // day
////                      {position: "I", start: "10", end: "11", name: "name", teacher: "teacher"}, // lesson
////                      {position: "II", start: "11", end: "12", name: "name", teacher: "teacher"}
////             ],
////             [ // day
////                      {position: "I", start: "10", end: "11", name: "name", teacher: "teacher"}, // lesson
////                      {position: "II", start: "11", end: "12", name: "name", teacher: "teacher"}
////             ]
//            ]
//            "next": [
//               [ // day
////                      {position: "I", start: "10", end: "11", name: "name", teacher: "teacher"}, // lesson
////                      {position: "II", start: "11", end: "12", name: "name", teacher: "teacher"}
////             ],
////             [ // day
////                      {position: "I", start: "10", end: "11", name: "name", teacher: "teacher"}, // lesson
////                      {position: "II", start: "11", end: "12", name: "name", teacher: "teacher"}
////             ]
//            ]
//          }
//        }


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


    //    @Override
    public void helpShow () {

        if (dialogCounter == 1 && activeContainer != ContainerName.PROFILE) setContainer(ContainerName.PROFILE);

        int dp = (int) getResources().getDisplayMetrics().density;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();

        View view = getLayoutInflater().inflate(R.layout.help_dialog, null);
        builder.setView(view);

        final Dialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.forms_example);
        dialog.show();

        TextView next = dialog.findViewById(R.id.helpDialogNext);
        TextView cancel = dialog.findViewById(R.id.helpDialogCancel);
        ImageView hadler1 = dialog.findViewById(R.id.helpDialogHandler1);
        ImageView hadler2 = dialog.findViewById(R.id.helpDialogHandler2);
        ImageView hadler3 = dialog.findViewById(R.id.helpDialogHandler3);
        ImageView hadler4 = dialog.findViewById(R.id.helpDialogHandler4);
        ImageView hadler5 = dialog.findViewById(R.id.helpDialogHandler5);
        ImageView hadler6 = dialog.findViewById(R.id.helpDialogHandler6);
        TextView text = dialog.findViewById(R.id.helpDialogText);
        TextView title = dialog.findViewById(R.id.helpDialogTitle);
        ImageView img = dialog.findViewById(R.id.helpDialogImage);

        switch (dialogCounter) {
            case 1: {
                img.setImageResource(R.drawable.profile);
                title.setText("Профиль");
                text.setText("Просматривайте актуальную информацию о парах за сегодня");
                hadler1.setImageResource(R.drawable.circle_active);
                hadler2.setImageResource(R.drawable.circle);
                hadler3.setImageResource(R.drawable.circle);
                hadler4.setImageResource(R.drawable.circle);
                hadler5.setImageResource(R.drawable.circle);
                hadler6.setImageResource(R.drawable.circle);
                next.setText("Далее");
                break;
            }

            case 2: {
                img.setImageResource(R.drawable.rating);
                title.setText("Рейтинг");
                text.setText("Для вашего удобства мы составляем рейтинг потока, в любой момент вы можете оценить свои успехи");
                hadler1.setImageResource(R.drawable.circle_active);
                hadler2.setImageResource(R.drawable.circle);
                hadler3.setImageResource(R.drawable.circle);
                hadler4.setImageResource(R.drawable.circle);
                hadler5.setImageResource(R.drawable.circle);
                hadler6.setImageResource(R.drawable.circle);
                next.setText("Далее");
                break;
            }

            case 3: {
                img.setImageResource(R.drawable.progress);
                title.setText("Успеваемость");
                text.setText("На этом же экране вы можете отслеживать свою общую успеваемость");
                hadler1.setImageResource(R.drawable.circle_active);
                hadler2.setImageResource(R.drawable.circle);
                hadler3.setImageResource(R.drawable.circle);
                hadler4.setImageResource(R.drawable.circle);
                hadler5.setImageResource(R.drawable.circle);
                hadler6.setImageResource(R.drawable.circle);
                next.setText("Далее");
                break;
            }

            case 4: {
                img.setImageResource(R.drawable.user_settings);
                title.setText("Маршрутизация");
                text.setText("С помощью этого окна, вы можете попасть быстрее попасть на интересующий экран");
                hadler1.setImageResource(R.drawable.circle_active);
                hadler2.setImageResource(R.drawable.circle);
                hadler3.setImageResource(R.drawable.circle);
                hadler4.setImageResource(R.drawable.circle);
                hadler5.setImageResource(R.drawable.circle);
                hadler6.setImageResource(R.drawable.circle);
                next.setText("Далее");
                break;
            }

            case 5: {
                img.setImageResource(R.drawable.subject);
                title.setText("Журнал");
                text.setText("На этом экране вы можете узнать подробную информацию об успеваемости по каждому предмету");
                hadler1.setImageResource(R.drawable.circle);
                hadler2.setImageResource(R.drawable.circle_active);
                hadler3.setImageResource(R.drawable.circle);
                hadler4.setImageResource(R.drawable.circle);
                hadler5.setImageResource(R.drawable.circle);
                hadler6.setImageResource(R.drawable.circle);
                next.setText("Далее");
                break;
            }

            case 6: {
                img.setImageResource(R.drawable.result);
                title.setText("Аттестация");
                text.setText("В этом разделе можно посмотреть свою успеваемость на сессиях");
                hadler1.setImageResource(R.drawable.circle);
                hadler2.setImageResource(R.drawable.circle_active);
                hadler3.setImageResource(R.drawable.circle);
                hadler4.setImageResource(R.drawable.circle);
                hadler5.setImageResource(R.drawable.circle);
                hadler6.setImageResource(R.drawable.circle);
                next.setText("Далее");
                break;
            }

            case 7: {
                img.setImageResource(R.drawable.settings);
                title.setText("Настройки");
                text.setText("Этот экран позволяет узнать информацию о приложении, синхронизировать данные или выйти из аккаунта");
                hadler1.setImageResource(R.drawable.circle);
                hadler2.setImageResource(R.drawable.circle);
                hadler3.setImageResource(R.drawable.circle_active);
                hadler4.setImageResource(R.drawable.circle);
                hadler5.setImageResource(R.drawable.circle);
                hadler6.setImageResource(R.drawable.circle);
                next.setText("Далее");
                break;
            }

            case 8: {
                img.setImageResource(R.drawable.synch);
                title.setText("Синхронизация данных");
                text.setText("Для экокономии вашего времени и трафика, мы кешируем редко используемые данные, если вы заметили ошибку, пожалуйста, нажмите кнопку синхронизации");
                hadler1.setImageResource(R.drawable.circle);
                hadler2.setImageResource(R.drawable.circle);
                hadler3.setImageResource(R.drawable.circle_active);
                hadler4.setImageResource(R.drawable.circle);
                hadler5.setImageResource(R.drawable.circle);
                hadler6.setImageResource(R.drawable.circle);
                next.setText("Далее");
                break;
            }

            case 9: {
                img.setImageResource(R.drawable.main);
                title.setText("Меню");
                text.setText("На этом экране вы можете перейти в любую часть приложения");
                hadler1.setImageResource(R.drawable.circle);
                hadler2.setImageResource(R.drawable.circle);
                hadler3.setImageResource(R.drawable.circle);
                hadler4.setImageResource(R.drawable.circle_active);
                hadler5.setImageResource(R.drawable.circle);
                hadler6.setImageResource(R.drawable.circle);
                next.setText("Далее");
                break;
            }

            case 10: {
                img.setImageResource(R.drawable.teacher);
                title.setText("Преподаватели");
                text.setText("На этом экране вы можете найти любого преподавателя");
                hadler1.setImageResource(R.drawable.circle);
                hadler2.setImageResource(R.drawable.circle);
                hadler3.setImageResource(R.drawable.circle);
                hadler4.setImageResource(R.drawable.circle_active);
                hadler5.setImageResource(R.drawable.circle);
                hadler6.setImageResource(R.drawable.circle);
                next.setText("Далее");
                break;
            }

            case 11: {
                img.setImageResource(R.drawable.teacher_schedule);
                title.setText("График преподавателя");
                text.setText("И посмотреть его расписание");
                hadler1.setImageResource(R.drawable.circle);
                hadler2.setImageResource(R.drawable.circle);
                hadler3.setImageResource(R.drawable.circle);
                hadler4.setImageResource(R.drawable.circle_active);
                hadler5.setImageResource(R.drawable.circle);
                hadler6.setImageResource(R.drawable.circle);
                next.setText("Далее");
                break;
            }

            case 12: {
                img.setImageResource(R.drawable.bell);
                title.setText("Изменения и нововсти");
                text.setText("Здесь вы можете узнать новости и изменения расписания из официальной группы VK нашего факультета");
                hadler1.setImageResource(R.drawable.circle);
                hadler2.setImageResource(R.drawable.circle);
                hadler3.setImageResource(R.drawable.circle);
                hadler4.setImageResource(R.drawable.circle);
                hadler5.setImageResource(R.drawable.circle);
                hadler6.setImageResource(R.drawable.circle_active);
                next.setText("Далее");
                break;
            }

            case 13: {
                img.setImageResource(R.drawable.schedule);
                title.setText("Расписание");
                text.setText("На этом экране вы можете просмотреть расписание вашей группы");
                hadler1.setImageResource(R.drawable.circle);
                hadler2.setImageResource(R.drawable.circle);
                hadler3.setImageResource(R.drawable.circle);
                hadler4.setImageResource(R.drawable.circle);
                hadler5.setImageResource(R.drawable.circle_active);
                hadler6.setImageResource(R.drawable.circle);
                next.setText("Понял!");
                break;
            }
        }

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogCounter++;
//                if (dialogCounter == 5) setContainer(ContainerName.LESSONS);
//                if (dialogCounter == 7) setContainer(ContainerName.SETTINGS);
//                if (dialogCounter == 9) setContainer(ContainerName.HOME);
//                if (dialogCounter == 12) setContainer(ContainerName.NOTIFICATION);
//                if (dialogCounter == 13) setContainer(ContainerName.SCHEDULE);
                dialog.dismiss();
                if (dialogCounter < 14) {
                    Timer dialogTimer = new Timer();
                    dialogTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    helpShow();
                                }
                            });
                        }
                    }, 100);
                } else {
                    dialogCounter = 1;
                    setContainer(ContainerName.PROFILE);
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogCounter = 1;
                dialog.dismiss();
            }
        });
        return;
    }



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
        main.removeView(errorScreen);
        main.removeView(itogScreen);
        main.removeView(settingsScreen);
        main.removeView(backConnectScreen);
        setContainer(ContainerName.PROFILE);
        main.addView(navigation);
        main.addView(userHelpScreen);

        if (appFirstRun) {
            // создаем диалог по таймеру
            Timer dialogTimer = new Timer();

            dialogTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    MainActivity.this.runOnUiThread(new Runnable(){
                        @Override
                        public void run() {
                            helpShow();
                        }
                    });
                }
            }, 2000);
        }


        todayLessonsView = findViewById(R.id.todayLessonsView);

        // анимация

        AlphaAnimation anim1 = new AlphaAnimation(0.0f, 1.0f);
        anim1.setDuration(300);
        anim1.setStartOffset(300);
        userHelpScreen.startAnimation(anim1);
        AlphaAnimation anim2 = new AlphaAnimation(0.0f, 1.0f);
        anim2.setDuration(300);
        anim2.setStartOffset(600);
        navigation.startAnimation(anim2);
        AlphaAnimation anim3 = new AlphaAnimation(0.0f, 1.0f);
        anim3.setDuration(300);
        anim3.setStartOffset(900);
        RelativeLayout firstAnimLayout = findViewById(R.id.profileFirstAnimated);
        firstAnimLayout.startAnimation(anim3);
        AlphaAnimation anim4 = new AlphaAnimation(0.0f, 1.0f);
        anim4.setDuration(300);
        anim4.setStartOffset(1200);
        LinearLayout secondAnimLayout = findViewById(R.id.profileSecondAnimated);
        secondAnimLayout.startAnimation(anim4);
        AlphaAnimation anim5 = new AlphaAnimation(0.0f, 1.0f);
        anim5.setDuration(300);
        anim5.setStartOffset(1500);
        todayLessonsView.startAnimation(anim5);
        TextView todayExercisesText = findViewById(R.id.todayExercisesText);
        todayExercisesText.startAnimation(anim5);

        // если нет пар показываем картинку

        if (exercisesByDay.equals(new JSONArray())) {
            todayLessonsView.removeAllViews();

            ImageView img = new ImageView(getApplicationContext());
            LinearLayout.LayoutParams imgLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            imgLP.setMargins(30*dp, 0, 30*dp, 0);
            img.setLayoutParams(imgLP);
            img.setImageResource(R.drawable.shutdown_pink);
            todayLessonsView.addView(img);

            TextView text = new TextView(getApplicationContext());
            LinearLayout.LayoutParams textLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            textLP.setMargins(30*dp,0,30*dp,10*dp);
            text.setLayoutParams(textLP);
            text.setTypeface(light);
            text.setTextColor(getResources().getColor(R.color.greyColor));
            text.setTextSize(14);
            text.setText("Пар не было, можно отдыхать");
            text.setGravity(Gravity.CENTER);
            todayLessonsView.addView(text);

        } else {
            todayLessonsView.removeAllViews();
        }

        // инициализируем картинку

        ImageView img = (ImageView) findViewById(R.id.profileImage);
        img.setImageBitmap(studentAvatarBitmap);
        img.setScaleType(ImageView.ScaleType.FIT_XY);


        // создаем слушатели для кнопок

        home = findViewById(R.id.home);
        schedule = findViewById(R.id.schedule);
        profile = findViewById(R.id.profile);
        lessons = findViewById(R.id.lessons);
        settings = findViewById(R.id.settings);
        userHelp = findViewById(R.id.notification);



        // наш обработчик кликов

        navigationButtonClickListener wasClicked = new navigationButtonClickListener();

        userHelp.setOnClickListener(wasClicked);

        home.setOnClickListener(wasClicked);
        schedule.setOnClickListener(wasClicked);
        profile.setOnClickListener(wasClicked);
        lessons.setOnClickListener(wasClicked);
        settings.setOnClickListener(wasClicked);

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
                    String performance = valueInfo.getString("performance").equals("null") ? "нет" : "▲";
                    if (valueInfo.getString("performance").equals("2")) performance = "▼";



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
                case TEACHERSCHEDULE: {
                    if (v.getId() == teacherScheduleScreen.getId()) return;
                    main.removeView(teacherScheduleScreen);
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
                case SETTINGS: {
                    if (v.getId() == settings.getId()) return;
                    main.removeView(settingsScreen);
                    break;
                }
                case BACKCONNECT: {
                    main.removeView(backConnectScreen);
                    break;
                }
                case ERROR: {
                    main.removeView(errorScreen);
                    break;
                }
                case ITOG: {
                    main.removeView(itogScreen);
                    break;
                }
                case TEACHERS: {
                    main.removeView(teachersScreen);
                    break;
                }
            }

            // и добавляем новый

            if (v.getId() == home.getId()) {
//                System.out.println("You clicked home");
                setContainer(ContainerName.HOME);
            }

            if (v.getId() == schedule.getId()) {
//                System.out.println("You clicked schedule");
                setContainer(ContainerName.SCHEDULE);
            }

            if (v.getId() == profile.getId()) {
//                System.out.println("You clicked profile");
                setContainer(ContainerName.PROFILE);
            }

            if (v.getId() == lessons.getId()) {
//                System.out.println("You clicked lessons");
                setContainer(ContainerName.LESSONS);
            }

            if (v.getId() == userHelp.getId()) {
//                System.out.println("You clicked notifications");
                setContainer(ContainerName.NOTIFICATION);
            }

            if (v.getId() == settings.getId()) {
//                System.out.println("You clicked settings");
                setContainer(ContainerName.SETTINGS);
//                resetApp();
//                setLoginFormContainer();
            }

            // но если кликнута кнопка изменений в расписании, нужно еще выкинуть контент от вк



            if (activeContainer == ContainerName.NOTIFICATION) {

                sendGetVKWallPostsRequest(new String[] {"40"});
                setLoadingToList(ContainerName.NOTIFICATION);

            }



        }

    }




    class scheduleDaysClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v)
        {
            if (v.getId() == monday.getId()) {
                if (!mondayIsActive) {
                    mondayIsActive = true;

                    if (tuesdayIsActive) {
                        tuesdayIsActive = false;
                        tuesday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (wednesdayIsActive) {
                        wednesdayIsActive = false;
                        wednesday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (thursdayIsActive) {
                        thursdayIsActive = false;
                        thursday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (fridayIsActive) {
                        fridayIsActive = false;
                        friday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (saturdayIsActive) {
                        saturdayIsActive = false;
                        saturday.setTextColor(getResources().getColor(R.color.greyColor));
                    }

                    monday.setTextColor(getResources().getColor(R.color.pinkColor));
                    if (activeScheduleWeek == 0) {
                        onGetScheduleOfGroupRequestCompleted("now", "0");
                    } else {
                        onGetScheduleOfGroupRequestCompleted("next", "0");
                    }
                } else {
                    mondayIsActive = false;
                    monday.setTextColor(getResources().getColor(R.color.greyColor));
                    if (activeScheduleWeek == 0) {
                        onGetScheduleOfGroupRequestCompleted("now", "");
                    } else {
                        onGetScheduleOfGroupRequestCompleted("next", "");
                    }
                }
            }

            if (v.getId() == tuesday.getId()) {
                if (!tuesdayIsActive) {
                    tuesdayIsActive = true;

                    if (mondayIsActive) {
                        mondayIsActive = false;
                        monday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (wednesdayIsActive) {
                        wednesdayIsActive = false;
                        wednesday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (thursdayIsActive) {
                        thursdayIsActive = false;
                        thursday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (fridayIsActive) {
                        fridayIsActive = false;
                        friday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (saturdayIsActive) {
                        saturdayIsActive = false;
                        saturday.setTextColor(getResources().getColor(R.color.greyColor));
                    }

                    tuesday.setTextColor(getResources().getColor(R.color.pinkColor));
                    if (activeScheduleWeek == 0) {
                        onGetScheduleOfGroupRequestCompleted("now", "1");
                    } else {
                        onGetScheduleOfGroupRequestCompleted("next", "1");
                    }
                } else {
                    tuesdayIsActive = false;
                    tuesday.setTextColor(getResources().getColor(R.color.greyColor));
                    if (activeScheduleWeek == 0) {
                        onGetScheduleOfGroupRequestCompleted("now", "");
                    } else {
                        onGetScheduleOfGroupRequestCompleted("next", "");
                    }
                }
            }

            if (v.getId() == wednesday.getId()) {
                if (!wednesdayIsActive) {
                    wednesdayIsActive = true;

                    if (mondayIsActive) {
                        mondayIsActive = false;
                        monday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (tuesdayIsActive) {
                        tuesdayIsActive = false;
                        tuesday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (thursdayIsActive) {
                        thursdayIsActive = false;
                        thursday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (fridayIsActive) {
                        fridayIsActive = false;
                        friday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (saturdayIsActive) {
                        saturdayIsActive = false;
                        saturday.setTextColor(getResources().getColor(R.color.greyColor));
                    }

                    wednesday.setTextColor(getResources().getColor(R.color.pinkColor));
                    if (activeScheduleWeek == 0) {
                        onGetScheduleOfGroupRequestCompleted("now", "2");
                    } else {
                        onGetScheduleOfGroupRequestCompleted("next", "2");
                    }
                } else {
                    wednesdayIsActive = false;
                    wednesday.setTextColor(getResources().getColor(R.color.greyColor));
                    if (activeScheduleWeek == 0) {
                        onGetScheduleOfGroupRequestCompleted("now", "");
                    } else {
                        onGetScheduleOfGroupRequestCompleted("next", "");
                    }
                }
            }

            if (v.getId() == thursday.getId()) {
                if (!thursdayIsActive) {
                    thursdayIsActive = true;

                    if (mondayIsActive) {
                        mondayIsActive = false;
                        monday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (tuesdayIsActive) {
                        tuesdayIsActive = false;
                        tuesday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (wednesdayIsActive) {
                        wednesdayIsActive = false;
                        wednesday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (fridayIsActive) {
                        fridayIsActive = false;
                        friday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (saturdayIsActive) {
                        saturdayIsActive = false;
                        saturday.setTextColor(getResources().getColor(R.color.greyColor));
                    }

                    thursday.setTextColor(getResources().getColor(R.color.pinkColor));
                    if (activeScheduleWeek == 0) {
                        onGetScheduleOfGroupRequestCompleted("now", "3");
                    } else {
                        onGetScheduleOfGroupRequestCompleted("next", "3");
                    }
                } else {
                    thursdayIsActive = false;
                    thursday.setTextColor(getResources().getColor(R.color.greyColor));
                    if (activeScheduleWeek == 0) {
                        onGetScheduleOfGroupRequestCompleted("now", "");
                    } else {
                        onGetScheduleOfGroupRequestCompleted("next", "");
                    }
                }
            }

            if (v.getId() == friday.getId()) {
                if (!fridayIsActive) {
                    fridayIsActive = true;

                    if (mondayIsActive) {
                        mondayIsActive = false;
                        monday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (tuesdayIsActive) {
                        tuesdayIsActive = false;
                        tuesday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (wednesdayIsActive) {
                        wednesdayIsActive = false;
                        wednesday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (thursdayIsActive) {
                        thursdayIsActive = false;
                        thursday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (saturdayIsActive) {
                        saturdayIsActive = false;
                        saturday.setTextColor(getResources().getColor(R.color.greyColor));
                    }

                    friday.setTextColor(getResources().getColor(R.color.pinkColor));
                    if (activeScheduleWeek == 0) {
                        onGetScheduleOfGroupRequestCompleted("now", "4");
                    } else {
                        onGetScheduleOfGroupRequestCompleted("next", "4");
                    }
                } else {
                    fridayIsActive = false;
                    friday.setTextColor(getResources().getColor(R.color.greyColor));
                    if (activeScheduleWeek == 0) {
                        onGetScheduleOfGroupRequestCompleted("now", "");
                    } else {
                        onGetScheduleOfGroupRequestCompleted("next", "");
                    }
                }
            }

            if (v.getId() == saturday.getId()) {
                if (!saturdayIsActive) {
                    saturdayIsActive = true;

                    if (mondayIsActive) {
                        mondayIsActive = false;
                        monday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (tuesdayIsActive) {
                        tuesdayIsActive = false;
                        tuesday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (wednesdayIsActive) {
                        wednesdayIsActive = false;
                        wednesday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (thursdayIsActive) {
                        thursdayIsActive = false;
                        thursday.setTextColor(getResources().getColor(R.color.greyColor));
                    }
                    if (fridayIsActive) {
                        fridayIsActive = false;
                        friday.setTextColor(getResources().getColor(R.color.greyColor));
                    }

                    saturday.setTextColor(getResources().getColor(R.color.pinkColor));
                    if (activeScheduleWeek == 0) {
                        onGetScheduleOfGroupRequestCompleted("now", "5");
                    } else {
                        onGetScheduleOfGroupRequestCompleted("next", "5");
                    }
                } else {
                    saturdayIsActive = false;
                    saturday.setTextColor(getResources().getColor(R.color.greyColor));
                    if (activeScheduleWeek == 0) {
                        onGetScheduleOfGroupRequestCompleted("now", "");
                    } else {
                        onGetScheduleOfGroupRequestCompleted("next", "");
                    }
                }
            }
        }
    }

    String currentTeacherId;
    String currentTeacherName;

    class teachersScheduleClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v)
        {
            currentTeacherId = "t" + v.getId();
            RelativeLayout box = findViewById(v.getId());
            TextView view = (TextView) box.getChildAt(0);
            currentTeacherName = view.getText().toString();
            sendGetScheduleOfTeacherRequest(new String[] {"now", currentTeacherId});
            teacherNowWeek = true;
            setContainer(ContainerName.TEACHERSCHEDULE);
            setLoadingToList(ContainerName.TEACHERSCHEDULE);
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
//            System.out.println(buffer);
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

//                        System.out.println("1: " +  readyExercisesByLesson);
//                        System.out.println("2: " +  readyExercisesByLessonVisits);


                        JSONObject valueInfo;
                        try {
                            valueInfo = readyExercisesByLessonVisits.getJSONObject(v.getId()+"").getJSONArray(value.getString("id")).getJSONObject(0);
                            String presence = valueInfo.getString("presence").equals("0") ? "нет" : "да";
                            String point = valueInfo.getString("point").toString().equals("null")  ? "нет" : valueInfo.getString("point");
                            if (point.equals("1")) point = "зачет";
                            String delay = valueInfo.getString("delay").toString().equals("null")  ? "нет" : "да";
                            String performance = valueInfo.getString("performance").equals("null") ? "нет" : "▲";
                            if (valueInfo.getString("performance").equals("2")) performance = "▼";



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