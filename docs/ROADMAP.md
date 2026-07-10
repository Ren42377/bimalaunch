# Bimalaunch Roadmap

Maps the SMAN7 feature set (see `SMAN7_ANALYSIS.md`) onto native-core milestones. Each milestone keeps
the same four-function JNI surface; only native logic and platform adapters grow.

## Milestone 1 (this build) - Foundation + proof - DONE

- Gradle + Compose + NDK/CMake project, namespaced `com.ren42377.bimalaunch`.
- `libnativecore.so` with initialize / getManifest / dispatch / shutdown.
- Feature registry, dispatcher, dummy license (`developer`), website config parser.
- Native loader (assets -> internal -> read-only -> load, with packaged fallback).
- Generic Compose renderer driven entirely by the native manifest.
- GitHub Actions debug build compiling the native library.

## Milestone 2 - WebView feature end-to-end

- WebView screen driven by native website config (URL, UA, headers, cookie policy, redirect rules).
- Native script manager: injection scripts served from native, not Kotlin string constants.
- JSBridge anti-cheat (emulator/forbidden-app/kiosk) with logic in native, thin JNI callbacks.
- WebViewActionBridge seam (capture / clickAt / scrollTo / evaluateJavascript) exposed to native.

## Milestone 3 - Floating bubble

- Floating service + in-app host in Kotlin; menu items, geometry, snap/dock, discard, pet state
  machine all computed in native and returned as render/animation state.

## Milestone 4 - Clawd vision + auto-play

- Native detection pre/post-processing (letterbox, decode, NMS, dedup, IoU, quantization).
- TFLite Interpreter call remains platform (or tflite C API inside the .so later).
- Auto-play STABLE/BALANCE/FAST state machine, decision/JSON parsing, timing policy, prompts in native.
- Dataset labeling coordinate math in native; file IO platform.

## Milestone 5 - AI chat + providers + settings + practice

- AI session/state/prompt logic and settings schema in native; HTTP, OAuth token stores, and the
  practice local server as platform adapters driven by dispatch.

## Milestone 6 - Hardening

- String encryption, control-flow split, resource separation, obfuscation preparation.
- Real license backend behind the existing `LicenseProvider` interface (swap dummy for network).
- Native loader switched to backend download source without architecture change.
