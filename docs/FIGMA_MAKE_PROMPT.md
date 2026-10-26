# Промпт для Figma Make — Vela Client 26.2

Создай high-fidelity интерактивный прототип интерфейса Minecraft-клиента `Vela Client` для Minecraft 26.2. Это компактный, быстрый и цельный dark UI с собственной визуальной системой. Интерфейс должен выглядеть как готовый игровой продукт: аккуратная сетка, чистый Inter, чёткий рендер на целых координатах, спокойные анимации и единый фирменный фиолетово-индиговый акцент.

Используй русский язык для описаний, подсказок, настроек, ошибок и диалогов. Названия вкладок и функций оставь на английском: `Visuals`, `HUD`, `Utilities`, `Markers`, `Friends`, `Configs`, `Fullbright`, `Custom Sky`, `Auto Reconnect` и т. п.

## Визуальная система

- Основное окно Client GUI: 820×500 px, адаптивный минимум 680×420 px без обрезания.
- Sidebar 164 px, header 56 px, внешний radius 14 px, radius карточек 10 px, controls 8–9 px.
- Safe area 24 px, базовая сетка 4 px, интервалы 8/12/16/20/24/32.
- Фон окна `#10141C`, sidebar `#0B0E14`, surface `#141923`, hover `#191F2B`, inner `#0C1017`.
- Border `#252D3D`, hover border `#39445B`, primary `#756CFF`, bright `#918BFF`, cyan detail `#55CFFF`.
- Text primary `#F3F5FA`, secondary `#9AA4B6`, muted `#626D80`, success `#45D39A`, danger `#FF667C`.
- Градиент использовать только в небольших фирменных деталях: `#756CFF → #55CFFF`. Не заполнять им крупные панели.
- Шрифт только Inter: page title 19/24 SemiBold, module title 14/19 Medium, body 12/17 Regular, caption 11/15 Medium, button 12/17 SemiBold.
- Все элементы и текст располагаются на целых координатах. Проверь кириллицу, kerning, baseline, длинные строки и scaling.

Подготовь полноценные design tokens, variants и компоненты: Button primary/secondary/ghost/destructive, Toggle, Module Card, Nav Item, Input, Search, Dropdown, Tooltip, Toast, Dialog, Drawer, Tabs, Chip, Slider, Color Picker, Keybind Recorder, Friend Row, Config Row, Account Row, Empty/Loading/Error State.

## Иконки и бренд

Используй тонкие смысловые иконки с единым stroke 1.5 и размером 16×16. Иконки обязательны у вкладок sidebar: глаз для `Visuals`, экран для `HUD`, инструмент для `Utilities`, метка для `Markers`, пользователь для `Friends`, облако/профиль для `Configs`. Системные icon-only действия — Search, Settings, Back, Close, More, chevron — тоже получают понятные иконки.

Не ставь отдельную иконку возле каждой функции в module cards. Карточки функций строятся на типографике, toggle, bind и chevron. В expanded settings небольшие иконки допустимы только там, где они реально ускоряют распознавание действия. Не используй emoji, font icons, разноцветные квадраты и случайные декоративные символы.

Логотип не генерируй и не перерисовывай. Везде используй один заменяемый компонент `Brand/LogoAsset` с вариантами 32×32, 64×64 и large. Вокруг него нет пунктирной рамки и подписи `logo`.

## Client GUI

Sidebar содержит `Brand/LogoAsset`, `Vela`, подпись `Client 26.2` и вкладки `Visuals`, `HUD`, `Utilities`, `Markers`, `Friends`, `Configs`. `Accounts` здесь отсутствует. Активная вкладка: слегка более светлая поверхность, тонкая вертикальная accent-линия 2 px, светлая иконка и текст. Не делать сплошную яркую заливку. Внизу sidebar только `v0.1.0-alpha`, без системных кнопок окна.

Header содержит название раздела, короткое русское описание, один глобальный Search и рабочую кнопку Settings справа. Фильтров `All / Enabled / Bound` нет. Search сохраняет запрос при переходах, ищет функции, друзей, маркеры и конфиги по всему клиенту. `Ctrl+K` ставит фокус в Search. В GUI не показывать FPS, ping и техническую статистику.

Контент module-разделов: две колонки карточек 306×66 px. Одновременно видно 8–10 строк. Карточка содержит английское название функции, русское описание, спокойный toggle и chevron. Иконки функций отсутствуют. Bind в обычном состоянии скрыт, при hover появляется как небольшой muted text.

Expanded state открывает настройки внутри карточки без скачка header и соседних колонок: заранее измерить высоту, анимировать clip-height и opacity. Scrollbar 3 px проявляется только во время прокрутки. Нижней status strip нет.

ПКМ по карточке открывает фирменное context menu: `Set bind`, `Bind mode`, `Reset settings`, `Open details`. `Set bind` запускает key-capture overlay. Режимы: `Toggle` и `Hold`; есть `Clear bind`, Escape отменяет захват. Dropdown, select и context menu остаются внутри окна, корректно меняют направление возле края и имеют правильный z-index.

## Состояния и анимации

Верни выразительный, но аккуратный hover навигации и меню. При наведении nav item плавно сдвигает текст и иконку на 2 px вправо, фон осветляется до `#191F2B`, border становится чуть заметнее, и у левого края появляется короткая accent-линия. Переход 120 ms cubic-bezier(.2,.8,.2,1). Active state устойчивый и не прыгает при hover.

Module card при hover поднимается визуально на 1 px через border/shadow без изменения layout. Toggle 150 ms, раскрытие 190 ms, page transition — opacity + translateY 4 px за 160 ms, dialog — fade + scale .98→1 за 180 ms. Press state 70 ms. `Reduced Motion` заменяет перемещения на короткий fade.

Каждый интерактивный элемент имеет default, hover, pressed, focused, disabled. Никаких резких масштабирований, бесконечных декоративных анимаций и blur на каждом слое.

## Разделы

- `Visuals`: Fullbright, Custom Sky, Particles, Hit Color, Motion Blur, Entity Highlight.
- `Utilities`: Auto Reconnect, Screenshot Manager, Chat Timestamps, Inventory Preview, Quick Commands.
- `Markers`: компактный список name/color/dimension/coordinates, Add marker и edit drawer.
- `Friends`: avatar/nick/alias/note/favorite, Add modal, empty state, проверка дублей, подтверждение удаления.
- `Configs`: строки высотой 52 px с Load/Duplicate/Rename/Export/Delete, autosave, create modal, import/export JSON.

В `Friends` и `Configs` нет дополнительных поисковых строк. Кнопки Add/Create открывают отдельный modal и не превращают глобальный Search в поле ввода.

## Color Picker и Settings

Settings в правом верхнем углу всегда нажимается и открывает отдельный screen/side sheet с разделами `Appearance`, `Interface`, `Sounds`, `Controls`.

- Appearance: фирменный Accent Color, пресеты, opacity поверхностей, live preview.
- Interface: UI scale 75–115%, compact density, animations, Reduced Motion, tooltips, language.
- Sounds: UI sounds, общая громкость, отдельные звуки GUI open/close, module enable/disable, dropdown/context menu, Preview.
- Controls: Open GUI bind, Search bind, Chat Drag Mode button, Reset controls.

Color Picker полностью кастомный: anti-aliased hue circle, Saturation/Value field, R/G/B/Alpha sliders, editable HEX `#RRGGBB`/`#RRGGBBAA`, Copy/Paste/Reset, recent colors, live preview. Неверный HEX подсвечивается и не стирает введённый текст.

## HUD и Drag Mode

`HUD` — обычный список module cards: Armor, Keystrokes, Coordinates, Effects, Ping, CPS. В Client GUI нет кнопки `Edit positions`.

При открытом стандартном Minecraft Chat возле chat input появляется компактная кнопка Vela `Drag Mode`. Она включает overlay над реальной игрой. В Drag Mode можно двигать Vela HUD и vanilla HUD: Chat, Boss Bar, Scoreboard, Player List, Action Bar, Hotbar, Effects, Subtitles. Покажи bounding boxes, smart guides, snapping, safe zones, relative anchors, undo/redo, Save/Reset/Exit и Auto Arrange. Элементы не могут уйти за экран или перекрыться после смены разрешения.

## Account Manager

Account Manager — отдельный экран из главного меню, без sidebar модулей. Компактное окно 620×430 px с заголовком `Accounts` и Back.

В Account Manager есть четыре полноценных способа добавления профиля: `Microsoft`, `Offline`, `Ely.by`, `LittleSkin`. Покажи их компактными provider tabs внутри Add Account dialog. Последний выбранный провайдер можно запоминать, но Microsoft располагается первым.

- Microsoft: browser OAuth + PKCE, ссылка `Создать Microsoft-аккаунт`, состояния waiting/success/cancelled/expired/network error.
- Offline: отдельное поле nickname с Minecraft-валидацией 3–16 символов, проверкой дублей и понятной пометкой, что официальные online-mode серверы недоступны.
- Ely.by: login/email, password, optional 2FA code, ссылка регистрации, loading/error state.
- LittleSkin: email, password, ссылка регистрации, loading/error state.

После входа каждая строка показывает настоящую Minecraft-head/skin, nickname, короткий UUID, дату последнего использования и действия Activate/Remove. При загрузке скина — skeleton, при ошибке — стандартный Minecraft skin. Цветные инициалы вместо скина не использовать.

Пароли Ely.by и LittleSkin не сохраняются; сохраняются только выданные провайдером сессионные токены в защищённом хранилище ОС. Microsoft-пароль никогда не вводится в клиенте. При смене аккаунта во время подключения к серверу показывается предупреждение о необходимости отключиться.

Empty state: крупный спокойный заголовок `Добавьте игровой профиль`, короткое объяснение и кнопка `Добавить аккаунт`, которая открывает provider dialog. Не создавать offline-профиль автоматически и не подставлять демонстрационный ник.

## Главное меню

Fullscreen dark layout с лёгкой холодной текстурой. Контент располагается в левой трети, как в удачной ранней версии: `Brand/LogoAsset`, Vela и вертикальное меню `Singleplayer`, `Multiplayer`, `Vela Client`, `Accounts`, `Settings`, `Quit`. Справа располагается крупная, полупрозрачная фирменная композиция из `Brand/LogoAsset` и тонких дуг/линий, которая заполняет пустое пространство, но не конкурирует с меню.

`Vela Client` выглядит как остальные пункты и не получает постоянную яркую заливку. Hover меню выразительный: короткая accent-линия раскрывается слева, текст сдвигается на 4 px и становится белым, рядом мягко проявляется небольшой arrow; фон пункта становится лишь немного светлее. Active/focus не меняет ширину и положение всей колонки. Текущий nickname — одна строка в нижнем левом углу. Внизу по центру `Vela 0.1.0 / Minecraft 26.2`.

## Загрузочный экран

Создай `Splash/Loading` в композиции 1280×720. Слева находится компактный блок: малый `Brand/LogoAsset`, `Vela Client`, статус загрузки, progress bar 240×2 и короткий detail. Блок выровнен по вертикальному центру с отступом 72 px от левого края.

Правую половину заполняет крупный `Brand/LogoAsset` размером примерно 340×340, частично уходящий за правый край. Его opacity 10–16%, вокруг 2–3 тонкие дуги/орбиты с opacity до 10%. Это именно фирменное заполнение пространства, не отдельная карточка и не фоновая фотография.

Progress track `#202736`, fill `#756CFF → #55CFFF`. Logo слева проявляется за 300 ms; крупный знак справа медленно проявляется и смещается на 8 px за 700 ms; progress плавно меняет ширину; status меняется через crossfade. Минимальное видимое время 1.4 секунды, затем fade-out 350 ms. Подготовь состояния 0/35/72/100% и error.

## Финальная проверка

Создай страницы `Cover`, `Design System`, `Client GUI`, `HUD Position Edit`, `Markers`, `Friends`, `Configs`, `Account Manager`, `Main Menu`, `Loading Screen`, `Prototype Flow`.

Свяжи основные сценарии: Main Menu → Vela Client → module/settings/config; Main Menu → Accounts → Microsoft browser flow; Minecraft Chat → Drag Mode → move HUD → Save. Проверь resize, русский текст, tab navigation, Escape, dropdown у нижнего края, hover/press/focus, отсутствие обрезания, стабильность layout и WCAG AA.
