param(
    [ValidateSet('voices', 'speak')][string]$Operation,
    [string]$TextPath,
    [string]$VoicePath,
    [string]$OutputPath
)
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$synthesizer = $null
try {
    Add-Type -AssemblyName System.Speech
    $synthesizer = New-Object System.Speech.Synthesis.SpeechSynthesizer
    if ($Operation -eq 'voices') {
        foreach ($installed in $synthesizer.GetInstalledVoices()) {
            if ($installed.Enabled -and $installed.VoiceInfo.Culture.Name.StartsWith('en')) {
                $encoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($installed.VoiceInfo.Name))
                [Console]::WriteLine("VOICE`t$encoded`t$($installed.VoiceInfo.Culture.Name)")
            }
        }
    } else {
        $text = [IO.File]::ReadAllText($TextPath, [Text.Encoding]::UTF8)
        $voice = [IO.File]::ReadAllText($VoicePath, [Text.Encoding]::UTF8)
        $synthesizer.SelectVoice($voice)
        $format = New-Object System.Speech.AudioFormat.SpeechAudioFormatInfo(22050, [System.Speech.AudioFormat.AudioBitsPerSample]::Sixteen, [System.Speech.AudioFormat.AudioChannel]::Mono)
        $synthesizer.SetOutputToWaveFile($OutputPath, $format)
        $synthesizer.Speak($text)
        $synthesizer.SetOutputToNull()
    }
} catch {
    [Console]::Error.WriteLine("Windows speech is unavailable: " + $_.Exception.Message)
    exit 1
} finally {
    if ($null -ne $synthesizer) { $synthesizer.Dispose() }
}
