# StudySphere

A comprehensive Android study companion app built with Java that helps students organize their academic life — manage subjects, track assignments, set study goals, take notes, and stay focused with a built-in Pomodoro timer. This project was created to demonstrate Android development with Java and is designed for personal academic use.

> **Note:** User data including notes and assignment details are stored locally on-device using Room (SQLite) and optionally synced to Firebase Firestore. While this application is suitable for personal usage, please be aware that note content is not end-to-end encrypted in the cloud sync layer. If you choose to store sensitive academic information, do so at your own discretion and consider implementing additional security measures.

## Features

- **Dashboard**: Overview of study hours, pending assignments, and upcoming deadlines with progress charts
- **Subjects**: Add, edit, and color-label subjects with credit hours and teacher information
- **Assignments**: Create and manage assignments with due dates, priority levels, and completion tracking
- **Notes**: Write and organize rich-text notes per subject with pin and search functionality
- **Study Planner**: Schedule study sessions using a built-in calendar and time-block planner
- **Grade Calculator**: Input marks and weights to auto-calculate GPA and subject grades
- **Pomodoro Timer**: Built-in focus timer with customizable work and break intervals and notifications
- **Reminders & Alerts**: Local push notifications for upcoming assignments and scheduled study sessions
- **Authentication**: Sign up, sign in with Email/Password and Google Sign-In via Firebase Authentication
- **Dark / Light Theme**: Adaptive UI with system-default and manual theme toggle support
- **Offline Support**: Full offline functionality using Room with automatic background sync to Firestore
- **Cloud Backup**: Automatic data backup and restore via Firebase Firestore on login

## Tech Stack

- **Language**: Java (Android SDK)
- **Architecture**: MVVM (Model-View-ViewModel) with Repository Pattern
- **UI**: Material Design 3, RecyclerView, ViewPager2, BottomNavigationView
- **Local Database**: SQLite via Room Persistence Library
- **Cloud Backend**: Firebase Firestore (real-time sync and cloud storage)
- **Authentication**: Firebase Authentication (Email/Password and Google Sign-In)
- **Dependency Injection**: Hilt (Dagger)
- **Reactive Programming**: RxJava 3 with LiveData and ViewModel
- **Networking**: Retrofit 2 + OkHttp 4
- **Charts**: MPAndroidChart
- **Image Loading**: Glide
- **Notifications**: AlarmManager + NotificationCompat
- **Min SDK**: API 24 (Android 7.0 Nougat)
- **Target SDK**: API 34 (Android 14)

## Getting Started

### Prerequisites

- Android Studio (Hedgehog 2023.1.1 or later)
- JDK 17 or higher
- Android SDK with API levels 24–34 (installable via Android Studio SDK Manager)
- Git
- Firebase account for Authentication and Firestore ([Firebase Console](https://console.firebase.google.com/))
- `google-services.json` downloaded from your Firebase project

### Installation

1. Clone the repository:
   ```
   git clone https://github.com/your-username/StudySphere.git
   cd StudySphere
   ```

2. Open in Android Studio:
   ```
   # Launch Android Studio → File → Open → select the StudySphere/ directory
   # Wait for Gradle sync to complete automatically
   ```

3. Add your Firebase configuration file:
   ```
   # Download google-services.json from your Firebase project console
   # Place it inside the app/ directory:
   app/google-services.json
   ```

4. Configure local environment variables:
   Create or update the `local.properties` file in the project root with the following:
   ```
   sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk

   MAPS_API_KEY="your_google_maps_api_key"
   STUDYSPHERE_BASE_URL="https://api.yourdomain.com/"
   ```

5. Build and run the app:
   ```
   ./gradlew assembleDebug
   # or press the Run ▶ button in Android Studio
   ```

6. Select a connected Android device or an AVD (emulator) with API 24 or higher and click **Run**

### Setting up Firebase

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Click **Add project** and follow the setup wizard
3. Register an Android app using your package name: `com.yourdomain.studysphere`
4. Download the `google-services.json` file and place it inside the `app/` directory
5. Enable **Email/Password** and **Google** sign-in under **Authentication → Sign-in method**
6. Create a **Firestore Database** in production mode under **Build → Firestore Database**
7. Apply the following Firestore security rules under **Firestore Database → Rules**:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```

### Setting up Notifications

1. Notifications are powered by Android's built-in `AlarmManager` and `NotificationCompat` — no third-party account is needed
2. Ensure `POST_NOTIFICATIONS` permission is declared in `AndroidManifest.xml` (already included)
3. On Android 13 (API 33) and above, the app will automatically prompt the user to grant notification permission on first launch
4. To customize default reminder lead time, update the constant in `NotificationHelper.java`:
   ```
   DEFAULT_REMINDER_OFFSET_MINS = 30; // Minutes before due date to trigger reminder
   ```

## Project Structure

```
StudySphere/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/yourdomain/studysphere/
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/              # Room database, DAOs, and entities
│   │   │   │   │   ├── remote/             # Firebase Firestore data sources
│   │   │   │   │   └── repository/         # Repository layer bridging local + remote
│   │   │   │   ├── di/                     # Hilt dependency injection modules
│   │   │   │   ├── ui/                     # Activities, Fragments, and ViewModels
│   │   │   │   │   ├── auth/               # Login and registration screens
│   │   │   │   │   ├── dashboard/          # Home dashboard with progress overview
│   │   │   │   │   ├── subjects/           # Subject list and detail screens
│   │   │   │   │   ├── assignments/        # Assignment management screens
│   │   │   │   │   ├── notes/              # Notes editor and list screens
│   │   │   │   │   ├── planner/            # Calendar and study session planner
│   │   │   │   │   ├── timer/              # Pomodoro timer screen
│   │   │   │   │   ├── grades/             # Grade calculator screen
│   │   │   │   │   └── settings/           # App settings and user profile
│   │   │   │   ├── utils/                  # Utility and helper classes
│   │   │   │   ├── service/                # Background sync and reminder services
│   │   │   │   └── StudySphereApp.java     # Application class (Hilt entry point)
│   │   │   ├── res/
│   │   │   │   ├── layout/                 # XML layout files
│   │   │   │   ├── drawable/               # Icons and drawable assets
│   │   │   │   ├── values/                 # Colors, strings, themes, dimensions
│   │   │   │   ├── values-night/           # Dark theme overrides
│   │   │   │   ├── navigation/             # Navigation graph XML
│   │   │   │   └── menu/                   # Bottom nav and options menu XML
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                           # Unit tests (JUnit 4)
│   │   └── androidTest/                    # Instrumented tests (Espresso)
│   ├── google-services.json                # Firebase config (DO NOT commit)
│   └── build.gradle                        # App-level Gradle config
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle                            # Project-level Gradle config
├── settings.gradle
├── local.properties                        # Local SDK path and secrets (DO NOT commit)
├── .gitignore
├── LICENSE
└── README.md
```

## Database Schema

The application uses Room Persistence Library as an abstraction layer over SQLite. The schema includes entities for:

- **Subject** — stores subject name, color label, credit hours, and teacher name
- **Assignment** — stores title, description, due date, priority level, completion status, and a foreign key reference to Subject
- **Note** — stores title, rich-text content, pinned status, and a foreign key reference to Subject
- **StudySession** — stores start time, end time, duration in minutes, session type, and a foreign key reference to Subject

You can find the complete entity definitions in the `app/src/main/java/com/yourdomain/studysphere/data/local/entity/` directory and the Room database configuration in `AppDatabase.java`.

## Customization

- **Theme**: Primary and secondary colors can be changed in `res/values/themes.xml` using Material Design 3 color tokens; night-mode overrides go in `res/values-night/themes.xml`
- **Pomodoro Timer**: Default work duration, short break, and long break lengths can be adjusted as constants in `TimerViewModel.java`
- **Subject Categories**: New subject category types can be added to the `SubjectCategory` enum in `Subject.java`
- **Notification Lead Time**: The default reminder offset before assignment due dates can be configured in `NotificationHelper.java`
- **Currency / GPA Scale**: Grade scale (4.0, 5.0, percentage) can be selected in the user profile settings screen

## Deployment

This application can be built and distributed as a signed APK or Android App Bundle:

1. Generate a keystore file using `keytool` and configure signing in `app/build.gradle` under `signingConfigs`
2. Build the release APK:
   ```
   ./gradlew assembleRelease
   # Output: app/build/outputs/apk/release/app-release.apk
   ```
3. Or build an App Bundle for Google Play Store:
   ```
   ./gradlew bundleRelease
   # Output: app/build/outputs/bundle/release/app-release.aab
   ```
4. Upload the `.aab` file to [Google Play Console](https://play.google.com/console/) under **Release → Production**

## License

[MIT License](LICENSE) - Free to use, modify, and distribute with attribution. Commercial use is permitted.
