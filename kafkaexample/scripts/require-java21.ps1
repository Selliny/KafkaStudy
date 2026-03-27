$ErrorActionPreference = "Stop"

if ($env:JAVA_HOME) {
  $javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
  $javacExe = Join-Path $env:JAVA_HOME "bin\javac.exe"
  if (!(Test-Path $javaExe) -or !(Test-Path $javacExe)) {
    Write-Host "This project requires JDK 21+. JAVA_HOME does not point to a complete JDK."
    Write-Host "Set JAVA_HOME to a JDK 21 installation and rerun .\mvnw.cmd."
    exit 1
  }
} else {
  $javaCmd = Get-Command java -ErrorAction SilentlyContinue
  $javacCmd = Get-Command javac -ErrorAction SilentlyContinue
  if (!$javaCmd -or !$javacCmd) {
    Write-Host "This project requires JDK 21+. Set JAVA_HOME to a JDK 21 installation and rerun .\mvnw.cmd."
    exit 1
  }
  $javaExe = $javaCmd.Source
}

$versionLine = (& cmd.exe /c ('"' + $javaExe + '" -version 2>&1') | Select-Object -First 1).ToString()
if ($versionLine -notmatch '"([^"]+)"') {
  Write-Host "Unable to determine Java version from: $versionLine"
  exit 1
}

$rawVersion = $Matches[1]
if ($rawVersion.StartsWith("1.")) {
  $majorVersion = [int]($rawVersion.Split(".")[1])
} else {
  $majorVersion = [int]($rawVersion.Split(".")[0])
}

if ($majorVersion -lt 21) {
  Write-Host "This project requires JDK 21+. mvnw.cmd resolved: $versionLine"
  Write-Host "Set JAVA_HOME to a JDK 21 installation and rerun .\mvnw.cmd."
  exit 1
}
