package kr.ac.baeseok.p_capston; // <-- 재형님의 패키지명으로 변경

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast; // 테스트용
import androidx.annotation.NonNull;

// Firebase 관련 import
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task; // <-- Task 클래스 import 확인


// 알람 스케줄링에 필요한 import
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // BOOT_COMPLETED 액션을 받았는지 확인
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed. Attempting to reschedule alarms.");
            // context를 사용하여 UI 스레드에서 토스트 메시지 띄우기
            Toast.makeText(context.getApplicationContext(), "기기 부팅 완료, 알람 복구 시도...", Toast.LENGTH_LONG).show();

            // 알림 게시 권한 확인 (Android 13 이상)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Cannot reschedule notifications.");
                    Toast.makeText(context.getApplicationContext(), "알림 권한이 없어 복약 알람을 받을 수 없습니다.", Toast.LENGTH_LONG).show();
                    return; // 권한 없으면 복구 중단
                }
            }

            // 알람 복구 로직 시작 - Firebase 초기화 및 사용자 확인
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            FirebaseUser currentUser = mAuth.getCurrentUser();
            final String currentUserId; // <-- final 키워드 추가
            if (currentUser != null) {
                currentUserId = currentUser.getUid();
                Log.d(TAG, "BootReceiver: User logged in: " + currentUserId);
            } else {
                Log.w(TAG, "BootReceiver: No user logged in.");
                Toast.makeText(context.getApplicationContext(), "알람 복구에 실패했습니다 (로그인 필요).", Toast.LENGTH_SHORT).show();
                return; // 사용자 ID 없으면 알람 복구 로직 중단
            }

            // Firebase DB 객체와 사용자 ID가 모두 유효한 경우에만 Firestore 작업 수행
            if (db != null && currentUserId != null) {
                // Firestore에서 현재 사용자의 모든 알람 정보 불러오기 (Collection Group Query 사용)
                // alarms 컬렉션 그룹에서 현재 사용자 알람만 필터링
                // 이 쿼리에는 userId 및 hour, minute 필드에 대한 색인이 필요합니다.
                db.collectionGroup("alarms")
                        .whereEqualTo("userId", currentUserId) // alarms 문서에 userId 필드가 저장되어 있다는 가정 하에 필터링
                        .orderBy("hour").orderBy("minute") // 시간/분으로 정렬 (색인 필요!)
                        .get() // 데이터 가져오기 시작 (Task<QuerySnapshot> 반환)
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() { // Task 완료 시 호출될 콜백
                            @Override // OnCompleteListener 인터페이스의 추상 메서드 구현
                            public void onComplete(@NonNull Task<QuerySnapshot> task) { // <-- 이 task는 Collection Group Query의 Task
                                if (task.isSuccessful() && task.getResult() != null) { // <-- task 객체를 사용하여 성공 여부 및 결과 확인 (성공 시)
                                    int rescheduledCount = 0; // 복구된 알람 카운트 초기화

                                    // Collection Group Query 결과는 현재 사용자 알람 문서 목록입니다.
                                    for (DocumentSnapshot alarmDoc : task.getResult().getDocuments()) { // 가져온 각 알람 문서에 대해 반복
                                        // 알람 정보를 가져와서 다시 스케줄링
                                        String alarmDocId = alarmDoc.getId();
                                        Long systemAlarmIdLong = alarmDoc.getLong("systemAlarmId");
                                        String medicineName = alarmDoc.getString("medicineName");
                                        String timeOfDayLabel = alarmDoc.getString("timeOfDayLabel");
                                        Long hourLong = alarmDoc.getLong("hour");
                                        Long minuteLong = alarmDoc.getLong("minute");
                                        List<Long> daysLong = (List<Long>) alarmDoc.get("days"); // Firestore Array는 List<Long>으로 가져옴
                                        String dosage = alarmDoc.getString("dosage");
                                        String userId = alarmDoc.getString("userId"); // alarms 문서에 저장된 userId


                                        // 알람 데이터 유효성 및 현재 사용자 알람인지 최종 확인
                                        if (systemAlarmIdLong != null && medicineName != null && timeOfDayLabel != null && hourLong != null && minuteLong != null && daysLong != null && userId != null && userId.equals(currentUserId)) {
                                            int systemAlarmId = systemAlarmIdLong.intValue();
                                            int hour = hourLong.intValue();
                                            int minute = minuteLong.intValue();
                                            List<Integer> selectedDays = new ArrayList<>();
                                            for(Long day : daysLong) { // List<Long>을 List<Integer>로 변환
                                                selectedDays.add(day.intValue());
                                            }

                                            // 부팅 시점 기준 다음 알람 시각 계산 및 스케줄링
                                            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                                            if (alarmManager != null) {
                                                // SCHEDULE_EXACT_ALARM 권한 확인
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                                    Log.w(TAG, "SCHEDULE_EXACT_ALARM permission not granted. Cannot reschedule exact alarm during boot.");
                                                    // 사용자에게 권한 필요 안내 방법 필요 (Activity에서)
                                                } else { // SCHEDULE_EXACT_ALARM 권한이 있는 경우
                                                    Calendar nextAlarmCalendar = getNextAlarmCalendarForBoot(hour, minute, selectedDays); // 부팅 시점 기준 계산

                                                    if (nextAlarmCalendar != null) {
                                                        long triggerTime = nextAlarmCalendar.getTimeInMillis();

                                                        // AlarmReceiver를 호출할 Intent 생성 (원본 알람 정보를 그대로 담아서 다시 보냄)
                                                        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
                                                        alarmIntent.putExtra(AlarmReceiver.EXTRA_ALARM_DOC_ID, alarmDocId);
                                                        alarmIntent.putExtra(AlarmReceiver.EXTRA_SYSTEM_ALARM_ID, systemAlarmId);
                                                        alarmIntent.putExtra(AlarmReceiver.EXTRA_MEDICINE_NAME, medicineName);
                                                        alarmIntent.putExtra(AlarmReceiver.EXTRA_TIME_OF_DAY_LABEL, timeOfDayLabel);
                                                        alarmIntent.putExtra(AlarmReceiver.EXTRA_HOUR, hour);
                                                        alarmIntent.putExtra(AlarmReceiver.EXTRA_MINUTE, minute);
                                                        alarmIntent.putExtra(AlarmReceiver.EXTRA_DAYS, new ArrayList<>(selectedDays)); // ArrayList로 감싸서 보냄
                                                        alarmIntent.putExtra(AlarmReceiver.EXTRA_DOSAGE, dosage);
                                                        // TODO: userId를 AlarmReceiver로 전달해야 할 수도 있습니다
                                                        // alarmIntent.putExtra(AlarmReceiver.EXTRA_USER_ID, userId);


                                                        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                                                context,
                                                                systemAlarmId, // 스케줄링 할 때와 동일한 ID 사용
                                                                alarmIntent,
                                                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // 기존 PendingIntent 업데이트 플래그 및 불변 플래그
                                                        );

                                                        // 알람 스케줄링 (정확한 시간 알람)
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // API 23 이상
                                                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                                                            Log.d(TAG, "Rescheduled alarm (setExactAndAllowWhileIdle): " + medicineName + " at " + timeOfDayLabel + ", Next: " + nextAlarmCalendar.getTime().toString());
                                                        } else { // API 23 미만
                                                            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                                                            Log.d(TAG, "Rescheduled alarm (set): " + medicineName + " at " + timeOfDayLabel + ", Next: " + nextAlarmCalendar.getTime().toString());
                                                        }
                                                        rescheduledCount++; // 스케줄링 성공 시 카운트 증가

                                                    } else { // Could not calculate next alarm time
                                                        Log.w(TAG, "Could not calculate next alarm time for " + medicineName + " at " + timeOfDayLabel + ". Not rescheduling.");
                                                    }
                                                } // else (canScheduleExactAlarms) 끝
                                            } else { // AlarmManager is null
                                                Log.e(TAG, "AlarmManager is null. Cannot reschedule alarm during boot.");
                                            }

                                        } else { // Skipping invalid or incomplete alarm data
                                            Log.w(TAG, "Skipping invalid or incomplete alarm data during boot reschedule: " + alarmDoc.getId());
                                        }
                                    } // for (DocumentSnapshot alarmDoc ...) 끝
                                    // 모든 알람 문서 처리가 완료된 후
                                    Log.d(TAG, "BootReceiver: Finished processing alarms. Rescheduled: " + rescheduledCount);
                                    Toast.makeText(context.getApplicationContext(), rescheduledCount + "개의 복약 알람을 복구했습니다.", Toast.LENGTH_SHORT).show();

                                } // <-- 이 위치에 닫는 중괄호 '}'를 추가했습니다.
                                else { // 데이터 가져오기 실패 (Task is not successful)
                                    // !!! 이 else는 위의 if (task.isSuccessful() ...)에 해당합니다. !!!
                                    if (task.getResult() != null && task.getResult().isEmpty()) { // 쿼리 결과가 비어있는 경우 (알람 없음)
                                        Log.d(TAG, "No alarms found for user " + currentUserId + " during boot reschedule.");
                                        Toast.makeText(context.getApplicationContext(), "복구할 알람이 없습니다.", Toast.LENGTH_SHORT).show();
                                    } else { // 데이터 가져오기 실패 (task.isSuccessful() == false 인 경우)
                                        Log.e(TAG, "Error getting alarms for user " + currentUserId + " during boot reschedule.", task.getException());
                                        Toast.makeText(context.getApplicationContext(), "알람 복구 실패.", Toast.LENGTH_SHORT).show();
                                    }
                                } // else 끝 (task.isSuccessful()에 대한 else)
                            } // onComplete 메서드 끝
                        }); // addOnCompleteListener 끝 (Collection Group Query)

            } else {
                Log.w(TAG, "BootReceiver: DB is null or user is not logged in. Cannot load alarms.");
                Toast.makeText(context.getApplicationContext(), "알람 복구에 실패했습니다 (로그인 필요).", Toast.LENGTH_SHORT).show();
            }

        } // <-- BOOT_COMPLETED 액션 확인 끝
    } // <-- onReceive 끝


    // --- 부팅 시점 기준으로 다음 알람 시각을 계산하는 메서드 ---
    // (AlarmRegistrationActivity의 getNextAlarmCalendar와 유사하나, 현재 시점 기준 계산)
    private Calendar getNextAlarmCalendarForBoot(int hour, int minute, List<Integer> selectedDays) {
        if (hour == -1 || minute == -1 || selectedDays == null || selectedDays.isEmpty()) {
            return null;
        }

        Calendar now = Calendar.getInstance(); // 부팅된 현재 시각
        Calendar nextAlarm = null;

        // 오늘부터 시작하여 다음 7일 동안 설정된 요일 중 가장 가까운 미래 시각 찾기
        for (int i = 0; i < 7; i++) {
            Calendar checkDay = (Calendar) now.clone();
            checkDay.add(Calendar.DAY_OF_YEAR, i); // 오늘부터 i일 후

            int dayOfWeekToCheck = checkDay.get(Calendar.DAY_OF_WEEK);

            if (selectedDays.contains(dayOfWeekToCheck)) {
                Calendar potentialNextAlarm = (Calendar) checkDay.clone();
                potentialNextAlarm.set(Calendar.HOUR_OF_DAY, hour);
                potentialNextAlarm.set(Calendar.MINUTE, minute);
                potentialNextAlarm.set(Calendar.SECOND, 0);
                potentialNextAlarm.set(Calendar.MILLISECOND, 0);

                // 현재 시각 (now) 보다 미래 시각이라면 -> 다음 알람 시각 후보
                if (potentialNextAlarm.getTimeInMillis() > now.getTimeInMillis()) {
                    nextAlarm = potentialNextAlarm;
                    break; // 가장 가까운 미래 시각 발견
                }
            }
        }

        // 만약 7일 안에 미래 시각을 찾지 못했다면 (즉, 설정 시간이 부팅 시점보다 과거인 경우)
        // 다음 주 같은 요일의 시간으로 설정합니다. (이 로직이 필요할 수 있습니다.)
        if (nextAlarm == null) {
            // 부팅 시점으로부터 1주일 후를 기준으로 다시 요일 검색
            Calendar oneWeekLater = (Calendar) now.clone();
            oneWeekLater.add(Calendar.WEEK_OF_YEAR, 1);

            for (int i = 0; i < 7; i++) {
                Calendar checkDay = (Calendar) oneWeekLater.clone();
                checkDay.add(Calendar.DAY_OF_YEAR, i);

                int dayOfWeekToCheck = checkDay.get(Calendar.DAY_OF_WEEK);

                if (selectedDays.contains(dayOfWeekToCheck)) {
                    Calendar potentialNextAlarm = (Calendar) checkDay.clone();
                    potentialNextAlarm.set(Calendar.HOUR_OF_DAY, hour);
                    potentialNextAlarm.set(Calendar.MINUTE, minute);
                    potentialNextAlarm.set(Calendar.SECOND, 0);
                    potentialNextAlarm.set(Calendar.MILLISECOND, 0);

                    // 1주일 후 시점부터 계산한 결과 중 가장 빠른 시각
                    nextAlarm = potentialNextAlarm;
                    break; // 다음 알람 시각 발견
                }
            }
        }


        if (nextAlarm == null) {
            Log.w(TAG, "BootReceiver: Could not find next alarm time within 14 days. Not rescheduling.");
            return null;
        }

        return nextAlarm;
    }
}
