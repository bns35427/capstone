package kr.ac.baeseok.p_capston;

import static androidx.core.content.ContextCompat.startActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.EditText;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import androidx.annotation.NonNull;

import com.opencsv.CSVReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class CapstonActivity extends AppCompatActivity {

    private static final String TAG = "CapstonActivity";
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1;

    // UI 요소
    private Button buttonCapture;
    private LinearLayout linearLayoutMedicineItems;
    private Button buttonSetAlarm;
    private Button buttonCheckMedicine;
    private Button buttonBack;

    private EditText editTextSearchMedicine;
    private Button buttonSearchMedicine;

    private ActivityResultLauncher<Intent> captureConfirmLauncher;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    // CSV에서 불러온 전체 약 리스트
    private List<Medicine> medicineList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capston);

        // Firebase 초기화
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadMedicineData();

        // 로그인 확인
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "No user logged in. Redirecting to MainActivity.");
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(CapstonActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        } else {
            currentUserId = currentUser.getUid();
            Log.d(TAG, "User logged in: " + currentUserId);
        }

        requestNotificationPermission();

        // UI 연결
        buttonCapture = findViewById(R.id.buttonCapture);
        linearLayoutMedicineItems = findViewById(R.id.linearLayoutMedicineItems);
        buttonSetAlarm = findViewById(R.id.buttonSetAlarm);
        buttonCheckMedicine = findViewById(R.id.buttonCheckMedicine);
        buttonBack = findViewById(R.id.buttonBack);

        // --- 검색창 UI 연결 ---
        editTextSearchMedicine = findViewById(R.id.editTextSearchMedicine);
        buttonSearchMedicine = findViewById(R.id.buttonSearchMedicine);

        // --- CSV 데이터 로드 ---


        // --- 검색 버튼 클릭 리스너 ---
        buttonSearchMedicine.setOnClickListener(v -> {
            String query = editTextSearchMedicine.getText().toString().trim();
            Log.d(TAG, "검색 버튼 클릭됨. 입력값: " + query);

            if (query.isEmpty()) {
                Toast.makeText(this, "약 이름을 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            List<Medicine> matched = searchMedicines(query);
            showSearchResults(matched);
        });

        // CaptureConfirmActivity 실행 및 결과 처리
        captureConfirmLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Log.d(TAG, "Returned from CaptureConfirmActivity with RESULT_OK. Reloading medicines.");
                            loadRegisteredMedicines();
                            Toast.makeText(CapstonActivity.this, "약 정보 등록 완료!", Toast.LENGTH_SHORT).show();
                        } else if (result.getResultCode() == RESULT_CANCELED) {
                            Log.d(TAG, "Returned from CaptureConfirmActivity with RESULT_CANCELED.");
                            Toast.makeText(CapstonActivity.this, "약 정보 등록이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // 알람등록 버튼
        if (buttonSetAlarm != null) {
            buttonSetAlarm.setOnClickListener(v -> {
                if (checkNotificationPermission()) {
                    Intent intent = new Intent(CapstonActivity.this, AllAlarmsActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(CapstonActivity.this, "알림 권한이 필요합니다. 앱 설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
                    requestNotificationPermission();
                }
            });
        }

        // 약 봉투 촬영 버튼
        if (buttonCapture != null) {
            buttonCapture.setOnClickListener(v -> {
                FirebaseUser userForCapture = mAuth.getCurrentUser();
                if (userForCapture != null) {
                    Intent intent = new Intent(CapstonActivity.this, CaptureConfirmActivity.class);
                    captureConfirmLauncher.launch(intent);
                } else {
                    Toast.makeText(CapstonActivity.this, "약 등록은 로그인 후 가능합니다.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // 복약체크 버튼
        if (buttonCheckMedicine != null) {
            buttonCheckMedicine.setOnClickListener(v -> {
                Log.d(TAG, "Medication Check button clicked. Starting MedicationCheckActivity.");
                Intent intent = new Intent(CapstonActivity.this, MedicationCheckActivity.class);
                startActivity(intent);
            });
        }

        // 뒤로가기 버튼
        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> finish());
        }

        // Firestore에서 등록된 약 정보 불러오기
        loadRegisteredMedicines();
    }

    // --- CSV 로드 메서드 ---
//    private void loadMedicineDataFromCsv() {
//        medicineList = new ArrayList<>();
//        try (CSVReader reader = new CSVReader(new InputStreamReader(getAssets().open("medicines.csv")))) {
//            String[] nextLine;
//            while ((nextLine = reader.readNext()) != null) {
//                if (nextLine.length >= 3) {
//                    medicineList.add(new Medicine(nextLine[0].trim(), nextLine[1].trim(), nextLine[2].trim()));
//                }
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "CSV 로딩 오류", e);
//        }
//    }

    // --- 검색 메서드 ---
    private List<Medicine> searchMedicines(String query) {
        Log.d(TAG, "searchMedicines() 실행. 검색어: " + query);

        List<Medicine> result = new ArrayList<>();

        for (Medicine m : medicineList) {
            String medicineName = m.getName().toLowerCase(Locale.getDefault()).replaceAll("\\s", "");

            String lowerQuery = query.toLowerCase(Locale.getDefault());
            Log.d(TAG, "lowerquery" + lowerQuery);
            Log.d(TAG, "medicineame " + medicineName);

            if (lowerQuery != null && lowerQuery.contains(medicineName)) {
                result.add(m);
            }
        }
        return result;
    }

    // --- 검색 결과 표시 및 추가 버튼 처리 ---
    private void showSearchResults(List<Medicine> matched) {
        linearLayoutMedicineItems.removeAllViews();
        if (matched.isEmpty()) {
            addEmptyMedicineListMessage("검색 결과가 없습니다.");
            return;
        }
        LayoutInflater inflater = getLayoutInflater();
        for (Medicine medicine : matched) {
            View itemView = inflater.inflate(R.layout.item_activity, linearLayoutMedicineItems, false);
            ((TextView) itemView.findViewById(R.id.textViewItemMedicineName)).setText(medicine.getName());
            ((TextView) itemView.findViewById(R.id.textViewItemMedicineInfo)).setText(medicine.getInfo());
            int resId = getResources().getIdentifier(medicine.getImagePath(), "drawable", getPackageName());
            ((ImageView) itemView.findViewById(R.id.imageViewItemMedicine)).setImageResource(resId == 0 ? R.drawable.iv0 : resId);

            // 기존 버튼 대신 "추가" 버튼만 동적으로 추가
            Button buttonAdd = new Button(this);
            buttonAdd.setText("추가");

            LinearLayout linearLayoutRoot = itemView.findViewById(R.id.linearLayoutRoot);
            linearLayoutRoot.addView(buttonAdd);  // ✅ 올바른 추가 위치

//            linearLayoutMedicineItems.addView(itemView);

            buttonAdd.setOnClickListener(v -> {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    saveMedicineToFirestore(currentUser.getUid(), medicine, aVoid -> {
                        Toast.makeText(this, "약이 추가되었습니다.", Toast.LENGTH_SHORT).show();
                        loadRegisteredMedicines();
                    }, e -> {});
                }
            });

            linearLayoutMedicineItems.addView(itemView);
        }
    }

    // --- Firestore 저장 메서드 ---
    private void saveMedicineToFirestore(String userId, Medicine medicine,
                                         com.google.android.gms.tasks.OnSuccessListener<Void> onSuccessListener,
                                         com.google.android.gms.tasks.OnFailureListener onFailureListener) {
        if (db == null) return;
        db.collection("users").document(userId)
                .collection("registeredMedicines")
                .add(medicine.toMap())
                .addOnSuccessListener(docRef -> {
                    if (onSuccessListener != null) onSuccessListener.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show();
                    if (onFailureListener != null) onFailureListener.onFailure(e);
                });
    }
    private void loadMedicineData() {
        medicineList = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(getAssets().open("medicines.csv")))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length >= 3) {
                    medicineList.add(new Medicine(nextLine[0].trim(), nextLine[1].trim(), nextLine[2].trim()));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "CSV 로딩 오류", e);
        }
    }


    // --- Firestore에서 사용자 등록 약 정보 불러오기 ---
    private void loadRegisteredMedicines() {
        if (currentUserId == null || db == null || linearLayoutMedicineItems == null) {
            Log.w(TAG, "loadRegisteredMedicines: User ID, DB, or Layout is null.");
            if (linearLayoutMedicineItems != null) {
                linearLayoutMedicineItems.removeAllViews();
                addEmptyMedicineListMessage("로그인이 필요하거나 오류 발생.");
            }
            return;
        }
        linearLayoutMedicineItems.removeAllViews();
        Log.d(TAG, "Cleared existing medicine items from layout.");

        db.collection("users").document(currentUserId)
                .collection("registeredMedicines")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult() != null && !task.getResult().isEmpty()) {
                                Log.d(TAG, "Loaded " + task.getResult().size() + " registered medicines.");
                                displayRegisteredMedicines(task.getResult().getDocuments());
                            } else {
                                Log.d(TAG, "No registered medicines found for user: " + currentUserId);
                                addEmptyMedicineListMessage("등록된 약 정보가 없습니다.\n'약 봉투 촬영' 또는 '약 검색'으로 등록해주세요.");
                            }
                        } else {
                            Log.e(TAG, "Error getting registered medicines from Firestore.", task.getException());
                            addEmptyMedicineListMessage("약 목록 불러오기 실패.");
                            Toast.makeText(CapstonActivity.this, "약 목록 불러오기 실패.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // --- 불러온 약 목록을 화면에 표시 ---
    private void displayRegisteredMedicines(List<DocumentSnapshot> medicineDocuments) {
        if (linearLayoutMedicineItems == null || medicineDocuments == null || medicineDocuments.isEmpty()) {
            addEmptyMedicineListMessage("등록된 약 정보가 없습니다.\n'약 봉투 촬영' 또는 '약 검색'으로 등록해주세요.");
            return;
        }
        linearLayoutMedicineItems.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();

        for (DocumentSnapshot document : medicineDocuments) {
            String medicineId = document.getId();
            String name = document.getString("name");
            String info = document.getString("info");
            String imagePath = document.getString("imagePath");

            View itemView = inflater.inflate(R.layout.item_medicine_info, linearLayoutMedicineItems, false);

            ImageView itemImageView = itemView.findViewById(R.id.imageViewItemMedicine);
            TextView itemTextViewName = itemView.findViewById(R.id.textViewItemMedicineName);
            TextView itemTextViewInfo = itemView.findViewById(R.id.textViewItemMedicineInfo);
            Button buttonItemSetAlarm = itemView.findViewById(R.id.buttonItemSetAlarm);
            Button buttonItemDelete = itemView.findViewById(R.id.buttonItemDelete);

            if (itemTextViewName != null) itemTextViewName.setText(name);
            if (itemTextViewInfo != null) itemTextViewInfo.setText(info);

            if (itemImageView != null) {
                if (imagePath != null && !imagePath.isEmpty()) {
                    int imageResourceId = getResources().getIdentifier(imagePath, "drawable", getPackageName());
                    if (imageResourceId != 0) {
                        itemImageView.setImageResource(imageResourceId);
                    } else {
                        itemImageView.setImageResource(R.drawable.iv0);
                    }
                } else {
                    itemImageView.setImageResource(R.drawable.iv0);
                }
            }

            if (buttonItemSetAlarm != null) {
                final String currentMedicineId = medicineId;
                final String currentMedicineName = name;
                buttonItemSetAlarm.setOnClickListener(v -> {
                    if (checkNotificationPermission()) {
                        Intent intent = new Intent(CapstonActivity.this, AlarmRegistrationActivity.class);
                        intent.putExtra(AlarmRegistrationActivity.EXTRA_MEDICINE_ID, currentMedicineId);
                        intent.putExtra(AlarmRegistrationActivity.EXTRA_MEDICINE_NAME, currentMedicineName);
                        startActivity(intent);
                    } else {
                        Toast.makeText(CapstonActivity.this, "알림 권한이 필요합니다. 앱 설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
                    }
                });
            }

            if (buttonItemDelete != null) {
                final String medicineIdToDelete = medicineId;
                buttonItemDelete.setOnClickListener(v -> deleteMedicine(medicineIdToDelete));
            }

            linearLayoutMedicineItems.addView(itemView);
        }
    }

    // 등록된 약이 없을 때 메시지 표시
    private void addEmptyMedicineListMessage(String message) {
        if (linearLayoutMedicineItems != null) {
            linearLayoutMedicineItems.removeAllViews();
            TextView emptyMessage = new TextView(this);
            emptyMessage.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            emptyMessage.setText(message);
            emptyMessage.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyMessage.setPadding(0, 16, 0, 16);
            linearLayoutMedicineItems.addView(emptyMessage);
        }
    }

    // 등록된 약 정보 삭제
    private void deleteMedicine(String medicineId) {
        if (currentUserId == null || db == null || medicineId == null) {
            Log.w(TAG, "Cannot delete medicine: User ID, DB, or Medicine ID is null.");
            Toast.makeText(this, "약 정보 삭제 실패 (정보 부족).", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("users").document(currentUserId)
                .collection("registeredMedicines").document(medicineId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Medicine data deleted from Firestore: " + medicineId);
                    Toast.makeText(this, "약 정보가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    loadRegisteredMedicines();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting medicine data from Firestore.", e);
                    Toast.makeText(this, "약 정보 삭제 실패.", Toast.LENGTH_SHORT).show();
                });
    }

    // 알림 권한 요청
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting POST_NOTIFICATIONS permission...");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission already granted.");
            }
        } else {
            Log.d(TAG, "POST_NOTIFICATIONS permission automatically granted on this Android version.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "POST_NOTIFICATIONS permission granted by user.");
                Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "POST_NOTIFICATIONS permission denied by user.");
                Toast.makeText(this, "알림 권한이 거부되어 알림을 받을 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}

