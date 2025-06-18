package kr.ac.baeseok.p_capston; // <-- 재형님의 패키지명으로 변경

import java.util.List;

// 알람 정보를 담을 데이터 모델 클래스
public class AllAlarmItem {
    private String firestoreAlarmDocId; // Firestore 알람 문서 ID
    private String parentMedicineId; // 이 알람이 속한 약의 문서 ID
    private int systemAlarmId; // 시스템 알람 스케줄링에 사용된 ID
    private String medicineName; // 약 이름
    private String timeOfDayLabel; // 시간대 (예: 아침, 점심, 저녁)
    private int hour;
    private int minute;
    private List<Integer> days; // 요일 목록
    private String dosage; // 복용량/메모


    // 생성자
    public AllAlarmItem(String firestoreAlarmDocId, String parentMedicineId, int systemAlarmId, String medicineName, String timeOfDayLabel, int hour, int minute, List<Integer> days, String dosage) {
        this.firestoreAlarmDocId = firestoreAlarmDocId;
        this.parentMedicineId = parentMedicineId;
        this.systemAlarmId = systemAlarmId;
        this.medicineName = medicineName;
        this.timeOfDayLabel = timeOfDayLabel;
        this.hour = hour;
        this.minute = minute;
        this.days = days;
        this.dosage = dosage;
    }

    // Getter 메서드들 (필요한 정보에 접근하기 위해)
    public String getFirestoreAlarmDocId() {
        return firestoreAlarmDocId;
    }

    public String getParentMedicineId() {
        return parentMedicineId;
    }

    public int getSystemAlarmId() {
        return systemAlarmId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getTimeOfDayLabel() {
        return timeOfDayLabel;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public List<Integer> getDays() {
        return days;
    }

    public String getDosage() {
        return dosage;
    }

    // Setter는 필요하다면 추가합니다. 현재는 필요 없을 것 같습니다.
}

