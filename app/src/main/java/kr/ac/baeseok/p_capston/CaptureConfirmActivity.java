package kr.ac.baeseok.p_capston;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.Locale;

public class CaptureConfirmActivity extends AppCompatActivity {

    private static final String TAG = "CaptureConfirmActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private Button buttonTakePicture;
    private ImageView imageViewPhotoPreview;
    private TextView textViewStatus;
    private LinearLayout linearLayoutMatchedMedicine;
    private Button buttonRegisterConfirm;
    private Button buttonBackConfirm;

    private List<Medicine> medicineList;
    private List<Medicine> matchedMedicinesList;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_confirm);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        buttonTakePicture = findViewById(R.id.buttonTakePicture);
        imageViewPhotoPreview = findViewById(R.id.imageViewPhotoPreview);
        textViewStatus = findViewById(R.id.textViewStatus);
        linearLayoutMatchedMedicine = findViewById(R.id.linearLayoutMatchedMedicine);
        buttonRegisterConfirm = findViewById(R.id.buttonRegisterConfirm);
        buttonBackConfirm = findViewById(R.id.buttonBackConfirm);

        loadMedicineData();

        buttonTakePicture.setOnClickListener(v -> {
            String[] options = {"갤러리에서 사진 선택", "카메라로 촬영"};
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("사진 선택 방법")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            // 갤러리에서 사진 선택
                            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            intent.setType("image/*");
                            startActivityForResult(intent, 1001);
                        } else if (which == 1) {
                            // 카메라로 촬영
                            checkCameraPermissionAndTakePicture();
                        }
                    })
                    .show();
        });

//        buttonTakePicture.setOnClickListener(v -> checkCameraPermissionAndTakePicture());

        buttonRegisterConfirm.setVisibility(View.GONE);
        buttonRegisterConfirm.setText("모두 등록");
        buttonRegisterConfirm.setOnClickListener(v -> {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (matchedMedicinesList != null && !matchedMedicinesList.isEmpty() && currentUser != null) {
                saveAllMedicinesToFirestore(currentUser.getUid(), matchedMedicinesList);
            } else {
                Toast.makeText(this, "등록할 약 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonBackConfirm.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        linearLayoutMatchedMedicine.setVisibility(View.GONE);
        buttonRegisterConfirm.setVisibility(View.GONE);
        textViewStatus.setText("약 봉투를 촬영해주세요.");
        imageViewPhotoPreview.setImageDrawable(null);
        matchedMedicinesList = null;
    }
    private void checkCameraPermissionAndTakePicture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "카메라 앱을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            if (imageBitmap != null) {
                // 카메라 썸네일은 회전 정보 없음 → 그대로 사용
                imageViewPhotoPreview.setImageBitmap(imageBitmap);
                performOcrWithCloudVision(imageBitmap);
            }
        } else if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            // 갤러리에서 사진 선택 처리
            try {
                Uri imageUri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                Bitmap rotatedBitmap = rotateImageIfRequired(bitmap, imageUri);
                imageViewPhotoPreview.setImageBitmap(rotatedBitmap);
                performOcrWithCloudVision(rotatedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "이미지 불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 이미지를 회전 보정
    private Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) {
        try {
            InputStream input = getContentResolver().openInputStream(selectedImage);
            ExifInterface ei = new ExifInterface(input);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return rotateImage(img, 90);
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return rotateImage(img, 180);
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return rotateImage(img, 270);
                default:
                    return img;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return img;
        }
    }

    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


    private void performOcrWithCloudVision(Bitmap bitmap) {
        String base64Image = bitmapToBase64(bitmap);
        String apiKey = "AIzaSyCB4C_7AFRJJ0u6q2KLfSsqDQPsVKBpcZE"; // 실제 API 키로 대체 필요

        try {
            JSONObject requestBody = new JSONObject();
            JSONArray requestsArray = new JSONArray();
            JSONObject image = new JSONObject();
            image.put("content", base64Image);

            JSONObject feature = new JSONObject();
            feature.put("type", "TEXT_DETECTION");

            JSONObject request = new JSONObject();
            request.put("image", image);
            request.put("features", new JSONArray().put(feature));

// 👇 여기에 추가
            JSONObject imageContext = new JSONObject();
            imageContext.put("languageHints", new JSONArray().put("ko")); // 한국어 OCR 힌트
            request.put("imageContext", imageContext); // 요청에 넣기

            requestsArray.put(request);
            requestBody.put("requests", requestsArray);

            String url = "https://vision.googleapis.com/v1/images:annotate?key=" + apiKey;
            RequestQueue queue = Volley.newRequestQueue(this);

            JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                    response -> {
                        try {
                            JSONArray responses = response.getJSONArray("responses");
                            JSONObject annotation = responses.getJSONObject(0).optJSONObject("fullTextAnnotation");
                            String recognizedText = annotation != null ? annotation.getString("text") : "";
                            Log.d("OCR_Result", "recognizedText = " + recognizedText); // ✅ 로그 추가
                            processRecognizedText(recognizedText);
                        } catch (JSONException e) {
                            Log.e(TAG, "OCR 응답 처리 중 오류", e);
                            textViewStatus.setText("텍스트 분석 오류");
                        }
                    },
                    error -> {
                        Log.e(TAG, "Vision API 요청 실패", error);
                        textViewStatus.setText("서버 통신 실패");
                    }
            );

            queue.add(jsonRequest);
        } catch (JSONException e) {
            Log.e(TAG, "OCR 요청 JSON 구성 실패", e);
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }

    private void processRecognizedText(String recognizedText) {
        if (recognizedText != null && !recognizedText.trim().isEmpty()) {
            textViewStatus.setText("");
            //굳이 사용자가 인식된 텍스트를 알 필요 없는 것 같아서 제외.
            //textViewStatus.setText("인식된 텍스트: " + (recognizedText.length() > 50 ? recognizedText.substring(0, 50) + "..." : recognizedText));
            matchedMedicinesList = lookupMedicines(recognizedText);
            displayMatchedMedicines(matchedMedicinesList);
        } else {
            textViewStatus.setText("텍스트가 감지되지 않았습니다.");
            linearLayoutMatchedMedicine.setVisibility(View.GONE);
            buttonRegisterConfirm.setVisibility(View.GONE);
            matchedMedicinesList = null;
        }
    }

    private List<Medicine> lookupMedicines(String recognizedText) {
        List<Medicine> matched = new ArrayList<>();
        if (medicineList == null || recognizedText == null) return matched;
        Log.d("recog" , recognizedText);

        // recognizedText에서 약물 이름을 추출하는 과정 (여기서는 공백 기준으로 나눠봄)
        String[] extractedNames = extractDrugNames(recognizedText);

        for (String extractedName : extractedNames) {


            Log.d("extract" , extractedName);

            String cleanText = extractedName.toLowerCase(Locale.getDefault()).replaceAll("[\\s\n]", "");

            // DB에서 일치하는 약물 찾기
            for (Medicine m : medicineList) {
                String medicineName = m.getName().toLowerCase(Locale.getDefault()).replaceAll("\\s", "");
                // 유사도 점수 계산
                double similarity = getSimilarityScore(cleanText, medicineName);
                Log.d("SimilarityCheck", "OCR: " + cleanText + " vs DB: " + medicineName + " = " + similarity);
                if (similarity > 0.6) {
                    matched.add(m);
                }
            }
        }
        Log.d("mateched", "mateched: " + matched);

        return matched;
    }

    private String[] extractDrugNames(String recognizedText) {
        // 예시: 공백이나 숫자, 특수문자를 기준으로 분리
        // 이 부분은 상황에 따라 다르게 처리 가능 (정규식 사용 등)
        return recognizedText.split("[\\s]+");  // 공백 기준으로 분리
    }


    private double getSimilarityScore(String s1, String s2) {
        int maxLen = Math.max(s1.length(), s2.length());
        if (maxLen == 0) return 1.0; // 둘 다 비어있으면 동일

        int distance = levenshtein(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    private int levenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }



    private void displayMatchedMedicines(List<Medicine> medicinesToDisplay) {
        linearLayoutMatchedMedicine.removeAllViews();
        if (medicinesToDisplay == null || medicinesToDisplay.isEmpty()) {
            textViewStatus.setText("일치하는 약 정보를 찾을 수 없습니다.");
            linearLayoutMatchedMedicine.setVisibility(View.GONE);
            buttonRegisterConfirm.setVisibility(View.GONE);
            return;
        }

        LayoutInflater inflater = getLayoutInflater();
        Log.d("MatchedMedicines", "List size: " + medicinesToDisplay.size());
        for (Medicine medicine : medicinesToDisplay) {
            Log.d("medicine", "medicine: " + medicine);

            View itemView = inflater.inflate(R.layout.item_medicine_info, linearLayoutMatchedMedicine, false);
            ((TextView) itemView.findViewById(R.id.textViewItemMedicineName)).setText(medicine.getName());
            ((TextView) itemView.findViewById(R.id.textViewItemMedicineInfo)).setText(medicine.getInfo());

            int resId = getResources().getIdentifier(medicine.getImagePath(), "drawable", getPackageName());
            ((ImageView) itemView.findViewById(R.id.imageViewItemMedicine)).setImageResource(resId == 0 ? R.drawable.iv0 : resId);

            itemView.findViewById(R.id.buttonItemDelete).setOnClickListener(v -> {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    saveMedicineToFirestore(currentUser.getUid(), medicine, aVoid -> {
                        setResult(RESULT_OK);
                        finish();
                    }, e -> {});
                }
            });

            linearLayoutMatchedMedicine.addView(itemView);
        }

        linearLayoutMatchedMedicine.setVisibility(View.VISIBLE);
        buttonRegisterConfirm.setVisibility(View.VISIBLE);
    }

    private void saveAllMedicinesToFirestore(String userId, List<Medicine> list) {
        for (Medicine medicine : list) {
            saveMedicineToFirestore(userId, medicine, null, null);
        }
        setResult(RESULT_OK);
        finish();
    }

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


}