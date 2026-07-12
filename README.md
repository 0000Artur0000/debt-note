# Smart Subscription Registry

REST API для учёта подписок и регулярных платежей.

Стек: Java 21, Spring Boot, PostgreSQL 16, Spring Data JPA, Flyway, Gradle, Docker Compose.

## Запуск

```bash
cp .env.example .env
docker compose up --build
```

Flyway применяет миграции автоматически. Сервис доступен на `http://localhost:8080`, Swagger UI — на `http://localhost:8080/docs`.

Остановка:

```bash
docker compose down
```

Переменные окружения перечислены в `.env.example`.

## API

| Метод | Путь | Назначение |
| --- | --- | --- |
| `POST` | `/obligations` | создать обязательство |
| `GET` | `/obligations` | получить список; фильтры `category`, `status` |
| `GET` | `/obligations/upcoming?days=7` | получить ближайшие платежи |
| `POST` | `/obligations/{id}/pay` | зафиксировать оплату |
| `PATCH` | `/obligations/{id}/cancel` | отменить обязательство |
| `DELETE` | `/obligations/{id}` | удалить обязательство |
| `GET` | `/obligations/events` | подключиться к SSE |

## Примеры

```bash
curl -X POST http://localhost:8080/obligations \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Яндекс.Плюс",
    "amount": 399.00,
    "currency": "RUB",
    "category": "subscription",
    "recurrence": "monthly",
    "next_payment_date": "2026-08-09"
  }'

curl 'http://localhost:8080/obligations?category=subscription&status=active'
curl 'http://localhost:8080/obligations/upcoming?days=7'
OBLIGATION_ID='UUID из ответа POST /obligations'
curl -X POST "http://localhost:8080/obligations/${OBLIGATION_ID}/pay"
```

## Бизнес-правила

- Прошедшая дата при создании даёт статус `expired`, но не ошибку.
- Дубль активного `title` создаётся с `warning`.
- Lazy expiry применяется только к разовым обязательствам. Рекуррентная запись остаётся `active`, пока пользователь явно не оплатит или не отменит её.
- Следующая дата считается от текущего графика, а не от даты оплаты. Исходный день сохраняется: после `31 января → 28 февраля` следующий платёж снова приходится на 31-е, когда месяц это допускает. Аналогично восстанавливается 29 февраля.
- `upcoming` содержит только активные обязательства; суммы не конвертируются между валютами.
- Удаление каскадно удаляет payments и после commit отправляет SSE-событие `obligation_deleted`.

## Тесты

```bash
bash ./gradlew clean check
bash scripts/compose-smoke.sh
```

Unit/integration-тесты используют H2 и не требуют внешней БД. Smoke поднимает PostgreSQL через Compose и проверяет create, duplicate warning, upcoming и pay.

## Компромиссы

- Нет авторизации, пользователей, аудита и идемпотентности запросов.
- SSE хранит соединения в памяти одного процесса; для нескольких экземпляров потребовался бы broker или outbox.
- Unit-тесты используют H2; PostgreSQL проверяется отдельным Compose smoke.
- При большем времени добавил бы Testcontainers, пагинацию и метрики.

## Мотивация

Интересен backend AI-продукта: API, бизнес-правила, данные и эксплуатация. В команде вижу себя Java backend-разработчиком, который отвечает за реализацию и надёжность сервиса. Готов уделять проекту около 20 часов в неделю на долгосрочной основе.
