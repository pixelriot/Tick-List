# Tick List
Create and share your shopping and other lists with your family and friends. A minimalistic, mobile app build with Svelte, Tauri and shadcn-svelte. Syncs with your Supabase database.

<img width="300" height="610" alt="image" src="https://github.com/user-attachments/assets/f18f1077-8153-447a-a066-08ff4e2f343b" />
<img width="300" height="610" alt="image" src="https://github.com/user-attachments/assets/05cd2402-5875-4058-a57e-c42eefee9984" />
<img width="300" height="610" alt="image" src="https://github.com/user-attachments/assets/d0ca4ca7-35a9-4b39-a633-6c1941f21e48" />

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

### Supabase Sync Setup
1. Create a Supabase project and grab the URL + anon key.
2. Copy `.env.example` to `.env` and set:
   - `PUBLIC_SUPABASE_URL`
   - `PUBLIC_SUPABASE_ANON_KEY`
3. Run the SQL in `src/lib/supabase/schema.sql` inside the Supabase SQL editor.
4. Enable Realtime for `list_items` tables.

### Supabase Schema Notes
- `lists` stores the list metadata; `list_items` stores the items; `list_shares` maps share codes to list ids.
- `deleted_at` is used for soft deletes so clients can reconcile changes reliably.
- `updated_at` is used for last-write-wins conflict resolution (latest timestamp wins).
- The `touch_updated_at` trigger updates `updated_at` on every update for consistent server timestamps.
- Indexes on `list_id`, `updated_at`, and `share_code` keep list sync and share lookups fast.
- Row Level Security (RLS) is enabled on all Supabase tables and policies restrict list/item access to actively shared lists.
- If your project already has the tables, re-run `src/lib/supabase/schema.sql` (or apply an equivalent migration) to add RLS policies.

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
