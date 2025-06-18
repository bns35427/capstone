package kr.ac.baeseok.p_capston; // <<< 여기에 재형 님의 실제 패키지 이름을 입력하세요. >>>

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull; // 혹시 필요한 경우를 대비해 추가합니다.
import androidx.annotation.Nullable; // 혹시 필요한 경우를 대비해 추가합니다.
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat; // 이전 권한 요청 방식 지원을 위해 필요할 수 있습니다.
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast; // 사용자에게 메시지를 보여주기 위해 필요합니다.
import android.view.View; // OnClickListener 사용 시 필요합니다.

//public class SelectImageActivity extends AppCompatActivity {
//
//    // ActivityResultLauncher를 사용하므로, startActivityForResult의 요청 코드는 직접적으로 사용되지 않습니다.
//    // final int GET_GALLERY_IMAGE = 200; // 이전 방식의 요청 코드
//
//    private ImageView imageViewPhotoPreview; // 이미지를 표시할 ImageView
//    private Button buttonSelectImage; // 갤러리 선택 버튼
//
//    // 런타임 권한 요청 결과를 처리하기 위한 ActivityResultLauncher
//    private ActivityResultLauncher<String> requestPermissionLauncher;
//
//    // 갤러리 인텐트 결과를 처리하기 위한 ActivityResultLauncher
//    private ActivityResultLauncher<Intent> pickImageLauncher;
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        // <<< 레이아웃 파일 이름이 activity_capston인지 activity_main인지 확인하고 수정하세요. >>>
//        setContentView(R.layout.activity_capston);
//
//        // <<< XML 레이아웃 파일의 이미지 뷰 ID와 버튼 ID가 맞는지 확인하고 수정하세요. >>>
//        imageViewPhotoPreview = findViewById(R.id.imageViewPhotoPreview); // 예시 ID, 실제 ID로 변경 필요
//        buttonSelectImage = findViewById(R.id.buttonSelectImage); // 예시 ID, 실제 ID로 변경 필요
//
//        // 이미지 뷰와 버튼이 레이아웃에 존재하는지 확인하는 것이 좋습니다.
//        if (buttonSelectImage != null) {
//            buttonSelectImage.setOnClickListener(v -> {
//                // 버튼 클릭 시 권한 확인 및 요청 절차 시작
//                checkAndRequestPermissions();
//            });
//        } else {
//            // 만약 buttonSelectImage를 찾지 못했다면 (ID가 잘못되었거나 레이아웃 파일이 틀렸다면)
//            // 여기에서 로그를 남겨서 문제를 알 수 있도록 합니다.
//            // Log.e("SelectImageActivity", "Button with ID buttonSelectImage not found in layout.");
//            // 사용자에게 버튼을 찾을 수 없다는 메시지를 표시할 수도 있습니다.
//        }
//
//        // ActivityResultLauncher 초기화 (권한 요청 결과 처리)
//        requestPermissionLauncher = registerForActivityResult(
//                new ActivityResultContracts.RequestPermission(),
//                isGranted -> {
//                    // 권한 요청 결과 콜백
//                    if (isGranted) {
//                        // 권한이 허용된 경우 갤러리 열기
//                        openGallery();
//                    } else {
//                        // 권한이 거부된 경우 사용자에게 메시지 표시
//                        Toast.makeText(this, "갤러리 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
//                        // 여기서 권한이 영구적으로 거부되었는지 확인하고 설정 화면으로 이동하도록 안내하는 로직을 추가할 수 있습니다.
//                    }
//                });
//
//        // ActivityResultLauncher 초기화 (갤러리 선택 결과 처리)
//        pickImageLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    // 갤러리에서 이미지 선택 후 결과 콜백
//                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
//                        // 이미지가 성공적으로 선택되었고 데이터가 있는 경우
//                        Uri selectedImageUri = result.getData().getData(); // 선택된 이미지의 Uri 가져오기
//
//                        if (imageViewPhotoPreview != null) {
//                            // 이미지 뷰에 선택된 이미지 표시
//                            imageViewPhotoPreview.setImageURI(selectedImageUri);
//                        }
//                        // 이제 selectedImageUri를 사용하여 필요한 다음 작업을 수행할 수 있습니다 (예: 서버 업로드, OCR 처리 등).
//                    }
//                    // 사용자가 이미지 선택을 취소한 경우에는 별도의 처리가 필요 없을 수 있습니다.
//                });
//    }
//
//    // 갤러리 접근에 필요한 권한을 확인하고, 없으면 요청하는 메서드
//    private void checkAndRequestPermissions() {
//        // 타겟 SDK 버전에 따라 요청할 권한을 결정합니다.
//        String permission;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            // Android 13 (API 레벨 33) 이상에서는 READ_MEDIA_IMAGES 권한 사용
//            permission = Manifest.permission.READ_MEDIA_IMAGES;
//        } else {
//            // Android 13 미만 버전에서는 READ_EXTERNAL_STORAGE 권한 사용
//            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
//        }
//
//        // 현재 권한 상태 확인
//        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
//            // 권한이 이미 허용된 경우 바로 갤러리 열기
//            openGallery();
//        } else {
//            // 권한이 허용되지 않은 경우 사용자에게 권한 요청
//            requestPermissionLauncher.launch(permission); // ActivityResultLauncher를 사용하여 권한 요청
//        }
//    }
//
//    // 갤러리 인텐트를 시작하는 메서드
//    private void openGallery() {
//        // 갤러리에서 이미지를 선택하는 인텐트 생성
//        Intent intent = new Intent(Intent.ACTION_PICK);
//        intent.setType("image/*"); // 이미지 파일만 선택 가능하도록 MIME 타입 설정
//
//        // ActivityResultLauncher를 사용하여 갤러리 인텐트 실행
//        pickImageLauncher.launch(intent);
//    }
//
//    // 참고: ActivityResultLauncher를 사용하기 때문에 onActivityResult와 onRequestPermissionsResult 메서드는
//    // 여기서는 더 이상 명시적으로 오버라이드하여 사용하지 않아도 됩니다.
//    // 만약 이전 버전 API를 함께 사용해야 하는 경우가 있다면 필요에 따라 추가할 수 있습니다.
//}
