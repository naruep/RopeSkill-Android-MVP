param(
    [Parameter(Mandatory = $true)]
    [string]$SigningProperties
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repositoryRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repositoryRoot

$resolvedProperties = (Resolve-Path $SigningProperties).Path
$repositoryPrefix = [IO.Path]::GetFullPath($repositoryRoot).TrimEnd('\') + '\'
if ($resolvedProperties.StartsWith($repositoryPrefix, [StringComparison]::OrdinalIgnoreCase)) {
    throw "Signing properties must be stored outside the Git repository."
}

if (-not $env:JAVA_HOME) {
    $androidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
    if (-not (Test-Path $androidStudioJbr)) {
        throw "JAVA_HOME is not set and the Android Studio JBR was not found."
    }
    $env:JAVA_HOME = $androidStudioJbr
}
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:ROPESKILL_SIGNING_PROPERTIES = $resolvedProperties

$trackedSecrets = @(
    & git ls-files -- "*.jks" "*.keystore" "*.p12" "*.pfx" "keystore.properties" "release-signing.properties"
)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to inspect tracked files."
}
if ($trackedSecrets.Count -gt 0) {
    throw "A signing secret appears to be tracked by Git. Stop and remove it before packaging."
}

& .\gradlew.bat clean packageProductionRelease
if ($LASTEXITCODE -ne 0) {
    throw "Production Gradle packaging failed."
}

$apk = Get-Item ".\app\build\outputs\apk\release\app-release.apk"
$aab = Get-Item ".\app\build\outputs\bundle\release\app-release.aab"

$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$buildTools = Get-ChildItem "$sdk\build-tools" -Directory |
    Sort-Object { [version]($_.Name -replace '-.*$', '') } -Descending |
    Select-Object -First 1
if (-not $buildTools) {
    throw "Android SDK Build Tools were not found."
}

$apkSigner = Join-Path $buildTools.FullName "apksigner.bat"
$aapt = Join-Path $buildTools.FullName "aapt.exe"
$jarSigner = Join-Path $env:JAVA_HOME "bin\jarsigner.exe"
$keyTool = Join-Path $env:JAVA_HOME "bin\keytool.exe"

& $apkSigner verify --verbose --print-certs $apk.FullName
if ($LASTEXITCODE -ne 0) {
    throw "APK signature verification failed."
}

& $jarSigner -verify $aab.FullName
if ($LASTEXITCODE -ne 0) {
    throw "AAB signature verification failed."
}

& $keyTool -printcert -jarfile $aab.FullName
if ($LASTEXITCODE -ne 0) {
    throw "Unable to read the AAB signing certificate."
}

$badging = @(& $aapt dump badging $apk.FullName)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to inspect the APK package metadata."
}
$packageLine = $badging | Select-String "^package:"
if (-not $packageLine) {
    throw "APK package/version metadata was not found."
}

$manifestTree = @(& $aapt dump xmltree $apk.FullName AndroidManifest.xml)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to inspect the APK manifest."
}
if (-not ($manifestTree | Select-String "android:allowBackup.*0x0")) {
    throw "Release APK does not contain allowBackup=false."
}
if ($manifestTree | Select-String "android:debuggable|android:testOnly") {
    throw "Release APK unexpectedly contains a debuggable or test-only attribute."
}

Write-Host ""
Write-Host "Package metadata:"
$packageLine

Write-Host ""
Write-Host "Production artifacts:"
$apk, $aab | Select-Object FullName, Length, LastWriteTime

Write-Host ""
Write-Host "SHA-256:"
$apk, $aab | Get-FileHash -Algorithm SHA256 | Select-Object Path, Hash
