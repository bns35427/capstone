package kr.ac.baeseok.p_capston; // <-- 재형님의 패키지명으로 변경

// 복약 체크 목록의 항목 타입을 정의하는 인터페이스
public interface MedicationCheckListItem {
    int TYPE_HEADER = 0; // 시간대 헤더 항목 타입
    int TYPE_MEDICATION = 1; // 복약 항목 타입

    // 항목 타입을 반환하는 메서드
    int getType();
}

