# VibeFit App

VibeFit is a fashion and AI-powered mobile application that allows users to:
- Try on outfits virtually using face fusion and pose transfer.
- Upload, edit, and share posts with text, images, or videos.
- Manage size profiles and receive personalized outfit recommendations.
- Browse community posts via Explore, Forum, and Trends tabs.

## 📱 Features

- **Authentication**: Register, login, logout, reset password, update profile.
- **AI Features**:
  - Outfit recommendation using Firebase Vertex AI (Gemini).
  - Virtual Try-On with face fusion (GlamAI API).
- **Post/Forum Creation**:
  - Upload one or multiple images.
  - Auto-suggest category based on source tab (Explore → tutorial/pattern, Forum → forum, Trends → trends).
- **Community**:
  - Like, comment, reply, favorite, and edit/delete own posts or comments.

## 🔧 Requirements

- Android Studio **Meerkat (2024.3.2)** or newer
- **Java 21 (OpenJDK 21.0.6)**  
- **Gradle 8.3**  
- Android SDK API Level **33+**
- Internet connection
- A configured Firebase project (Auth, Firestore, Storage, Vertex AI)

## 🚀 Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/siongyee03/FYP_Degree.git
   ```
2. **Open in Android Studio**
   Make sure to select the project root folder, not just the /app subfolder.
3. **Create your own** `google-services.json`:
   Download it from your Firebase Console.
   Place it in:
   ```bash
   /app/google-services.json
   ```
4. Create local.properties
   In the project root, add:
   ```ini
   sdk.dir=/path/to/your/sdk
   ```
   Example:
   ```ini
   sdk.dir=C:\\Users\\yourname\\AppData\\Local\\Android\\Sdk
   ```

5. **Create a** `gradle.properties` **file** in the root directory with the following:
   ```ini
   UNSPLASH_API_KEY=your_unsplash_key
   
   VIRTUAL_TRYON_API_KEY=your_tryon_key
   
   IMGBB_API_KEY=your_imgbb_key
   ```
   These keys are required for API calls (virtual try-on, fashion image search, image upload).

6. **Sync and Build**
   - Click **"Sync Gradle"** in Android Studio.
   - Then click **"Run"** or **Shift + F10**.

## 🗂️ Project Structure
```pgsql
VibeFitApp/
├── app/
│   ├── java/com/example/vibefitapp/
│   │   ├── activities/
│   │   ├── adapters/
│   │   ├── models/
│   │   ├── utils/
│   ├── res/
│   ├── google-services.json         
├── build.gradle.kts
├── gradle.properties                
├── local.properties                 
└── README.md
```

## 📦 Dependencies
- Firebase Auth, Firestore, Storage, App Check
- Glide
- OkHttp
- Gson / JSON
- Firebase Vertex AI SDK
- Any additional dependencies are listed in build.gradle.kts

## 📁 Dataset Notes
Public data is fetched from Unsplash API.

## 🔐 Security Notes
The google-services.json and gradle.properties are excluded from version control.  

Ensure that you add your own credentials when testing or deploying the app.
