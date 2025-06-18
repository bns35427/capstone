package kr.ac.baeseok.p_capston; // <-- 재형님의 패키지명으로 변경

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.view.View; // View 사용
import android.widget.Button; // Button 사용


// Firebase 관련 import
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import java.util.ArrayList;
import java.util.List;

public class AllAlarmsActivity extends AppCompatActivity implements OnAlarmDeleteListener {

    private static final String TAG = "AllAlarmsActivity";

    private RecyclerView recyclerViewAllAlarms;
    private AllAlarmsAdapter allAlarmsAdapter;
    private List<AllAlarmItem> alarmItemList;

    private Button buttonBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_alarms);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerViewAllAlarms = findViewById(R.id.recyclerViewAllAlarms);
        buttonBack = findViewById(R.id.buttonBack_AllAlarms);

        recyclerViewAllAlarms.setLayoutManager(new LinearLayoutManager(this));

        alarmItemList = new ArrayList<>();
        // this는 OnAlarmDeleteListener 인터페이스를 구현한 AllAlarmsActivity 자신입니다.
        allAlarmsAdapter = new AllAlarmsAdapter(alarmItemList, this);
        recyclerViewAllAlarms.setAdapter(allAlarmsAdapter);

        if (buttonBack != null) {
            buttonBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        } else {
            Log.e(TAG, "Error: Back button (ID 'buttonBack_AllAlarms') not found in layout!");
        }

        // 등록된 모든 알람 불러오기
        loadAllRegisteredAlarms();
    }


    private void loadAllRegisteredAlarms() {
        if (currentUserId == null || db == null) {
            Log.w(TAG, "Cannot load all alarms: User ID or DB is null.");
            Toast.makeText(this, "알람 목록을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firestore Collection Group Query를 사용하여 모든 'alarms' 서브컬렉션에서 문서 가져오기
        // 이 쿼리를 사용하려면 Firestore에 해당 컬렉션 그룹에 대한 색인 생성이 필수입니다!
        db.collectionGroup("alarms")
                // 현재 사용자 알람만 필터링 (.whereEqualTo("userId", currentUserId))
                // 알람 저장 시 userId 필드를 추가했다는 가정 하에 필터링 추가
                .whereEqualTo("userId", currentUserId) // <-- userId 필드로 필터링 추가
                .orderBy("hour").orderBy("minute") // 시간/분으로 정렬 (색인 필요!)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            alarmItemList.clear();
                            if (task.getResult() != null && !task.getResult().isEmpty()) {
                                Log.d(TAG, "Loaded " + task.getResult().size() + " total alarms.");
                                for (DocumentSnapshot document : task.getResult().getDocuments()) {
                                    // Firestore 문서에서 데이터 가져와 AllAlarmItem 객체 생성
                                    String firestoreAlarmDocId = document.getId(); // 알람 문서 ID
                                    // Collection Group Query 결과에서 부모 문서(약 문서)의 ID 가져오기
                                    String parentMedicineId = document.getReference().getParent().getParent().getId(); // 약 문서 ID 가져오기

                                    String timeOfDayLabel = document.getString("timeOfDayLabel");
                                    Long hourLong = document.getLong("hour");
                                    Long minuteLong = document.getLong("minute");
                                    List<Long> daysLong = (List<Long>) document.get("days"); // <-- 요일 목록 가져오기
                                    String dosage = document.getString("dosage");
                                    Long systemAlarmIdLong = document.getLong("systemAlarmId");
                                    String medicineName = document.getString("medicineName"); // 알람 문서에서 약 이름 가져오기
                                    String userId = document.getString("userId"); // <-- userId 가져오기 (필터링 확인용)


                                    // 가져온 데이터 유효성 확인 및 AllAlarmItem 객체 생성
                                    if (hourLong != null && minuteLong != null && daysLong != null && systemAlarmIdLong != null && medicineName != null && userId != null) { // <-- userId null 체크 추가
                                        List<Integer> days = new ArrayList<>();
                                        for(Long day : daysLong) {
                                            days.add(day.intValue());
                                        }
                                        int systemAlarmId = systemAlarmIdLong.intValue();

                                        // 현재 사용자 알람인지 다시 한번 확인 (CollectionGroup 쿼리 필터링 사용 시 불필요하지만 안전장치)
                                        if (!userId.equals(currentUserId)) {
                                            Log.w(TAG, "Skipping alarm from another user: " + firestoreAlarmDocId);
                                            continue;
                                        }


                                        alarmItemList.add(new AllAlarmItem(
                                                firestoreAlarmDocId,
                                                parentMedicineId,
                                                systemAlarmId,
                                                medicineName,
                                                timeOfDayLabel,
                                                hourLong.intValue(),
                                                minuteLong.intValue(),
                                                days, // <-- 가져온 요일 목록 전달
                                                dosage));
                                    } else {
                                        Log.w(TAG, "Skipping invalid or incomplete alarm data from Firestore: " + firestoreAlarmDocId);
                                    }
                                }

                                allAlarmsAdapter.setAlarmList(alarmItemList); // 어댑터 데이터 업데이트

                            } else {
                                Log.d(TAG, "No registered alarms found.");
                                alarmItemList.clear();
                                allAlarmsAdapter.setAlarmList(alarmItemList);
                                Toast.makeText(AllAlarmsActivity.this, "등록된 알람이 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Error getting all registered alarms from Firestore.", task.getException());
                            Toast.makeText(AllAlarmsActivity.this, "알람 목록 불러오기 실패.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    @Override
    public void onDeleteClick(String firestoreAlarmDocId, String parentMedicineId, int systemAlarmId) {
        Log.d(TAG, "Delete clicked for Alarm Doc ID: " + firestoreAlarmDocId + ", Medicine ID: " + parentMedicineId + ", System Alarm ID: " + systemAlarmId);

        // 알람 삭제 및 시스템 알람 취소 로직 호출
        cancelAlarm(currentUserId, parentMedicineId, firestoreAlarmDocId, systemAlarmId);
    }


    // --- 알람 삭제 및 시스템 알람 취소 메서드 ---
    private void cancelAlarm(String userId, String medicineId, String alarmDocId, int systemAlarmId) {
        if (userId == null || medicineId == null || alarmDocId == null || db == null) {
            Log.w(TAG, "Cannot cancel alarm: User ID, Medicine ID, Alarm Doc ID, or DB is null.");
            Toast.makeText(this, "알람 삭제 실패 (정보 부족).", Toast.LENGTH_SHORT).show();
            return;
        }

        // 시스템 알람 취소
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent alarmIntent = new Intent(this, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    systemAlarmId, // 스케줄링 시 사용한 ID와 동일
                    alarmIntent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                Log.d(TAG, "System alarm cancelled with ID: " + systemAlarmId);
            } else {
                Log.w(TAG, "PendingIntent not found for ID: " + systemAlarmId + ". System alarm may not have been scheduled or already cancelled.");
            }
        } else {
            Log.e(TAG, "AlarmManager is null. Cannot cancel system alarm.");
        }

        // Firestore에서 알람 데이터 삭제
        db.collection("users").document(userId)
                .collection("registeredMedicines").document(medicineId)
                .collection("alarms").document(alarmDocId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Alarm data deleted from Firestore: " + alarmDocId);
                    Toast.makeText(this, "알람이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    loadAllRegisteredAlarms(); // 전체 알람 목록 다시 불러오기
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting alarm data from Firestore.", e);
                    Toast.makeText(this, "알람 삭제 실패.", Toast.LENGTH_SHORT).show();
                });
    }
}
