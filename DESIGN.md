# Design — ProxyKillSwitcher

Android-утилита: весь IPv4-трафик устройства либо идёт в выбранный HTTP CONNECT прокси, либо напрямую. Это не клиент с каталогом серверов, не Clash/Xray и не VPN-маркетплейс.

Стартовый экран приложения — Kill Switch Home. Системный диалог VPN и шторка Quick Settings — часть продукта, не отдельные «фичевые» экраны навигации.

## Принципы

1. **Тумблер — герой.** Он отражает фактическое состояние VPN: пока туннель не поднят, выключен.
2. **Короткий стек экранов.** Home → Saved proxies → Proxy editor, назад — Back. Нет нижней навигации, табов, drawer, аккаунта и онбординга.
3. **Сначала проверить, потом сохранить.** Save в редакторе недоступен, пока черновик не прошёл Check на текущих host / port / protocol. Выбор строки в списке тоже проходит Check.
4. **Выключено = прямой интернет.** Нет промежуточного «VPN поднят, но blackhole».
5. **Спокойная панель, не маркетинг.** Без глобусов, замков, щитов и градиентных «connected»-героев.
6. **Поднятый VPN замораживает каталог.** Пока туннель жив, список и редактор только для чтения — сначала выключить тумблер.

## Платформа

- Android phone, Material 3, Jetpack Compose, edge-to-edge.
- Светлая и тёмная темы следуют системе (`AppTheme`).
- Отступы от системных inset: `WindowInsets.safeDrawing`.
- Токены живут в `:core:designsystem`. Экраны фич токены не дублируют.

## Цвет

| Роль | Light | Dark |
|---|---|---|
| Primary | `#3D5AFE` | `#8C9EFF` |
| On primary | `#FFFFFF` | `#00174E` |
| Background | `#FFFFFF` | `#121212` |
| On background | `#1A1A1A` | `#E6E6E6` |
| Error | `#D32F2F` | `#EF9A9A` |

Успех проверки прокси — не error. Использовать primary / on-background, не зелёный вне палитры, пока в теме нет отдельного success-токена.

## Типографика

`AppTypography` (sans, без кастомного шрифта):

| Стиль | Размер | Где |
|---|---|---|
| `titleLarge` | 22sp | Заголовок экрана |
| `bodyLarge` | 16sp | Статус тумблера, поля, кнопки |
| `bodyMedium` | 14sp | Хелпер, статус проверки, snackbar |

## Сетка и ритм

- Экран: колонка на всю ширину, padding **24dp**.
- Вертикальный gap между блоками: **20dp**.
- Строка списка: 64dp, radio — адрес — delete, разделитель hairline.
- Кнопки Check / Save: ряд, gap **12dp**, равный вес визуально, не растягивать на всю высоту экрана.
- Поля: `fillMaxWidth`, single line.
- Не вкладывать карточку в карточку. Фон экрана = `background`.

## Информационная архитектура

```
KillSwitchHome          стартовый экран
  ├ Switch              вкл / выкл маршрутизации
  ├ Active proxy        выбранный endpoint, только чтение
  ├ Manage proxies      → SavedProxyList
  └ Helper              только если тумблер заблокирован
SavedProxyList          каталог троек host|port|protocol
  ├ Radio               выбор active через Check
  ├ Delete              удаление строки
  └ FAB +               → SavedProxyEditor (пустой)
SavedProxyEditor        новая или существующая тройка
  ├ Host / port         черновик
  └ Check / Save        проверка, запись в каталог, active

Quick Settings tile     Proxy VPN (три состояния)
Foreground notification пока VPN поднят
System VPN consent      Android VpnService.prepare, не наш UI
```

## Home — структура сверху вниз

1. Заголовок **Proxy kill switch** (`titleLarge`), под ним hairline.
2. Блок состояния (`surface-block`, radius 8dp, padding 20dp): строка статуса — слева лейбл, справа `Switch`; ниже подпись **Active proxy**, адрес `host:port` моноширинным и тег протокола **HTTP**. Пока VPN поднят, блок заливается `primary`.
3. Строка **Manage proxies** с шевроном на всю ширину → Saved proxies.
4. Хелпер блокировки тумблера — только если `isToggleEnabled == false`.

Адрес и протокол — два текста, а не одна строка `http://host:port`.

Тумблер не превращать в гигантскую круглую power-кнопку по центру.

## Состояния Home

Switch enabled, когда активный (выбранный в списке) endpoint валиден: host не пустой, port в `1..65535`.

| Состояние | Блок | Лейбл | Справа | Active proxy | Хелпер |
|---|---|---|---|---|---|
| Прямой интернет | `surface-block` | Direct connection | Switch off | `host:port` + HTTP | нет |
| VPN поднят | `primary` | Proxy enabled | Switch on | `host:port` + HTTP | нет |
| Подключение | `surface-block` | Connecting… | progress 16dp вместо Switch | `host:port` + HTTP | нет |
| Нет выбранного прокси | `surface-block` | Direct connection | Switch off, disabled | No proxy selected | Save a valid endpoint before connecting. |

Туннель не поднялся — Switch возвращается в off, Toast **Could not connect through the proxy** (long).
**Manage proxies** доступна всегда; при поднятом VPN список открывается замороженным.

Отказ в системном VPN-диалоге возвращает тумблер в off. Отдельный error-экран не нужен.

Протокол только HTTP: выбора нет, чипов нет; в списке и на Home это тег **HTTP**, в редакторе — read-only строка **Protocol · HTTP**. Для HTTP в продукте UDP через прокси не туннелируется — это не рисуется отдельным баннером на Home, пока нет отдельного copy в коде.

## Saved proxies

1. Верхняя строка: `IconButton` Back + **Saved proxies** (`titleLarge`), под ней hairline.
2. Плашка **Turn off the VPN to change saved proxies.** — только пока VPN поднят.
3. Строки `Proxy Row` 64dp: `RadioButton` (active) · адрес `host:port` моноширинным + тег **HTTP** · progress 16dp при выборе · `IconButton` Delete. Тап по строке открывает редактор этой тройки, тап по radio — выбор.
4. FAB **+** справа снизу — пустой редактор.

| Состояние | Radio | Delete | FAB | Дополнительно |
|---|---|---|---|---|
| Список | active отмечен | enabled | есть | — |
| Выбор строки | у выбираемой disabled, рядом progress | enabled | есть | active не меняется до успешного Check |
| Выбор не прошёл | active прежний | enabled | есть | Toast **Proxy check failed** |
| Пусто | — | — | есть | текст **No saved proxies** |
| VPN поднят | disabled | disabled | нет | плашка, строки не кликабельны |

Удаление active-строки очищает active endpoint: Home переходит в **No proxy selected**.

## Proxy editor

1. Верхняя строка: `IconButton` Back + **Proxy editor**, hairline.
2. Плашка **Turn off the VPN to edit proxies.** — только пока VPN поднят.
3. `OutlinedTextField` **Proxy host**, `OutlinedTextField` **Proxy port** (numeric).
4. Read-only строка: слева **Protocol**, справа **HTTP** моноширинным.
5. Ряд **Check proxy** (outlined) / **Save proxy** (filled), gap 12dp, равная ширина.
6. Статус проверки — только если не пустой.

| Состояние | Поля | Check | Save | Статус |
|---|---|---|---|---|
| Новый, пустые поля (FAB) | пустые, лейбл внутри | disabled | disabled | пусто |
| Правка, черновик валиден | заполнены | enabled | disabled | пусто |
| Checking | как было | progress 16dp, disabled | disabled | Checking proxy… |
| Check OK для текущей тройки | как было | enabled | enabled | Proxy check succeeded (`primary`) |
| Check failed | как было | enabled | disabled | Proxy check failed (`error`) |
| VPN поднят | disabled | disabled | disabled | — |

Любая правка host / port сбрасывает результат проверки. Успешный Save закрывает редактор и возвращает в список; новая тройка становится active. Save без успешного Check — Toast **Check the proxy before saving** (страховка: кнопка в этом случае и так disabled).

## Системные поверхности

### Quick Settings — `Proxy VPN`

Иконка: вектор `ic_kill_switch_tile` (ключ / VPN-path), не маркетинговый щит.

| Состояние плитки | Когда |
|---|---|
| Inactive | VPN выключен, активный endpoint валиден |
| Active | VPN включён |
| Unavailable (серая) | пустой/невалидный активный endpoint (нет выбранного прокси) |

Тап по unavailable ничего не делает. Первый старт с плитки может показать системный VPN-consent и свернуть шторку.

### Foreground notification

Пока сервис жив:

- Channel name: **Proxy kill switch**
- Title: **Proxy enabled**
- Text: **Device traffic is routed through the saved proxy**

Отдельный rich-style баннер в Compose не дублировать: это системное уведомление.

## Компоненты Material 3

Разрешены: `Text`, `Switch`, `OutlinedTextField`, `Button`, `IconButton` (Back, Delete), `RadioButton`, `FloatingActionButton`, `CircularProgressIndicator`, Toast.

Не использовать на этих экранах: NavigationBar, NavigationRail, TopAppBar с actions-меню, ModalBottomSheet для настроек прокси, `FilterChip` протокола, `Card` ради декора.

Индикатор проверки — 16dp, stroke 2dp, внутри кнопки Check, не полноэкранный overlay.

## Copy (канон, EN)

Строки UI сейчас зашиты в Compose / сервис. Не перефразировать в макетах.

- Proxy kill switch
- Proxy enabled
- Direct connection
- Connecting…
- Active proxy
- No proxy selected
- Manage proxies
- Save a valid endpoint before connecting.
- Could not connect through the proxy
- Saved proxies
- No saved proxies
- Turn off the VPN to change saved proxies.
- Proxy editor
- Turn off the VPN to edit proxies.
- Proxy host
- Proxy port
- Protocol
- HTTP
- Check proxy
- Save proxy
- Checking proxy…
- Proxy check succeeded
- Proxy check failed
- Check the proxy before saving
- Proxy VPN *(плитка)*
- Device traffic is routed through the saved proxy *(текст уведомления)*

Имя приложения: **ProxyKillSwitcher**.

## Пример заполнения

Макеты и Preview: host `192.168.1.10`, port `1080`, HTTP, тумблер off, Check enabled, Save disabled.

Черновик после правки для состояния «можно Save»: host `10.0.0.5`, port `1080`, статус **Proxy check succeeded**.

## Адаптив и a11y

- Только phone portrait как целевой макет. Tablet / landscape не проектировать отдельно в v1.
- `Switch` и поля должны быть доступны TalkBack с лейблами из copy выше.
- Back, Delete и FAB — icon-only: обязательный contentDescription **Back**, **Delete**, **Add proxy**. Radio в строке озвучивает адрес.
- Контраст текста на background соответствует токенам light/dark.
- Не прятать статус проверки только в цвет: всегда есть текст.

## Вне скоупа v1

Per-app split, логин прокси, always-on VPN UI, IPv6 dual-stack, список серверов, логи, вложенные Settings, аккаунт, карусель онбординга, glassmorphism, mesh-градиенты.

## Реализация

| Что | Где |
|---|---|
| Тема, цвет, type | `core/designsystem/.../theme/` |
| Home Compose | `feature/killswitch/.../presentation/ui/KillSwitchUiScreen.kt` |
| Состояния Home | `KillSwitchUiStateMapper` |
| Список | `SavedProxyListUiScreen.kt`, `SavedProxyListUiStateMapper` |
| Редактор, состояния Check / Save | `SavedProxyEditorUiScreen.kt`, `SavedProxyEditorUiStateMapper` |
| Плитка | `KillSwitchTileService`, `ic_kill_switch_tile` |
| Уведомление | `ProxyVpnService` |

Compose: `UiScreen` собирает VM и side effects, `Content` только рисует `UiState` и шлёт `Intent`. Превью каждого Content — `@Preview` + `AppTheme` + PreviewData.

Макеты (в т.ч. Google Stitch) обязаны совпадать с этой IA и copy. Визуальную иерархию можно уплотнить, набор компонентов — нет.

Макет: `proxy killswitch.pen` — ряды Home, Saved proxies, Proxy editor (+ доска состояний кнопок), системные поверхности (иконка, плитка), Dark. Компоненты: Text Field, Button, Switch, Proxy Row, QS Tile.
