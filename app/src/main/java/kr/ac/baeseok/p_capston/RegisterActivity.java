package kr.ac.baeseok.p_capston;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private FirebaseAuth auth; // Firebase 인증 객체
    private FirebaseFirestore firestore; // Firebase Firestore 객체

    // UI 요소 선언 (activity_register.xml의 ID와 일치한다고 가정)
    private EditText editTextUsername;
    private EditText editTextPhone;
    //private EditText editTextEmail; -> private EditText editTextPhone;
    private EditText editTextPassword;
    private Button buttonRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // activity_register.xml 레이아웃 설정

        // Firebase 인스턴스 초기화
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance(); // Firestore 인스턴스 초기화

        // 레이아웃 파일(activity_register.xml)의 UI 요소 연결
        editTextUsername = findViewById(R.id.editTextName); // XML ID: editTextName 가정
        editTextPhone = findViewById(R.id.editTextPhone);  // XML ID: editTextEmail 가정
        editTextPassword = findViewById(R.id.editTextPassword); // XML ID: editTextPassword 가정
        buttonRegister = findViewById(R.id.buttonRegister); // XML ID: buttonRegister 가정


        // 회원가입 버튼 클릭 리스너 설정
        if (buttonRegister != null) {
            buttonRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    registerUser(); // 회원가입 처리 메서드 호출
                }
            });
        } else {
            Log.e(TAG, "Error: Register button not found in layout!");
            Toast.makeText(this, "회원가입 버튼을 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
        }
    }

    // 회원가입 처리 메서드 (Firebase Authentication 사용자 생성)
    private void registerUser() {
        if (editTextUsername == null || editTextPhone == null || editTextPassword == null) {
            Log.e(TAG, "Error: UI elements not initialized properly.");
            Toast.makeText(this, "앱 오류 발생: 입력 필드를 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        String username = editTextUsername.getText().toString().trim();
        String phone = editTextPhone.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // 입력값 유효성 검사
        if (TextUtils.isEmpty(username)) {
            editTextUsername.setError("사용자 이름을 입력해주세요.");
            editTextUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            editTextPhone.setError("휴대폰 번호를 입력해주세요.");
            editTextPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("비밀번호로 사용할 숫자 6자리를 입력해주세요.");
            editTextPassword.requestFocus();
            return;
        }
        if (password.length() < 6) { // Firebase Auth 최소 비밀번호 길이 6자
            editTextPassword.setError("비밀번호는 6자 이상이어야 합니다.");
            editTextPassword.requestFocus();
            return;
        }


        //빠른 구현을 위해 "휴대폰번호+@도메인" 형태의 가짜 이메일을 만들기.<<휴대폰번호를위한.
        //휴대폰번호를 fakeEmail로 변환하여 Firebase에 전달
        String fakeEmail = phone + "@phoneuser.com";


        // 이메일과 비밀번호로 Firebase Authentication 사용자 생성
        auth.createUserWithEmailAndPassword(fakeEmail, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // 사용자 생성 성공
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser firebaseUser = auth.getCurrentUser(); // 생성된 사용자 정보 가져오기
                            if (firebaseUser != null) {
                                // Firestore에 사용자 추가 정보 (이름, 이메일) 저장
                                saveUserData(firebaseUser.getUid(), username, phone);
                            } else {
                                Log.w(TAG, "createUserWithEmail: success but user is null.");
                            }

                            Toast.makeText(RegisterActivity.this, "회원가입 성공!", Toast.LENGTH_LONG).show();
                            navigateToMainActivity(); // 회원가입 성공 시 로그인 화면으로 이동

                        } else {
                            // 사용자 생성 실패
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            String errorMessage = "회원가입 실패했습니다.";
                            // 실패 원인에 따른 메시지
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                errorMessage = "이미 존재하는 휴대폰 번호입니다."; // 이미 해당 휴대폰번호로 가입된 경우
                            } else {
                                Log.e(TAG, "Registration failed: " + task.getException().getMessage());
                                // errorMessage = "회원가입 실패: " + task.getException().getMessage(); // 상세 오류 메시지 (개발용)
                                errorMessage = "회원가입 실패했습니다."; // 일반적인 오류 메시지
                            }
                            Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Firestore에 사용자 추가 정보 저장 (사용자 UID를 문서 ID로 사용)
    private void saveUserData(String userId, String name, String phone) {
        if (firestore == null) {
            Log.e(TAG, "Error: Firestore not initialized.");
            return;
        }

        // Firestore에 저장할 데이터 Map 생성
        Map<String, Object> user = new HashMap<>();
        user.put("name", name); // 사용자 이름 또는 ID
        user.put("phone", phone); // 사용자  휴대폰번호

        // "users" 컬렉션 아래에 사용자 UID를 문서 ID로 사용하여 문서 생성/업데이트
        firestore.collection("users")
                .document(userId) // 문서 ID를 사용자 UID로 지정
                .set(user) // 데이터 저장 (문서가 없으면 생성, 있으면 덮어쓰기)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Firestore user data saved for ID: " + userId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Firestore user data save error for ID: " + userId, e);
                        // 사용자에게 직접 오류 메시지를 보여주지 않을 수도 있습니다. (선택 사항)
                        // Toast.makeText(RegisterActivity.this, "사용자 정보 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 메인 액티비티 (로그인 화면)로 화면 전환
    private void navigateToMainActivity() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        // 회원가입 성공 후에는 이전 Activity 스택을 모두 지우고 새로운 Task로 시작
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // 현재 Activity (RegisterActivity) 종료
    }
}


