# Download and execute Maven without installing it globally
$ErrorActionPreference = "Stop"

Write-Host "Downloading portable Maven..."
Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip" -OutFile "maven.zip"

Write-Host "Extracting Maven..."
Expand-Archive -Path "maven.zip" -DestinationPath "." -Force

Write-Host "Compiling the Unified Security Console with Maven..."
cmd.exe /c ".\apache-maven-3.9.6\bin\mvn.cmd clean package -DskipTests"

Write-Host "Cleanup..."
Remove-Item -Path "maven.zip" -Force
Remove-Item -Path ".\apache-maven-3.9.6" -Recurse -Force

Write-Host "Compilation complete. The launcher.bat will now run the updated JAR!"
