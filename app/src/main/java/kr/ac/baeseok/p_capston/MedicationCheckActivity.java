package kr.ac.baeseok.p_capston; // <-- 재형님의 패키지명으로 변경

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;

// RecyclerView 관련 import
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Firebase 관련 import
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;


// 데이터 모델 및 어댑터 import
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;


// MedicationCheckAdapter의 체크 리스너 인터페이스 구현
public class MedicationCheckActivity extends AppCompatActivity implements OnMedicationCheckListener { // OnMedicationCheckListener 인터페이스 구현

    private static final String TAG = "MedicationCheckActivity";

    private RecyclerView recyclerViewMedicationCheck;
    private Button buttonBack;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    private List<MedicationCheckListItem> medicationCheckListItems;
    private MedicationCheckAdapter medicationCheckAdapter;

    private String todayDateString; // 오늘 날짜 문자열 (YYYY-MM-DD 형식)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_check);

        Log.d(TAG, "MedicationCheckActivity created.");

        // 오늘 날짜 문자열 생성 (복용 기록 저장 및 불러오기 시 사용)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        todayDateString = sdf.format(Calendar.getInstance().getTime());
        Log.d(TAG, "Today's date for medication check: " + todayDateString);


        // UI 요소 연결
        recyclerViewMedicationCheck = findViewById(R.id.recyclerViewMedicationCheck);
        buttonBack = findViewById(R.id.buttonBack_MedicationCheck);

        // RecyclerView 설정
        recyclerViewMedicationCheck.setLayoutManager(new LinearLayoutManager(this));

        // Adapter 초기화 (medicationCheckListItems 목록과 this (OnMedicationCheckListener 구현체) 전달)
        medicationCheckListItems = new ArrayList<>();
        medicationCheckAdapter = new MedicationCheckAdapter(medicationCheckListItems, this); // <-- this 전달 (리스너)
        recyclerViewMedicationCheck.setAdapter(medicationCheckAdapter);


        // Firebase 객체 초기화 및 사용자 ID 가져오기
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            // 로그인된 사용자가 있는 경우 복약 체크 목록 불러오기 시작
            loadMedicationsForCheck(); // 복약 목록 불러오기
        } else {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        // 뒤로가기 버튼 클릭 리스너 설정
        if (buttonBack != null) {
            buttonBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        } else {
            Log.e(TAG, "Error: Back button (ID 'buttonBack_MedicationCheck') not found in layout!");
        }

    } // <-- onCreate 끝


    // --- Firestore에서 복약 체크를 위한 알람 목록 불러오는 메서드 ---
    // 사용자의 모든 알람을 불러온 후, 현재 날짜 기준으로 필터링하고, 해당 날짜의 복용 상태 기록을 함께 불러와 반영합니다.
    private void loadMedicationsForCheck() {
        if (currentUserId == null || db == null || todayDateString == null) {
            Log.w(TAG, "Cannot load medications for check: User ID, DB, or Date is null.");
            Toast.makeText(this, "복약 목록을 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firestore Collection Group Query를 사용하여 현재 사용자의 모든 'alarms' 서브컬렉션 문서 가져오기
        db.collectionGroup("alarms")
                .whereEqualTo("userId", currentUserId) // 현재 사용자 알람만 필터링
                .orderBy("hour").orderBy("minute") // 시간/분으로 정렬
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) { // 쿼리 성공 및 결과 있음
                            List<MedicationCheckItem> todayMedications = new ArrayList<>(); // 오늘 복용할 알람만 담을 임시 목록

                            if (!task.getResult().isEmpty()) {
                                Log.d(TAG, "Loaded " + task.getResult().size() + " total user alarms.");

                                // 현재 요일 가져오기 (Calendar.DAY_OF_WEEK는 일요일=1 ~ 토요일=7)
                                int currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

                                // 가져온 알람 문서 목록을 순회하며 오늘 요일에 해당하는 알람만 필터링
                                for (DocumentSnapshot alarmDoc : task.getResult().getDocuments()) {
                                    // 알람 정보를 가져와 MedicationCheckItem 객체 생성
                                    String firestoreAlarmDocId = alarmDoc.getId();
                                    String parentMedicineId = alarmDoc.getReference().getParent().getParent().getId();
                                    String timeOfDayLabel = alarmDoc.getString("timeOfDayLabel");
                                    Long hourLong = alarmDoc.getLong("hour");
                                    Long minuteLong = alarmDoc.getLong("minute");
                                    List<Long> daysLong = (List<Long>) alarmDoc.get("days");
                                    String dosage = alarmDoc.getString("dosage");
                                    String userId = alarmDoc.getString("userId");
                                    String medicineName = alarmDoc.getString("medicineName");


                                    // 데이터 유효성 및 현재 사용자 알람, 그리고 오늘 요일에 해당 하는지 최종 확인
                                    if (firestoreAlarmDocId != null && parentMedicineId != null && timeOfDayLabel != null && hourLong != null && minuteLong != null && daysLong != null && userId != null && medicineName != null && userId.equals(currentUserId)) {
                                        List<Integer> days = new ArrayList<>();
                                        for(Long day : daysLong) {
                                            days.add(day.intValue());
                                        }

                                        // 오늘 요일에 해당 하는지 확인
                                        if (days.contains(currentDayOfWeek)) {
                                            // 오늘 복용해야 할 약이라면 MedicationCheckItem 객체 생성 및 임시 목록에 추가
                                            MedicationCheckItem item = new MedicationCheckItem(
                                                    firestoreAlarmDocId,
                                                    parentMedicineId,
                                                    0, // systemAlarmId는 이 목록에선 당장 필요 없을 수 있음
                                                    medicineName,
                                                    timeOfDayLabel,
                                                    hourLong.intValue(),
                                                    minuteLong.intValue(),
                                                    days, // 요일 목록
                                                    dosage, // 복용량
                                                    userId // 사용자 ID
                                            );
                                            todayMedications.add(item); // 오늘 복용할 알람 목록에 추가
                                        }
                                    } else {
                                        Log.w(TAG, "Skipping invalid or incomplete alarm data for medication check: " + alarmDoc.getId());
                                    }
                                }

                                // --- 불러온 오늘 복용 알람 목록을 시간대별로 분류하고 RecyclerView에 표시할 최종 목록 구성 ---

                                // 시간대별 리스트 분리
                                List<MedicationCheckItem> morningList = new ArrayList<>();
                                List<MedicationCheckItem> lunchList = new ArrayList<>();
                                List<MedicationCheckItem> eveningList = new ArrayList<>();
                                // TODO: 다른 시간대 (예: 취침 전) 있다면 추가 분류

                                for (MedicationCheckItem item : todayMedications) { // 오늘 복용할 알람 목록 순회
                                    // 시간대별로 분류
                                    if ("아침".equals(item.getTimeOfDayLabel())) {
                                        morningList.add(item);
                                    } else if ("점심".equals(item.getTimeOfDayLabel())) {
                                        lunchList.add(item);
                                    } else if ("저녁".equals(item.getTimeOfDayLabel())) {
                                        eveningList.add(item);
                                    }
                                    // TODO: 다른 시간대 분류
                                }

                                // 시간 순서대로 정렬 (MedicationCheckItem 객체에 hour, minute 필드 있음)
                                Comparator<MedicationCheckItem> timeComparator = Comparator.comparingInt(MedicationCheckItem::getHour)
                                        .thenComparingInt(MedicationCheckItem::getMinute);
                                Collections.sort(morningList, timeComparator);
                                Collections.sort(lunchList, timeComparator);
                                Collections.sort(eveningList, timeComparator);


                                // --- Firestore에서 오늘 날짜의 복용 상태 기록을 불러와 복약 항목에 반영 ---
                                // 각 알람의 서브컬렉션에서 오늘 날짜 문서의 isTaken 필드를 가져옴
                                loadMedicationCheckStatuses(morningList, lunchList, eveningList); // 복용 상태 불러와 반영하는 새 메서드 호출


                            } else { // 쿼리 결과가 비어있는 경우 (등록된 알람 없음)
                                Log.d(TAG, "No alarms found for user " + currentUserId + " for medication check.");
                                medicationCheckListItems.clear(); // <-- 멤버 변수 목록 비움
                                medicationCheckAdapter.setCheckItemList(medicationCheckListItems); // <-- 멤버 변수 전달 (빈 목록)
                                Toast.makeText(MedicationCheckActivity.this, "등록된 알람이 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else { // 데이터 가져오기 실패 (Task is not successful)
                            Log.e(TAG, "Error getting alarms for user " + currentUserId + " for medication check.", task.getException());
                            Toast.makeText(MedicationCheckActivity.this, "복약 목록 불러오기 실패.", Toast.LENGTH_SHORT).show();
                        }
                    } // onComplete 메서드 끝
                }); // addOnCompleteListener 끝

    } // <-- loadMedicationsForCheck 끝


    // --- 불러온 오늘 복용 알람 목록에 대해 해당 날짜의 복용 상태 기록을 불러와 반영하는 메서드 ---
    private void loadMedicationCheckStatuses(List<MedicationCheckItem> morningList, List<MedicationCheckItem> lunchList, List<MedicationCheckItem> eveningList) {
        if (db == null || currentUserId == null || todayDateString == null) {
            Log.w(TAG, "Cannot load medication statuses: DB, User ID, or Date is null.");
            buildAndDisplayMedicationList(morningList, lunchList, eveningList); // 상태 없이 목록만 표시
            return;
        }

        List<MedicationCheckItem> allTodayMedications = new ArrayList<>();
        allTodayMedications.addAll(morningList);
        allTodayMedications.addAll(lunchList);
        allTodayMedications.addAll(eveningList);
        // TODO: 다른 시간대 있다면 추가

        if (allTodayMedications.isEmpty()) {
            buildAndDisplayMedicationList(morningList, lunchList, eveningList); // 복용할 약 없으면 바로 목록 표시
            return;
        }

        // 각 MedicationCheckItem에 대해 복용 기록을 불러오는 Task 리스트 생성
        List<Task<DocumentSnapshot>> statusTasks = new ArrayList<>();
        for (MedicationCheckItem item : allTodayMedications) {
            if (item.getParentMedicineId() != null && item.getFirestoreAlarmDocId() != null) {
                // Firestore 경로: users/userId/registeredMedicines/medicineId/alarms/alarmDocId/checkHistory/todayDateString
                DocumentReference statusDocRef = db.collection("users").document(currentUserId)
                        .collection("registeredMedicines").document(item.getParentMedicineId())
                        .collection("alarms").document(item.getFirestoreAlarmDocId())
                        .collection("checkHistory").document(todayDateString); // 오늘 날짜 문서

                statusTasks.add(statusDocRef.get()); // 상태 문서 가져오는 Task 추가
            }
        }

        // 모든 상태 불러오기 Task가 완료될 때까지 대기
        com.google.android.gms.tasks.Tasks.whenAllSuccess(statusTasks)
                .addOnSuccessListener(new OnSuccessListener<List<Object>>() { // List<Object>는 각 Task의 결과를 담음
                    @Override
                    public void onSuccess(List<Object> results) {
                        Log.d(TAG, "Successfully loaded " + results.size() + " medication status documents.");
                        // 불러온 상태 결과를 바탕으로 MedicationCheckItem 목록 업데이트
                        for (int i = 0; i < results.size(); i++) {
                            DocumentSnapshot statusDoc = (DocumentSnapshot) results.get(i);
                            if (statusDoc.exists()) {
                                // 해당 알람 항목을 찾아서 복용 상태 반영
                                // results 리스트와 statusTasks 리스트의 순서가 일치한다고 가정
                                MedicationCheckItem item = allTodayMedications.get(i);
                                Boolean isTaken = statusDoc.getBoolean("isTaken"); // Firestore 필드 이름 'isTaken'
                                if (isTaken != null) {
                                    item.setTakenForToday(isTaken); // MedicationCheckItem의 isTakenForToday 필드 업데이트
                                    Log.d(TAG, "Status loaded for " + item.getMedicineName() + " (" + item.getTimeOfDayLabel() + "): isTaken=" + isTaken);
                                }
                            }
                        }
                        // 상태 반영이 완료된 목록으로 RecyclerView 구성 및 표시
                        buildAndDisplayMedicationList(morningList, lunchList, eveningList);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error loading medication status documents.", e);
                        Toast.makeText(MedicationCheckActivity.this, "복용 기록 불러오기 실패.", Toast.LENGTH_SHORT).show();
                        // 상태 불러오기 실패 시에도 일단 목록은 표시
                        buildAndDisplayMedicationList(morningList, lunchList, eveningList);
                    }
                });
    }


    // --- 최종 복약 목록 (헤더 + 항목)을 구성하고 RecyclerView에 표시하는 메서드 ---
    private void buildAndDisplayMedicationList(List<MedicationCheckItem> morningList, List<MedicationCheckItem> lunchList, List<MedicationCheckItem> eveningList) {
        medicationCheckListItems.clear(); // 기존 목록 비움

        // 아침 항목 추가
        if (!morningList.isEmpty()) {
            medicationCheckListItems.add(new TimeslotHeader("아침 복약"));
            medicationCheckListItems.addAll(morningList);
        }
        // 점심 항목 추가
        if (!lunchList.isEmpty()) {
            medicationCheckListItems.add(new TimeslotHeader("점심 복약"));
            medicationCheckListItems.addAll(lunchList);
        }
        // 저녁 항목 추가
        if (!eveningList.isEmpty()) {
            medicationCheckListItems.add(new TimeslotHeader("저녁 복약"));
            medicationCheckListItems.addAll(eveningList);
        }
        // TODO: 다른 시간대 있다면 추가

        // 어댑터 데이터 업데이트하여 화면에 표시
        medicationCheckAdapter.setCheckItemList(medicationCheckListItems);

        // 복약할 알람이 있는지 최종 확인 (헤더 포함 목록이 비어있다면)
        if (medicationCheckListItems.isEmpty()) {
            Toast.makeText(MedicationCheckActivity.this, "오늘 복용할 알람이 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }


    // --- OnMedicationCheckListener 인터페이스 구현 메서드 (어댑터에서 체크박스 상태 변경 시 호출됨) ---
    @Override
    public void onCheckChanged(String firestoreAlarmDocId, boolean isChecked) {
        // 어댑터로부터 알람 문서 ID와 변경된 체크 상태를 받아서 Firestore에 저장
        Log.d(TAG, "Medication check status changed for Alarm Doc ID: " + firestoreAlarmDocId + ", Checked: " + isChecked);

        saveMedicationCheckStatus(currentUserId, firestoreAlarmDocId, todayDateString, isChecked); // 복용 상태 저장 메서드 호출
    }


    // --- 특정 알람 항목의 복용 완료 상태를 Firestore에 저장하는 메서드 ---
    private void saveMedicationCheckStatus(String userId, String alarmDocId, String dateString, boolean isTaken) {
        if (userId == null || alarmDocId == null || dateString == null || db == null) {
            Log.w(TAG, "Cannot save medication status: User ID, Alarm Doc ID, Date, or DB is null.");
            Toast.makeText(this, "복용 기록 저장 실패 (정보 부족).", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firestore에 저장할 복용 기록 데이터 Map 생성
        Map<String, Object> statusData = new HashMap<>();
        statusData.put("isTaken", isTaken); // 복용 여부 (true/false)
        statusData.put("checkTimestamp", Calendar.getInstance().getTime()); // 체크 시점 타임스탬프 (선택 사항)
        // TODO: 복용량, 복용 시간 등 추가 정보 저장 고려

        // Firestore 경로: users/userId/registeredMedicines/{약ID}/alarms/alarmDocId/checkHistory/dateString
        // 약 ID(parentMedicineId)가 필요하지만, 현재 onCheckChanged에는 알람 ID만 전달됨.
        // MedicationCheckItem 모델에 parentMedicineId가 있으므로, 이를 사용하여 Firestore 경로를 구성해야 합니다.
        // onCheckChanged 메서드에 parentMedicineId도 함께 전달하거나,
        // 여기 메서드 내에서 alarmDocId를 가지고 Firestore에서 해당 알람 문서를 다시 불러와 parentMedicineId를 얻어야 합니다.
        // 복잡해지므로, OnMedicationCheckListener에서 parentMedicineId도 함께 전달하도록 수정하는 것이 좋습니다.

        // OnMedicationCheckListener 인터페이스와 MedicationCheckAdapter의 onCheckChanged 메서드 시그니처를 수정했습니다.
        // OnMedicationCheckListener { void onCheckChanged(String firestoreAlarmDocId, String parentMedicineId, boolean isChecked); }
        // MedicationCheckAdapter { checkListener.onCheckChanged(medicationItem.getFirestoreAlarmDocId(), medicationItem.getParentMedicineId(), isChecked); }
        // MedicationCheckActivity의 onCheckChanged 메서드 시그니처도 수정했습니다.

        // 해당 알람의 부모 약 ID를 MedicationCheckItem에서 찾아와 사용해야 합니다.
        // 현재 OnMedicationCheckListener에서는 알람 문서 ID만 받으므로,
        // 여기에서는 alarmDocId를 가지고 medicationCheckListItems 목록에서 해당 MedicationCheckItem을 찾아 parentMedicineId를 가져와야 합니다.
        // 아니면 리스너에서 parentMedicineId를 함께 전달하도록 수정해야 합니다.
        // MedicationCheckAdapter의 onCheckChanged 시그니처를 수정했으므로, 여기에서도 받도록 수정합니다.


        // Firestore 경로 구성에 필요한 parentMedicineId를 OnMedicationCheckListener에서 함께 전달받도록 수정했습니다.
        String parentMedicineId = findParentMedicineId(alarmDocId); // medicationCheckListItems 목록에서 해당 알람 항목을 찾아 부모 약 ID 가져옴

        if (parentMedicineId == null) {
            Log.e(TAG, "Cannot find parent medicine ID for alarm: " + alarmDocId);
            Toast.makeText(this, "복용 기록 저장 실패 (약 정보 오류).", Toast.LENGTH_SHORT).show();
            return;
        }


        db.collection("users").document(currentUserId)
                .collection("registeredMedicines").document(parentMedicineId) // 부모 약 문서 지정
                .collection("alarms").document(alarmDocId) // 해당 알람 문서 지정
                .collection("checkHistory").document(dateString) // checkHistory 서브컬렉션 아래 오늘 날짜 문서
                .set(statusData) // set() 메서드로 데이터 저장 (문서가 없으면 생성, 있으면 덮어쓰기)
                .addOnSuccessListener(aVoid -> { // 저장 성공 시
                    Log.d(TAG, "Medication status saved for Alarm Doc ID: " + alarmDocId + " on " + dateString + ": isTaken=" + isTaken);
                    // Toast.makeText(this, "복용 기록 저장 완료", Toast.LENGTH_SHORT).show(); // 저장 성공 토스트 (반복적일 수 있음)
                })
                .addOnFailureListener(e -> { // 저장 실패 시
                    Log.e(TAG, "Error saving medication status for Alarm Doc ID: " + alarmDocId + " on " + dateString, e);
                    Toast.makeText(this, "복용 기록 저장 실패.", Toast.LENGTH_SHORT).show();
                    // 실패 시 체크박스 상태를 원래대로 되돌리는 등의 UI 처리 고려
                });
    }


    // --- MedicationCheckListItems 목록에서 알람 문서 ID로 부모 약 문서 ID를 찾는 헬퍼 메서드 ---
    private String findParentMedicineId(String alarmDocId) {
        if (alarmDocId == null || medicationCheckListItems == null) {
            return null;
        }
        for (MedicationCheckListItem item : medicationCheckListItems) {
            if (item.getType() == MedicationCheckListItem.TYPE_MEDICATION) { // 복약 항목인 경우에만 확인
                MedicationCheckItem medicationItem = (MedicationCheckItem) item;
                if (alarmDocId.equals(medicationItem.getFirestoreAlarmDocId())) {
                    return medicationItem.getParentMedicineId();
                }
            }
        }
        return null; // 해당하는 알람 항목을 찾지 못한 경우
    }


    // TODO: 시스템 뒤로가기 이벤트 처리 (필요시)
    // @Override
    // public void onBackPressed() {
    //     super.onBackPressed();
    // }

    // TODO: 액션바에 뒤로가기 버튼 표시 (필요시)
    // @Override
    // public boolean onSupportNavigateUp() {
    //     onBackPressed();
    //     return true;
    // }
}
