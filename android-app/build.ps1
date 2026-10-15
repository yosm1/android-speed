$ErrorActionPreference='Stop'
$base='D:\exe\android-speed'
$java='D:\Program Files\JetBrains\IntelliJ IDEA 2026.2.1\jbr\bin'
$env:JAVA_HOME=Split-Path $java
$env:PATH="$java;$env:PATH"
$tools="$base\build-tools\android-14"
$jar="$base\platform\android-34\android.jar"
$src="$PSScriptRoot\app\src\main"
$out="$PSScriptRoot\build"
New-Item -ItemType Directory -Force -Path "$out\classes","$out\dex" | Out-Null
function Check {if($LASTEXITCODE -ne 0){throw "Build command failed: $LASTEXITCODE"}}
& "$java\javac.exe" -encoding UTF-8 --release 8 -g -classpath $jar -d "$out\classes" "$src\java\local\speed\MainActivity.java"; Check
& "$java\java.exe" -cp "$base\downloads\r8.jar" com.android.tools.r8.D8 --lib $jar --min-api 26 --output "$out\dex" (Get-ChildItem "$out\classes" -Recurse -Filter '*.class' | ForEach-Object FullName); Check
& "$tools\aapt.exe" package -f -M "$src\AndroidManifest.xml" -I $jar -A "$src\assets" -F "$out\unsigned.apk"; Check
Push-Location "$out\dex"
& "$tools\aapt.exe" add "$out\unsigned.apk" classes.dex; Check
Pop-Location
& "$tools\zipalign.exe" -f 4 "$out\unsigned.apk" "$out\aligned.apk"; Check
if(!(Test-Path "$PSScriptRoot\debug.keystore")){
& "$java\keytool.exe" -genkeypair -keystore "$PSScriptRoot\debug.keystore" -storepass android -keypass android -alias local-speed -dname 'CN=Local Speed' -keyalg RSA -validity 3650; Check
}
& "$tools\apksigner.bat" sign --ks "$PSScriptRoot\debug.keystore" --ks-pass pass:android --out "$out\LayaSpeed.apk" "$out\aligned.apk"; Check
& "$tools\apksigner.bat" verify "$out\LayaSpeed.apk"; Check

