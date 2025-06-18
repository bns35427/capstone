package kr.ac.baeseok.p_capston; // <-- 재형님의 패키지명으로 변경

// 복약 체크 목록의 시간대 헤더 데이터를 담는 모델 클래스
public class TimeslotHeader implements MedicationCheckListItem {
    private String title; // 시간대 제목 (예: "아침 복약")

    // 생성자
    public TimeslotHeader(String title) {
        this.title = title;
    }

    // Getter
    public String getTitle() {
        return title;
    }

    // MedicationCheckListItem 인터페이스 구현
    @Override
    public int getType() {
        return TYPE_HEADER; // 헤더 타입 반환
    }
}
