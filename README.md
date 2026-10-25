# JavaAnime

A Java desktop studio for Windows that turns a short English description into a narrated 2D cartoon scene. Animation, narration, and MP4 export run locally. No account, API key, or internet connection is needed after setup.

## Open the portable app

1. Extract **JavaAnime-1.0.0-windows-x64.zip** to a folder you can write to.
2. Open the extracted **JavaAnime** folder and double-click **JavaAnime.exe**.
3. Keep the `app` and `runtime` folders beside the executable. Java and the MP4 encoder are included.

Windows 10/11 x64 is supported. The package is unsigned.

## Make an animation

1. Type up to 500 characters, or choose an example.
2. Check the interpreted **Action**, **Setting**, **Lighting**, and **Weather**. Change them to customize the result.
3. Choose an English Windows voice, or turn **Read my text aloud** off.
4. Select **Generate animation**. Use Play/Pause, Restart, or the timeline to preview.
5. Select **Export MP4**, choose a filename, and wait for the export to finish. Cancel stops the export and keeps an existing destination file intact.

Examples:

- “A person walks through a park in the rain.”
- “A girl dances on the beach at night.”
- “A man runs through a city street.”
- “A person waves in a sunny park.”
- “A boy jumps on the beach.”

The scene uses one cartoon character and one action: walking, running, jumping, waving, or dancing. Settings are park, city, and beach, with day/night and clear/rain. Missing scenery defaults to a daytime park. Unsupported or ambiguous actions require choosing an action in the controls. This is a built-in scene system, not a general-purpose AI video generator; details such as named people, costume requests, multiple characters, and action sequences are not interpreted. Prompts are matched with English keywords and common action variations. The controls let you correct the interpretation.

Narration reads your original text. Clip length is at least five seconds and includes the entire narration followed by at least half a second of silence. Videos are 1280×720 at 24 fps with H.264 video and AAC audio. No captions or watermarks are added.

## Narration troubleshooting

JavaAnime uses Windows' installed desktop speech voices through `System.Speech`. If no English voice is listed, install an English text-to-speech voice in Windows Settings and reopen the app. Some Windows voices are available only to newer Windows speech APIs and may not appear here. The installed voices determine pronunciation and speech quality.

If speech is unavailable in a restricted environment, turn narration off to generate a silent animation. If an audio output device cannot be opened, the app previews silently and retains the narration for export. A failed narration generation is reported; the app does not silently omit requested narration.

## Build from source

From the project folder, use PowerShell:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\package.ps1
```

First build downloads a pinned Temurin JDK 21, Maven, and FFmpeg to `.tools`, verifies each archive against `scripts/toolchain.json`, and downloads pinned Maven dependencies. No system-wide installation or PATH changes persist. Later builds can use the cached tools. The app itself has no third-party Java runtime dependencies.

`build.ps1` compiles and runs the tests. `run.ps1` builds and opens the studio. `package.ps1` builds, produces a reduced Java runtime with `jlink`, and uses `jpackage` to create an app folder and ZIP in `dist`. An installer and WiX are not required. Generated packages are ignored by Git. `package.ps1 -SkipBuild` packages an already verified JAR.

Java-only commands, after bootstrap:

```powershell
.\.tools\jdk\bin\java.exe -jar .\target\JavaAnime.jar
.\.tools\jdk\bin\java.exe -Djava.awt.headless=true -jar .\target\JavaAnime.jar --render-check .\target\visual-checks
.\.tools\jdk\bin\java.exe -Djava.awt.headless=true -jar .\target\JavaAnime.jar --smoke-test .\target\samples
.\.tools\jdk\bin\java.exe -Djava.awt.headless=true -jar .\target\JavaAnime.jar --speech-check .\target\samples
```

The optional `javaanime.ffmpeg` Java property points to a different encoder executable during development. Packaged builds resolve the bundled encoder relative to the application folder, so they work from other working directories.

## Project structure

- `model`: scene choices and generated clip lifetime.
- `parser`: prompt interpretation and action-selection feedback.
- `render`: deterministic cartoon drawing shared by preview and export.
- `service`: Windows narration, helper processes, cancellation, and MP4 encoding.
- `ui`: studio interface, audio preview, and playback timeline.

Tests cover prompt interpretation, every scene combination, character movement, playback timing, MP4 codec/resolution/framerate, narration duration, cancellation, failed encoders, and invalid destinations. Encoder integration tests require the project-local FFmpeg installed by bootstrap. Rendering checks produce a scene sheet and a studio screenshot for visual inspection. `--speech-check` validates actual installed Windows voices and writes a narrated sample.

## License

JavaAnime is provided under GPL-3.0-or-later; see `LICENSE`. The portable package includes the application source ZIP. Bundled components retain their own licenses, notices, and source references; see `THIRD_PARTY_NOTICES.md`.
