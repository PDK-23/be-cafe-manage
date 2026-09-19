$file = Join-Path $PSScriptRoot '.logs/processes.json'
if (Test-Path -LiteralPath $file) {
  foreach ($entry in (Get-Content -Raw $file | ConvertFrom-Json)) {
    $process = Get-CimInstance Win32_Process -Filter "ProcessId = $($entry.pid)" -ErrorAction SilentlyContinue
    if ($process -and $process.Name -eq 'java.exe' -and $process.CommandLine.Contains("$($entry.service)-service-1.0.0.jar")) {
      Get-CimInstance Win32_Process -Filter "ParentProcessId = $($entry.pid)" | Where-Object { $_.Name -eq 'java.exe' -and $_.CommandLine.Contains("$($entry.service)-service-1.0.0.jar") } | ForEach-Object { Stop-Process -Id $_.ProcessId }
      Stop-Process -Id $entry.pid -ErrorAction SilentlyContinue
    }
  }
}
