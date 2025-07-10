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
  
## 🔑 API Key Setup

To run the app properly, you will need to set up your own API keys for the following services:

### 🔥 Firebase (Auth, Firestore, Storage, Vertex AI, App Check)
1. Go to [https://console.firebase.google.com](https://console.firebase.google.com)
2. Create a new project.
3. Add an Android app (package name must match your project’s package name, e.g., `com.example.vibefitapp`)
4. Download `google-services.json` and place it in the `/app/` directory.
5. In the Firebase Console, enable the following services:
   - **Authentication** → Sign-in method → Enable **Email/Password**
   - **Cloud Firestore** → Create a database → Start in test mode (for development)
   - **Storage** → Create storage bucket → Start in test mode
   - **App Check**:
     - Go to **App Check** > **Your Android App** > Manage Debug Tokens 
     - Copy your app’s Debug Secret printed in Logcat (from `FirebaseAppCheck.getToken()`)
     - Paste it into the Firebase App Check Console to allow debug requests
   - **Enable Firebase Vertex AI (Gemini)**:
     1. Go to: [Firebase AI Logic](https://console.firebase.google.com/project/_/ailogic)
     2. Select your app.
     3. Click Enable Vertex AI (Gemini).
     4. Ensure your Firebase project is linked to a Google Cloud Project.
     5. In [Google Cloud Console](https://console.cloud.google.com/), enable:
        - Vertex AI API
        - Ensure billing is enabled (Google offers free tier)
    You don’t need to handle API keys for Firebase AI — it uses built-in credentials.
       
---

### 🧠 Glam AI (Virtual Try-On)
1. Go to [https://glamai.com](https://glamai.com) or the official Glam AI API provider you're using.
2. Sign up or log in.
3. Get your `API_KEY` from the developer dashboard.
4. Set `VIRTUAL_TRYON_API_KEY=your_key` in `gradle.properties`.

---

### 📷 Unsplash API (Fashion Image Search)
1. Visit [https://unsplash.com/developers](https://unsplash.com/developers)
2. Register as a developer and create a new app.
3. Copy the **Access Key** (you don’t need the Secret Key).
4. Set `UNSPLASH_API_KEY=your_key` in `gradle.properties`.

---

### 🖼 ImgBB API (Upload Outfit Image)
1. Go to [https://api.imgbb.com/](https://api.imgbb.com/)
2. Sign up and get your API key.
3. Set `IMGBB_API_KEY=your_key` in `gradle.properties`.

---

## 🗂️ Project Structure
```pgsql
VibeFitApp/
├── app/
│   ├── src/
│   ├── google-services.json
│   ├── build.gradle.kts (Module:app)              
├── build.gradle.kts (project)
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

## 🔑 Password Policy
For account security, the following Firebase Authentication password policy is enforced:
✅ Enforcement Mode: Required

✅ Must include:
- At least one uppercase letter (A–Z)
- At least one lowercase letter (a–z)
- At least one numeric character (0–9)

✅ Minimum length: 8 characters

If a user attempts to register or reset a password that does not meet these criteria, the system will reject the request and prompt them to use a stronger password.

📌 Where to configure it:
In Firebase Console > Authentication > Settings > Password policy, you can manually configure and enforce these rules for production deployment.

## 📄 License
This project is licensed under the MIT License – see the [LICENSE](./LICENSE) file for details.

## 🙋‍♂️ Author
Wong Siong Yee
Final Year Project (FYP) 2025 – Multimedia University Melaka (MMU)

Feel free to open an issue or contact me on GitHub for questions.
