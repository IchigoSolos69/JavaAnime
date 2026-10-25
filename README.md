# JavaAnime

JavaAnime is an offline Java desktop application for Windows that turns a short English description into a narrated 2D cartoon animation. Describe a scene, adjust its settings, preview it, and export an MP4 video.

**Version:** 1.0.0 | **Platform:** Windows 10/11 x64 | **Java:** 21 | **License:** GPL-3.0-or-later

The application uses built-in cartoon drawings and keyword-based prompt interpretation. Animation generation requires no AI model, account, API key, subscription, or internet connection. Internet access is needed for the initial source-build downloads.

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Run the portable app](#run-the-portable-app)
- [Create an animation](#create-an-animation)
- [Supported prompts and limitations](#supported-prompts-and-limitations)
- [Video and audio specifications](#video-and-audio-specifications)
- [Build and run from source](#build-and-run-from-source)
- [Tests and diagnostics](#tests-and-diagnostics)
- [Package the Windows app](#package-the-windows-app)
- [Configuration and tool discovery](#configuration-and-tool-discovery)
- [Architecture and project structure](#architecture-and-project-structure)
- [Local data and cleanup](#local-data-and-cleanup)
- [Troubleshooting](#troubleshooting)
- [Development notes](#development-notes)
- [License and bundled components](#license-and-bundled-components)

## Features

- One cartoon character performing walking, running, jumping, waving, or dancing.
- Park, city, and beach backgrounds with day/night lighting and optional rain.
- English text input with a 500-character limit and example prompts.
- Editable action, setting, lighting, and weather controls.
- Optional narration using an installed English Windows desktop speech voice.
- Preview with Play/Pause, Restart, timeline seeking, and elapsed/total time.
- MP4 export at 1280 × 720 pixels and 24 frames per second.
- Background generation/export tasks, progress feedback, and cancellation.
- A portable Windows executable with its own Java runtime and video encoder.

## Requirements

| Requirement | Portable app | Source build |
| --- | --- | --- |
| Operating system | Windows 10/11 x64 | Windows 10/11 x64 |
| Java | Included; no separate installation needed | JDK 21 downloaded into `.tools` |
| Build tools | Not required | Windows PowerShell, `curl.exe`, and `tar.exe` available to the scripts |
| Video encoder | Bundled FFmpeg | Downloaded and verified by bootstrap |
| Narration | Windows PowerShell and an English `System.Speech` desktop voice | Same requirement for real speech checks |
| Audio output | Needed to hear narrated previews | Needed for interactive audio preview |
| Internet | Not required by the app | Needed initially for tools and Maven dependencies |
| File access | Writable temporary and export folders | Writable project, temporary, and output folders |

The studio opens at 1250 × 860 with a minimum window size of 1080 × 760. The controls scroll vertically when space is limited. The project does not specify measured minimum CPU, RAM, or free-disk requirements; export speed depends on the computer and clip length.

## Run the portable app

1. Obtain `JavaAnime-1.0.0-windows-x64.zip` from a generated `dist` folder or a supplied distribution.
2. Extract the ZIP completely to a folder on your computer.
3. Open the extracted `JavaAnime` folder and double-click **JavaAnime.exe**.
4. Keep the executable, `app`, and `runtime` folders together.

The package is an unsigned portable application. It does not use an installation wizard or create a Start menu shortcut. Java and FFmpeg are included.

Build artifacts are ignored by Git, so a fresh source checkout may not contain `dist`. Follow [Package the Windows app](#package-the-windows-app) to generate the ZIP locally.

## Create an animation

1. **Describe the scene.** Type a short English description or choose an example. The counter shows the 500-character limit; edits or pastes that exceed it are rejected.
2. **Review the interpretation.** The app updates Action, Setting, Lighting, and Weather after typing pauses. Adjust these controls as needed.
3. **Choose narration.** Leave **Read my text aloud** enabled and select a voice, or turn it off for a silent clip.
4. **Generate.** Select **Generate animation**. If the prompt has no recognized action or several supported actions, choose one action manually before generating.
5. **Preview.** Playback starts after generation. Use Play/Pause, Restart, or the timeline slider.
6. **Export.** Select **Export MP4** and choose a filename. The app adds `.mp4` when missing and asks before replacing an existing destination.

Export saves the **last generated clip**. After changing the prompt, scene controls, voice, or narration toggle, generate again to apply those changes.

Narration reads the entered text. Changing scene controls changes the drawing rather than rewriting the narration. Silent clips last five seconds. Narrated clips last at least five seconds and include the full speech followed by at least half a second of silence.

## Supported prompts and limitations

### Examples

| Prompt | Action | Setting | Lighting | Weather |
| --- | --- | --- | --- | --- |
| A person walks through a park in the rain. | Walking | Park | Day | Rain |
| A girl dances on the beach at night. | Dancing | Beach | Night | Clear |
| A man runs through a city street. | Running | City | Day | Clear |
| A person waves in a sunny park. | Waving | Park | Day | Clear |
| A boy jumps on the beach. | Jumping | Beach | Day | Clear |

All prompts use the same built-in cartoon character. Words such as “girl,” “man,” or “boy” do not select a different character model.

### Action vocabulary

Matching is case-insensitive and uses whole words.

| Action | Recognized words |
| --- | --- |
| Walking | `walk`, `walks`, `walking`, `walked`, `stroll`, `strolls`, `strolling` |
| Running | `run`, `runs`, `running`, `ran`, `jog`, `jogs`, `jogging` |
| Jumping | `jump`, `jumps`, `jumping`, `jumped`, `hop`, `hops`, `hopping` |
| Waving | `wave`, `waves`, `waving`, `waved` |
| Dancing | `dance`, `dances`, `dancing`, `danced` |

### Scenery and defaults

| Choice | Interpretation |
| --- | --- |
| Beach | `beach`, `ocean`, `seaside`, or `shore` |
| City | `city`, `street`, `town`, or `urban`, when no beach keyword is present |
| Park | Default when neither beach nor city keywords are present |
| Night | `night`, `nighttime`, `moon`, `midnight`, or `evening` |
| Day | Default when no night keyword is present |
| Rain | `rain`, `rains`, `raining`, `rainy`, `rainfall`, or `storm` |
| Clear | Default; `no rain`, `without rain`, and the corresponding `rainfall` phrases suppress rain |

The parser has limited handling for negated actions and “ocean waves” as scenery. It is a small rule-based parser, so natural-language descriptions can be misinterpreted. Use the controls to correct the scene.

### Current limits

- One character, one action, and one setting per clip.
- English keyword interpretation and English Windows speech voices only.
- No arbitrary scenes, AI-generated artwork, custom characters, costumes, uploaded assets, or camera instructions.
- No multi-scene stories or action sequences; several recognized actions require manual selection.
- No lip sync, subtitles, captions, background music, or audio-file import.
- No project save/load, editable timeline tracks, GIF export, or configurable export resolution/frame rate.
- No captions or watermarks are added to the exported scene.

## Video and audio specifications

| Property | Value |
| --- | --- |
| Container | MP4 |
| Resolution | 1280 × 720 pixels, 16:9 |
| Frame rate | 24 fps |
| Video encoder | FFmpeg `libx264`, H.264 |
| Video settings | `veryfast` preset, CRF 20, `yuv420p` pixel format |
| Narration source | Installed Windows `System.Speech` desktop voice |
| Intermediate audio | WAV, 22,050 Hz, 16-bit PCM, mono |
| Exported narration | AAC, requested bitrate 128 kbit/s |
| Silent export | Video only, without an audio track |
| MP4 layout | `+faststart` enabled |

Duration is rounded up to a whole video frame:

```text
durationSeconds = ceil(max(5, speechSeconds + 0.5) × 24) / 24
```

Audio is padded with silence to the end of the clip. Preview and export share the same renderer and time-based animation logic. Frames stream to FFmpeg instead of retaining the entire animation in memory.

Export first writes a temporary `.javaanime-*.mp4` file in the destination folder. Only a successful encode is published to the chosen filename. Cancellation or encoder failure preserves an existing destination, and cleanup retries briefly when Windows retains a temporary-file handle. Publishing attempts an atomic move, with a regular replacement move when atomic moves are unsupported.

## Build and run from source

Run these commands from the project root in PowerShell. Keep paths containing spaces quoted.

### Build and test

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
```

This bootstraps the tools, compiles the app, runs the tests, and produces `target/JavaAnime.jar`.

### Open the development app

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1
```

This builds first, then launches the studio with project-local Java and an explicit FFmpeg path.

### Bootstrap tools only

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\bootstrap.ps1
```

Bootstrap downloads the versions in [scripts/toolchain.json](scripts/toolchain.json), checks the archive hashes, and extracts the tools under `.tools`. Verified tools are reused. Maven dependencies are cached under `.tools/m2`. The scripts do not install tools system-wide or permanently change system PATH or execution policy.

| Tool or dependency | Pinned version | Purpose |
| --- | --- | --- |
| Eclipse Temurin JDK | 21.0.12.1+1 | Compile, run, link a runtime, and package the launcher |
| Apache Maven | 3.9.11 | Build and dependency management |
| FFmpeg essentials Windows build | 9.0.2 | MP4 encoding; the development download also supplies `ffprobe` for tests |
| JUnit Jupiter | 5.11.4 | Test-only dependency |
| Maven Compiler Plugin | 3.14.0 | Java 21 compilation |
| Maven Surefire Plugin | 3.5.3 | Test execution |
| Maven JAR Plugin | 3.4.2 | Executable JAR and main-class manifest |

Tool URLs and SHA-256/SHA-512 hashes are in `scripts/toolchain.json`. Java dependencies and plugin versions are in [pom.xml](pom.xml). The app has no third-party Java runtime dependencies. FFmpeg is a separate executable, and speech uses Windows components.

### Script options

| Script | Default operation | Optional switch |
| --- | --- | --- |
| `bootstrap.ps1` | Download, verify, and extract missing tools | None |
| `build.ps1` | Compile, test, and package the JAR | `-SkipTests` skips test execution |
| `run.ps1` | Build with tests, then open the studio | None |
| `package.ps1` | Build with tests, then create portable/source ZIPs | `-SkipBuild` uses an existing JAR |

Use the defaults for a verified distribution. The skip switches do not establish that an existing build is current or tested.

To launch an already built JAR from the project root:

```powershell
& .\.tools\jdk\bin\java.exe -jar .\target\JavaAnime.jar
```

## Tests and diagnostics

### Automated tests

The suite contains **32 test cases**, including parameterized prompt examples. Tests run in headless mode.

| Test class | Coverage |
| --- | --- |
| `PromptParserTest` | Supported words, defaults, ambiguous/unsupported actions, empty/long input, word boundaries, negation, and ocean-wave handling |
| `SceneRendererTest` | All 60 action/setting/lighting/weather combinations, movement, and deterministic frames |
| `AnimationClipTest` | Minimum duration, narration tail, frame alignment, and invalid durations |
| `VideoExporterTest` | Playable H.264 MP4, resolution/frame rate, AAC audio and duration, cancellation, failed encoders, invalid destinations, and preservation of existing output |
| `MainPanelTest` | Silent generation, playback/restart, manual action selection, input limit, and Generate-button visibility at the minimum window size |
| `PlaybackTimelineTest` | Pause, resume, seeking, duration bounds, and restart |

Video integration tests use project-local FFmpeg and its sibling `ffprobe.exe`; bootstrap these tools before running the complete suite. They create temporary media and decode exported files to check validity. Real Windows speech and audible playback are separate from the headless tests.

For a test-only Maven run after bootstrapping:

```powershell
. .\scripts\bootstrap.ps1
& .\.tools\maven\bin\mvn.cmd '-Dmaven.repo.local=.tools/m2' test
```

Reports are written under `target/surefire-reports`. The existing implementation was verified with all 32 cases passing and a portable-app narrated export; rerun the relevant checks after code or toolchain changes.

### Visual and media checks

These diagnostics use a fixed sample scene; they are not a general command-line prompt-to-video interface. Each option takes exactly one output-folder argument. They create that folder and may replace their fixed-name outputs.

```powershell
& .\.tools\jdk\bin\java.exe '-Djava.awt.headless=true' -jar .\target\JavaAnime.jar --render-check .\target\visual-checks
& .\.tools\jdk\bin\java.exe '-Djava.awt.headless=true' -jar .\target\JavaAnime.jar --smoke-test .\target\samples
& .\.tools\jdk\bin\java.exe '-Djava.awt.headless=true' -jar .\target\JavaAnime.jar --speech-check .\target\samples
```

| Option | Outputs | Additional requirement |
| --- | --- | --- |
| `--render-check` | `scene-check.png`, `studio-check.png`, `JavaAnime.ico` | No FFmpeg or Windows voice needed |
| `--smoke-test` | Five-second `sample-silent.mp4` | FFmpeg |
| `--speech-check` | Voice listing, `narration.wav`, and `sample-narrated.mp4` using the first available English voice | FFmpeg and a working Windows desktop speech voice |

Screenshots and sample media are generated under `target`, not tracked in the repository. Inspect the PNGs after artwork or layout changes. The implementation's portable launcher was also checked from an extracted folder with Java absent from its environment and an unrelated working directory.

## Package the Windows app

Run packaging on Windows:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\package.ps1
```

To package a previously built and verified JAR:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\package.ps1 -SkipBuild
```

The script generates visual-check assets and an icon, creates a reduced runtime with `jlink`, and uses `jpackage --type app-image` to create the launcher. The runtime includes `java.desktop`, `java.logging`, and their required dependencies. The launcher sets a 512 MB maximum Java heap and resolves app resources relative to its own folder. FFmpeg runs in a separate process.

| Output | Location |
| --- | --- |
| Portable Windows ZIP | `dist/JavaAnime-1.0.0-windows-x64.zip` |
| Application source ZIP | `dist/JavaAnime-1.0.0-source.zip` |
| Uncompressed app | `target/portable-<timestamp>/output/JavaAnime/` |
| Executable JAR | `target/JavaAnime.jar` |

The extracted portable package contains:

```text
JavaAnime/
├── JavaAnime.exe
├── app/
│   ├── JavaAnime.jar
│   ├── JavaAnime.cfg
│   ├── tools/ffmpeg.exe
│   └── notices/
│       ├── FFmpeg-LICENSE.txt
│       ├── FFmpeg-build.txt
│       ├── Temurin-NOTICE.txt
│       └── toolchain.json
├── runtime/                    # Reduced Java runtime and its legal notices
├── JavaAnime-source.zip        # App source, tests, and build scripts
├── README.md
├── LICENSE
└── THIRD_PARTY_NOTICES.md
```

No installer, WiX setup, or code-signing step is included. The scripts provide a repeatable packaging procedure, not a guarantee of byte-for-byte identical ZIPs. Editing the source README does not update existing distribution ZIPs; rerun packaging to include documentation changes.

## Configuration and tool discovery

The UI supplies scene and voice settings. There is no persistent preferences file, user-facing configuration format, HTTP server, or public web API.

Developers can set these Java system properties **before** `-jar`:

| Property | Purpose |
| --- | --- |
| `javaanime.ffmpeg` | Explicit path to an existing encoder executable |
| `javaanime.appDir` | App directory containing `tools/ffmpeg.exe`; set automatically by the portable launcher |
| `java.awt.headless` | Set to `true` for diagnostics/tests; omit for the interactive studio |

FFmpeg discovery checks an existing `javaanime.ffmpeg` file, then `javaanime.appDir/tools/ffmpeg.exe`, then `tools/ffmpeg.exe` and `.tools/ffmpeg/bin/ffmpeg.exe` relative to the working directory. It does not search PATH. An invalid override file is skipped in favor of the remaining locations.

Example with an explicit encoder:

```powershell
& .\.tools\jdk\bin\java.exe '-Djavaanime.ffmpeg=C:\Tools\ffmpeg\bin\ffmpeg.exe' -jar .\target\JavaAnime.jar
```

Speech uses `%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe`, falling back to `powershell.exe` when `SystemRoot` is unavailable. Its fixed helper script is embedded in the JAR and extracted for speech operations.

## Architecture and project structure

```text
English prompt → PromptParser → editable controls → SceneSpec
                                                       │
                                           ClipGenerator + SpeechService
                                                       │
                                                 AnimationClip
                                                  /         \
                         PreviewPlayer + SceneRenderer     VideoExporter
                                       │                       │
                              Swing animation + audio     FFmpeg → MP4
```

| Component | Responsibility |
| --- | --- |
| `Main` | Starts the studio or dispatches diagnostic arguments |
| `MainPanel` | Prompt editing, scene controls, asynchronous generation/export, and feedback |
| `PromptParser` | Validates text and detects actions, setting, lighting, weather, and selection guidance |
| `SceneSpec` | Immutable action, setting, lighting, and weather record |
| `AnimationClip` | Prompt/scene, duration, optional narration path, frame count, and temporary workspace lifetime |
| `SceneRenderer` | Java2D cartoon drawing and movement as a function of scene and time |
| `ClipGenerator` | Creates a workspace, optional narration, and clip duration |
| `SpeechService` / `speech.ps1` | Discovers enabled English voices and synthesizes WAV through `System.Speech` |
| `PreviewPlayer` / `PlaybackTimeline` | Audio playback, timing, pause/resume, seeking, and restart |
| `VideoExporter` | Streams BGR frames to FFmpeg, reports progress, and publishes the finished MP4 |
| `ProcessRunner` / `Cancellation` | Helper-process timeouts, bounded output capture, and cancellation |
| `ToolPaths` | Resolves FFmpeg and Windows PowerShell |
| `Theme` / `AppIcon` / `PreviewCanvas` | Styling, app icon, and scaled scene preview |
| `Diagnostics` | Visual checks and fixed-scene media checks |

```text
JavaAnime/
├── pom.xml
├── README.md
├── LICENSE
├── THIRD_PARTY_NOTICES.md
├── scripts/
│   ├── bootstrap.ps1
│   ├── build.ps1
│   ├── run.ps1
│   ├── package.ps1
│   └── toolchain.json
├── src/
│   ├── main/
│   │   ├── java/com/javaanime/
│   │   │   ├── Main.java
│   │   │   ├── Diagnostics.java
│   │   │   ├── model/
│   │   │   ├── parser/
│   │   │   ├── render/
│   │   │   ├── service/
│   │   │   └── ui/
│   │   └── resources/speech.ps1
│   └── test/java/com/javaanime/
├── .tools/                     # Ignored: tool downloads and Maven cache
├── target/                     # Ignored: build, tests, checks, and package staging
└── dist/                       # Ignored: distribution ZIPs
```

Voice discovery, generation, and export use `SwingWorker` tasks. Swing updates stay on the UI thread, and a timer refreshes the preview at approximately 24 fps. During narrated playback, audio position keeps the timeline aligned. Helper output is bounded to avoid unbounded log buffering.

## Local data and cleanup

- Prompt interpretation, drawing, and narration run locally. The app does not make network requests to generate content.
- Speech text and the voice name are passed as UTF-8 files, not inserted into executable PowerShell command text.
- Voice-discovery folders use a `javaanime-voices-` prefix; clip workspaces use `javaanime-scene-`. Both are created in Java's temporary directory.
- Narrated clip workspaces contain the helper script, prompt/voice text files, and `narration.wav` while needed.
- Temporary workspaces are cleaned up when operations finish or fail, a clip is replaced, or the app closes normally. A crash, forced termination, or Windows file lock can leave temporary files behind.
- Exported MP4s remain at the destination selected by the user. Diagnostic media remains in its requested output folder.
- Prompts, scene settings, and preferences are not saved as reusable projects or session history.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| No English voice is listed | Add an English text-to-speech voice in Windows Settings and reopen the app. Only enabled desktop voices exposed through `System.Speech` appear; some newer Windows voices may not appear. |
| Narration fails | Try another listed voice, check Windows PowerShell and `System.Speech`, or turn **Read my text aloud** off. Requested narration failures are reported rather than silently omitted. |
| Preview has no sound | Check the Windows audio output and volume. If an audio device cannot be opened, the app reports this and can preview silently while keeping narration for export. |
| The action is missing or ambiguous | Choose a supported action manually, then regenerate. Use the vocabulary above. |
| A recent edit is absent from the video | Generate again before exporting; export uses the last generated clip. |
| MP4 encoder is missing | Extract the whole portable ZIP, preserving `app/tools/ffmpeg.exe`. For source builds, run `build.ps1` or set an explicit encoder property. A PATH installation alone is not used. |
| Export fails or cannot save | Choose an existing writable folder and valid filename. Check disk space, programs locking the destination, and whether the encoder can run. |
| A helper takes too long | Voice discovery allows 30 seconds, speech synthesis two minutes, and the encoder process ten minutes. Shorten the prompt or investigate the relevant voice/encoder problem. |
| Java or Maven is not found | Use the supplied scripts to bootstrap project-local tools; a system `java` or `mvn` command is not required. |
| A download fails | Check internet access and the URLs in `scripts/toolchain.json`, then rerun bootstrap. Partial downloads use a `.part` suffix. |
| Checksum mismatch | Remove only the archive named in the error and download again. Do not disable validation to accept an unexpected archive. |
| Unverified tool or extraction directory | Rename the exact project-local directory named by bootstrap and rerun. Existing directories without matching verification markers are not overwritten automatically. |
| Packaging reports an unwritable folder | Check project permissions and whether a sandbox or security policy restricts Java packaging tools. Run packaging in a normal writable Windows development environment. |
| Controls do not all fit | Scroll the left-hand controls or enlarge the window. Generate stays in the fixed bottom area. |

## Development notes

- Keep code compatible with the Java 21 target in `pom.xml`.
- New scene choices require updating the relevant `SceneSpec` enum, prompt interpretation where applicable, renderer behavior, and tests. Choice controls populate from the enums.
- Keep preview and export on the shared renderer so their animation behavior stays consistent.
- Run real-media tests after export or timing changes, and inspect visual-check PNGs after UI or artwork changes.
- Run `--speech-check` after speech changes. Headless tests alone do not validate installed speech engines or audio hardware.
- Tool-version changes require updating URLs and trusted archive hashes in `scripts/toolchain.json`, rebuilding, and checking packaging. Keep bundled-component notices consistent with the downloads.
- No CI workflow or automated GUI screenshot comparison is configured in this repository.

## License and bundled components

JavaAnime is provided under **GPL-3.0-or-later**; see [LICENSE](LICENSE). The portable package includes the app source, tests, and build scripts in `JavaAnime-source.zip`.

Temurin/OpenJDK, FFmpeg, and build-only dependencies retain their own licenses and notices. Windows speech engines and voices belong to the user's Windows installation and are not redistributed by JavaAnime. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for component-license details and source references. Runtime legal files and packaged `app/notices` files preserve the distribution notices.
