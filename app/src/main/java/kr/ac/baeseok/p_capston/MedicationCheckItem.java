package kr.ac.baeseok.p_capston; // <-- 재형님의 패키지명으로 변경

import java.util.List;

// 복약 체크 목록에 표시될 데이터를 담는 모델 클래스 (MedicationCheckListItem 구현)
public class MedicationCheckItem implements MedicationCheckListItem {
    // 이 알람 설정 자체에 대한 정보
    private String firestoreAlarmDocId; // Firestore 알람 문서 ID
    private String parentMedicineId; // 이 알람이 속한 약의 문서 ID
    private int systemAlarmId; // 시스템 알람 스케줄링에 사용된 ID
    private String medicineName; // 약 이름
    private String timeOfDayLabel; // 시간대 (예: 아침, 점심, 저녁)
    private int hour; // 알람 시간 (시)
    private int minute; // 알람 시간 (분)
    private List<Integer> days; // 알람이 설정된 요일 목록 (Calendar.MONDAY 등)
    private String dosage; // 복용량/메모
    private String userId; // 알람을 등록한 사용자 ID



    // 복약 체크 관련 정보 (이 필드는 나중에 필요시 복용 상태 저장을 위해 사용될 수 있습니다.)
    private boolean isTakenForToday; // TODO: 특정 날짜에 복용했는지 여부 (나중에 상태 저장 기능 구현 시 사용)


    // 생성자 (isTakenForToday 필드 추가)
    public MedicationCheckItem(String firestoreAlarmDocId, String parentMedicineId, int systemAlarmId, String medicineName, String timeOfDayLabel, int hour, int minute, List<Integer> days, String dosage, String userId) {
        this.firestoreAlarmDocId = firestoreAlarmDocId;
        this.parentMedicineId = parentMedicineId;
        this.systemAlarmId = systemAlarmId;
        this.medicineName = medicineName;
        this.timeOfDayLabel = timeOfDayLabel;
        this.hour = hour;
        this.minute = minute;
        this.days = days;
        this.dosage = dosage;
        this.userId = userId;
        this.isTakenForToday = false; // 기본값: 복용하지 않음
    }

    // Getter 메서드들
    public String getFirestoreAlarmDocId() { return firestoreAlarmDocId; }
    public String getParentMedicineId() { return parentMedicineId; }
    public int getSystemAlarmId() { return systemAlarmId; }
    public String getMedicineName() { return medicineName; }
    public String getTimeOfDayLabel() { return timeOfDayLabel; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }
    public List<Integer> getDays() { return days; }
    public String getDosage() { return dosage; }
    public String getUserId() { return userId; }

    // 복용 완료 상태 Getter/Setter (나중에 사용)
    public boolean isTakenForToday() { return isTakenForToday; }
    public void setTakenForToday(boolean takenForToday) { isTakenForToday = takenForToday; }


    // MedicationCheckListItem 인터페이스 구현
    @Override
    public int getType() {
        return TYPE_MEDICATION; // 복약 항목 타입 반환
    }
}
