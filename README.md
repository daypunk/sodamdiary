<div align="center">

# 소담일기

<a href="https://m.onestore.co.kr/v2/ko-kr/app/0001003018">
  <img src="https://github.com/user-attachments/assets/80d651c5-9319-46aa-800f-10346171cfd8" width="400" />
</a>

<br/>

👆 클릭 시 원스토어로 이동

</div>

## Summary

소담일기는 시각장애인이 독립적으로 사진을 촬영하고 관리할 수 있도록 설계된 안드로이드 앱입니다. TalkBack 스크린 리더와 완전히 호환되며, 음성 입력만으로 모든 기능을 사용할 수 있습니다.

핵심 특징:
- TalkBack 포커스 순서를 정밀하게 제어해 화면 진입 시 헤더가 아닌 본문이 먼저 읽히도록 설계
- Naver CLOVA STT를 활용한 음성 녹음과 텍스트 변환 동시 처리
- 네트워크 없이도 사용 가능한 오프라인 로컬 DB 구조
- 촬영한 사진을 서버 AI가 분석하여 일기 형태의 설명 생성

---

## Technical Highlights

### 1. TalkBack 접근성 위계구조

일반적인 안드로이드 앱에서 TalkBack은 화면 상단부터 순차적으로 읽습니다. 이는 네비게이션 버튼이 본문보다 먼저 읽히는 문제를 야기합니다. 소담일기는 `ScreenLayout` 컴포넌트에서 이 문제를 해결합니다.

접근성 설계 원칙:
- `traversalIndex`를 사용하여 읽기 순서 명시적 제어 (0: 본문, 1: 하단 버튼, 2: 상단 헤더)
- `suppressHeaderUntilFocused` 플래그로 화면 전환 시 헤더를 일정 시간 숨김 처리
- `initialFocusRequester`로 포커스 대상을 명시하여 원하는 요소가 먼저 읽히도록 강제
- 모든 버튼에 `contentDescription` 적용, 단순히 "버튼"이 아닌 동작 설명 제공

### 2. 녹음과 STT 동시 처리

음성 입력 시 녹음 파일 저장과 텍스트 변환을 병렬로 수행합니다. Naver CLOVA Speech SDK의 `onRecord` 콜백에서 PCM 데이터를 받아 WAV 파일로 저장하면서, 동시에 STT 결과를 수신합니다.

타이머 기반 STT 생애주기:
- 발화 시작 없이 6초 경과 시 자동 취소
- 발화 후 6초 침묵 감지 시 자동 종료
- 부분 결과(partial result) 수신 시 침묵 타이머 리셋

저장된 음성 파일은 사진 상세 화면에서 재생할 수 있어, 사용자가 촬영 당시 말한 내용을 다시 들을 수 있습니다.

### 3. 오프라인 로컬 DB

Room을 DB가 모든 데이터의 Single source of truth 역할을 합니다. 네트워크 요청이 실패하더라도 사진은 로컬에 저장됩니다.

데이터 구조:
- 사진 파일 경로, 촬영 일시, GPS 좌표, 주소
- 사용자 음성 녹음 파일 경로
- 사용자 음성 변환 텍스트
- 서버 생성 일기, 검색용 태그

사진 삭제 시 연관된 음성 파일도 함께 삭제하여 저장 공간을 관리합니다.

---

## Tech Stack

| 영역 | 기술 |
|------|------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Local DB | Room |
| Network | Retrofit2, OkHttp |
| STT | Naver CLOVA Speech SDK |
| Location | Android Location Services |

---

## Screen Flow

```
MainScreen
    ├── CameraScreen (촬영)
    │       └── PhotoPreviewScreen (미리보기)
    │               └── PhotoInfoChoiceScreen (음성 입력)
    │                       └── PhotoDetailScreen (일기 확인)
    │
    └── GalleryScreen (갤러리)
            ├── PhotoDetailScreen (상세 보기)
            └── SearchResultScreen (음성 검색 결과)
```

촬영 flow:
1. 카메라로 사진 촬영
2. 미리보기에서 사진 확인
3. 음성으로 메모 추가 (서버 이미지 캡셔닝 실행, 추론 시간 단축)
4. 서버가 AI 일기 생성, 결과 화면에서 TalkBack으로 청취

갤러리 flow:
1. 전체 사진 목록 탐색
2. 음성 검색으로 원하는 사진 찾기
3. 상세 화면에서 일기 청취 및 음성 메모 재생

---

## Project Structure

```
com.example.sodam_diary/
├── data/
│   ├── database/       # Room DB, DAO
│   ├── entity/         # PhotoEntity
│   ├── network/        # Retrofit API
│   └── repository/     # 데이터 계층 통합
├── ui/
│   ├── components/     # ScreenLayout (접근성 레이아웃)
│   ├── screens/        # 각 화면 Composable
│   └── theme/          # 색상, 타이포그래피
├── utils/
│   ├── VoiceRecorder   # STT + 녹음 통합
│   ├── VoicePlayer     # 음성 재생
│   ├── AudioWriterWAV  # WAV 변환
│   ├── LocationHelper  # 위치 정보
│   └── PhotoManager    # 이미지 파일 처리
└── MainActivity        # Navigation Host
```

---

## Requirements

- Android 8.0 (API 26) 이상
- 카메라, 마이크, 위치 권한 필요
- TalkBack 활성화 권장


