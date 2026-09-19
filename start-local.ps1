param([switch]$Demo)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$profile = if ($Demo) { 'local,demo' } else { 'local' }
$javaExe = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME/bin/java.exe")) { "$env:JAVA_HOME/bin/java.exe" } else { (Get-Command java).Source }
$services = @('permission','auth','catalog','pos')
foreach ($port in @(8080,8081,8082,8083)) {
  $client = New-Object System.Net.Sockets.TcpClient
  try { $client.Connect('127.0.0.1', $port); throw "Port $port is already in use. Stop existing services before starting." }
  catch [System.Net.Sockets.SocketException] { }
  finally { $client.Dispose() }
}
$logs = Join-Path $PSScriptRoot '.logs'
New-Item -ItemType Directory -Force $logs | Out-Null
$started = @()
foreach ($service in $services) {
  $jar = Join-Path $PSScriptRoot "$service-service/target/$service-service-1.0.0.jar"
  if (!(Test-Path -LiteralPath $jar)) { throw "Missing $jar. Run ./mvnw.cmd package first." }
  $process = Start-Process -FilePath $javaExe -ArgumentList @('-jar', "`"$jar`"", "--spring.profiles.active=$profile") -WindowStyle Hidden -PassThru -RedirectStandardOutput "$logs/$service.out.log" -RedirectStandardError "$logs/$service.err.log"
  $started += @{ service = $service; pid = $process.Id }
  Write-Output "$service-service started (PID $($process.Id))"
}
$started | ConvertTo-Json | Set-Content -Encoding UTF8 "$logs/processes.json"
Write-Output 'Frontend: cd ../fe-cafe-manage; npm run dev'
