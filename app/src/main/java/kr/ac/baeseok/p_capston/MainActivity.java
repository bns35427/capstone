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
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private EditText editTextPhone; // XML ID: EmailAddress
    private EditText editTextPassword;     // XML ID: Password
    private Button buttonLogin;         // XML ID: button (로그인 버튼)
    private Button buttonRegister;      // XML ID: button2 (회원가입 버튼)

    private FirebaseAuth mAuth; // Firebase 인증 객체

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // activity_main.xml 레이아웃 설정

        // Firebase Auth 인스턴스 초기화
        mAuth = FirebaseAuth.getInstance();

        // 레이아웃 파일(activity_main.xml)의 UI 요소 연결
        editTextPhone = findViewById(R.id.PhoneNum);
        editTextPassword = findViewById(R.id.Password);
        buttonLogin = findViewById(R.id.button);
        buttonRegister = findViewById(R.id.button2);

        // 로그인 버튼 (ID: button) 클릭 리스너
        if (buttonLogin != null) {
            buttonLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loginUser(); // Firebase 인증 로직 수행
                }
            });
        } else {
            Log.e(TAG, "Error: Login button (ID 'button') not found in layout!");
            Toast.makeText(this, "로그인 버튼을 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
        }

        // 회원가입 버튼 (ID: button2) 클릭 리스너
        if (buttonRegister != null) {
            buttonRegister.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // RegisterActivity로 이동 (activity_register.xml 화면)
                    Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                    startActivity(intent);
                }
            });
        } else {
            Log.e(TAG, "Error: Register button (ID 'button2') not found in layout!");
        }

        // 앱 시작 시 이미 로그인된 사용자가 있는지 확인 (선택 사항)
        // FirebaseUser currentUser = mAuth.getCurrentUser();
        // if (currentUser != null) {
        // 이미 로그인 되어 있다면 바로 CapstonActivity로 이동
        // navigateToCapstonActivity();
        // }
        // 참고: 보통 로그인 화면에서는 로그인 상태를 바로 확인하지 않고, 메인 화면(CapstonActivity)에서 확인 후 로그인 화면으로 돌려보내는 방식을 많이 사용합니다.
    }

    // Firebase Authentication을 사용하여 사용자 로그인 처리
    private void loginUser() {
        if (editTextPhone == null || editTextPassword == null) {
            Log.e(TAG, "Error: Email/Password EditTexts not initialized properly.");
            Toast.makeText(this, "앱 오류 발생: 입력 필드를 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
            return;
        }

        String phone = editTextPhone.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(phone)) {
            editTextPhone.setError("휴대폰 번호를 입력해주세요.");
            editTextPhone.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("비밀번호를 입력해주세요.");
            editTextPassword.requestFocus();
            return;
        }

        //폰번호에 가짜 이메일도메인 연결.<<빠른 구현위해.
        String fakeEmail = phone + "@phoneuser.com";

        // 이메일과 비밀번호로 Firebase 로그인 시도
        mAuth.signInWithEmailAndPassword(fakeEmail, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // 로그인 성공
                            Log.d(TAG, "signInWithEmail:success");
                            Toast.makeText(MainActivity.this, "로그인 성공!", Toast.LENGTH_SHORT).show();
                            navigateToCapstonActivity(); // 로그인 성공 시 CapstonActivity로 이동
                        } else {
                            // 로그인 실패
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            String errorMessage = "로그인 실패했습니다.";
                            // 실패 원인에 따른 메시지
                            if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                errorMessage = "등록되지 않은 휴대폰 번호이거나, 탈퇴한 계정입니다.";
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "잘못된 비밀번호입니다.";
                            } else {
                                Log.e(TAG, "Authentication failed: " + task.getException().getMessage());
                                // errorMessage = "로그인 중 오류가 발생했습니다. 다시 시도해주세요."; // 상세 오류 메시지 (개발용)
                                errorMessage = "로그인 실패했습니다."; // 일반적인 오류 메시지
                            }
                            Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // CapstonActivity로 화면 전환 메서드
    private void navigateToCapstonActivity() {
        Intent intent = new Intent(MainActivity.this, CapstonActivity.class);
        // 로그인 후에는 이전 Activity 스택을 모두 지우고 새로운 Task로 시작 (사용자가 뒤로가기 눌러도 로그인 화면으로 돌아가지 않도록)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // 현재 Activity (MainActivity) 종료
    }
}
