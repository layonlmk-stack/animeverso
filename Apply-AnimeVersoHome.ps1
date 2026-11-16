[CmdletBinding()]
param(
    [string]$TargetPath = (Join-Path $env:USERPROFILE 'AndroidStudioProjects\AnimeVerso\app\src\main\java\com\example\animeverso\ui\AnimeVersoApp.kt')
)

$ErrorActionPreference = 'Stop'
$target = [IO.Path]::GetFullPath($TargetPath)
if (-not (Test-Path -LiteralPath $target -PathType Leaf)) {
    throw "AnimeVersoApp.kt não encontrado: $target"
}

$uri = 'https://raw.githubusercontent.com/layonlmk-stack/animeverso/main/AnimeVersoApp.kt'
$response = Invoke-WebRequest -Uri $uri -UseBasicParsing
if ($response.StatusCode -ne 200 -or $response.Content -notmatch 'MangaHero' -or $response.Content -notmatch 'ANIMEVERSO') {
    throw 'Não foi possível validar o arquivo atualizado baixado do GitHub.'
}

$backup = "$target.backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
Copy-Item -LiteralPath $target -Destination $backup
[IO.File]::WriteAllText($target, $response.Content, [Text.UTF8Encoding]::new($false))
Write-Host "Tela inicial atualizada em: $target"
Write-Host "Backup salvo em: $backup"
