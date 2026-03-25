# GitHub Self-Hosted Runner Deployment

미니PC에서 GitHub Actions self-hosted runner를 사용해 `backend` 를 원격 배포하는 방법입니다.

## 권장 방식

- GitHub 저장소에서 Actions 실행
- 미니PC에 설치된 Windows self-hosted runner가 작업 수행
- 실제 배포는 `C:\attendance-app\backend\scripts\deploy-prod.ps1` 또는 `restart-prod.ps1` 실행

이 방식이면 다른 PC에서 GitHub 웹 UI만 열 수 있어도 배포할 수 있습니다.

## 준비 사항

- 미니PC에 `backend` 소스가 `C:\attendance-app\backend` 에 있어야 함
- `.env.prod` 가 준비되어 있어야 함
- Docker Desktop 또는 Docker Engine 이 미니PC에서 정상 동작해야 함
- 저장소에 GitHub Actions 사용 가능해야 함

## 1. 미니PC에 self-hosted runner 설치

GitHub 저장소에서:

1. `Settings`
2. `Actions`
3. `Runners`
4. `New self-hosted runner`
5. OS는 `Windows` 선택

runner 폴더는 예를 들어 아래처럼 두는 것을 추천합니다.

```text
C:\actions-runner-backend
```

GitHub가 보여주는 PowerShell 명령을 미니PC에서 그대로 실행해 등록합니다.

## 2. runner 라벨 설정

이 workflow는 아래 라벨을 기대합니다.

- `self-hosted`
- `windows`
- `attendance-prod`

runner 등록 시 custom label 로 `attendance-prod` 를 추가하세요.

## 3. 미니PC backend 경로 준비

배포 workflow는 아래 경로를 사용합니다.

```text
C:\attendance-app\backend
```

여기에 아래 스크립트가 있어야 합니다.

- `scripts\deploy-prod.ps1`
- `scripts\restart-prod.ps1`

## 4. GitHub Actions workflow

workflow 파일:

- `/Users/hyeonseobkim/workspace/attendance-app/backend/.github/workflows/deploy-self-hosted.yml`

지원 동작:

- `deploy`: GitHub 최신 코드 pull 후 재배포
- `restart`: pull 없이 현재 코드 기준 재기동

## 5. 실행 방법

GitHub 저장소에서:

1. `Actions`
2. `Deploy To Mini PC`
3. `Run workflow`
4. `deploy_type` 선택
   - `deploy`
   - `restart`
5. branch 입력
   - 보통 `main`

## 6. 동작 방식

### deploy

아래 스크립트를 실행합니다.

```text
C:\attendance-app\backend\scripts\deploy-prod.ps1
```

이 스크립트는:

- `git fetch`
- `git checkout`
- `git pull`
- `docker compose up --build -d`
- 컨테이너 상태 확인
- 최근 로그 출력

### restart

아래 스크립트를 실행합니다.

```text
C:\attendance-app\backend\scripts\restart-prod.ps1
```

이 스크립트는:

- pull 없이 현재 코드 기준 재빌드/재기동
- 컨테이너 상태 확인
- 최근 로그 출력

## 7. 보안 주의

public 저장소에서 self-hosted runner 사용은 주의가 필요합니다.

- 가능하면 수동 실행(`workflow_dispatch`)만 사용
- 자동 push 배포는 충분히 안정화된 뒤 검토
- runner는 운영 미니PC 전용으로 사용

## 8. 추천 운영 방식

- 평소 코드 반영: GitHub Actions `deploy`
- 긴급 재기동: GitHub Actions `restart`
- 미니PC에서 직접 처리해야 할 때만 BAT 더블클릭 사용

