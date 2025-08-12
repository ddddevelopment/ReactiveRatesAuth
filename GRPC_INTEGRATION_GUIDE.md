# Интеграция gRPC сервиса пользователей с системой аутентификации

## Обзор

Этот документ описывает интеграцию внешнего gRPC сервиса пользователей с существующей системой аутентификации Spring Security. Система теперь использует внешний сервис пользователей для всех операций с пользователями, сохраняя при этом существующие механизмы аутентификации и авторизации.

## Архитектура интеграции

### Компоненты

1. **GrpcUser** - DTO класс, реализующий `UserDetails` для работы с Spring Security
2. **UserGrpcService** - Сервис для взаимодействия с gRPC сервисом пользователей
3. **CustomUserDetailsService** - Обновлен для использования gRPC сервиса
4. **AuthService** - Обновлен для работы с внешними пользователями
5. **RefreshTokenService** - Расширен для поддержки GrpcUser

### Поток аутентификации

```
1. Клиент отправляет запрос на аутентификацию
2. CustomUserDetailsService загружает пользователя через gRPC
3. Spring Security проверяет учетные данные
4. AuthService генерирует JWT токены
5. RefreshToken сохраняется в локальной БД
```

## Ключевые изменения

### 1. GrpcUser - Адаптер для Spring Security

```java
public class GrpcUser implements UserDetails {
    // Реализует все методы UserDetails
    // Адаптирует данные из gRPC UserResponse
    // Поддерживает роли и права доступа
}
```

### 2. UserGrpcService - Основной сервис интеграции

```java
@Service
public class UserGrpcService {
    // Создание пользователей через gRPC
    // Загрузка пользователей для Spring Security
    // Аутентификация пользователей
    // Получение данных пользователей
}
```

### 3. CustomUserDetailsService - Обновлен для gRPC

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    // Использует UserGrpcService для загрузки пользователей
    // Обрабатывает ошибки gRPC
    // Логирует операции
}
```

### 4. AuthService - Интеграция с gRPC

```java
@Service
public class AuthService {
    // Регистрация через gRPC сервис
    // Аутентификация с использованием gRPC
    // Обновление токенов с актуальными данными
    // Выход из системы
}
```

## Операции с пользователями

### Регистрация

```java
// 1. Проверка существования пользователя через gRPC
userGrpcService.getUserByUsername(username);

// 2. Создание пользователя через gRPC
userGrpcService.createUser(username, email, password, firstName, lastName, phoneNumber);

// 3. Получение созданного пользователя
GrpcUser grpcUser = userGrpcService.getGrpcUserByUsername(username);

// 4. Генерация токенов
var accessToken = jwtService.generateAccessToken(grpcUser);
var refreshToken = refreshTokenService.createRefreshToken(grpcUser);
```

### Аутентификация

```java
// 1. Spring Security аутентификация (использует CustomUserDetailsService)
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(username, password)
);

// 2. Получение пользователя через gRPC
GrpcUser grpcUser = userGrpcService.getGrpcUserByUsername(username);

// 3. Генерация токенов
var accessToken = jwtService.generateAccessToken(grpcUser);
var refreshToken = refreshTokenService.createRefreshToken(grpcUser);
```

### Обновление токенов

```java
// 1. Валидация refresh token
// 2. Получение актуальных данных пользователя через gRPC
GrpcUser grpcUser = userGrpcService.getGrpcUserByUsername(username);

// 3. Генерация новых токенов
var accessToken = jwtService.generateAccessToken(grpcUser);
var refreshToken = refreshTokenService.createRefreshToken(grpcUser);
```

## Обработка ошибок

### gRPC ошибки

```java
try {
    UserResponse response = stub.getUserByUsername(request);
    return response;
} catch (Exception e) {
    log.error("Error getting user by username via gRPC: {}", e.getMessage(), e);
    throw new RuntimeException("Failed to get user via gRPC", e);
}
```

### Spring Security ошибки

```java
try {
    UserDetails userDetails = userGrpcService.loadUserByUsername(username);
    return userDetails;
} catch (UsernameNotFoundException e) {
    log.warn("User not found: {}", username);
    throw e;
} catch (Exception e) {
    log.error("Error loading user details for username: {}", username, e);
    throw new UsernameNotFoundException("Error loading user: " + username, e);
}
```

## Конфигурация

### application.yml

```yaml
spring:
  grpc:
    client:
      channels:
        users-service:
          address: localhost:9090
          negotiation-type: plaintext
```

### GrpcClientConfiguration

```java
@Configuration
public class GrpcClientConfiguration {
    @Bean
    public UsersServiceGrpc.UsersServiceBlockingStub usersServiceStub(GrpcChannelFactory channelFactory) {
        return UsersServiceGrpc.newBlockingStub(channelFactory.createChannel("users-service"));
    }
}
```

## Безопасность

### Пароли

- Пароли передаются в gRPC сервис в зашифрованном виде
- Локально создается временный зашифрованный пароль для совместимости
- Рекомендуется добавить метод проверки пароля в gRPC сервис

### Токены

- JWT токены генерируются локально
- Refresh токены сохраняются в локальной БД
- Данные пользователей обновляются при каждом запросе

## Мониторинг и логирование

### Логирование gRPC операций

```java
log.info("Creating user via gRPC: {}", username);
log.info("User created successfully: {}", response.getUsername());
log.error("Error creating user via gRPC: {}", e.getMessage(), e);
```

### Метрики

Рекомендуется добавить:
- Количество успешных/неуспешных gRPC вызовов
- Время ответа gRPC сервиса
- Количество ошибок аутентификации

## Производительность

### Кэширование

Для улучшения производительности рекомендуется:
- Кэшировать данные пользователей
- Использовать connection pooling для gRPC
- Добавить retry механизмы

### Оптимизации

```java
// Пример кэширования пользователей
@Cacheable("users")
public GrpcUser getGrpcUserByUsername(String username) {
    // Получение пользователя через gRPC
}
```

## Тестирование

### Unit тесты

```java
@Test
public void testUserAuthentication() {
    // Mock gRPC сервис
    when(stub.getUserByUsername(any())).thenReturn(userResponse);
    
    // Тест аутентификации
    AuthResponse response = authService.login(loginRequest);
    
    // Проверка результата
    assertNotNull(response.getAccessToken());
}
```

### Интеграционные тесты

```java
@Test
public void testGrpcIntegration() {
    // Запуск gRPC сервера
    // Тест полного цикла аутентификации
}
```

## Миграция

### Поэтапная миграция

1. **Этап 1**: Добавление gRPC клиента
2. **Этап 2**: Обновление CustomUserDetailsService
3. **Этап 3**: Обновление AuthService
4. **Этап 4**: Тестирование и отладка
5. **Этап 5**: Удаление локальных пользователей

### Rollback план

- Сохранение локальных пользователей как fallback
- Feature toggle для переключения между режимами
- Мониторинг ошибок gRPC

## Заключение

Интеграция gRPC сервиса пользователей успешно реализована с сохранением всех существующих механизмов аутентификации. Система теперь использует внешний сервис для управления пользователями, обеспечивая централизованное управление данными пользователей.
