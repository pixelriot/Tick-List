# Tick List
Create and share your shopping and other lists with your family and friends.

## Development

### Prerequisites
- Node.js
- Rust (for Tauri desktop and mobile apps)
- **Android Development**: Android Studio with SDK and NDK
- **iOS Development**: Xcode (macOS only)

### Setup
```bash
npm install
```

### Development Commands

#### Web Development
```bash
# Start development server
npm run dev

# Preview production build
npm run preview
```

#### Desktop App (Tauri)
```bash
# Start Tauri development
npm run tauri dev

# Build Tauri app for current platform
npm run tauri build

# Build for specific targets (examples)
npm run tauri build -- --target x86_64-pc-windows-msvc  # Windows
npm run tauri build -- --target x86_64-apple-darwin     # macOS
npm run tauri build -- --target x86_64-unknown-linux-gnu # Linux
```

#### Mobile App (Tauri)
```bash
# Initialize Android development (run once)
npm run tauri android init

# Initialize iOS development (run once, macOS only)
npm run tauri ios init

# Start Android development on connected device
npm run tauri android dev

# Start iOS development on connected device (macOS only)
npm run tauri ios dev

# Build Android APK
npm run tauri android build

# Build iOS app (macOS only)
npm run tauri ios build
```

##### Mobile Prerequisites
- **Android**: Android Studio with SDK and NDK installed
- **iOS**: Xcode (macOS only)
- USB debugging enabled on device
- Device connected via USB

##### Mobile Debugging
- **Web Debugging**: Use Chrome DevTools at `chrome://inspect/#devices`
- **Native Debugging**: Open `src-tauri/gen/android` or `src-tauri/gen/ios` in Android Studio/Xcode
- **Hot Reload**: Changes automatically update on device during development

#### Code Quality
```bash
# Type checking and linting
npm run check

# Watch mode for type checking
npm run check:watch

# Lint code
npm run lint

# Format code
npm run format
```

#### Build Commands
```bash
# Build for web deployment
npm run build
```
