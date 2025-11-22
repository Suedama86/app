# Swedish Vehicle Lookup - AI Agent Instructions

## Project Overview
Android app that queries Swedish vehicle registration data via SMS to 71640. Uses Jetpack Compose for UI, voice recognition for input, and broadcast receivers for SMS handling.

## Project Structure
- **Root**: `SweVech/` (Gradle project root - run all build commands here)
- **App module**: `app/` with package `com.example.swedishvehiclelookup`
- **Source files**: All Kotlin code in `app/src/main/java/com/example/swedishvehiclelookup/`

## Build System
- Gradle-based Android project
- Kotlin 1.9.0, Android Gradle Plugin 8.2.0
- MinSDK 24, TargetSDK 34, CompileSDK 34
- Build from `SweVech/`: `./gradlew build` or `gradle build`

## Architecture & Key Components

### MainActivity Pattern
- **Duplicate files**: Both `MainActivity.kt` and `Mainactivityv2.kt` exist with identical content
- Single Activity architecture using Jetpack Compose
- State managed via `mutableStateOf` at activity level (not ViewModel)
- Three core states: `_registrationNumber`, `_responseText`, `_isLoading`

### SMS Communication Flow
1. User inputs registration number → `sendVehicleLookupSms()` sends to "71640"
2. `SmsReceiver` (manifest-registered) intercepts incoming SMS via `android.provider.Telephony.SMS_RECEIVED`
3. If sender contains "71640", broadcasts custom intent: `SMS_RECEIVED_ACTION`
4. `localSmsReceiver` in MainActivity receives broadcast and updates UI state
5. 45-second timeout handler provides fallback message if no response

### Broadcast Receiver Pattern
- **Global receiver**: `SmsReceiver.kt` filters SMS from 71640, rebroadcasts locally
- **Local receiver**: Created in `setupLocalSmsReceiver()`, listens for `SMS_RECEIVED_ACTION`
- Uses `RECEIVER_NOT_EXPORTED` on Android 13+ for security
- Proper lifecycle: registered in `onCreate()`, unregistered in `onDestroy()`

### Permissions Workflow
- Runtime permissions: SMS (send/receive/read), RECORD_AUDIO
- Version-aware: Skips READ_SMS on Android 13+
- All checked via `checkPermission()` before feature use
- Uses ActivityResultContracts pattern for voice recognition launcher

## Technology Stack

### UI (Jetpack Compose)
- Material3 design system
- Single `VehicleLookupScreen` composable with state hoisting
- Common patterns:
  - `MaterialTheme.colorScheme.primaryContainer` for info cards
  - `verticalScroll(rememberScrollState())` for scrollable content
  - `Icons.Default.Mic` and `Icons.Default.Clear` from extended icons
  - Emoji prefixes for visual feedback (📤, ⏳, ✓, ❌)

### Voice Recognition
- Swedish locale: `"sv-SE"` language model
- Uses `speechRecognizerLauncher` (ActivityResultLauncher pattern)
- `cleanRegistrationNumber()` sanitizes voice input: uppercase, alphanumeric only

### SMS Handling
- SmsManager API with version check: `getSystemService()` on Android 12+, `getDefault()` on older
- No delivery/sent intent tracking (null PendingIntents)
- High priority receiver: `android:priority="999"` in manifest

## Code Conventions

### State Management
```kotlin
private val _registrationNumber = mutableStateOf("")
// Access in composables: 
val registrationNumber by remember { _registrationNumber }
```

### Input Validation
- Registration numbers: uppercase, filter `char.isLetterOrDigit() || char.isWhitespace()`
- Voice input: `cleanRegistrationNumber()` removes all non-alphanumeric

### Error Handling
- Toast notifications for user feedback
- Try-catch blocks around SMS operations and receiver registration
- Graceful fallback for voice recognition unavailability

## Critical Dependencies
- `androidx.compose:compose-bom:2024.02.00` (BOM-managed Compose versions)
- `androidx.compose.material:material-icons-extended` (for Mic icon)
- Kotlin Compose compiler: 1.5.1 (matches Kotlin 1.9.0)

## Common Tasks

### Adding New Permissions
1. Update `AndroidManifest.xml` `<uses-permission>`
2. Add to `requestPermissions()` permissions list
3. Add version checks if permission deprecated/changed

### Modifying SMS Logic
- **Sender filter**: Check `SmsReceiver.onReceive()` - currently checks if sender contains "71640"
- **Response timeout**: Adjust 45000ms delay in `sendVehicleLookupSms()`
- **Service number**: Hardcoded "71640" in multiple places (button text, SMS destination, receiver filter)

### UI Customization
- Main composable: `VehicleLookupScreen()` in MainActivity
- Modify spacing: Use multiples of 8dp/12dp/16dp (Material Design spacing)
- Colors: Reference `MaterialTheme.colorScheme.*` (auto dark mode support)

## Known Issues
- Duplicate MainActivity files (`MainActivity.kt` and `Mainactivityv2.kt`) - unclear which is active
- No ViewModel/repository pattern - state management in Activity
- SMS service number "71640" hardcoded throughout (not in strings.xml)
- No unit tests present in project structure
