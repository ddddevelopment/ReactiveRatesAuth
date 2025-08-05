# ReactiveRates Auth Service

Микросервис аутентификации с поддержкой JWT токенов и refresh токенов.

## Возможности

- ✅ Регистрация пользователей
- ✅ Аутентификация пользователей
- ✅ JWT Access токены (15 минут)
- ✅ Refresh токены (7 дней)
- ✅ Обновление токенов
- ✅ Выход из системы
- ✅ Автоматическая очистка истекших токенов
- ✅ Swagger UI документация

## Технологии

- **Spring Boot 3.x**
- **Spring Security**
- **JWT (JSON Web Tokens)**
- **PostgreSQL**
- **Spring Data JPA**
- **Swagger/OpenAPI 3**

## Конфигурация

### База данных

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/authdb
    username: postgres
    password: root
```

### JWT токены

```yaml
jwt:
  secret: mySecretKeymySecretKeymySecretKeymySecretKey
  access-token:
    expiration: 900000  # 15 минут
  refresh-token:
    expiration: 604800000  # 7 дней
```

## API Endpoints

### 1. Регистрация пользователя

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john.doe@example.com",
  "password": "password123"
}
```

**Ответ:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "email": "john.doe@example.com"
}
```

### 2. Аутентификация

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "password123"
}
```

**Ответ:** Аналогично регистрации

### 3. Обновление токена

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Ответ:** Новые access и refresh токены

### 4. Выход из системы

```http
DELETE /api/auth/logout
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Ответ:**
```json
{
  "username": "john_doe",
  "message": "Successfully logged out",
  "details": "All active sessions have been terminated"
}
```

## Использование токенов

### Access Token
- Используется для аутентификации API запросов
- Время жизни: 15 минут
- Передается в заголовке: `Authorization: Bearer <token>`

### Refresh Token
- Используется для получения нового access токена
- Время жизни: 7 дней
- Хранится в базе данных
- Передается в теле запроса при обновлении

## Безопасность

1. **Access токены** имеют короткое время жизни (15 минут)
2. **Refresh токены** хранятся в базе данных и могут быть отозваны
3. **Автоматическая очистка** истекших токенов каждый день в 2:00
4. **Валидация токенов** на уровне Spring Security
5. **Разделение типов токенов** через claims

## Запуск

1. Убедитесь, что PostgreSQL запущен
2. Создайте базу данных `authdb`
3. Запустите приложение:

```bash
./mvnw spring-boot:run
```

4. Откройте Swagger UI: http://localhost:8080/swagger-ui.html

## Структура базы данных

### Таблица `users`
- `id` - первичный ключ
- `username` - уникальное имя пользователя
- `email` - уникальный email
- `password` - зашифрованный пароль
- `role` - роль пользователя (USER/ADMIN)

### Таблица `refresh_tokens`
- `id` - первичный ключ
- `token` - уникальный refresh токен
- `expiry_date` - дата истечения
- `user_id` - внешний ключ на пользователя

## Мониторинг

- Логирование всех операций с токенами
- Автоматическая очистка истекших токенов
- Обработка ошибок с детальными сообщениями 