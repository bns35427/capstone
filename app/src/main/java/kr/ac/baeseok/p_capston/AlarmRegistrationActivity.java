package kr.ac.baeseok.p_capston; // <-- 재형님의 패키지명으로 변경

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat; // ActivityCompat 사용


import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;
import android.view.LayoutInflater;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import androidx.annotation.NonNull;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import android.provider.Settings;


public class AlarmRegistrationActivity extends AppCompatActivity {

    private static final String TAG = "AlarmRegistrationActivity";
    public static final String EXTRA_MEDICINE_NAME = "MEDICINE_NAME";
    public static final String EXTRA_MEDICINE_ID = "MEDICINE_ID";

    // 알림 권한 요청을 위한 Request Code (CapstonActivity와 동일해도 무방)
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;


    private TextView textViewAlarmRegistrationTitle;

    private LinearLayout linearLayoutMorningAlarm;
    private TextView textViewMorningTitle;
    private Button buttonSetTimeMorning;
    private TextView textViewSelectedTimeMorning;
    private LinearLayout linearLayoutDaysMorning;
    private Button[] buttonDaysMorning = new Button[7];
    private TextInputEditText editTextDosageMorning;
    private Button buttonRegisterAlarmMorning;

    private LinearLayout linearLayoutLunchAlarm;
    private TextView textViewLunchTitle;
    private Button buttonSetTimeLunch;
    private TextView textViewSelectedTimeLunch;
    private LinearLayout linearLayoutDaysLunch;
    private Button[] buttonDaysLunch = new Button[7];
    private TextInputEditText editTextDosageLunch;
    private Button buttonRegisterAlarmLunch;

    private LinearLayout linearLayoutEveningAlarm;
    private TextView textViewEveningTitle;
    private Button buttonSetTimeEvening;
    private TextView textViewSelectedTimeEvening;
    private LinearLayout linearLayoutDaysEvening;
    private Button[] buttonDaysEvening = new Button[7];
    private TextInputEditText editTextDosageEvening;
    private Button buttonRegisterAlarmEvening;

    private TextView textViewRegisteredAlarmsTitle;
    private LinearLayout linearLayoutRegisteredAlarms;

    private Button buttonCancelAlarmRegistration;
    private Button buttonSaveAllAlarms;

    // ★ All-medicines save 버튼
    private Button buttonSaveAllAlarmsAllMed;




    private int morningAlarmHour = -1, morningAlarmMinute = -1;
    private int lunchAlarmHour = -1, lunchAlarmMinute = -1;
    private int eveningAlarmHour = -1, eveningAlarmMinute = -1;
    private Set<Integer> morningSelectedDays = new HashSet<>();
    private Set<Integer> lunchSelectedDays = new HashSet<>();
    private Set<Integer> eveningSelectedDays = new HashSet<>();

    private final int[] calendarDays = {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    };

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String medicineId;
    private String medicineName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_registration);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        String currentUserId = null;
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        final String finalCurrentUserId = currentUserId;


        Intent intent = getIntent();
        if (intent != null) {
            medicineId = intent.getStringExtra(EXTRA_MEDICINE_ID);
            medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME);
            Log.d(TAG, "Received Medicine ID: " + medicineId + ", Name: " + medicineName);

            if (medicineId == null || medicineName == null) {
                Log.e(TAG, "Medicine ID or Name not received. Cannot set alarm.");
                Toast.makeText(this, "알람 설정 정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

        } else {
            Log.e(TAG, "Intent is null. Cannot get medicine info.");
            Toast.makeText(this, "알람 설정 정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // !!! 알림 권한 요청 !!! (Android 13 이상에서 필요)
        // AlarmRegistrationActivity에서도 알람을 설정/스케줄링하므로 권한 요청 필요
        requestNotificationPermission();

        // 알림 및 리마인더 권한 확안.
        requestExactAlarmPermissionIfNeeded();


        textViewAlarmRegistrationTitle = findViewById(R.id.textViewAlarmRegistrationTitle);
        if (textViewAlarmRegistrationTitle != null) {
            textViewAlarmRegistrationTitle.setText(medicineName + " 알람 등록");
        }

        linearLayoutMorningAlarm = findViewById(R.id.linearLayoutMorningAlarm);
        textViewMorningTitle = findViewById(R.id.textViewMorningTitle);
        buttonSetTimeMorning = findViewById(R.id.buttonSetTime_morning);
        textViewSelectedTimeMorning = findViewById(R.id.textViewSelectedTime_morning);
        linearLayoutDaysMorning = findViewById(R.id.linearLayoutDays_morning);
        editTextDosageMorning = findViewById(R.id.editTextDosage_morning);
        buttonRegisterAlarmMorning = findViewById(R.id.buttonRegisterAlarm_morning);

        int[] morningDayButtonIds = {R.id.buttonDayMon_morning, R.id.buttonDayTue_morning, R.id.buttonDayWed_morning, R.id.buttonDayThu_morning, R.id.buttonDayFri_morning, R.id.buttonDaySat_morning, R.id.buttonDaySun_morning};
        for (int i = 0; i < 7; i++) {
            buttonDaysMorning[i] = findViewById(morningDayButtonIds[i]);
        }

        linearLayoutLunchAlarm = findViewById(R.id.linearLayoutLunchAlarm);
        textViewLunchTitle = findViewById(R.id.textViewLunchTitle);
        buttonSetTimeLunch = findViewById(R.id.buttonSetTime_lunch);
        textViewSelectedTimeLunch = findViewById(R.id.textViewSelectedTime_lunch);
        linearLayoutDaysLunch = findViewById(R.id.linearLayoutDays_lunch);
        editTextDosageLunch = findViewById(R.id.editTextDosage_lunch);
        buttonRegisterAlarmLunch = findViewById(R.id.buttonRegisterAlarm_lunch);

        int[] lunchDayButtonIds = {R.id.buttonDayMon_lunch, R.id.buttonDayTue_lunch, R.id.buttonDayWed_lunch, R.id.buttonDayThu_lunch, R.id.buttonDayFri_lunch, R.id.buttonDaySat_lunch, R.id.buttonDaySun_lunch};
        for (int i = 0; i < 7; i++) {
            buttonDaysLunch[i] = findViewById(lunchDayButtonIds[i]);
        }

        LinearLayout linearLayoutEveningAlarm = findViewById(R.id.linearLayoutEveningAlarm);
        textViewEveningTitle = findViewById(R.id.textViewEveningTitle);
        buttonSetTimeEvening = findViewById(R.id.buttonSetTime_evening);
        textViewSelectedTimeEvening = findViewById(R.id.textViewSelectedTime_evening);
        linearLayoutDaysEvening = findViewById(R.id.linearLayoutDays_evening);
        editTextDosageEvening = findViewById(R.id.editTextDosage_evening);
        buttonRegisterAlarmEvening = findViewById(R.id.buttonRegisterAlarm_evening);

        int[] eveningDayButtonIds = {R.id.buttonDayMon_evening, R.id.buttonDayTue_evening, R.id.buttonDayWed_evening, R.id.buttonDayThu_evening, R.id.buttonDayFri_evening, R.id.buttonDaySat_evening, R.id.buttonDaySun_evening};
        for (int i = 0; i < 7; i++) {
            final int dayIndex = i;
            buttonDaysEvening[i] = findViewById(eveningDayButtonIds[i]);
        }

        textViewRegisteredAlarmsTitle = findViewById(R.id.textViewRegisteredAlarmsTitle);
        linearLayoutRegisteredAlarms = findViewById(R.id.linearLayoutRegisteredAlarms);

        buttonCancelAlarmRegistration = findViewById(R.id.buttonCancelAlarmRegistration);
        buttonSaveAllAlarms = findViewById(R.id.buttonSaveAllAlarms);


        buttonSetTimeMorning.setOnClickListener(v -> showTimePickerDialog(true, false, false));
        buttonSetTimeLunch.setOnClickListener(v -> showTimePickerDialog(false, true, false));
        buttonSetTimeEvening.setOnClickListener(v -> showTimePickerDialog(false, false, true));


        for (int i = 0; i < 7; i++) {
            final int dayIndex = i;
            buttonDaysMorning[i].setOnClickListener(v -> toggleDaySelection(buttonDaysMorning[dayIndex], morningSelectedDays, calendarDays[dayIndex]));
            buttonDaysLunch[i].setOnClickListener(v -> toggleDaySelection(buttonDaysLunch[dayIndex], lunchSelectedDays, calendarDays[dayIndex]));
            buttonDaysEvening[i].setOnClickListener(v -> toggleDaySelection(buttonDaysEvening[dayIndex], eveningSelectedDays, calendarDays[dayIndex]));
        }


        // 각 시간대별 등록 버튼 클릭 리스너
        buttonRegisterAlarmMorning.setOnClickListener(v -> {
            String dosage = editTextDosageMorning.getText() != null ? editTextDosageMorning.getText().toString() : "";
            saveAndScheduleAlarm(finalCurrentUserId, "아침", morningAlarmHour, morningAlarmMinute, new ArrayList<>(morningSelectedDays), dosage,medicineId, medicineName);
        });

        buttonRegisterAlarmLunch.setOnClickListener(v -> {
            String dosage = editTextDosageLunch.getText() != null ? editTextDosageLunch.getText().toString() : "";
            saveAndScheduleAlarm(finalCurrentUserId, "점심", lunchAlarmHour, lunchAlarmMinute, new ArrayList<>(lunchSelectedDays), dosage, medicineId, medicineName);
        });

        buttonRegisterAlarmEvening.setOnClickListener(v -> {
            String dosage = editTextDosageEvening.getText() != null ? editTextDosageEvening.getText().toString() : "";
            saveAndScheduleAlarm(finalCurrentUserId, "저녁", eveningAlarmHour, eveningAlarmMinute, new ArrayList<>(eveningSelectedDays), dosage, medicineId, medicineName);
        });


        // --- 하단 버튼 클릭 리스너 설정 ---
        buttonCancelAlarmRegistration.setOnClickListener(v -> finish()); // 취소 버튼: 현재 Activity 종료

        // "모두 저장" 버튼 클릭 리스너 (시간 설정된 알람만 확인하여 저장하고 스케줄링)
        buttonSaveAllAlarms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int savedCount = 0; // 저장된 알람 개수 카운트

                // 알림 권한이 있는지 확인한 후에만 저장 시도
                // checkNotificationPermission() 메서드 호출
                if (!checkNotificationPermission()) { // <-- 여기서 오류가 났었습니다.
                    Toast.makeText(AlarmRegistrationActivity.this, "알림 권한이 필요합니다. 앱 설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
                    // 알림 권한 요청 메서드 호출 (사용자가 직접 허용하도록 유도)
                    requestNotificationPermission(); // 이 메서드는 onCreate에도 있습니다. 필요시 여기서도 호출
                    return; // 권한 없으면 저장 중단
                }


                // 아침 알람 설정 확인 및 저장
                if (morningAlarmHour != -1 && !morningSelectedDays.isEmpty()) {
                    String dosage = editTextDosageMorning.getText() != null ? editTextDosageMorning.getText().toString() : "";
                    // saveAndScheduleAlarm 메서드는 내부에서 알림 권한 재확인 및 스케줄링 로직 포함
                    saveAndScheduleAlarm(finalCurrentUserId, "아침", morningAlarmHour, morningAlarmMinute, new ArrayList<>(morningSelectedDays), dosage, medicineId, medicineName);
                    savedCount++;
                } else {
                    Log.d(TAG, "아침 알람 설정 미완료 (시간 또는 요일). 저장 안 함.");
                }

                // 점심 알람 설정 확인 및 저장
                if (lunchAlarmHour != -1 && !lunchSelectedDays.isEmpty()) {
                    String dosage = editTextDosageLunch.getText() != null ? editTextDosageLunch.getText().toString() : "";
                    saveAndScheduleAlarm(finalCurrentUserId, "점심", lunchAlarmHour, lunchAlarmMinute, new ArrayList<>(lunchSelectedDays), dosage, medicineId, medicineName);
                    savedCount++;
                } else {
                    Log.d(TAG, "점심 알람 설정 미완료 (시간 또는 요일). 저장 안 함.");
                }

                // 저녁 알람 설정 확인 및 저장
                if (eveningAlarmHour != -1 && !eveningSelectedDays.isEmpty()) {
                    String dosage = editTextDosageEvening.getText() != null ? editTextDosageEvening.getText().toString() : "";
                    saveAndScheduleAlarm(finalCurrentUserId, "저녁", eveningAlarmHour, eveningAlarmMinute, new ArrayList<>(eveningSelectedDays), dosage, medicineId, medicineName);
                    savedCount++;
                } else {
                    Log.d(TAG, "저녁 알람 설정 미완료 (시간 또는 요일). 저장 안 함.");
                }

                // TODO: 다른 시간대 (예: 취침 전) 있다면 추가 확인 및 저장


                // 저장 완료 메시지 표시
                if (savedCount > 0) {
                    // saveAndScheduleAlarm 내부에서 이미 각 알람 저장 성공 시 토스트 메시지를 표시하므로
                    // 여기서는 총 몇 개가 저장되었는지 별도 메시지 필요 없을 수 있습니다.
                    // Toast.makeText(AlarmRegistrationActivity.this, savedCount + "개의 알람 설정이 저장 및 예약되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AlarmRegistrationActivity.this, "저장할 알람 설정이 없습니다.\n시간과 요일을 설정해주세요.", Toast.LENGTH_SHORT).show();
                }

                // 모두 저장 후 Activity 종료 (선택 사항)
                // finish();
            }
        });


        // --- Activity 시작 시 등록된 알람 목록 불러와 화면에 표시 ---
        if (medicineId != null && finalCurrentUserId != null) { // 약 ID와 사용자 ID가 유효한 경우에만 불러오기 시도
            loadRegisteredAlarms(finalCurrentUserId); // 등록된 알람 목록 불러오기
        }

        buttonSaveAllAlarmsAllMed = findViewById(R.id.buttonSaveAllAlarmsAllMed);
        buttonSaveAllAlarmsAllMed.setOnClickListener(v -> {

            // 1) 알림 권한 체크
            if (!checkNotificationPermission()) {
                Toast.makeText(this,
                        "알림 권한이 필요합니다. 앱 설정에서 권한을 허용해주세요.",
                        Toast.LENGTH_LONG).show();
                requestNotificationPermission();


                return;
            }

            // 2) 최소 하나라도 설정된 시간/요일이 있는지 확인
            boolean hasConfig =
                    (morningAlarmHour != -1 && !morningSelectedDays.isEmpty()) ||
                            (lunchAlarmHour   != -1 && !lunchSelectedDays.isEmpty())   ||
                            (eveningAlarmHour != -1 && !eveningSelectedDays.isEmpty());

            if (!hasConfig) {
                Toast.makeText(this,
                        "먼저 시간과 요일을 설정해주세요.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = mAuth.getCurrentUser().getUid();

            // 3) 내 모든 약 목록 가져오기
            db.collection("users").document(userId)
                    .collection("registeredMedicines")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Toast.makeText(this,
                                    "약 목록을 불러오지 못했습니다.",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int totalSaved = 0;

                        // ------------- ① 위치 : buttonSaveAllAlarmsAllMed.setOnClickListener 안 -------------
                        for (DocumentSnapshot doc : task.getResult()) {

                            // (1) 각 약의 ID/이름을 final 지역 변수로 보존
                            final String targetMedicineId   = doc.getId();
                            final String targetMedicineName = doc.getString("name");

                            /* ---- 아침 ---- */
                            if (morningAlarmHour != -1 && !morningSelectedDays.isEmpty()) {
                                String dosage = editTextDosageMorning.getText() != null
                                        ? editTextDosageMorning.getText().toString() : "";

                                // (2) 새 파라미터 2개(약ID, 약이름) 전달
                                saveAndScheduleAlarm(userId,
                                        "아침",
                                        morningAlarmHour, morningAlarmMinute,
                                        new ArrayList<>(morningSelectedDays),
                                        dosage,
                                        targetMedicineId,        // ★
                                        targetMedicineName);     // ★

                                totalSaved++;
                            }

                            /* ---- 점심 ---- */
                            if (lunchAlarmHour != -1 && !lunchSelectedDays.isEmpty()) {
                                String dosage = editTextDosageLunch.getText() != null
                                        ? editTextDosageLunch.getText().toString() : "";

                                saveAndScheduleAlarm(userId,
                                        "점심",
                                        lunchAlarmHour, lunchAlarmMinute,
                                        new ArrayList<>(lunchSelectedDays),
                                        dosage,
                                        targetMedicineId,        // ★
                                        targetMedicineName);     // ★

                                totalSaved++;
                            }

                            /* ---- 저녁 ---- */
                            if (eveningAlarmHour != -1 && !eveningSelectedDays.isEmpty()) {
                                String dosage = editTextDosageEvening.getText() != null
                                        ? editTextDosageEvening.getText().toString() : "";

                                saveAndScheduleAlarm(userId,
                                        "저녁",
                                        eveningAlarmHour, eveningAlarmMinute,
                                        new ArrayList<>(eveningSelectedDays),
                                        dosage,
                                        targetMedicineId,        // ★
                                        targetMedicineName);     // ★

                                totalSaved++;
                            }
                        }


                        Toast.makeText(this,
                                totalSaved + "개의 알람이 모든 약에 적용되었습니다.",
                                Toast.LENGTH_SHORT).show();
                    });
        });







    } // <-- onCreate 끝


    // --- 알림 권한 요청 메서드 (Android 13 이상) ---
    // AlarmRegistrationActivity에서도 알람을 설정/스케줄링하므로 권한 요청 메서드가 필요합니다.
    private void requestNotificationPermission() {
        // Android 13 (API 33) 이상인지 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Build.VERSION_CODES.TIRAMISU는 API 33
            // 알림 권한이 부여되었는지 확인
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // 권한이 부여되지 않았다면 사용자에게 권한 요청
                Log.d(TAG, "AlarmRegistration: Requesting POST_NOTIFICATIONS permission...");
                // 권한 요청 팝업을 띄웁니다.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS); // 정의된 Request Code 사용
            } else {
                Log.d(TAG, "AlarmRegistration: POST_NOTIFICATIONS permission already granted.");
            }
        } else {
            // Android 13 미만 버전에서는 Manifest에 선언된 권한이 설치 시 자동으로 부여됩니다.
            Log.d(TAG, "AlarmRegistration: POST_NOTIFICATIONS permission automatically granted on this Android version.");
        }
    }

    // 권한 요청 결과 처리 (사용자가 권한 허용 또는 거부 시 호출됨)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) { // 우리가 요청한 알림 권한 요청에 대한 응답이라면
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 사용자가 알림 권한을 허용한 경우
                Log.d(TAG, "AlarmRegistration: POST_NOTIFICATIONS permission granted by user.");
                Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                // TODO: 권한 허용 후 필요한 작업 수행 (예: 저장된 알람 다시 스케줄링 등)
            } else {
                // 사용자가 알림 권한을 거부한 경우
                Log.d(TAG, "AlarmRegistration: POST_NOTIFICATIONS permission denied by user.");
                Toast.makeText(this, "알림 권한이 거부되어 알림을 받을 수 없습니다.", Toast.LENGTH_LONG).show();
                // TODO: 알림 기능이 제한될 수 있음을 사용자에게 안내하는 UI 표시 등
            }
        }
        // 다른 권한 요청 결과 처리 추가
    }

    // --- 알림 권한이 있는지 확인하는 헬퍼 메서드 ---
    // AlarmRegistrationActivity에서 알림 권한 체크를 위해 필요한 메서드
    private boolean checkNotificationPermission() { // <-- 이 메서드가 여기에 추가됩니다.
        // Android 13 (API 33) 이상에서만 런타임 알림 권한 체크가 필요합니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Build.VERSION_CODES.TIRAMISU는 API 33
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        // Android 13 미만에서는 Manifest에 선언되어 있으면 항상 허용된 것으로 간주
        return true;
    }


    // --- TimePickerDialog를 띄우는 헬퍼 메서드 ---
    private void showTimePickerDialog(boolean isMorning, boolean isLunch, boolean isEvening) {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // 이미 설정된 시간이 있으면 해당 시간으로 TimePicker 초기화
        if (isMorning && morningAlarmHour != -1) {
            hour = morningAlarmHour;
            minute = morningAlarmMinute;
        } else if (isLunch && lunchAlarmHour != -1) {
            hour = lunchAlarmHour;
            minute = lunchAlarmMinute;
        } else if (isEvening && eveningAlarmHour != -1) {
            hour = eveningAlarmHour;
            minute = eveningAlarmMinute;
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
                        // 선택된 시간을 텍스트 뷰에 표시
                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);

                        if (isMorning) {
                            morningAlarmHour = selectedHour;
                            morningAlarmMinute = selectedMinute;
                            textViewSelectedTimeMorning.setText(formattedTime);
                            Log.d(TAG, "아침 알람 시간 설정: " + formattedTime);
                        } else if (isLunch) {
                            lunchAlarmHour = selectedHour;
                            lunchAlarmMinute = selectedMinute;
                            textViewSelectedTimeLunch.setText(formattedTime);
                            Log.d(TAG, "점심 알람 시간 설정: " + formattedTime);
                        } else if (isEvening) {
                            eveningAlarmHour = selectedHour;
                            eveningAlarmMinute = selectedMinute;
                            textViewSelectedTimeEvening.setText(formattedTime);
                            Log.d(TAG, "저녁 알람 시간 설정: " + formattedTime);
                        }
                    }
                }, hour, minute, false); // is24HourView: true (24시간 형식), false (오전/오후)

        timePickerDialog.show(); // TimePicker 다이얼로그 표시
    }

    // --- 요일 선택 상태를 토글하는 헬퍼 메서드 ---
    private void toggleDaySelection(Button button, Set<Integer> selectedDaysSet, int dayOfWeek) {
        boolean isSelected = selectedDaysSet.contains(dayOfWeek); // 이미 선택된 요일인지 확인

        if (isSelected) {
            // 이미 선택되어 있다면 선택 해제
            selectedDaysSet.remove(dayOfWeek);
            // 버튼 배경 및 텍스트 색상 변경 (선택 해제 상태)
            button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
            button.setTextColor(ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_primary));
            Log.d(TAG, "요일 선택 해제: " + getDayName(dayOfWeek));
        } else {
            // 선택되어 있지 않다면 선택
            selectedDaysSet.add(dayOfWeek);
            // 버튼 배경 및 텍스트 색상 변경 (선택 상태)
            button.setBackgroundColor(ContextCompat.getColor(this, com.google.android.material.R.color.design_default_color_primary));
            button.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            Log.d(TAG, "요일 선택: " + getDayName(dayOfWeek));
        }
    }

    // Calendar 요일 값을 한글 요일 문자열로 변환
    private String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "월";
            case Calendar.TUESDAY: return "화";
            case Calendar.WEDNESDAY: return "수";
            case Calendar.THURSDAY: return "목";
            case Calendar.FRIDAY: return "금";
            case Calendar.SATURDAY: return "토";
            case Calendar.SUNDAY: return "일";
            default: return "";
        }
    }

    // 선택된 요일 목록 (Calendar 값 리스트)을 "반복: 월, 수, 금" 형태의 문자열로 변환 (표시용)
    private String selectedDaysListToString(List<Integer> selectedDays) {
        if (selectedDays == null || selectedDays.isEmpty()) {
            return "미설정";
        }
        List<String> dayNames = new ArrayList<>();
        List<Integer> sortedDays = new ArrayList<>(selectedDays); // 요일 순서대로 정렬 (월~일)
        Collections.sort(sortedDays);

        for (int day : sortedDays) {
            // Calendar.DAY_OF_WEEK 값에 해당하는 한글 요일 이름을 dayNames 리스트에 추가
            switch (day) {
                case Calendar.MONDAY: dayNames.add("월"); break;
                case Calendar.TUESDAY: dayNames.add("화"); break;
                case Calendar.WEDNESDAY: dayNames.add("수"); break;
                case Calendar.THURSDAY: dayNames.add("목"); break;
                case Calendar.FRIDAY: dayNames.add("금"); break;
                case Calendar.SATURDAY: dayNames.add("토"); break;
                case Calendar.SUNDAY: dayNames.add("일"); break;
            }
        }
        // dayNames 리스트의 요일 이름들을 콤마와 공백으로 합쳐서 "반복: " 접두사와 함께 반환
        return "반복: " + String.join(", ", dayNames);
    }


    // --- 알람 설정 데이터를 Firestore에 저장하고 시스템 알람을 스케줄링하는 메서드 ---
    // 이 메서드는 개별 "등록" 버튼이나 "모두 저장" 버튼에서 호출됩니다.
    private void saveAndScheduleAlarm(String userId, String timeOfDayLabel, int hour, int minute, List<Integer> selectedDays, String dosage, String targetMedicineId,String targetMedicineName) {
        // 알람 저장에 필요한 정보가 모두 있는지 확인
        if (userId == null || medicineId == null || medicineName == null || db == null) {
            Toast.makeText(this, "사용자 또는 약 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        // 알람 설정 입력이 유효한지 확인 (이중 체크, 이미 호출 전에 확인했지만 안전하게)
        if (hour == -1 || selectedDays == null || selectedDays.isEmpty()) {
            Toast.makeText(this, timeOfDayLabel + " 알람 시간과 요일을 설정해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 시스템 알람 (PendingIntent)에 사용할 고유 ID 생성 (랜덤 + 타임스탬프 일부)
        int systemAlarmId = (int) (System.currentTimeMillis() % 100000) + new Random().nextInt(100000);

        // Firestore에 저장할 알람 데이터 Map 생성
        Map<String, Object> alarmData = new HashMap<>();
        alarmData.put("timeOfDayLabel", timeOfDayLabel);
        alarmData.put("hour", hour);
        alarmData.put("minute", minute);
        alarmData.put("days", selectedDays); // 요일 목록 저장
        alarmData.put("dosage", dosage); // 복용량/메모 저장
        alarmData.put("systemAlarmId", systemAlarmId); // 시스템 알람 ID 저장
        alarmData.put("medicineName", targetMedicineName); // 약 이름 필드를 알람 문서에 추가 저장
        alarmData.put("userId", userId); // 사용자 ID 필드를 알람 문서에 추가 저장 (CollectionGroup 쿼리 필터링용)


        // Firestore에 알람 데이터 저장 (users/userId/registeredMedicines/medicineId/alarms 서브컬렉션)
        // add() 메서드는 새 문서를 추가하고 Firestore가 자동으로 문서 ID를 생성합니다.
        db.collection("users").document(userId)
                .collection("registeredMedicines").document(targetMedicineId) // 현재 약 문서 지정
                .collection("alarms") // alarms 서브컬렉션에 추가
                .add(alarmData) // 새 문서로 추가
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() { // 저장 성공 시 호출될 콜백
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // documentReference.getId()로 새로 생성된 알람 문서의 ID를 얻을 수 있습니다.
                        Log.d(TAG, timeOfDayLabel + " Alarm data saved to Firestore: " + documentReference.getId());
                        Toast.makeText(AlarmRegistrationActivity.this, timeOfDayLabel + " 알람 설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();

                        // Firestore 저장 성공 후 시스템 알람 스케줄링
                        // 새로 생성된 알람 문서 ID와 시스템 알람 ID를 함께 전달합니다.
                        scheduleSystemAlarm(documentReference.getId(), systemAlarmId, timeOfDayLabel, hour, minute, selectedDays, dosage, targetMedicineName);


                        // 필요하면 목록 새로고침 (targetMedicineId 사용)
                        if (targetMedicineId.equals(medicineId)) { // 현재 화면의 약과 같을 때만
                            loadRegisteredAlarms(userId);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() { // 저장 실패 시 호출될 콜백
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error saving alarm data to Firestore.", e);
                        Toast.makeText(AlarmRegistrationActivity.this, timeOfDayLabel + " 알람 설정 저장 실패.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- 시스템 알람을 스케줄링하는 메서드 ---
    // (AlarmReceiver가 호출될 PendingIntent를 AlarmManager에 등록)
    private void scheduleSystemAlarm(String alarmDocId, int systemAlarmId, String timeOfDayLabel, int hour, int minute, List<Integer> selectedDays, String dosage, String notiMedicineName) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null. Cannot schedule system alarm.");
            Toast.makeText(this, "알람 설정에 실패했습니다. (시스템 오류)", Toast.LENGTH_SHORT).show();
            return;
        }

        // 알람 스케줄링 권한 확인 (Android 12 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Build.VERSION_CODES.S는 API 31
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "SCHEDULE_EXACT_ALARM permission is not granted. Cannot schedule exact alarm.");
                Toast.makeText(this, timeOfDayLabel + " 알람: 정확한 알람 설정을 위해 '알람 및 미리 알림' 권한이 필요합니다.\n앱 설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
                requestExactAlarmPermissionIfNeeded();
                return; // 권한이 없으므로 스케줄링 중단
            }
        }

        // AlarmReceiver를 호출할 Intent 생성
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        // 알람 정보를 Intent Extra에 담아 Receiver로 전달 (알람이 울릴 때 Receiver에서 사용할 데이터)
        alarmIntent.putExtra(AlarmReceiver.EXTRA_ALARM_DOC_ID, alarmDocId); // Firestore 알람 문서 ID
        alarmIntent.putExtra(AlarmReceiver.EXTRA_SYSTEM_ALARM_ID, systemAlarmId); // 시스템 알람 ID
        alarmIntent.putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, notiMedicineName); // 약 이름
        alarmIntent.putExtra(AlarmReceiver.EXTRA_TIME_OF_DAY_LABEL, timeOfDayLabel); // 시간대
        alarmIntent.putExtra(AlarmReceiver.EXTRA_HOUR, hour); // 시
        alarmIntent.putExtra(AlarmReceiver.EXTRA_MINUTE, minute); // 분
        alarmIntent.putExtra(AlarmReceiver.EXTRA_DAYS, new ArrayList<>(selectedDays)); // 요일 목록 (ArrayList<Integer>로 변환하여 전달)
        alarmIntent.putExtra(AlarmReceiver.EXTRA_DOSAGE, dosage); // 복용량/메모
        // TODO: userId를 AlarmReceiver로 전달해야 할 수도 있습니다 (Receiver에서 Firestore 작업 시)
        // alarmIntent.putExtra(AlarmReceiver.EXTRA_USER_ID, userId);


        // PendingIntent 생성 (알람 시간이 되었을 때 실행될 Intent를 감쌈)
        // PendingIntent의 요청 코드는 알람 업데이트/취소 시 사용되므로 각 알람 설정마다 고유해야 합니다.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                systemAlarmId, // 시스템 알람 ID를 요청 코드로 사용 (고유해야 함)
                alarmIntent,
                // FLAG_IMMUTABLE: 생성된 PendingIntent는 변경되지 않음을 나타냄 (API 23 이상 권장)
                // FLAG_UPDATE_CURRENT: 동일한 요청 코드의 PendingIntent가 이미 있다면 Extra 데이터만 업데이트 (재스케줄링 시 유용)
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // TODO: 이 함수는 설정된 시간과 요일을 기준으로 '현재 시각'을 고려하여 '가장 가까운 다음 알람 시각' 하나만 계산합니다.
        // TODO: 요일 반복 알람을 완벽하게 구현하려면, AlarmReceiver가 알람 수신 후 '다음 알람 시각'을 계산하여 AlarmManager에 다시 스케줄링하는 로직이 필요합니다.

        // 설정된 시간과 요일을 기준으로 다음 알람 시각 계산 (함수 호출 시점의 현재 시각 기준)
        Calendar nextAlarmCalendar = getNextAlarmCalendar(hour, minute, selectedDays);

        if (nextAlarmCalendar == null) {
            Log.w(TAG, timeOfDayLabel + " 알람: 다음 알람 시각 계산 불가. 스케줄링 안 함.");
            Toast.makeText(this, timeOfDayLabel + " 알람: 유효한 다음 알람 시각이 없습니다.", Toast.LENGTH_SHORT).show();
            return; // 다음 알람 시각 계산 실패 시 스케줄링 중단
        }

        long triggerTime = nextAlarmCalendar.getTimeInMillis(); // 계산된 다음 알람 시각의 Milliseconds 값
        Log.d(TAG, timeOfDayLabel + " 알람: 다음 알람 시각 계산됨 - " + nextAlarmCalendar.getTime().toString());

        // 알람 스케줄링 API 선택 (권한 확인 후 실행)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // API 23 (Marshmallow) 이상
            // setExactAndAllowWhileIdle: Doze 모드에서도 정확한 시간에 알람 실행 (SCHEDULE_EXACT_ALARM 권한 필요)
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            Log.d(TAG, timeOfDayLabel + " 알람 설정됨 (setExactAndAllowWhileIdle): " + String.format(Locale.getDefault(), "%02d:%02d", hour, minute) + ", Next: " + nextAlarmCalendar.getTime().toString());
        } else { // API 23 미만
            // set: 이전 버전에서 사용되던 정확한 알람
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            Log.d(TAG, timeOfDayLabel + " 알람 설정됨 (set): " + String.format(Locale.getDefault(), "%02d:%02d", hour, minute) + ", Next: " + nextAlarmCalendar.getTime().toString());
        }
    }

    // --- 설정된 시간과 요일을 기준으로 가장 가까운 다음 알람 시각을 계산하는 메서드 ---
    // (함수 호출 시점의 '현재 시각'을 기준으로 계산)
    private Calendar getNextAlarmCalendar(int hour, int minute, List<Integer> selectedDays) {
        if (hour == -1 || minute == -1 || selectedDays == null || selectedDays.isEmpty()) {
            return null; // 알람 설정 정보 불충분
        }

        Calendar now = Calendar.getInstance(); // 함수 호출 시점의 현재 시각
        Calendar nextAlarm = null; // 다음 알람 시각 후보

        boolean foundNext = false;

        // 오늘부터 시작하여 다음 7일 동안 설정된 요일 중 가장 가까운 미래 시각 찾기
        for (int i = 0; i < 7; i++) { // 최대 다음 7일 이내에 다음 알람이 있어야 함
            Calendar checkDay = (Calendar) now.clone(); // 현재 날짜/시간 복사
            checkDay.add(Calendar.DAY_OF_YEAR, i); // 오늘부터 i일 후의 날짜

            int dayOfWeekToCheck = checkDay.get(Calendar.DAY_OF_WEEK); // 해당 날짜의 요일

            // 설정된 요일 목록에 해당 요일이 포함되어 있는지 확인
            if (selectedDays.contains(dayOfWeekToCheck)) {
                // 해당 요일의 설정 시간으로 Calendar 객체 생성
                Calendar potentialNextAlarm = (Calendar) checkDay.clone();
                potentialNextAlarm.set(Calendar.HOUR_OF_DAY, hour);
                potentialNextAlarm.set(Calendar.MINUTE, minute);
                potentialNextAlarm.set(Calendar.SECOND, 0);
                potentialNextAlarm.set(Calendar.MILLISECOND, 0);

                // 현재 시각 (now) 보다 미래 시각이라면 -> 다음 알람 시각 후보
                if (potentialNextAlarm.getTimeInMillis() > now.getTimeInMillis()) {
                    nextAlarm = potentialNextAlarm; // 가장 가까운 미래 시각 발견
                    foundNext = true;
                    break; // 가장 가까운 미래 시각을 찾았으므로 루프 종료
                }
                // 만약 potentialNextAlarm <= now 이면, 해당 요일의 이 시간은 이미 지났으므로 건너뛰고 다음 날짜 확인
            }
        }

        if (!foundNext) {
            // 다음 7일 안에 설정된 요일을 찾지 못한 경우 (예외적 상황이지만 발생 가능)
            Log.w(TAG, "다음 7일 안에 설정된 알람 요일을 찾을 수 없습니다.");
            return null;
        }

        return nextAlarm;
    }


    // --- Firestore에서 등록된 알람 목록 (현재 약에 대한) 불러와 화면에 표시하는 메서드 ---
    // 이 메서드는 현재 Activity의 medicineId에 해당하는 알람만 불러옵니다.
    private void loadRegisteredAlarms(String userId) {
        // 필요한 정보가 모두 있는지 확인
        if (userId == null || medicineId == null || db == null || linearLayoutRegisteredAlarms == null) {
            Log.w(TAG, "Cannot load registered alarms: User, Medicine ID, DB or Layout is null.");
            addEmptyAlarmListMessage("알람 목록을 불러올 수 없습니다 (정보 부족).");
            return;
        }

        // 기존에 표시된 목록을 모두 제거하여 중복 표시 방지
        linearLayoutRegisteredAlarms.removeAllViews();
        Log.d(TAG, "Cleared existing registered alarm items for loading.");

        // Firestore에서 현재 약의 alarms 서브컬렉션 데이터 가져오기
        db.collection("users").document(userId)
                .collection("registeredMedicines").document(medicineId)
                .collection("alarms") // alarms 서브컬렉션
                .orderBy("hour").orderBy("minute") // 시간과 분 순서로 정렬
                .get() // 비동기 데이터 가져오기 요청
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() { // 작업 완료 시 호출될 콜백
                    @Override // OnCompleteListener 인터페이스의 추상 메서드 구현
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) { // 데이터 가져오기 성공
                            if (task.getResult() != null && !task.getResult().isEmpty()) {
                                Log.d(TAG, "Loaded " + task.getResult().size() + " registered alarms.");
                                // 가져온 알람 문서 목록을 화면에 표시
                                displayRegisteredAlarms(task.getResult().getDocuments());
                            } else { // 등록된 알람 문서가 없는 경우
                                Log.d(TAG, "No registered alarms found for this medicine.");
                                addEmptyAlarmListMessage("등록된 알람이 없습니다.");
                            }
                        } else { // 데이터 가져오기 실패
                            Log.e(TAG, "Error getting registered alarms from Firestore.", task.getException()); // 오류 상세 정보 로그 출력
                            addEmptyAlarmListMessage("알람 목록 불러오기 실패.");
                            Toast.makeText(AlarmRegistrationActivity.this, "알람 목록 불러오기 실패.", Toast.LENGTH_SHORT).show();
                        }
                    } // onComplete 메서드 끝
                }); // addOnCompleteListener 끝
    }

    // --- 불러온 알람 목록 (현재 약에 대한)을 화면에 동적으로 표시하는 메서드 ---
    private void displayRegisteredAlarms(List<DocumentSnapshot> alarmDocuments) {
        // 목록을 표시할 레이아웃 컨테이너나 문서 목록이 유효한지 확인
        if (linearLayoutRegisteredAlarms == null || alarmDocuments == null || alarmDocuments.isEmpty()) {
            addEmptyAlarmListMessage("등록된 알람이 없습니다."); // 목록이 비어있거나 유효하지 않으면 메시지 표시
            return;
        }

        linearLayoutRegisteredAlarms.removeAllViews(); // 다시 한번 비우기 전에 (안전장치)

        LayoutInflater inflater = getLayoutInflater(); // 레이아웃(XML)을 뷰 객체로 만들 때 사용

        for (DocumentSnapshot document : alarmDocuments) { // 가져온 각 알람 문서에 대해 반복
            // Firestore 문서에서 알람 데이터 가져오기
            String alarmDocId = document.getId(); // 알람 문서의 Firestore 고유 ID
            String timeOfDayLabel = document.getString("timeOfDayLabel"); // 예: "아침", "점심", "저녁"
            Long hourLong = document.getLong("hour"); // Firestore Number 타입은 Long으로 가져옴
            Long minuteLong = document.getLong("minute");
            List<Long> daysLong = (List<Long>) document.get("days"); // Firestore Array 타입은 List<Long>으로 가져옴
            String dosage = document.getString("dosage");
            Long systemAlarmIdLong = document.getLong("systemAlarmId");
            String medicineName = document.getString("medicineName"); // 알람 문서에 저장된 약 이름 가져오기


            // 가져온 데이터가 유효한지 최소한의 체크
            if (hourLong == null || minuteLong == null || daysLong == null || systemAlarmIdLong == null || medicineName == null) {
                Log.w(TAG, "Skipping invalid or incomplete alarm data from Firestore: " + alarmDocId);
                continue; // 필수 필드가 누락된 경우 해당 알람 항목은 건너뛰기
            }

            // Long 타입을 int나 List<Integer> 등으로 변환
            int hour = hourLong.intValue();
            int minute = minuteLong.intValue();
            List<Integer> days = new ArrayList<>();
            for (Long day : daysLong) {
                days.add(day.intValue());
            }
            int systemAlarmId = systemAlarmIdLong.intValue();


            // item_registered_alarm.xml 레이아웃을 현재 Activity의 Context를 사용하여 뷰 객체로 만듦
            View itemView = inflater.inflate(R.layout.item_registered_alarm, linearLayoutRegisteredAlarms, false);

            // 생성된 뷰 (itemView) 안에서 item_registered_alarm.xml에 정의된 UI 요소들을 찾습니다.
            TextView textViewAlarmTime = itemView.findViewById(R.id.textViewAlarmTime);
            TextView textViewAlarmDays = itemView.findViewById(R.id.textViewAlarmDays);
            TextView textViewAlarmDosage = itemView.findViewById(R.id.textViewAlarmDosage);
            Button buttonDeleteAlarm = itemView.findViewById(R.id.buttonDeleteAlarm);

            // --- 가져온 알람 데이터를 UI 요소에 설정 ---
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            // 약 이름을 함께 표시
            if (textViewAlarmTime != null) textViewAlarmTime.setText(medicineName + " - " + timeOfDayLabel + " " + formattedTime);
            // 요일 정보 표시
            if (textViewAlarmDays != null) textViewAlarmDays.setText(selectedDaysListToString(days)); // selectedDaysListToString 헬퍼 메서드 사용
            // 복용량/메모 표시 (데이터가 없으면 숨김)
            if (textViewAlarmDosage != null) {
                if (dosage != null && !dosage.trim().isEmpty()) {
                    textViewAlarmDosage.setText("복용량/메모: " + dosage);
                    textViewAlarmDosage.setVisibility(View.VISIBLE); // 데이터가 있으면 보이게
                } else {
                    textViewAlarmDosage.setVisibility(View.GONE); // 데이터가 없으면 숨김
                }
            }

            // --- 삭제 버튼 클릭 리스너 설정 ---
            if (buttonDeleteAlarm != null) {
                // 클릭 시점의 현재 로그인 사용자 ID를 가져와서 삭제 메서드에 전달
                final String finalCurrentUserId = mAuth.getCurrentUser().getUid();
                buttonDeleteAlarm.setOnClickListener(v -> {
                    // 알람 삭제 및 시스템 알람 취소 메서드 호출
                    // 현재 Activity의 medicineId, 현재 알람의 문서 ID, 시스템 알람 ID를 전달
                    cancelAlarm(finalCurrentUserId, alarmDocId, systemAlarmId);
                });
            }

            // 완성된 항목 뷰를 linearLayoutRegisteredAlarms 컨테이너에 추가하여 화면에 표시
            linearLayoutRegisteredAlarms.addView(itemView);
        }

        // 목록이 비어 있으면 "등록된 알람이 없습니다" 메시지 표시
        if (alarmDocuments.isEmpty()) {
            addEmptyAlarmListMessage("등록된 알람이 없습니다.");
        }
    }

    // 등록된 알람이 없을 때 또는 불러오기 실패 시 메시지 표시 (현재 약에 대한)
    private void addEmptyAlarmListMessage(String message) {
        // 목록 컨테이너가 유효한지 확인
        if (linearLayoutRegisteredAlarms != null) {
            linearLayoutRegisteredAlarms.removeAllViews(); // 혹시 있을 수 있는 기존 뷰 삭제
            // 메시지를 표시할 TextView를 동적으로 생성
            TextView emptyMessage = new TextView(this);
            emptyMessage.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            emptyMessage.setText(message); // 메시지 텍스트 설정
            emptyMessage.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); // 중앙 정렬
            emptyMessage.setPadding(0, 16, 0, 16); // 상하 패딩
            linearLayoutRegisteredAlarms.addView(emptyMessage); // 컨테이너에 TextView 추가
        }
    }

    // --- 알람 삭제 및 시스템 알람 취소 메서드 ---
    // (현재 약에 대한 특정 알람 삭제)
    private void cancelAlarm(String userId, String alarmDocId, int systemAlarmId) {
        // 삭제에 필요한 정보가 모두 있는지 확인
        if (userId == null || medicineId == null || alarmDocId == null || db == null) {
            Log.w(TAG, "Cannot cancel alarm: User, Medicine ID, Alarm Doc ID, or DB is null.");
            Toast.makeText(this, "알람 삭제 실패 (정보 부족).", Toast.LENGTH_SHORT).show();
            return;
        }

        // 시스템 알람 취소
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // 스케줄링 할 때와 동일한 Intent 및 요청 코드(systemAlarmId)를 사용하여 PendingIntent 생성
            Intent alarmIntent = new Intent(this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    systemAlarmId, // 스케줄링 시 사용한 ID와 동일
                    alarmIntent,
                    // FLAG_NO_CREATE: PendingIntent가 없으면 새로 생성하지 않고 null 반환
                    // FLAG_IMMUTABLE: API 23 이상에서 권장
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (pendingIntent != null) {
                // PendingIntent가 존재하면 알람 취소
                alarmManager.cancel(pendingIntent);
                Log.d(TAG, "System alarm cancelled with ID: " + systemAlarmId);
            } else {
                // PendingIntent를 찾을 수 없는 경우 (이미 취소되었거나 스케줄링되지 않았을 수 있음)
                Log.w(TAG, "PendingIntent not found for ID: " + systemAlarmId + ". System alarm may not have been scheduled or already cancelled.");
            }
        } else {
            Log.e(TAG, "AlarmManager is null. Cannot cancel system alarm.");
        }

        // Firestore에서 알람 데이터 삭제 (현재 약 문서 아래의 특정 알람 문서 삭제)
        db.collection("users").document(userId)
                .collection("registeredMedicines").document(medicineId) // 현재 Activity의 medicineId 사용
                .collection("alarms").document(alarmDocId) // 삭제할 알람 문서 지정
                .delete() // 문서 삭제 요청
                .addOnSuccessListener(aVoid -> { // 삭제 성공 시
                    Log.d(TAG, "Alarm data deleted from Firestore: " + alarmDocId);
                    Toast.makeText(this, "알람이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    // 삭제 후 알람 목록 새로고침
                    loadRegisteredAlarms(userId); // 사용자 ID 전달
                })
                .addOnFailureListener(e -> { // 삭제 실패 시
                    Log.e(TAG, "Error deleting alarm data from Firestore.", e);
                    Toast.makeText(this, "알람 삭제 실패.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Android 12(API 31)+ 에서 ‘정확한 알람(알람 및 리마인더)’ 권한이
     * 꺼져 있으면 설정 화면으로 이동시켜 사용자가 직접 허용하도록 유도한다.
     */
    private void requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                // 아직 허용되지 않았을 때 ‘알람 및 리마인더’ 설정 화면으로 이동
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(
                        this,
                        "정확한 알람(알람 및 리마인더) 권한을 허용해 주세요.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }



    // Activity가 소멸될 때 호출
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AlarmRegistrationActivity destroyed.");
    }
}
