# Third-party notices

## Eclipse Temurin / OpenJDK

The portable application includes a reduced Eclipse Temurin 21 runtime. OpenJDK is distributed under GPL v2 with the Classpath Exception and additional third-party notices. The runtime's `legal` directory contains its module licenses and notices. `app/notices/Temurin-NOTICE.txt` preserves the distribution notice.

- Distribution and source: https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1
- OpenJDK source: https://github.com/adoptium/jdk21u
- Exact archive and checksum: `app/notices/toolchain.json`.

## FFmpeg

The app bundles the unmodified FFmpeg 9.0.2 essentials Windows executable distributed by Gyan. That build is licensed under GPL v3 and contains additional codec libraries. Its full license and build configuration, including the external library list, are preserved in `app/notices/FFmpeg-LICENSE.txt` and `app/notices/FFmpeg-build.txt`.

- Binary build: https://www.gyan.dev/ffmpeg/builds/packages/ffmpeg-9.0.2-essentials_build.zip
- Build provider: https://www.gyan.dev/ffmpeg/builds/
- FFmpeg source revision identified by that build: https://github.com/FFmpeg/FFmpeg/commit/946fcce07b
- FFmpeg source archive: https://github.com/FFmpeg/FFmpeg/archive/946fcce07b.tar.gz
- Exact archive and checksum: `app/notices/toolchain.json`.

FFmpeg is invoked as a separate local process. The app uses its libx264 H.264 encoder and native AAC encoder. Redistribution of this third-party binary is subject to its GPL terms, including corresponding-source requirements for its included libraries; the source references here do not replace those terms.

## Build-only tools

Maven (Apache License 2.0), JUnit Jupiter (Eclipse Public License 2.0), and Maven plugins are used during development. They are not bundled into the portable application's Java runtime classpath. Their distribution notices remain in the project-local downloads and dependency cache.

## Windows speech

The app calls the computer's existing Windows PowerShell and System.Speech components. It does not redistribute Microsoft speech engines or voices. No speech data is sent to a service.
