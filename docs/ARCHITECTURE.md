# Bimalaunch Architecture

Bimalaunch is an Android host over a native C++ core. The APK renders whatever the native core
describes; it does not know how any feature works.

```
Compose (generic renderer)
  -> Kotlin Host (NativeBridge / JNI)
    -> libnativecore.so
      -> business logic (Registry, Dispatcher, License, WebsiteConfig)
```

## JNI surface

Exactly four native functions live in `NativeBridge`. There is no per-feature JNI function; every
feature is expressed as JSON passed through `dispatch`.

| Function | Direction | Purpose |
|---|---|---|
| `nativeInitialize(configJson): String` | host -> core | Build registry, license, website config from config JSON. Returns status JSON. |
| `nativeGetManifest(): String` | host <- core | Return the UI manifest JSON. |
| `nativeDispatch(action, payloadJson): String` | host -> core | Route an action, return a result JSON describing an effect. |
| `nativeShutdown()` | host -> core | Release core state. |

## Native loader flow

`NativeLoader.ensureLoaded`:

```
assets/native/<abi>/libnativecore.so  (if present)
  -> copy to filesDir/libnativecore.so
    -> setReadOnly()
      -> System.load(absolutePath)
```

When the asset is absent (default this milestone) it falls back to `System.loadLibrary("nativecore")`
from the packaged jniLibs. The asset path exists so a future version can swap the source file for a
backend download without changing the architecture.

## Config JSON schema (input to initialize)

```json
{
  "appName": "Bimalaunch",
  "licenseMode": "dummy",
  "website": {
    "id": "example",
    "loginUrl": "https://example.com/login",
    "userAgent": "bimalaunch-host",
    "selectors": { "question": "...", "options": "..." },
    "scripts": []
  }
}
```

No domain, selector, or script is hardcoded in the APK. A new website is added by editing config only.

## Manifest JSON schema (output of getManifest)

```json
{
  "version": 1,
  "appName": "Bimalaunch",
  "website": { "id": "example", "loginUrl": "...", "configured": true },
  "root": {
    "id": "main",
    "component": "menu",
    "title": "Bimalaunch",
    "subtitle": "example",
    "children": [
      { "id": "webview", "component": "card", "title": "Buka Ujian",
        "subtitle": "...", "action": "open_webview", "props": { "available": true } }
    ]
  }
}
```

The renderer understands component types (`menu`, `card`, `button`, `dialog`, `loading`, `error`),
not feature names. Tapping a node calls `dispatch(node.action)`.

## Dispatch result schema

```json
{ "ok": true, "effect": "navigate|dialog|toast|error", "target": "...",
  "title": "...", "message": "...", "errorType": "...", "data": { } }
```

## Action catalog (milestone 1)

| Action | Effect |
|---|---|
| `open_webview` | navigate to `webview` with website config data (stub target) |
| `open_settings` | navigate to `settings` (stub target) |
| `open_clawd` / `open_ai` / `open_floating` | dialog "segera hadir" (migration pending) |
| `license_info` | dialog with license mode |
| `verify_license` | navigate on `developer`, else error |

## Security build flags (CMake)

C++20, `-O3`, `-flto`, strip (`-Wl,-s`) on release, `-fvisibility=hidden`,
`-fno-rtti`, `-fno-exceptions`, `--gc-sections`. nlohmann/json compiled with `JSON_NOEXCEPTION`.
String encryption and control-flow obfuscation are deferred to a hardening milestone.
