# Attendance Backend

Spring Boot 기반 출퇴근 체크 백엔드입니다.

## 기술 스택

- Java 17
- Spring Boot 3
- Gradle
- PostgreSQL
- Spring Data JPA
- Spring Security + JWT

## 패키지 구조

```text
com.attendance.backend
├── config
├── controller
├── domain
│   ├── entity
│   └── repository
├── dto
│   ├── attendance
│   └── auth
├── exception
├── security
├── service
└── util
```

## 주요 API

- `POST /api/auth/login`
- `POST /api/attendance/check-in`
- `POST /api/attendance/check-out`
- `GET /api/attendance/today`
- `GET /api/admin/employees`
- `GET /api/admin/attendance/today`
- `GET /api/admin/attendance/monthly/excel?year=2026&month=3`
- `PATCH /api/admin/company/location`
- `PATCH /api/admin/company/radius`

## 출근 API 응답 형식

```json
{
  "checkInTime": "2026-03-19T09:03:12",
  "late": true,
  "message": "지각으로 출근 처리되었습니다."
}
```

## 월별 엑셀 다운로드

관리자 전용 API로 월별 출퇴근 데이터를 `.xlsx` 파일로 다운로드할 수 있습니다.

- 요약 시트: 직원별 총 근무시간, 총 근무분, 출근일수
- 상세 시트: 날짜별 출근 시간, 퇴근 시간, 근무시간, 지각 여부, 상태

## 기본 시드 데이터

앱 시작 시 데이터가 없으면 아래 데이터가 생성됩니다.

- 회사명: `OpenAI Seoul Office`
- 관리자 계정: `ADMIN001 / admin1234`
- 사원 사번: `EMP001`
- 비밀번호: `password1234`
- 지각 기준 시간: `09:00`

## 실행 방법

1. PostgreSQL DB 생성
2. 환경 변수 설정
3. 애플리케이션 실행

```bash
export DB_URL=jdbc:postgresql://localhost:5432/attendance_db
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=your-very-long-jwt-secret-key
./gradlew bootRun --args='--spring.profiles.active=local'
```

Gradle Wrapper가 없다면:

```bash
gradle bootRun --args='--spring.profiles.active=local'
```

## 로컬 개발 실행 절차

현재 로컬 개발 기준은 다음과 같습니다.

- backend: `local` 프로필로 로컬에서 직접 `8080` 포트로 실행
- mobile: 로컬 backend 주소로 연결
- 기준 API 주소: `http://192.168.123.101:8080/api`
- 로컬 DB: H2 file DB (`src/main/resources/application-local.yml`)

backend 실행:

```bash
cd /Users/hyeonseobkim/workspace/attendance-app/backend
gradle bootRun --args='--spring.profiles.active=local'
```

또는 전용 task:

```bash
cd /Users/hyeonseobkim/workspace/attendance-app/backend
gradle bootRunLocal
```

backend 확인:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "employeeCode": "ADMIN001",
    "password": "admin1234"
  }'
```

mobile 실행:

```bash
cd /Users/hyeonseobkim/workspace/attendance-app/mobile
npm run start:local
```

모바일 API 기본값 파일:

- `/Users/hyeonseobkim/workspace/attendance-app/mobile/src/services/api.js`

로컬 프로필 설정 파일:

- `/Users/hyeonseobkim/workspace/attendance-app/backend/src/main/resources/application-local.yml`

## Docker Compose 실행

Docker가 설치되어 있다면 로컬 Java/Gradle/PostgreSQL 없이 바로 실행할 수 있습니다.

```bash
docker compose up --build
```

실행 후 접속:

- API 서버: `http://localhost:8080`
- PostgreSQL: `localhost:5432`

백그라운드 실행:

```bash
docker compose up --build -d
```

종료:

```bash
docker compose down
```

DB 데이터까지 함께 삭제:

```bash
docker compose down -v
```

## Hetzner 운영 배포

Hetzner VPS 1대에 운영 배포할 때는 개발용 `docker-compose.yml` 대신 운영용 파일을 사용합니다.

- 운영 Compose: [docker-compose.prod.yml](/Users/hyeonseobkim/workspace/attendance-app/backend/docker-compose.prod.yml)
- 운영 환경 변수 예시: [.env.prod.example](/Users/hyeonseobkim/workspace/attendance-app/backend/.env.prod.example)
- Nginx 설정: [infra/nginx/default.conf](/Users/hyeonseobkim/workspace/attendance-app/backend/infra/nginx/default.conf)

기본 구조:

- `postgres`: 내부 전용 PostgreSQL
- `app`: Spring Boot API
- `nginx`: 외부 80 포트 수신 후 app으로 프록시

배포 순서 예시:

```bash
cp .env.prod.example .env.prod
docker compose -f docker-compose.prod.yml --env-file .env.prod up --build -d
```

상태 확인:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
docker compose -f docker-compose.prod.yml --env-file .env.prod logs app --tail=100
```

Windows 미니PC에서 `git pull` 부터 재기동까지 한 번에 하려면 아래 스크립트를 사용할 수 있습니다.

- 배포 스크립트: [scripts/deploy-prod.ps1](/Users/hyeonseobkim/workspace/attendance-app/backend/scripts/deploy-prod.ps1)

실행 예시:

```powershell
cd C:\attendance-app\backend
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-prod.ps1
```

더블클릭 실행용 배치 파일:

- [deploy-prod.bat](/Users/hyeonseobkim/workspace/attendance-app/backend/scripts/deploy-prod.bat)

미니PC에서 `C:\attendance-app\backend\scripts\deploy-prod.bat` 를 더블클릭하면 같은 배포가 실행됩니다.

재기동만 하는 스크립트:

- [restart-prod.ps1](/Users/hyeonseobkim/workspace/attendance-app/backend/scripts/restart-prod.ps1)
- [restart-prod.bat](/Users/hyeonseobkim/workspace/attendance-app/backend/scripts/restart-prod.bat)

미니PC에서 `C:\attendance-app\backend\scripts\restart-prod.bat` 를 더블클릭하면 GitHub pull 없이 현재 코드 기준으로 재빌드/재기동만 수행합니다.

다른 브랜치를 배포할 때:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-prod.ps1 -Branch main
```

이미 최신 코드가 받아져 있다면 pull 없이 재기동만:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deploy-prod.ps1 -SkipPull
```

중지:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod down
```

주의:

- 운영에서는 `.env.prod` 에 강한 `POSTGRES_PASSWORD`, `JWT_SECRET` 을 사용하세요.
- 현재 설정은 HTTP 기준입니다. 도메인 연결 후 HTTPS는 Nginx + Certbot 또는 Cloudflare Tunnel/Proxy로 추가하는 것을 권장합니다.

## GitHub Actions 자동 배포

운영 배포는 `GitHub Actions -> Amazon ECR -> AWS App Runner` 흐름으로 구성할 수 있습니다.

- 워크플로 파일: [.github/workflows/deploy.yml](/Users/hyeonseobkim/workspace/attendance-app/backend/.github/workflows/deploy.yml)
- 배포 가이드: [docs/AWS_DEPLOYMENT.md](/Users/hyeonseobkim/workspace/attendance-app/backend/docs/AWS_DEPLOYMENT.md)

필요한 GitHub 설정:

- Variable: `AWS_REGION`
- Variable: `ECR_REPOSITORY`
- Secret: `AWS_ROLE_TO_ASSUME`

권장 방식:

- GitHub OIDC로 AWS Role Assume
- GitHub Actions가 ECR에 이미지 push
- App Runner는 ECR `latest` 이미지 자동 배포
