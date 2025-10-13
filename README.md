# 🛣️ Road Conditions

![Screenshot of the app. ](https://i.imgur.com/D0Ecl3s.jpeg)

## Overview

Road Conditions is a mobile application that leverages real-time sensor data from Android smartphones to visualize the quality of roads.

## Features

- **Map**: View map, see current location and see current detected road bumps on the map.
- **Activity Tracker**: Tracks the user with Google's "Activity Recognition Transition API" to determine if the user is driving. Manually enabling bump detection is supported as well.
- **Bump Detection**: Sends bump data to the database when a bump is detected while driving.
  
## Installation

### Prerequisites

- Install Android Studio
- Install Docker

### Steps

1. Clone the repository: `git clone https://github.com/JooRiiv/RoadConditions` or use Android Studio's clone feature

2. Add your Google Maps Api-Key into the local.properties file. 'MAPS_API_KEY="YOUR_API_KEY_HERE'

3. Add your endpoint to the file "BumpDetection.kt" found in "Frontend/app/src/main/java/com/example/roadconditions/"

4. Setup a Docker container app on Microsoft Azure for the Dockerfile.

5. Connect an Android Smartphone to Android Studio and optionally build an APK.

## Usage

1. Search for bump locations on the map.
2. Activate bump detection either manually or automatically using Google's Activity Recognition Transition API.
3. Sends bump data to database after the phone's accelerometer detects enough sudden movement during bump detection while driving.

## Technologies used

- **Frontend**:
  - Kotlin
  - Google Maps SDK
  - Android Studio
- **Backend**:
  - Ktor
  - Microsoft Azure
- **Database**:
  - MongoDB

## License

This project is licensed under the [MIT License](/LICENSE)
