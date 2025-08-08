#!/bin/bash
set -e

# === 1. 블루/그린 포트/컨테이너 결정 ===
if docker ps | grep -q spring-blue; then
  OLD=blue
  OLD_PORT=8080
  NEW=green
  NEW_PORT=8081
else
  OLD=green
  OLD_PORT=8081
  NEW=blue
  NEW_PORT=8080
fi

IMAGE="${DOCKER_USERNAME}/realtalk-backend:${GITHUB_SHA}"

echo "===== [배포] 현재 서비스:$OLD($OLD_PORT), 신규:$NEW($NEW_PORT) ====="

# === 2. 새 컨테이너 실행 ===
docker pull $IMAGE

docker run -d --name spring-$NEW \
  --network host \
  --restart unless-stopped \
  -e SERVER_PORT=$NEW_PORT \
  -e MYSQL_HOST="$MYSQL_HOST" \
  -e MYSQL_PORT="$MYSQL_PORT" \
  -e MYSQL_DATABASE="$MYSQL_DATABASE" \
  -e MYSQL_USERNAME="$MYSQL_USERNAME" \
  -e MYSQL_PASSWORD="$MYSQL_PASSWORD" \
  -e REDIS_HOST="$REDIS_HOST" \
  -e REDIS_PORT="$REDIS_PORT" \
  -e ANTHROPIC_API_KEY="$ANTHROPIC_API_KEY" \
  -e KAKAO_CLIENT_ID="$KAKAO_CLIENT_ID" \
  -e KAKAO_CLIENT_SECRET="$KAKAO_CLIENT_SECRET" \
  -e GOOGLE_CLIENT_ID="$GOOGLE_CLIENT_ID" \
  -e GOOGLE_CLIENT_SECRET="$GOOGLE_CLIENT_SECRET" \
  -e GOOGLE_CLIENT_NAME="$GOOGLE_CLIENT_NAME" \
  -e JWT_SECRET="$JWT_SECRET" \
  -e BASE_URL="$BASE_URL" \
  -e FRONTEND_URL="$FRONTEND_URL" \
  $IMAGE

# === 3. 헬스체크 (최대 30초) ===
echo "[배포] 헬스체크 대기..."
for i in {1..15}; do
  sleep 2
  if curl -fs http://localhost:$NEW_PORT/actuator/health | grep -q '"status":"UP"'; then
    echo "[배포] 헬스체크 통과!"
    break
  fi
  if [ $i -eq 15 ]; then
    echo "[배포] 헬스체크 실패! 롤백"
    docker logs spring-$NEW
    docker stop spring-$NEW
    docker rm spring-$NEW
    exit 1
  fi
done

# === 4. Nginx 프록시 포트 스위칭 ===
NGINX_CONF="/etc/nginx/sites-available/api.realtalks.co.kr"
if [ $NEW_PORT -eq 8080 ]; then
  sed -i 's/# server localhost:8080;/server localhost:8080;/' $NGINX_CONF
  sed -i 's/server localhost:8081;/# server localhost:8081;/' $NGINX_CONF
else
  sed -i 's/# server localhost:8081;/server localhost:8081;/' $NGINX_CONF
  sed -i 's/server localhost:8080;/# server localhost:8080;/' $NGINX_CONF
fi

# Nginx 리로드 (systemctl or 직접)
if command -v systemctl &> /dev/null; then
  nginx -t && nginx -s reload

echo "[배포] 프록시 스위칭 완료!"

# === 5. 이전 컨테이너 종료/삭제 ===
docker stop spring-$OLD || true
docker rm spring-$OLD || true

echo "===== [배포] 블루-그린 무중단 배포 완료! ====="