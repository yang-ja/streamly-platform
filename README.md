
````markdown
# streamly-platform

Java/Spring 기반 VOD 스트리밍 POC. 업로드 → 트랜스코딩(HLS) → 서명 URL 재생까지 단일 노트북/PC에서 재현.

## 구성
- services/
  - **ingest-service**: 업로드, 메타 저장, Kafka 이벤트 발행
  - **transcode-service**: Kafka 소비, FFmpeg HLS 변환, MinIO 업로드
  - **playback-api**: HLS `index.m3u8` 서명 URL 발급
- `docker-compose.yml`: MinIO, Kafka, Postgres 인프라

## 기술 스택
- Java 21, Spring Boot 3.5.x, JPA, Flyway, Kafka
- MinIO(Java SDK)로 S3 호환 스토리지
- FFmpeg CLI 기반 트랜스코딩

## 포트
- MinIO: 9000(API), 9001(Console)
- Postgres: 5432
- Kafka: 9092
- ingest: 8080, playback: 8082 (로컬 실행 시)

## 요구 사항
- Windows 10/11, PowerShell
- Docker Desktop, Git, JDK 21, FFmpeg(PATH 등록), Gradle(또는 Gradle Wrapper)

## 빠른 시작
1) 인프라 기동
```powershell
cd C:\dev\streamly-platform
docker compose up -d
````

2. 서비스 실행(각 콘솔 창)

```powershell
cd services\ingest-service;   .\gradlew.bat bootRun
cd services\transcode-service; .\gradlew.bat bootRun
cd services\playback-api;     .\gradlew.bat bootRun
```

3. 업로드 테스트

```powershell
curl -F "title=Sample" -F "subject=Science" -F "grade=7" -F "chapter=3" `
     -F "file=@C:\videos\sample.mp4" http://localhost:8080/v1/contents
# => {"id":"<CONTENT_ID>","status":"UPLOADED"}
```

4. 트랜스코딩 확인

* `transcode-service` 콘솔에서 ffmpeg 로그 확인
* MinIO 콘솔([http://localhost:9001](http://localhost:9001)) 로그인 `minio/minio12345`
* 버킷 `streamly` 내부 `hls/<CONTENT_ID>/index.m3u8` 생성 확인

5. 재생 URL 발급

```powershell
curl http://localhost:8082/v1/contents/<CONTENT_ID>/play
# => {"hls":"http://127.0.0.1:9000/streamly/hls/<CONTENT_ID>/index.m3u8?..."}
```

* HLS 데모 플레이어(hls.js demo)에 URL 붙여 재생 확인

## 환경 변수(기본값)

* `MINIO_ENDPOINT=http://localhost:9000`
* `MINIO_ACCESS_KEY=minio`, `MINIO_SECRET_KEY=minio12345`, `MINIO_BUCKET=streamly`
* `KAFKA_BOOTSTRAP_SERVERS=localhost:9092`
* `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/streamly`
* `SPRING_DATASOURCE_USERNAME=streamly`, `SPRING_DATASOURCE_PASSWORD=streamly1234`

## API 요약

* `POST /v1/contents` (ingest)

  * form-data: `title, subject, grade, chapter, file(@mp4)`
  * 202 + `{id, status}`
* `GET /v1/contents/{id}/play` (playback)

  * 200 + `{hls}`

## 트러블슈팅

* `docker compose up -d` 에러: 현재 디렉토리에 `docker-compose.yml` 존재 확인
* MinIO 9001 접속 불가: Docker Desktop 리소스 제한 확인
* ffmpeg 실패: `transcode-service` 로그에서 종료코드 확인, 로컬 FFmpeg 설치 상태 점검
* Kafka 연결 실패: 포트 9092 열림과 서비스 기동 순서 확인

## 로드맵

* [ ] 720p+1080p ABR + 마스터 m3u8
* [ ] Playback 서명 URL 만료 및 간이 워터마크
* [ ] 시청 이벤트 수집 API + ClickHouse 리포트
* [ ] CI/CD(GitHub Actions) + Dockerfile + K8s 매니페스트

## 라이선스

TBD (MIT 권장)

````

커밋:
```powershell
git add README.md
git commit -m "docs: add README with setup and usage"
git push origin main
````

