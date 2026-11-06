# DAEMON-EXPEDITE — Expedite

**Expedite** is an Android app (Kotlin, Jetpack Compose, Firebase) for booking campsites through a
lottery. Popular campsites have more demand than nights available, so instead of
first-come-first-served, campsite owners open a lottery: entrants join a waiting list during an
entry period, winners are drawn at random, and winners then book their stay.

- Course: CMPUT 301, Fall 2026 — [project problem description](https://ualberta-cmput301.github.io/projects/project_problem_descr.html)
- Team DAEMON: see [`doc/teams.txt`](doc/teams.txt)
- Project wiki (includes the UI design): [GitHub Wiki](https://github.com/CMPUT301F26DAEMON/DAEMON-EXPEDITE/wiki)

> **Heads-up on commit history:** Zorawar Gill's git was not configured at the start, so his commits are not linked to his GitHub account and don't show up in the usual contributor stats. [See his commits here](https://github.com/search?q=repo%3ACMPUT301F26DAEMON%2FDAEMON-EXPEDITE+author-name%3A%22Zorawar+Gill%22&type=commits).

## Roles

| Role | Who | What they can do |
|---|---|---|
| **Entrant** | Every user (signed in anonymously by device) | Browse/search campsites, join or leave lotteries, book a stay after winning, manage reservations and profile |
| **Owner** | Any entrant who switches to Owner on their profile | Register campsites, create/edit/cancel lotteries, see waiting lists and booked dates |
| **Admin** | Added manually in Firestore (`admins` collection) | Browse and remove campsites, profiles and images |

One person has the same id across `users`, `owners` and `admins`; the profile screen's role toggle
switches between them.

## Features

### Done (half-way checkpoint)
- **Accounts:** anonymous sign-in, entrant profile (name, email, phone, profile photo upload/delete), role switching, account deletion.
- **Browsing:** campsite list with search and filters (location, dates, guests), campsite details with image carousel, amenities, owner card, policies and map view.
- **Owner campsites:** register a campsite (with location picker and images), view, edit and delete it.
- **Lotteries (owner):** My Lotteries list, create lottery (entry period, multiple stay periods, number of winners, campsites), edit while open (can only grow), cancel, view waiting list.
- **Lotteries (entrant):** join/leave a waiting list, see lottery status and results.
- **Draw:** a Firebase Cloud Function (`functions/`) runs every 3 hours and draws random winners for lotteries whose entry period has ended.
- **Booking:** winners pick one campsite and one continuous date range from the stay periods, or decline; reservations can be cancelled; owners see booked dates on a calendar.
- **Admin:** admin home with campsite list and campsite removal.
- **Tests:** unit tests for model classes (with a mocked database), lottery/booking rules and ViewModels; Node test for the draw logic.

### Planned (next iterations)
- Messaging between entrants and owners (`Chat`, `Message` models exist; screens not built).
- Notifications (e.g. lottery results, booking reminders).
- Rating a stay after it ends.
- Taking a profile/campsite photo with the camera (gallery upload already works).
- Admin: browse and remove profiles and images from dedicated screens; QR code scanning.
- Owner reservations screen.

## Known issues / to fix in the next iteration
- Buttons for features that aren't built yet (Messages, Notifications, Rating, Take photo, admin Profiles/Images) show an *"isn't built yet"* toast.
- Some `Database` functions are still `TODO()` (`fetchAllImages`, `fetchAllChats`, `addNotification`, `fetchAllNotifications`); calling them shows an error message instead of crashing.
- Model classes in `backend/` (`User`, `Owner`, `Campsite`, `Lottery`, `Admin`) call `Database` directly. We plan to move these calls into ViewModels for easier testing and one consistent source of state.
- Some older screens still have outdated `TODO` comments (e.g. `EntrantProfileDetailsScreen.kt`).

## App flow

```
Splash (anonymous sign-in, load user, check admin)
  └─ Home (Entrant) ── campsite list, search, filters
       ├─ Campsite details ── policies, map, lottery status card
       │     └─ Join / leave lottery ──(draw)──> Won? ── Book stay  /  Decline
       ├─ My Lotteries (entrant)
       ├─ Reservations ── cancel reservation
       └─ Profile ── edit info / photo, role toggle ──┐
                                                       │
  ┌────────────────────────────────────────────────────┘
  ├─ Owner Home ── my campsites
  │     ├─ Register campsite ── select location
  │     ├─ Owner campsite details ── edit / delete, booked dates
  │     └─ My Lotteries ── create lottery
  │           └─ Lottery details ── edit / cancel, waiting list
  └─ Admin Home (admins only) ── campsite list ── admin campsite details (remove)
```

All routes and screen wiring are in `frontend/AppNavigation.kt` (`Routes` object at the top).

## Where things are

```
code/app/src/main/java/com/example/daemon_expedite/
├── MainActivity.kt          App entry point
├── backend/                 Model classes and data layer
│   ├── Database.kt          All Firestore / Storage / Auth access (interface + Firebase implementation)
│   ├── User, Owner, Admin   The three roles
│   ├── Campsite, Image, Rating, Reservation
│   ├── Lottery, WaitingList, DatePeriod
│   ├── LotteryRules.kt      Validation rules for creating/editing lotteries
│   ├── BookingRules.kt      Rules for what a winner can book
│   └── Chat, Message, Notification   Models for planned features
├── frontend/                Screens (*Screen.kt / *Home.kt) and their ViewModels (*ViewModel.kt)
│   ├── AppNavigation.kt     Every route and how screens connect
│   ├── SessionViewModel.kt  The signed-in user, shared by all screens
│   ├── UiState.kt           Loading / success / error state used by ViewModels
│   └── Entrant*, Owner*, Admin*, Campsite*, CreateLottery*, Booking*, RegisterCampsite* ...
├── ui_components/           Reusable Compose pieces (cards, buttons, form fields, calendars, map)
│   └── Appcolors / AppIcons / Apptypography   Design tokens used by all screens
└── ui/theme/                Material theme
code/app/src/test/...        Unit tests: backend_tests/ (models, rules), frontend_tests/ (ViewModels)
code/app/src/androidTest/... Instrumented tests (Firestore, UI components)
functions/                   Cloud Function that draws lottery winners (see functions/SETUP.txt)
doc/                         Team info
```

## Running
- **App:** open `code/` in Android Studio and run the `app` configuration (needs the Firebase config file `code/app/google-services.json`, which is not in the repo; ask the team for it).
- **Unit tests:** `cd code && ./gradlew testDebugUnitTest`
- **Cloud Function:** see [`functions/SETUP.txt`](functions/SETUP.txt) to deploy; `cd functions && npm test` runs its tests.

## AI Use Acknowledgement

Requirements analysis, feature design (including the lottery rules), UI design in Figma and
architecture decisions were done by our team over the course of the project.

Generative AI tools were used as coding assistants for parts of the implementation:

- **Anthropic, Claude Code** — used by Amitej and Keya to help implement some Android screens,
  ViewModels and model logic, the lottery-draw Cloud Function, and unit tests, based on the
  team's designs.
- **OpenAI, ChatGPT** — used by some team members to help understand and fix errors met during
  development.

All AI-assisted code was reviewed, edited and tested by the team, and we are responsible for
everything we submit. **Estimated extent: light to moderate**, limited to implementation help
and debugging.

Files containing AI-assisted code have a citation comment at the top giving the provider, tool,
subject and date, following the course outline. Files written without AI help have no citation.
