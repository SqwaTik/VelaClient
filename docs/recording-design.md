# Recording — implementation contract

Status: implemented, synthetic-video tests pass; live game/audio/performance validation outstanding.

## Использование

Utilities → Recorder: назначьте «Начать / остановить». Для откатов включите
«Откаты», выберите 30–600 секунд и назначьте «Сохранить откат». Загрузка конфига
не включает захват: необходимо нажатие клавиши в активном окне игры без меню.
Звук игры и микрофон выключаются независимо. Файлы находятся в recordings.

Встроенный Windows x64 компонент проверяется по SHA-256 и распаковывается
в .vela-runtime при первом использовании. Кодирование H.264: 60 FPS, 30 Мбит/с,
аппаратный кодировщик запрашивается у системы. Звук отдельного процесса требует
Windows build 20348+. При отсутствии микрофона его нужно отключить в настройках.

FFmpeg 9.0.2 сохраняет завершённые сегменты без повторного кодирования и без
перезапуска захвата. Продолжительность отката приблизительная, по границам
ключевых кадров. Последние незавершённые секунды могут не попасть в клип.
Сразу после включения буфер ещё не содержит полного выбранного интервала.
Десять минут при 30 Мбит/с требуют около 2.25 ГБ диска плюс запас. При остатке
менее 512 МБ запись останавливается. Неудачно завершённая сессия может оставить
сегменты в recordings/.buffer для восстановления.

Проверены обычная синтетическая запись и два последовательных отката,
полное декодирование файлов FFmpeg без ошибок. Нулевая нагрузка не гарантируется.
Лицензии компонентов включены. Перед публичным распространением нужна отдельная
проверка требований GPL к предоставлению исходников сборки FFmpeg.

## Behavior

- A user-assigned key starts/stops recording of the Minecraft window.
- Game audio and microphone have independent toggles. Game audio means only the Minecraft process, not desktop loopback.
- Replay buffer retains a selectable 30–600 seconds. A separate key exports the retained interval.
- Capture starts only after an explicit user action; restoring a config must not silently enable microphone capture.
- Settings/editor use the existing settings dialog styling and animation infrastructure.
- Show an unmistakable recording indicator and report capture/export failures. Never report a saved clip before finalization succeeds.

## Backend

ScreenRecorderLib 7 supports `WindowRecordingSource(HWND)`, `ProcessAudioSource(PID)`, `CaptureAudioSource.Default`, and hardware encoding. This can avoid synchronous Minecraft framebuffer readback and render-API hooks. Compatibility and actual overhead still require measurement.

Primary documentation: https://github.com/sskodje/ScreenRecorderLib/wiki/Quickstart-guide-v7.x.x-and-newer

The helper must validate that HWND belongs to the supplied game PID. Failure must not fall back to desktop capture or system-wide audio. Handle unavailable microphone permission and unsupported process-audio capture explicitly.

Replay design must keep continuous encoding, bounded disk/RAM, keyframe-aligned segments and safe export retention. Repeatedly restarting recording to create chunks is not acceptable because it loses frames/audio between chunks. Stop, crash, disk-full and game-exit paths must finalize or preserve recoverable output. No encoder work or file operations may block the game render loop.

## Verification still required

- Synthetic audio tests remain outstanding; no microphone or desktop capture during unattended tests.
- Test OpenGL/Vulkan, window resize, fullscreen toggle and minimized window handling.
- Verify independent audio toggles, A/V sync, clip duration and export integrity.
- Measure frame-time percentiles and recorder GPU/disk cost, not only peak FPS.

## Related fixes built in this change

Hotkeys live row sizing, vanilla pearl equip/use/restore sequence, item landing motion, hand following vanilla item poses, scoped crystal input handling, and settings-style command editor shell. Build/unit tests pass; runtime and visual verification remain outstanding.
