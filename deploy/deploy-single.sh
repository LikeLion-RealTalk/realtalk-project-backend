#!/bin/bash
set -euo pipefail

# ===== 설정 =====
CONTAINER_NAME="spring-app"
TMP_NAME="spring-tmp"
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

# 2) 임시 컨테이너로 사전 헬스체크 (포트 매핑 없음: 충돌 회피)
echo "[배포] 임시 컨테이너 기동: ${TMP_NAME}"
docker rm -f "${TMP_NAME}" >/dev/null 2>&1 || true
docker run -d --name "${TMP_NAME}" \
  --network host \
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
  "${IMAGE}"

# 3) 임시 컨테이너 내부 헬스체크 (최대 30초)
echo "[배포] 임시 컨테이너 헬스체크 대기..."
HEALTH_OK=false
for i in {1..15}; do
  sleep 2
  # 컨테이너 내부에서 8080 헬스엔드포인트 확인
  if docker exec "${TMP_NAME}" sh -c "curl -fs http://localhost:${PORT}/actuator/health | grep -q '\"status\":\"UP\"'"; then
    echo "[배포] 임시 컨테이너 헬스체크 통과!"
    HEALTH_OK=true
    break
  fi
done

if [[ "${HEALTH_OK}" != true ]]; then
  echo "[배포] 임시 컨테이너 헬스체크 실패! 로그 출력 후 중단"
  docker logs "${TMP_NAME}" || true
  docker rm -f "${TMP_NAME}" || true
  exit 1
fi

# 4) 실제 서비스 컨테이너를 8080으로 교체
echo "[배포] 실제 서비스 컨테이너 교체 시작"

# 4-1) 기존 서비스 중지/삭제
docker stop "${CONTAINER_NAME}" >/dev/null 2>&1 || true
docker rm "${CONTAINER_NAME}" >/dev/null 2>&1 || true

# 4-2) 새 이미지로 실제 서비스 컨테이너 기동 (8080 노출)
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
  "${IMAGE}"

# 4-3) 외부(호스트)에서 최종 헬스체크 (최대 30초)
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
      "${PREV_IMAGE_ID}"
  else
    echo "[배포] 이전 이미지 정보가 없어 롤백 불가"
  fi

  # 임시 컨테이너 정리 후 실패 종료
  docker rm -f "${TMP_NAME}" || true
  exit 1
fi

# 5) 임시 컨테이너 정리
docker rm -f "${TMP_NAME}" >/dev/null 2>&1 || true

echo "===== [배포] 단일 포트 배포 완료! (${CONTAINER_NAME} on :${PORT}) ====="