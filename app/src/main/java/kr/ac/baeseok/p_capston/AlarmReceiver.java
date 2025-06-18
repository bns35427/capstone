package kr.ac.baeseok.p_capston; // <-- 재형님의 패키지명으로 변경

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Calendar; // Calendar 사용
import android.app.AlarmManager; // AlarmManager 사용
import android.app.PendingIntent; // PendingIntent 사용
import android.os.Build; // Build.VERSION 사용
import android.Manifest; // Manifest.permission 사용
import android.content.pm.PackageManager; // PackageManager 사용
import androidx.core.content.ContextCompat; // ContextCompat 사용
import java.lang.SecurityException;


public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "medicine_alarm_channel";
    private static final CharSequence CHANNEL_NAME = "복약 알림";
    private static final String CHANNEL_DESCRIPTION = "설정된 복약 시간에 알림을 받습니다.";

    // AlarmRegistrationActivity에서 Intent Extra로 보낼 때 사용할 키 정의
    public static final String EXTRA_ALARM_DOC_ID = "ALARM_DOC_ID";
    public static final String EXTRA_SYSTEM_ALARM_ID = "SYSTEM_ALARM_ID";
    public static final String EXTRA_MEDICINE_NAME = "MEDICINE_NAME";
    public static final String EXTRA_TIME_OF_DAY_LABEL = "TIME_OF_DAY_LABEL";
    public static final String EXTRA_HOUR = "HOUR"; // 시
    public static final String EXTRA_MINUTE = "MINUTE"; // 분
    public static final String EXTRA_DAYS = "DAYS"; // 요일 목록 (ArrayList<Integer>)
    public static final String EXTRA_DOSAGE = "DOSAGE"; // 복용량/메모
    // TODO: userId 필드를 AlarmRegistrationActivity에서 저장하고 Receiver로 전달한다면,
    // TODO: 재스케줄링 시 userId를 사용하여 Firestore 데이터를 업데이트하는 등의 로직을 추가할 수 있습니다.
    // public static final String EXTRA_USER_ID = "USER_ID";


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received!");

        // 알림 게시 권한 확인 (Android 13 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot show notification.");
                return; // 권한이 없으므로 알림 표시 중단
            }
        }

        // Intent에서 알람 정보를 가져옵니다.
        String alarmDocId = intent.getStringExtra(EXTRA_ALARM_DOC_ID);
        int systemAlarmId = intent.getIntExtra(EXTRA_SYSTEM_ALARM_ID, 0);
        String medicineName = intent.getStringExtra(EXTRA_MEDICINE_NAME);
        String timeOfDayLabel = intent.getStringExtra(EXTRA_TIME_OF_DAY_LABEL);
        int hour = intent.getIntExtra(EXTRA_HOUR, -1);
        int minute = intent.getIntExtra(EXTRA_MINUTE, -1);
        ArrayList<Integer> selectedDays = intent.getIntegerArrayListExtra(EXTRA_DAYS); // 요일 목록
        String dosage = intent.getStringExtra(EXTRA_DOSAGE);
        // String userId = intent.getStringExtra(EXTRA_USER_ID); // TODO: userId를 전달받는다면 여기서 사용


        Log.d(TAG, "Received Alarm Data - ID: " + systemAlarmId + ", DocID: " + alarmDocId + ", Med: " + medicineName + ", Time: " + hour + ":" + minute + " (" + timeOfDayLabel + "), Days: " + selectedDays + ", Dosage: " + dosage);

        // 알림 내용 구성
        String notificationTitle = "복약 알림: " + (medicineName != null ? medicineName : "알 수 없는 약");
        String alarmTimeText = "";
        if (hour != -1 && minute != -1) {
            alarmTimeText = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
        }
        String notificationText = (timeOfDayLabel != null ? timeOfDayLabel : "") + " " + alarmTimeText + " 복용 시간입니다.";
        if (dosage != null && !dosage.trim().isEmpty()) {
            notificationText += " (" + dosage + ")";
        } else if (timeOfDayLabel == null && alarmTimeText.isEmpty()) {
            notificationText = "복약 시간입니다.";
        }


        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // TODO: 알림 클릭 시 이동할 Activity 설정 (선택 사항)

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // 알림 게시 (try-catch 블록으로 권한 경고 및 예외 처리)
        try {
            notificationManager.notify(systemAlarmId, builder.build());
            Log.d(TAG, "Notification displayed with ID: " + systemAlarmId);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: Failed to show notification due to missing permissions.", e);
        }


        // !!! 요일 반복 알람 - 다음 알람 스케줄링 !!!
        // 설정된 요일이 있다면 다음 알람 시각을 계산하여 다시 스케줄링합니다.
        if (hour != -1 && minute != -1 && selectedDays != null && !selectedDays.isEmpty()) {
            scheduleNextAlarm(context, systemAlarmId, alarmDocId, medicineName, timeOfDayLabel, hour, minute, selectedDays, dosage);
        } else {
            Log.d(TAG, "Alarm is not repeating or data incomplete. Not rescheduling.");
        }

    } // <-- onReceive 끝

    // --- 다음 알람을 스케줄링하는 메서드 ---
    private void scheduleNextAlarm(Context context, int systemAlarmId, String alarmDocId, String medicineName, String timeOfDayLabel, int hour, int minute, List<Integer> selectedDays, String dosage) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null. Cannot reschedule next alarm.");
            return;
        }

        // 알람 스케줄링 권한 확인 (Android 12 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "SCHEDULE_EXACT_ALARM permission is not granted. Cannot schedule next exact alarm.");
                // TODO: 사용자에게 권한 필요 안내 및 설정으로 유도 (Activity에서 해야 함)
                return; // 권한 없으므로 스케줄링 중단
            }
        }


        // 다음 알람 시각 계산 (오늘 알람이 울린 시각 기준으로 다음 요일/시간 계산)
        Calendar nextAlarmCalendar = getNextAlarmCalendar(hour, minute, selectedDays);

        if (nextAlarmCalendar == null) {
            Log.w(TAG, timeOfDayLabel + " 알람: 다음 알람 시각 계산 불가. 재스케줄링 안 함.");
            return;
        }

        long triggerTime = nextAlarmCalendar.getTimeInMillis();
        Log.d(TAG, timeOfDayLabel + " 알람: 다음 알람 시각 계산됨 - " + nextAlarmCalendar.getTime().toString());


        // AlarmReceiver를 호출할 Intent 생성 (원래 알람 정보를 그대로 담아서 다시 보냄)
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        alarmIntent.putExtra(EXTRA_ALARM_DOC_ID, alarmDocId);
        alarmIntent.putExtra(EXTRA_SYSTEM_ALARM_ID, systemAlarmId);
        alarmIntent.putExtra(EXTRA_MEDICINE_NAME, medicineName);
        alarmIntent.putExtra(EXTRA_TIME_OF_DAY_LABEL, timeOfDayLabel);
        alarmIntent.putExtra(EXTRA_HOUR, hour);
        alarmIntent.putExtra(EXTRA_MINUTE, minute);
        alarmIntent.putExtra(EXTRA_DAYS, new ArrayList<>(selectedDays)); // List는 putExtra로 바로 못 보내므로 ArrayList로 감싸서 보냄
        alarmIntent.putExtra(EXTRA_DOSAGE, dosage);
        // TODO: userId를 전달받았다면 여기서도 Extra에 담아서 보내야 합니다.
        // alarmIntent.putExtra(EXTRA_USER_ID, userId);


        // PendingIntent 생성 (기존 PendingIntent를 업데이트)
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                systemAlarmId, // 기존과 동일한 요청 코드(systemAlarmId) 사용
                alarmIntent,
                // FLAG_UPDATE_CURRENT: 기존 PendingIntent가 있다면 Extra 데이터만 업데이트
                // FLAG_IMMUTABLE: 생성된 PendingIntent는 변경되지 않음을 나타냄 (API 23 이상 권장)
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 다음 알람 스케줄링 (정확한 시간 알람)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // API 23 이상
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            Log.d(TAG, timeOfDayLabel + " 알람 재스케줄링됨 (setExactAndAllowWhileIdle): " + String.format(Locale.getDefault(), "%02d:%02d", hour, minute) + ", Next: " + nextAlarmCalendar.getTime().toString());
        } else { // API 23 미만
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            Log.d(TAG, timeOfDayLabel + " 알람 재스케줄링됨 (set): " + String.format(Locale.getDefault(), "%02d:%02d", hour, minute) + ", Next: " + nextAlarmCalendar.getTime().toString());
        }
    }


    // --- 알람이 울린 시각 기준으로 다음 알람 시각을 계산하는 메서드 ---
    // (설정된 시간과 요일 목록을 기준으로, 가장 가까운 미래 시각 계산)
    private Calendar getNextAlarmCalendar(int hour, int minute, List<Integer> selectedDays) {
        if (hour == -1 || minute == -1 || selectedDays == null || selectedDays.isEmpty()) {
            return null; // 알람 설정 정보 불충분
        }

        Calendar now = Calendar.getInstance(); // 알람이 울린 현재 시각
        Calendar nextAlarm = null; // 다음 알람 시각 후보

        // 오늘 알람이 울린 시각과 같은 시간/분으로 설정된 오늘 날짜 Calendar
        Calendar candidate = (Calendar) now.clone();
        candidate.set(Calendar.HOUR_OF_DAY, hour);
        candidate.set(Calendar.MINUTE, minute);
        candidate.set(Calendar.SECOND, 0);
        candidate.set(Calendar.MILLISECOND, 0);

        boolean foundNext = false;

        // 현재 시각부터 다음 7일 동안 설정된 요일 중 가장 가까운 시각 찾기
        // (알람이 울린 시각 기준으로 계산하므로 오늘 알람은 이미 울렸다고 간주)
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

                // 현재 알람이 울린 시각(now)보다 미래 시각이라면 -> 다음 알람 시각 후보
                // 예: 월요일 8시 알람이 울렸고, 설정 요일에 수, 금이 있다면, 가장 가까운 미래 시각은 이번 주 수요일 8시
                // 예: 금요일 8시 알람이 울렸고, 설정 요일에 월, 수, 금이 있다면, 가장 가까운 미래 시각은 다음 주 월요일 8시
                if (potentialNextAlarm.getTimeInMillis() > now.getTimeInMillis()) {
                    nextAlarm = potentialNextAlarm; // 가장 가까운 미래 시각 발견
                    foundNext = true;
                    break; // 가장 가까운 미래 시각을 찾았으므로 루프 종료
                }
                // 만약 potentialNextAlarm <= now 이면, 해당 요일의 이 시간은 이미 지났거나 현재 울리고 있는 알람 시각이므로 건너뛰고 다음 날짜 확인
            }
        }

        if (!foundNext) {
            // 다음 7일 안에 설정된 요일을 찾지 못한 경우 (오류 또는 설정 요일이 현재 시점에서 매우 먼 경우)
            Log.w(TAG, "다음 7일 안에 설정된 알람 요일을 찾을 수 없습니다. 재스케줄링 안 함.");
            return null;
        }

        return nextAlarm;
    }


    // 알림 채널 생성 (Android 8.0 이상)
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            if (notificationManager != null && notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
                channel.setDescription(CHANNEL_DESCRIPTION);

                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification Channel created.");
            }
        }
    }
}
