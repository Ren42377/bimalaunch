# SMAN7 Reference Analysis

Source of truth for migrating SMAN7 (`.refs/sman7`, package
`id.web.app.android.sman7kotabengkulu`, ~21K lines Kotlin) into the Bimalaunch native-core
architecture. SMAN7 is a Compose shell around a single embedded WebView that loads a school CBT exam
site, plus an AI-assisted auto-answer subsystem.

Key architectural finding: SMAN7 uses NO system overlay, NO MediaProjection, NO AccessibilityService.
The floating bubble is a Compose overlay inside the app window; screen reading and tapping act on the
app's own WebView via `WebViewActionBridge` (bitmap capture, `dispatchTouchEvent`, `scrollTo`,
`evaluateJavascript`). This is the natural JNI seam for the native core.

## Startup flow

`StartupDestination`: LicenseLoading -> LicenseInput -> OAuthSignIn -> Permission(MEDIA, INSTALLER)
-> WebView. License gate first (cached license or device lookup via Supabase RPC), then optional
one-time OAuth onboarding, then media + installer permissions, then WebView creation.

## Feature inventory and dependencies

Legend: WebView, Floating, Clawd (TFLite vision), License, Network, TFLite.

| # | Feature | Deps | Logic split |
|---|---|---|---|
| 1 | In-app CBT exam browser (custom UA marker) | WebView, License | URL/UA/selectors -> native config |
| 2 | Floating bubble (drag, edge-snap, discard) | Floating, WebView | geometry/snap math -> native; rendering -> Compose |
| 3 | Tools menu (Clawd/AI/Settings) | Floating | menu items -> native registry |
| 4 | AI chat (multi-session, streaming, models) | Network, License | session/state/prompt logic -> native; HTTP -> platform |
| 5 | AI attach image / capture screenshot | WebView | glue stays platform |
| 6 | AI provider auth (key + OAuth) | Network | token store/flow -> platform; policy -> native |
| 7 | AI key guide panel | Network | native dialog/dispatch |
| 8 | Clawd manual detection | Clawd, TFLite, WebView | pre/post-processing math -> native; tflite call -> JNI |
| 9 | Clawd draw-box overlay | Clawd, Floating | box math -> native; draw -> Compose |
| 10 | Clawd always-on | Clawd, TFLite | policy -> native |
| 11 | Clawd auto-play (STABLE/BALANCE/FAST) | Clawd, WebView, Network | state machine + prompts + timing -> native |
| 12 | Clawd pet mascot | Floating, WebView(SVG) | pet state machine -> native; SVG render -> platform |
| 13 | Clawd dataset labeling (YOLO export) | Clawd, storage | coordinate math -> native; file IO -> platform |
| 14 | Clawd model/runtime settings | Clawd, TFLite | config schema -> native |
| 15 | Auto-login (username/password prefill) | WebView | native config + dispatch |
| 16 | Practice mode (local HTTP mock server) | Network, WebView | server -> platform; config -> native |
| 17 | License activation/verification | License, Network | provider interface -> native (dummy now) |
| 18 | In-app tutorials/onboarding | Floating/Settings | shown-flag state -> native |
| 19 | Settings + AI prompt + model selection | License, Network | settings schema -> native |
| 20 | WhatsApp support contact | Network | dispatch action + platform intent |

## Website-specific surface to reproduce via config (not hardcode)

- Production URL: `https://cbt-sman7kotabengkulu.sch.id` (SMAN7 `WebViewController`).
- UA marker: `dkg-cbt-sekolahkita.co.id`.
- DOM selectors used by the scrape script: `[id^="soal-"]`, `.q`, `.options`, `.inneroption`, `img`.
- One coordinate-driving action interface (capture / clickAt / scrollTo / evaluateJavascript).
- Five JSBridge anti-cheat calls (kiosk toggle, emulator detect, forbidden-app detect) - pure device
  introspection, portable to native.

## Migration principle

Pure logic (geometry, state machines, detection math, decision/JSON parsing, timing policy, prompts,
settings schema, license verdict) moves into `libnativecore.so`. Platform glue (WebView, TFLite
Interpreter, SharedPreferences, MediaStore, OAuth, HTTP, local server, notifications, IME) stays in
Kotlin and is driven by native dispatch actions. No feature logic remains in Kotlin.
