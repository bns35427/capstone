package kr.ac.baeseok.p_capston; // <-- 재형님의 패키지명으로 변경

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Calendar;


interface OnAlarmDeleteListener {
    void onDeleteClick(String firestoreAlarmDocId, String parentMedicineId, int systemAlarmId);
}


public class AllAlarmsAdapter extends RecyclerView.Adapter<AllAlarmsAdapter.AllAlarmViewHolder> {

    private List<AllAlarmItem> alarmList;
    private OnAlarmDeleteListener deleteListener;

    public AllAlarmsAdapter(List<AllAlarmItem> alarmList, OnAlarmDeleteListener deleteListener) {
        this.alarmList = alarmList;
        this.deleteListener = deleteListener;
    }

    static class AllAlarmViewHolder extends RecyclerView.ViewHolder {
        TextView textViewMedicineName;
        TextView textViewAlarmTime;
        TextView textViewAlarmDays;
        TextView textViewAlarmDosage; // <-- 이 변수 선언이 여기에 있습니다.
        Button buttonDeleteAlarm;

        AllAlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewMedicineName = itemView.findViewById(R.id.textViewAllAlarmMedicineName);
            textViewAlarmTime = itemView.findViewById(R.id.textViewAllAlarmTime);
            textViewAlarmDays = itemView.findViewById(R.id.textViewAllAlarmDays);
            textViewAlarmDosage = itemView.findViewById(R.id.textViewAllAlarmDosage); // <-- 여기서 연결합니다.
            buttonDeleteAlarm = itemView.findViewById(R.id.buttonDeleteAllAlarm);
        }
    }

    @NonNull
    @Override
    public AllAlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_all_alarms, parent, false);
        return new AllAlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AllAlarmViewHolder holder, int position) {
        AllAlarmItem currentItem = alarmList.get(position);

        holder.textViewMedicineName.setText(currentItem.getMedicineName());

        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", currentItem.getHour(), currentItem.getMinute());
        holder.textViewAlarmTime.setText(currentItem.getTimeOfDayLabel() + " " + formattedTime);

        holder.textViewAlarmDays.setText(selectedDaysListToString(currentItem.getDays()));


        String dosage = currentItem.getDosage();
        if (dosage != null && !dosage.trim().isEmpty()) {
            holder.textViewAlarmDosage.setText("복용량/메모: " + dosage); // <-- 여기서 사용합니다.
            holder.textViewAlarmDosage.setVisibility(View.VISIBLE);
        } else {
            holder.textViewAlarmDosage.setVisibility(View.GONE); // <-- 여기서 사용합니다.
        }

        holder.buttonDeleteAlarm.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(
                        currentItem.getFirestoreAlarmDocId(),
                        currentItem.getParentMedicineId(),
                        currentItem.getSystemAlarmId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    public void setAlarmList(List<AllAlarmItem> newList) {
        alarmList = newList;
        notifyDataSetChanged();
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
