#!/usr/bin/env bash
set -euo pipefail

root_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$root_dir"

project_name=${COMPOSE_PROJECT_NAME:-debt-note-smoke}

cleanup() {
  docker compose -p "$project_name" down --volumes --remove-orphans
}
trap cleanup EXIT

docker compose -p "$project_name" up --build --detach --wait
app_port=$(docker compose -p "$project_name" port app 8080 | sed 's/.*://')
base_url="http://localhost:${app_port}"

payment_date=$(date -u -d "+3 days" +%F)
request="{\"title\":\"CI Subscription\",\"amount\":399.00,\"currency\":\"RUB\",\"category\":\"subscription\",\"recurrence\":\"monthly\",\"next_payment_date\":\"${payment_date}\"}"

created=$(curl --fail-with-body --silent --show-error \
  -H "Content-Type: application/json" \
  -d "$request" \
  "$base_url/obligations")
id=$(sed -n 's/.*"obligation":{"id":"\([^"]*\)".*/\1/p' <<<"$created")
[[ $id =~ ^[0-9a-f-]{36}$ ]]

duplicate=$(curl --fail-with-body --silent --show-error \
  -H "Content-Type: application/json" \
  -d "$request" \
  "$base_url/obligations")
grep -q '"warning":"' <<<"$duplicate"

upcoming=$(curl --fail-with-body --silent --show-error \
  "$base_url/obligations/upcoming?days=7")
grep -q "\"id\":\"$id\"" <<<"$upcoming"
grep -Eq '"RUB":798(\.0+)?[,}]' <<<"$upcoming"

paid=$(curl --fail-with-body --silent --show-error \
  -X POST "$base_url/obligations/$id/pay")
grep -q "\"obligation_id\":\"$id\"" <<<"$paid"

echo "Compose smoke passed"
