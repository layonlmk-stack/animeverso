[CmdletBinding()]
param(
    [string]$TargetPath = (Join-Path $env:USERPROFILE 'AndroidStudioProjects\AnimeVerso\app\src\main\java\com\example\animeverso\audio\AnimeNarrator.kt')
)

$ErrorActionPreference = 'Stop'
$path = [IO.Path]::GetFullPath($TargetPath)
if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
    throw "AnimeNarrator.kt não encontrado: $path"
}

$content = [IO.File]::ReadAllText($path)
if ($content.Contains('val stopPlayback: () -> Unit = {')) {
    Write-Host 'O ajuste já está aplicado.'
    exit 0
}

$pattern = '(?s)        val stopPlayback = \{\r?\n            runCatching \{ player\?\.stop\(\) \}\r?\n            player\?\.release\(\)\r?\n            player = null\r?\n            runCatching \{ tts\.stop\(\) \}\r?\n        \}'
$replacement = @'
        val stopPlayback: () -> Unit = {
            runCatching { player?.stop() }
            player?.release()
            player = null
            runCatching { tts.stop() }
            Unit
        }
'@
$updated = [regex]::Replace($content, $pattern, $replacement, 1)
if ($updated -ceq $content) {
    throw 'O trecho esperado não foi encontrado. Nenhum arquivo foi alterado.'
}

$backup = "$path.backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
Copy-Item -LiteralPath $path -Destination $backup
[IO.File]::WriteAllText($path, $updated, [Text.UTF8Encoding]::new($false))
Write-Host "Correção aplicada em: $path"
Write-Host "Backup salvo em: $backup"

