package kr.ac.baeseok.p_capston;

import java.util.HashMap;
import java.util.Map;

// 이 클래스는 약 정보를 저장하는 간단한 데이터 구조입니다.
public class Medicine {
    private String name; // 약 이름
    private String info; // 약 정보 (설명, 효능 등)
    private String imagePath; // 약 이미지 파일 이름 (예: drawable 폴더의 리소스 이름 확장자 제외)

    // Firestore에서 데이터를 읽어올 때 기본 생성자가 필요할 수 있습니다.
    public Medicine() {
        // 기본 생성자
    }

    // 데이터 생성 시 사용하는 생성자
    public Medicine(String name, String info, String imagePath) {
        this.name = name;
        this.info = info;
        this.imagePath = imagePath;
    }

    // 데이터를 가져오는 Getter 메서드들 (Firestore 필드 이름과 일치하는 이름 권장)
    public String getName() {
        return name;
    }

    public String getInfo() {
        return info;
    }

    public String getImagePath() {
        return imagePath;
    }

    // Firestore에서 데이터를 설정할 때 필요한 Setter 메서드들 (Firestore 사용 시 필요)
    public void setName(String name) { this.name = name; }
    public void setInfo(String info) { this.info = info; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }


    // TODO: 필요하다면 디버깅을 위한 toString 메서드를 추가할 수 있습니다.
    @Override
    public String toString() {
        return "Medicine{" +
                "name='" + name + '\'' +
                ", info='" + info + '\'' +
                ", imagePath='" + imagePath + '\'' +
                '}';
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("info", info);
        map.put("imagePath", imagePath);
        return map;
    }

}


