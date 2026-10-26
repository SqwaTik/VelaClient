# Сборка Vela Client 26.2

Требуется JDK 25. Версии Minecraft, Fabric Loader, Fabric API и Loom закреплены в `gradle.properties`.
Gradle Wrapper находится в архиве. Команда проверки и сборки: `gradlew.bat test build` (Windows) или `./gradlew test build`.

В `libs/` положите неизменённые релизные библиотеки `bobby-5.2.15+mc26.2.jar` и `IAS-9.0.7+26.2-fabric.jar`.
Bobby: https://modrinth.com/mod/bobby/version/KLiFVFju . IAS: https://github.com/The-Fireplace-Minecraft-Mods/In-Game-Account-Switcher .
IAS требуется и при запуске игры. Sodium и Iris — только необязательные интеграции, их compileOnly-зависимости Gradle скачивает автоматически.

Результаты: `build/libs/vela-client-0.1.12.jar` и `build/libs/vela-client-0.1.12-sources.jar`.
`gradlew.bat sourceBundle` создаёт архив исходного проекта без локальных аккаунтов, конфигураций игры, логов и рабочих каталогов.

Встроенный ZakoOpt 1.0.3 расположен в `src/zako/java`. Автор Zako, https://t.me/StarikZako .
Этот код и его изменения распространяются под LGPL-3.0-only; тексты лицензий и описание изменений находятся в `src/main/resources/META-INF/licenses/zakoopt`.
Для изменения/пересборки интеграции используются те же исходники и команды. Замените клиентский JAR в своей установке результатом сборки, сохранив предыдущую копию.

Измерение производительности: одинаковые разрешение, точка мира, число сущностей и настройки; 15 секунд стоять, затем 60 секунд двигаться по одному маршруту. `/vela perf start profile` запускает локальный профиль, `/vela perf stop` завершает его. Не переключайте F11/Alt+Tab во время сравнения. Оценивайте среднее время кадра, p95/p99 и максимальные задержки; пиковый FPS не является гарантией плавности.
