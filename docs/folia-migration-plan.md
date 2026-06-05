# План полного перехода CustomDaytime на Folia API 1.21.11

> В документе используется корректное название платформы **Folia** (в запросе указано “folio”). Целевая зависимость: `compileOnly("dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT")`.

## Проверенные источники API

- Folia Javadocs 1.21.11: https://jd.papermc.io/folia/1.21.11/
- `GlobalRegionScheduler`: https://jd.papermc.io/folia/1.21.11/io/papermc/paper/threadedregions/scheduler/GlobalRegionScheduler.html
- `RegionScheduler`: https://jd.papermc.io/folia/1.21.11/io/papermc/paper/threadedregions/scheduler/RegionScheduler.html
- Поддержка Paper/Folia и выбор scheduler: https://docs.papermc.io/paper/dev/folia-support
- Обзор региональной модели Folia: https://docs.papermc.io/folia/reference/overview/

## Краткий анализ текущего состояния

- Проект разделен на `api`, `common`, `folia`, `sponge`, что удобно для модульной миграции, а целевой Folia-слой выделен в модуль `folia`.
- В Folia-слое используются Folia scheduler API (`Bukkit.getGlobalRegionScheduler()` и `server.getAsyncScheduler()`), и модуль должен компилироваться против `dev.folia:folia-api`.
- Ранее `PlatformScheduler` был описан как “main thread scheduler”, что концептуально неверно для Folia: миры тикают по регионам, а глобальный регион обслуживает время мира, погоду, gamerules, sleep skip и консольные команды.
- Логика CustomDaytime в основном управляет временем мира (`getFullTime`, `setFullTime`, gamerules, sleep skip), поэтому основным безопасным контекстом для периодического контроллера должен быть `GlobalRegionScheduler`.
- Текущие адаптеры `FoliaWorld` напрямую читают/меняют `World`: это приемлемо только если вызов идет из корректного Folia-контекста. Для полного перехода нужно сделать контекст выполнения явным в API/сервисах.
- `WorldCache`, `WorldTimeManager` и `EventBus` используют обычные `HashMap`/списки без явной защиты от параллельного доступа. На Folia события игроков/миров могут приходить из разных регионов, поэтому общие структуры должны обновляться через единый безопасный контекст или потокобезопасные коллекции.
- `WorldActivityListener` сейчас вычисляет `totalPlayers` до отложенного scheduler-вызова, а `sleepingPlayerCount` — внутри него. Для Folia нужно унифицировать это правило и считать состояние мира только в запланированном корректном контексте.
- В `FoliaPlatform.world(WorldKey)` нет null-safe обработки отсутствующего мира: `new FoliaWorld(null)` приведет к NPE при первом обращении к `world.key()`/`world.getFullTime()`.
- `WorldTimeManager.stop(WorldKey)` останавливает контроллер, но не удаляет его из `controllers`, из-за чего повторный `start` после unload/load может не создать новый контроллер.
- Модуль `sponge` блокирует сборку Folia-слоя без `-PskipSponge=true`, потому что Gradle конфигурирует все проекты и SpongeVanilla падает на чтении version manifest. Для итераций по Folia нужен отдельный путь сборки/отключение Sponge.

## План задач

Каждый пункт ниже рассчитан как отдельная задача на одно следующее сообщение пользователя. Не объединять пункты без явного запроса, чтобы изменения были маленькими, проверяемыми и не ломали существующий функционал.

### 1. Перевести build-конфигурацию платформенного слоя на Folia API

**Цель:** модуль платформы должен компилироваться против `dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT`, а не маскировать ошибки через обычный Paper API.

**Что сделать:**

- В `gradle/libs.versions.toml` добавить версию и library alias для `dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT`.
- В `folia/build.gradle.kts` использовать `compileOnly(libs.folia.api)` вместо Paper API.
- Проверить, что репозиторий PaperMC Maven остается в `settings.gradle.kts`, так как Folia API публикуется через PaperMC Maven.
- Проверить `paper { foliaSupported = true }` и актуальность `apiVersion`/`gameVersions` для 1.21.11.

**Готовность:** `./gradlew :folia:compileJava` должен проходить или падать только из-за уже известной проблемы конфигурации `:sponge`, которую нужно зафиксировать отдельно.

### 2. Разблокировать изолированную сборку Folia-модуля без Sponge

**Цель:** получить быстрый и воспроизводимый CI/dev путь для Folia, не зависящий от SpongeVanilla.

**Что сделать:**

- Добавить Gradle-механизм отключения Sponge-проекта, например через property `-PskipSponge=true` в `settings.gradle.kts` или отдельный included build/profile.
- Убедиться, что `:api`, `:common`, `:folia` собираются без конфигурации `:sponge`.
- Не удалять Sponge в этой задаче, если нет отдельного решения по отказу от мультиплатформенности.
- Документировать команду проверки в README или в отдельной dev-документации.

**Готовность:** команда вида `./gradlew -PskipSponge=true :folia:build` проходит конфигурацию и компиляцию Folia-слоя.

### 3. Оформить платформенный слой как Folia-primary

**Цель:** убрать архитектурную неоднозначность: runtime target — Folia, а не обычный Paper runtime.

**Что сделать:**

- Выбрать стратегию:
  - модуль: `folia`;
  - jar: `CustomDaytimeFolia`;
  - main class: `CustomDaytimeFolia`;
  - platform classes: `FoliaPlatform`, `FoliaScheduler`, `FoliaWorld`, `FoliaTask`.
- Обновить `paper-plugin.yml` generation (`main`, archive name, Modrinth loaders/game versions).
- Сохранить обратную совместимость артефактов только если она нужна отдельно.

**Готовность:** plugin descriptor указывает на новый main class, shadowJar собирает корректный Folia jar, старые имена не остаются в публичном Folia-слое.

### 4. Переделать `PlatformScheduler` под модель Folia-контекстов

**Цель:** API должен выражать не “main thread”, а конкретные Folia-контексты: global, region, entity, async.

**Что сделать:**

- Заменить/расширить `PlatformScheduler` методами для:
  - global now/later/repeating — для world time, gamerules, weather, sleep skip, console commands;
  - async now/later — для HTTP/update check, файловых операций, тяжелых вычислений;
  - region by world/chunk/location — для будущих операций с блоками/чанками;
  - entity/player context — для будущих операций с игроками/сущностями.
- Вынести повторяющееся преобразование Folia `ScheduledTask` → `PlatformTask` в единый адаптер.
- Обновить комментарии API: не использовать термин “main thread” для Folia.
- Оставить старые методы временно deprecated только если требуется мягкая миграция common-кода.

**Готовность:** все текущие вызовы scheduler явно выбирают global или async; common-код больше не зависит от идеи единого main thread.

### 5. Сделать операции `PlatformWorld` контекстно-безопасными для Folia

**Цель:** исключить прямые чтения/записи `World` из произвольных потоков/регионов.

**Что сделать:**

- Разделить `PlatformWorld` на:
  - immutable identity (`WorldKey`, display/key string);
  - операции состояния мира (`time`, `setTime`, gamerules, player/sleeping counts), выполняемые через scheduler-контекст.
- Для операций времени использовать global scheduler, потому что глобальный регион Folia отвечает за day time/game time/weather/sleep skip.
- Сделать null-safe получение мира: `Platform.world(WorldKey)` должен возвращать `Optional<PlatformWorld>` или явно `null`-обработанный результат без создания адаптера над `null`.
- Решить, где хранятся snapshots счетчиков игроков/сна, чтобы не перебирать `world.getPlayers()` вне правильного контекста.

**Готовность:** `WorldTimeController` не вызывает `world.getFullTime()`/`world.setFullTime()` напрямую из неизвестного контекста; отсутствующий мир не приводит к NPE.

### 6. Перенести `WorldTimeController` на явный global-region цикл

**Цель:** вся логика изменения времени мира должна жить в одном Folia-safe контексте.

**Что сделать:**

- Запускать tick-контроллер через global repeating task.
- Внутри tick читать `doDaylightCycle`, `playersSleepingPercentage`, `fullTime` и вызывать `setFullTime` только в global контексте.
- Обновления `totalPlayers`/`sleepingPlayers` принимать как snapshots или события, доставленные в тот же global-контекст.
- Исправить жизненный цикл `WorldTimeManager.stop`: после `stop()` удалить контроллер из `controllers`.
- Проверить reload/unload/load сценарии, чтобы контроллер не оставался остановленным в map.

**Готовность:** один мир имеет максимум один активный repeating task, unload отменяет task и очищает manager, повторный load стартует новый controller.

### 7. Сделать EventBus, WorldCache и менеджеры безопасными при региональной многопоточности

**Цель:** исключить гонки между listener-событиями разных регионов и общими mutable-структурами common-слоя.

**Что сделать:**

- Выбрать модель:
  - все события common-слоя сериализуются через global scheduler; или
  - общие структуры переводятся на `ConcurrentHashMap`/thread-safe listeners, а операции мира дополнительно планируются в нужный Folia-контекст.
- Для текущего плагина предпочтителен первый вариант: простая единая очередь global-событий, потому что логика работает с временем мира.
- Обновить `WorldCache`, `WorldTimeManager`, `EventBus` согласно выбранной модели.
- Добавить тесты на повторную регистрацию/удаление мира и idempotent stop/start.

**Готовность:** нет прямых записей в `HashMap` из неизвестного регионального потока; события мира/игроков не создают race conditions.

### 8. Унифицировать Folia listener-адаптеры и snapshots событий

**Цель:** Bukkit/Folia listeners должны только собирать минимальный context и передавать работу в common без опасных вычислений.

**Что сделать:**

- Вынести повторяющийся код `new FoliaWorld(event.getWorld())`/fire common events в отдельный Folia event adapter/service.
- Для `PlayerJoinEvent`, `PlayerQuitEvent`, `PlayerChangedWorldEvent`, `PlayerBedEnterEvent`, `PlayerBedLeaveEvent` не читать итоговые счетчики до корректного scheduler-вызова.
- Учитывать, что `PlayerQuitEvent` может отражать состояние до фактического удаления игрока из мира; считать итоговый snapshot на следующем global tick или использовать явную delta-модель.
- Проверить отмену `TimeSkipEvent`/`ClockTimeSkipEvent.SkipReason.NIGHT_SKIP` в правильном событии и оставить только один источник логики отмены skip.

**Готовность:** listeners тонкие, DRY, не содержат бизнес-логики подсчета/ускорения времени.

### 9. Добавить тестовое покрытие common-логики без Minecraft runtime

**Цель:** безопасно менять Folia-адаптеры, не ломая расчеты времени.

**Что сделать:**

- Добавить unit-тесты для `WorldTimeController` через fake `PlatformWorld`/`PlatformScheduler`.
- Проверить сценарии:
  - day/night increment;
  - acceleration off/on;
  - external time change resync;
  - zero players;
  - sleep percentage threshold;
  - unload/stop/reload lifecycle.
- Вынести чистую математику времени в отдельный service/calculator, чтобы уменьшить необходимость мокать платформу.

**Готовность:** common-тесты проходят без запуска сервера, а изменения Folia-слоя не ломают расчеты.

### 10. Провести runtime-проверку на Folia 1.21.11

**Цель:** подтвердить не только компиляцию, но и корректное поведение на реальном Folia-сервере.

**Что сделать:**

- Подготовить run-конфигурацию под Folia 1.21.11, если используемый Gradle plugin это поддерживает; если нет — описать ручной smoke-test.
- Проверить запуск сервера без thread-check ошибок Folia.
- Проверить управление временем в `minecraft:overworld` из дефолтного конфига.
- Проверить сон нескольких игроков/ботов или ручные события при `playersSleepingPercentage`.
- Проверить unload/load мира, reload plugin/server restart.

**Готовность:** smoke-test checklist заполнен, thread violation/region access ошибок в логах нет.

### 11. Обновить документацию, README и release metadata

**Цель:** пользователи должны понимать, что целевая платформа — Folia 1.21.11, а не обычный Paper.

**Что сделать:**

- Обновить README: требования, supported loaders, Java version, Folia-specific notes.
- Обновить Modrinth metadata: loaders, gameVersions, changelog.
- Документировать, что обычный Paper не является целевой runtime-платформой, если отдельно не вводится compatibility-layer.
- Описать конфиг и ограничения: изменение времени работает по миру, не по отдельным регионам.

**Готовность:** README/metadata согласованы с build.gradle и фактическим jar.

### 12. Финальная чистка архитектуры и DRY

**Цель:** завершить переход без дублирования и с расширяемым ядром.

**Что сделать:**

- Удалить устаревшие Paper-названия, deprecated методы scheduler и временные адаптеры.
- Проверить разделение модулей: `commands`, `services`, `listeners`, `api`, `utils` — если команд пока нет, не создавать пустые пакеты.
- Свести платформенные классы к тонким адаптерам, а бизнес-логику оставить в `common`.
- Добавить архитектурные комментарии только там, где они предотвращают неправильное использование Folia API.

**Готовность:** код компилируется на Folia API, документация актуальна, нет дублирования platform-логики, common не зависит от Bukkit/Folia классов.

## Рекомендуемый порядок выполнения

1. Сначала выполнить пункты 1–2, чтобы получить надежную сборку против Folia API.
2. Затем выполнить пункты 4–7, потому что они устраняют основные риски Folia thread model.
3. После этого выполнить пункты 8–10 для runtime-валидации.
4. В конце выполнить пункты 3, 11–12, если нужно полностью переименовать артефакты и завершить cleanup.
