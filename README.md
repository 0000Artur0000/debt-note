# Smart Subscription Registry

REST API для учёта подписок и регулярных платежей. Стек: Java 21, Spring Boot 3, PostgreSQL 16, Spring Data JPA, Flyway, Gradle, Docker Compose.

## Запуск

```bash
docker compose up --build
```

Сервис: <http://localhost:8080>  
Swagger UI: <http://localhost:8080/docs>

Миграции Flyway применяются автоматически. Остановка:

```bash
docker compose down
```

## Переменные окружения

| Переменная | По умолчанию |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/subscriptions` |
| `SPRING_DATASOURCE_USERNAME` | `user` |
| `SPRING_DATASOURCE_PASSWORD` | `pass` |

В Docker Compose адрес БД заменён на `jdbc:postgresql://postgres:5432/subscriptions`.

## API

| Метод | Путь | Действие |
| --- | --- | --- |
| `POST` | `/obligations` | создать обязательство |
| `GET` | `/obligations` | получить список; фильтры `category`, `status` |
| `GET` | `/obligations/upcoming?days=7` | получить ближайшие платежи и суммы по валютам |
| `POST` | `/obligations/{id}/pay` | зафиксировать оплату |
| `PATCH` | `/obligations/{id}/cancel` | отменить обязательство |
| `DELETE` | `/obligations/{id}` | удалить обязательство |
| `GET` | `/obligations/events` | подключиться к SSE |

Значения enum передаются в нижнем регистре. `recurrence`: `monthly`, `quarterly`, `yearly` или `null`.

### Примеры

```bash
curl -X POST http://localhost:8080/obligations \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Яндекс.Плюс",
    "amount": 399.00,
    "currency": "RUB",
    "category": "subscription",
    "recurrence": "monthly",
    "next_payment_date": "2026-08-09"
  }'

curl "http://localhost:8080/obligations?category=subscription&status=active"
curl "http://localhost:8080/obligations/upcoming?days=7"
curl -X POST http://localhost:8080/obligations/{id}/pay
curl -X PATCH http://localhost:8080/obligations/{id}/cancel
curl -X DELETE http://localhost:8080/obligations/{id}
```

При активном дубле запись создаётся с полем `warning`. Дата в прошлом создаёт запись со статусом `expired`. Оплата или отмена неактивной записи возвращает `422`, неизвестный UUID — `404`.

## Бизнес-правила

**Lazy expiry.** Перед `GET /obligations` просроченные разовые записи переводятся в `expired`. Рекуррентные остаются активными: прошедшая дата платежа не означает отмену подписки.

**Перенос даты.** Следующая дата считается от текущего `next_payment_date`, а не от даты оплаты. `LocalDate` обрабатывает конец месяца: `31.01 + 1 месяц = 28.02` или `29.02` в високосный год.

## Архитектура

`Controller -> Service -> Repository -> PostgreSQL`. Бизнес-правила находятся в сервисе, схема БД — в миграции Flyway. Удаление обязательства каскадно удаляет платежи и отправляет SSE-событие `obligation_deleted`.

## Тесты

```bash
./gradlew test
```

Проверяются lazy expiry, дубли, все варианты recurrence, граничные даты, запрет действий для неактивных записей и HTTP-контракт. PostgreSQL для тестов не нужен.

## Компромиссы

Нет авторизации, аудита изменений, идемпотентности и Testcontainers. SSE-соединения хранятся в памяти процесса.

## Мотивация

Интересен backend AI-продукта: API, модель данных, бизнес-правила и интеграции. Готов уделять проекту около 20 часов в неделю, долгосрочно.
