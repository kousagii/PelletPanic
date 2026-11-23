param(
    [ValidateSet('Build', 'Run', 'Clean')]
    [string]$Task = 'Build'
)

$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

function Get-JavaTool([string]$Name) {
    if ($env:JAVA_HOME) {
        $tool = Join-Path $env:JAVA_HOME "bin/$Name.exe"
        if (!(Test-Path -LiteralPath $tool)) {
            throw "JAVA_HOME does not contain $Name.exe. Set it to a JDK directory (17 or newer)."
        }
        return $tool
    }
    $command = Get-Command "$Name.exe" -ErrorAction SilentlyContinue
    if (!$command) { throw "$Name.exe was not found. Install a JDK 17+ and set JAVA_HOME or PATH. See README.md." }
    return $command.Source
}

function Remove-BuildDirectory([string]$Name) {
    # Only these two fixed directories may be removed, inside this project.
    if ($Name -notin @('build', 'dist')) { throw 'Unexpected build directory.' }
    $target = [IO.Path]::GetFullPath((Join-Path $projectRoot $Name))
    if ([IO.Path]::GetDirectoryName($target) -ne $projectRoot) { throw "Unsafe output path: $target" }
    if (Test-Path -LiteralPath $target) {
        $item = Get-Item -LiteralPath $target -Force
        if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw "Output directory is a link: $target" }
        # Reject nested links too; output cleanup must never traverse user data.
        $links = @(Get-ChildItem -LiteralPath $target -Recurse -Force | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint })
        if ($links.Count) { throw "Output directory contains links: $target" }
        Remove-Item -LiteralPath $target -Recurse -Force
    }
}

try {
    if ($Task -eq 'Clean') {
        Remove-BuildDirectory 'build'
        Remove-BuildDirectory 'dist'
        Write-Host 'Removed build/ and dist/.'
        exit 0
    }

    $javac = Get-JavaTool 'javac'
    $jar = Get-JavaTool 'jar'
    $java = Get-JavaTool 'java'

    Remove-BuildDirectory 'build'
    Remove-BuildDirectory 'dist'
    $classes = Join-Path $projectRoot 'build/classes'
    $dist = Join-Path $projectRoot 'dist'
    New-Item -ItemType Directory -Path $classes, $dist -Force | Out-Null

    $sources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/pelletpanic') -Filter '*.java' | ForEach-Object FullName)
    & $javac --release 17 -encoding UTF-8 -g -Xlint:all -d $classes @sources
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed. A full JDK 17 or newer is required.' }
    Copy-Item -LiteralPath (Join-Path $projectRoot 'src/Images') -Destination $classes -Recurse
    Copy-Item -LiteralPath (Join-Path $projectRoot 'src/Fonts') -Destination $classes -Recurse
    Copy-Item -LiteralPath (Join-Path $projectRoot 'src/Audio') -Destination $classes -Recurse
    $gameJar = Join-Path $dist 'PelletPanic.jar'
    & $jar --create --file $gameJar --manifest (Join-Path $projectRoot 'manifest.mf') -C $classes .
    if ($LASTEXITCODE -ne 0) { throw 'JAR packaging failed.' }
    Write-Host 'Built dist/PelletPanic.jar (self-contained; no database dependencies).'

    if ($Task -eq 'Run') {
        Push-Location -LiteralPath $projectRoot
        try {
            & $java -jar $gameJar
        } finally {
            Pop-Location
        }
        if ($LASTEXITCODE -ne 0) { throw 'Game exited with an error.' }
    }
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}

