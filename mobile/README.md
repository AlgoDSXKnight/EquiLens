# EquiLens Mobile

Flutter companion for backend login/registration, company research and stored financial history, relative valuation summary, watchlists, portfolio positions, transactions and P&L. The backend owns financial rules and JWT identity; no client user ID is submitted. Company views label synthetic demo input and unverified sources explicitly.

Run `flutter pub get`. Native Android and iOS runner projects are included. Configure Android package `com.equilens.mobile` and iOS bundle ID `com.equilens.mobile`, then add `android/app/google-services.json` and `ios/Runner/GoogleService-Info.plist`. Enable Firebase Anonymous Authentication and Firestore. For the Android emulator run `flutter run --dart-define=RESEARCH_API_URL=http://10.0.2.2:8080/api --dart-define=PORTFOLIO_API_URL=http://10.0.2.2:8081/api`. The debug Android manifest permits HTTP for local development; use HTTPS for release builds. For iOS or physical devices, use a reachable HTTPS URL for the backend.

Firebase is optional and stores only the last selected section under the anonymous Firebase auth UID. Add the native Firebase config, enable Anonymous Authentication and Firestore, and deploy `firestore.rules`. If initialization fails, the application continues with backend REST features and does not store preferences.

Run widget tests with `flutter test`.
