# Swedish Vehicle Lookup

This is an Android application for Swedish vehicle lookups.

## Project Structure

```
SweVech/
├── build.gradle          # Root build configuration
├── settings.gradle       # Project settings and module inclusion
└── app/                  # Application module
    ├── build.gradle      # App module build configuration
    └── src/
        └── main/
            ├── AndroidManifest.xml
            └── java/
                └── com/example/swedishvehiclelookup/
```

## Building the Project

This project uses Gradle as the build system. To build from the SweVech directory:

```bash
cd SweVech
gradle build
```

Or if you have the Gradle wrapper installed:

```bash
cd SweVech
./gradlew build
```

## Project Root

The `SweVech` directory is the project root. All build commands should be run from this directory.
