package kr.ac.baeseok.p_capston; // <-- 재형님의 패키지명으로 변경

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton; // OnCheckedChangeListener 사용
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Calendar;


// 복용 완료 체크 이벤트 발생 시 Activity에 알리기 위한 인터페이스 정의
interface OnMedicationCheckListener {
    // 알람 문서 ID와 변경된 체크 상태를 Activity에 전달
    void onCheckChanged(String firestoreAlarmDocId, boolean isChecked);
}


public class MedicationCheckAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> { // RecyclerView.ViewHolder 사용

    private List<MedicationCheckListItem> checkItemList; // MedicationCheckListItem 목록 사용
    private OnMedicationCheckListener checkListener; // 복용 완료 체크 리스너 변수

    // 어댑터 생성자 (체크 리스너 받도록 수정)
    public MedicationCheckAdapter(List<MedicationCheckListItem> checkItemList, OnMedicationCheckListener checkListener) {
        this.checkItemList = checkItemList;
        this.checkListener = checkListener; // 리스너 초기화
    }

    // --- ViewHolder 클래스들 (항목 타입별로 정의) ---

    // 시간대 헤더 ViewHolder
    static class TimeslotHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTimeslotHeader;

        TimeslotHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTimeslotHeader = itemView.findViewById(R.id.textViewTimeslotHeader);
        }
    }

    // 복약 항목 ViewHolder
    static class MedicationCheckViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMedicineName;
        TextView textViewAlarmTime;
        TextView textViewAlarmDays;
        TextView textViewAlarmDosage;
        CheckBox checkBoxMedicationTaken;

        MedicationCheckViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMedicineName = itemView.findViewById(R.id.textViewCheckMedicineName);
            textViewAlarmTime = itemView.findViewById(R.id.textViewCheckAlarmTime);
            textViewAlarmDays = itemView.findViewById(R.id.textViewCheckAlarmDays);
            textViewAlarmDosage = itemView.findViewById(R.id.textViewCheckAlarmDosage);
            checkBoxMedicationTaken = itemView.findViewById(R.id.checkBoxMedicationTaken);
        }
    }

    // --- getItemViewType: 항목 위치에 따라 뷰 타입 반환 ---
    @Override
    public int getItemViewType(int position) {
        return checkItemList.get(position).getType();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == MedicationCheckListItem.TYPE_HEADER) {
            View headerView = inflater.inflate(R.layout.item_timeslot_header, parent, false);
            return new TimeslotHeaderViewHolder(headerView);
        } else { // viewType == MedicationCheckListItem.TYPE_MEDICATION
            View medicationView = inflater.inflate(R.layout.item_medication_check, parent, false);
            return new MedicationCheckViewHolder(medicationView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MedicationCheckListItem currentItem = checkItemList.get(position);

        int viewType = holder.getItemViewType();

        if (viewType == MedicationCheckListItem.TYPE_HEADER) {
            TimeslotHeader header = (TimeslotHeader) currentItem;
            TimeslotHeaderViewHolder headerHolder = (TimeslotHeaderViewHolder) holder;
            headerHolder.textViewTimeslotHeader.setText(header.getTitle());

        } else { // 복약 항목 ViewHolder인 경우 (viewType == MedicationCheckListItem.TYPE_MEDICATION)
            MedicationCheckItem medicationItem = (MedicationCheckItem) currentItem;
            MedicationCheckViewHolder medicationHolder = (MedicationCheckViewHolder) holder;

            // 약 이름 설정
            medicationHolder.textViewMedicineName.setText(medicationItem.getMedicineName());

            // 알람 시간 설정
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", medicationItem.getHour(), medicationItem.getMinute());
            medicationHolder.textViewAlarmTime.setText(medicationItem.getTimeOfDayLabel() + " " + formattedTime);

            // 요일 설정
            medicationHolder.textViewAlarmDays.setText(selectedDaysListToString(medicationItem.getDays()));

            // 복용량/메모 설정 (데이터가 없으면 숨김)
            String dosage = medicationItem.getDosage();
            if (dosage != null && !dosage.trim().isEmpty()) {
                medicationHolder.textViewAlarmDosage.setText("복용량/메모: " + dosage);
                medicationHolder.textViewAlarmDosage.setVisibility(View.VISIBLE);
            } else {
                medicationHolder.textViewAlarmDosage.setVisibility(View.GONE);
            }

            // !!! 복용 완료 체크박스 상태 설정 및 클릭 리스너 !!!

            // 중요: 리스너를 먼저 null로 설정하여 재활용 시 중복 호출 방지
            medicationHolder.checkBoxMedicationTaken.setOnCheckedChangeListener(null);

            // MedicationCheckItem 모델의 isTakenForToday 필드를 사용하여 체크박스 초기 상태 설정
            medicationHolder.checkBoxMedicationTaken.setChecked(medicationItem.isTakenForToday());

            // 체크박스 상태 변경 리스너 설정
            medicationHolder.checkBoxMedicationTaken.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // MedicationCheckItem 모델의 상태 업데이트 (Adapter 내부에서만 사용, DB 저장은 Activity에서)
                    medicationItem.setTakenForToday(isChecked);

                    // Activity (리스너)에 상태 변경 이벤트 알림
                    if (checkListener != null) {
                        checkListener.onCheckChanged(medicationItem.getFirestoreAlarmDocId(), isChecked);
                    }
                }
            });


        }
    }

    @Override
    public int getItemCount() {
        return checkItemList.size();
    }

    // 어댑터 데이터 업데이트 메서드 (데이터 변경 시 호출)
    public void setCheckItemList(List<MedicationCheckListItem> newList) {
        checkItemList = newList;
        notifyDataSetChanged(); // 데이터 변경 알림
    }

    // Calendar 요일 값을 문자열로 변환 (표시용)
    private String selectedDaysListToString(List<Integer> selectedDays) {
        if (selectedDays == null || selectedDays.isEmpty()) {
            return "미설정";
        }
        List<String> dayNames = new ArrayList<>();
        List<Integer> sortedDays = new ArrayList<>(selectedDays);
        Collections.sort(sortedDays);

        for (int day : sortedDays) {
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
        return "반복: " + String.join(", ", dayNames);
    }
}
