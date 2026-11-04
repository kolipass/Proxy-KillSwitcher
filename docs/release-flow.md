# Release Build & Delivery Flow

Документация полного цикла сборки, оптимизации, подписания и публикации релизных артефактов ProxyKillSwitcher.

---

## 1. Обзор архитектуры релизной сборки

Релизный пайплайн приложения спроектирован для производства двух типов артефактов:
- **APK (`.apk`)**: автономный установочный пакет для прямой дистрибуции (GitHub Releases, локальное тестирование).
- **AAB (`.aab`)**: Android App Bundle для публикации в Google Play Store со сплитами по ABI и плотности экранов.

### Ключевые компоненты:
1. **R8 / Minification & Resource Shrinking**:
   - `isMinifyEnabled = true` — сжатие байткода, удаление неиспользуемых классов/методов, обфускация.
   - `isShrinkResources = true` — удаление неиспользуемых ресурсов (drawable, layouts, xml).
   - `app/proguard-rules.pro` — правила для Metro DI, Kotlinx Serialization, Coroutines, Orbit MVI, Jetpack Navigation.
   - `core/tunnel/consumer-rules.pro` — защитные правила для JNI hev-socks5-tunnel (удержание JNI методов и callback `protectSocket(int)`).
2. **Гибкое подписание (Signing Configurations)**:
   - Поддержка продукционного keystore через переменные окружения или свойства Gradle.
   - Автоматический fallback на debug keystore при отсутствии релизных ключей (для локальной проверки разработчиками и CI pull-request валидации).
3. **Версионирование (Version Management)**:
   - `versionCode`: настраивается через `VERSION_CODE` (в CI по умолчанию равен номеру запуска `$GITHUB_RUN_NUMBER`).
   - `versionName`: настраивается через `VERSION_NAME` или извлекается из git-тега (например, `v1.0.0` -> `1.0.0`).

---

## 2. Локальная сборка релиза

### Быстрый запуск через скрипт

В репозитории подготовлен готовый скрипт `scripts/build-release.sh`:

```bash
# Сборка с полным циклом проверок (ktlint, detekt, unit-тесты)
./scripts/build-release.sh --version-name 1.0.0 --version-code 1

# Сборка только APK (без тестов)
./scripts/build-release.sh --apk --skip-tests

# Сборка только App Bundle (AAB)
./scripts/build-release.sh --bundle
```

Собранные артефакты и контрольные суммы SHA-256 складываются в папку `build/release-dist/`:
- `proxykillswitcher-v<version>-release.apk`
- `proxykillswitcher-v<version>-release.aab`
- `mapping-v<version>.txt` (файл соответствия R8 для деобфускации краш-репортов)
- `SHA256SUMS.txt`

### Сборка через Gradle напрямую

```bash
# Сборка APK
VERSION_NAME="1.0.0" VERSION_CODE=1 ./gradlew :app:assembleRelease

# Сборка App Bundle
VERSION_NAME="1.0.0" VERSION_CODE=1 ./gradlew :app:bundleRelease
```

---

## 3. Настройка релизного подписания (Signing Keys)

### Генерация собственного ключа

Для генерации нового хранилища ключей используйте скрипт:

```bash
./scripts/generate-release-keystore.sh release.keystore proxykillswitcher
```

Скрипт создаст PKCS12 keystore с ключом RSA 4096-bit со сроком действия 10 000 дней.

### Переменные окружения для сборки с боевым ключом

При локальной сборке или на CI задайте:

| Переменная | Описание |
|---|---|
| `RELEASE_KEYSTORE_PATH` | Путь к файлу `.keystore` / `.jks` |
| `RELEASE_KEYSTORE_PASSWORD` | Пароль к хранилищу ключей |
| `RELEASE_KEY_ALIAS` | Алиас ключа |
| `RELEASE_KEY_PASSWORD` | Пароль к ключу |

---

## 4. CI/CD автоматизация (GitHub Actions)

В проекте настроен workflow `.github/workflows/release.yaml`:

### Триггеры:
1. **Push git-тега** вида `v*.*.*` (например, `git tag v1.0.0 && git push origin v1.0.0`).
2. **Ручной запуск (`workflow_dispatch`)** через GitHub Actions UI с возможностью указать `version_name`, `version_code`, отметки `draft` или `prerelease`.

### Этапы пайплайна:
1. **Validate**:
   - Проверка форматирования: `ktlintCheck`.
   - Статический анализ: `detekt`.
   - Релизные юнит-тесты: `testReleaseUnitTest`.
2. **Build & Release**:
   - Декодирование `RELEASE_KEYSTORE_BASE64` из GitHub Secrets (при наличии).
   - Компиляция, оптимизация R8 и упаковка: `:app:assembleRelease` и `:app:bundleRelease`.
   - Подготовка артефактов и генерация `SHA256SUMS.txt`.
   - Загрузка сборки в GitHub Actions Artifacts.
   - Публикация GitHub Release с автоматическим генератором release notes и прикрепленными `.apk`, `.aab`, `mapping.txt`, `SHA256SUMS.txt`.

### Необходимые GitHub Secrets:
- `RELEASE_KEYSTORE_BASE64` (base64-строка keystore-файла)
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

*(Примечание: если секреты не заданы, релиз подписывается debug-сертификатом в качестве fallback, сборка не падает).*

---

## 5. R8 & ProGuard безопасность JNI

Приложение использует нативные C-библиотеки (`hev-socks5-tunnel`) и JNI.
R8 настроен так, чтобы предотвратить удаление или переименование критических сущностей:
- `core/tunnel/consumer-rules.pro` автоматически транслируется во все зависимые модули и защищает:
  - JNI-функции класса `Socks5Tunnel` (`TProxyStartService`, `TProxyStopService` и др.).
  - Статический метод обратного вызова из C в Java: `Socks5Tunnel.protectSocket(int)`.
- `app/proguard-rules.pro` сохраняет метаданные и рефлексивные конструкции для:
  - Metro DI кодогенерации и инъекций.
  - Сериализаторов `@Serializable` (Kotlinx Serialization).
  - Orbit MVI контейнеров.
  - Навигационных аргументов Jetpack Navigation Compose.
