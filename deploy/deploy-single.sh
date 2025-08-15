#!/bin/bash
set -euo pipefail

# ===== 설정 =====
CONTAINER_NAME="spring-app"
PORT=8080

# 새로 배포할 이미지 (GitHub Actions 같은 곳에서 환경변수로 전달)
IMAGE="${DOCKER_USERNAME}/realtalk-backend:${GITHUB_SHA}"

echo "===== [배포] 단일 포트(:${PORT}) 배포 시작 - IMAGE: ${IMAGE} ====="

# (옵션) 현재 실행 중인 컨테이너/이미지 정보
CURRENT_RUNNING=$(docker ps --filter "name=^/${CONTAINER_NAME}$" --format "{{.ID}}" || true)
PREV_IMAGE_ID=""
if [[ -n "${CURRENT_RUNNING}" ]]; then
  PREV_IMAGE_ID=$(docker inspect -f '{{.Image}}' "${CONTAINER_NAME}" || true)
  echo "[배포] 현재 실행 중: ${CONTAINER_NAME} (image=${PREV_IMAGE_ID})"
else
  echo "[배포] 현재 실행 중인 ${CONTAINER_NAME} 없음 (최초 배포일 수 있음)"
fi

# 1) 이미지 풀
echo "[배포] 이미지 pull..."
docker pull "${IMAGE}"

# 2) 실제 서비스 컨테이너를 교체
echo "[배포] 실제 서비스 컨테이너 교체 시작"

echo "[배포] MySQL 리슨 대기..."
until (echo > /dev/tcp/127.0.0.1/${MYSQL_PORT}) >/dev/null 2>&1; do
  echo "[배포] wait mysql..."; sleep 2
done

# 2-1) 기존 서비스 중지/삭제
docker stop "${CONTAINER_NAME}" >/dev/null 2>&1 || true
docker rm "${CONTAINER_NAME}" >/dev/null 2>&1 || true

# 2-2) 새 이미지로 실제 서비스 컨테이너 기동
docker run -d --name "${CONTAINER_NAME}" \
  --network host \
  --restart unless-stopped \
  -e SERVER_PORT=${PORT} \
  -e MYSQL_HOST="${MYSQL_HOST}" \
  -e MYSQL_PORT="${MYSQL_PORT}" \
  -e MYSQL_DATABASE="${MYSQL_DATABASE}" \
  -e MYSQL_USERNAME="${MYSQL_USERNAME}" \
  -e MYSQL_PASSWORD="${MYSQL_PASSWORD}" \
  -e REDIS_HOST="${REDIS_HOST}" \
  -e REDIS_PORT="${REDIS_PORT}" \
  -e ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY}" \
  -e KAKAO_CLIENT_ID="${KAKAO_CLIENT_ID}" \
  -e KAKAO_CLIENT_SECRET="${KAKAO_CLIENT_SECRET}" \
  -e GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID}" \
  -e GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET}" \
  -e GOOGLE_CLIENT_NAME="${GOOGLE_CLIENT_NAME}" \
  -e JWT_SECRET="${JWT_SECRET}" \
  -e BASE_URL="${BASE_URL}" \
  -e FRONTEND_URL="${FRONTEND_URL}" \
  # -e GCP_KEY_PATH="/keys/gcp.json" \ (cicd가 돌아야 웹소켓 연결 테스트를 하기 때문에 이부분 주석처리 하겠습니다)
  # --mount type=volume,source=gcp-keys,target=/keys,readonly=true \
  "${IMAGE}"

# 2-3) 외부(호스트)에서 최종 헬스체크 (최대 30초)
echo "[배포] 최종 헬스체크 대기..."
FINAL_OK=false
for i in {1..15}; do
  sleep 2
  if curl -fs "http://localhost:${PORT}/actuator/health" | grep -q '"status":"UP"'; then
    echo "[배포] 최종 헬스체크 통과!"
    FINAL_OK=true
    break
  fi
done

if [[ "${FINAL_OK}" != true ]]; then
  echo "[배포] 최종 헬스체크 실패! 롤백 시도"
  docker logs "${CONTAINER_NAME}" || true
  docker rm -f "${CONTAINER_NAME}" || true

  if [[ -n "${PREV_IMAGE_ID}" ]]; then
    echo "[배포] 이전 이미지로 롤백 재기동: ${PREV_IMAGE_ID}"
    docker run -d --name "${CONTAINER_NAME}" \
      --network host \
      --restart unless-stopped \
      -e SERVER_PORT=${PORT} \
      -e MYSQL_HOST="${MYSQL_HOST}" \
      -e MYSQL_PORT="${MYSQL_PORT}" \
      -e MYSQL_DATABASE="${MYSQL_DATABASE}" \
      -e MYSQL_USERNAME="${MYSQL_USERNAME}" \
      -e MYSQL_PASSWORD="${MYSQL_PASSWORD}" \
      -e REDIS_HOST="${REDIS_HOST}" \
      -e REDIS_PORT="${REDIS_PORT}" \
      -e ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY}" \
      -e KAKAO_CLIENT_ID="${KAKAO_CLIENT_ID}" \
      -e KAKAO_CLIENT_SECRET="${KAKAO_CLIENT_SECRET}" \
      -e GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID}" \
      -e GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET}" \
      -e GOOGLE_CLIENT_NAME="${GOOGLE_CLIENT_NAME}" \
      -e JWT_SECRET="${JWT_SECRET}" \
      -e BASE_URL="${BASE_URL}" \
      -e FRONTEND_URL="${FRONTEND_URL}" \
      # -e GCP_KEY_PATH="/keys/gcp.json" \ (cicd가 돌아야 웹소켓 연결 테스트를 하기 때문에 이부분 주석처리 하겠습니다)
      # --mount type=volume,source=gcp-keys,target=/keys,readonly=true \
      "${PREV_IMAGE_ID}"
  else
    echo "[배포] 이전 이미지 정보가 없어 롤백 불가"
  fi

  exit 1
fi

echo "===== [배포] 배포 완료! (${CONTAINER_NAME} on :${PORT}) ====="