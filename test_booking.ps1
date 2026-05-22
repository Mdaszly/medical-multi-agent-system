$body = @{
    department = "内科"
    scheduleDate = "2026-05-26"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/appointment/list/doctor" -Method "POST" -Headers @{"Content-Type"="application/json"} -Body $body -UseBasicParsing
    Write-Host "Success: " $response.Content
} catch {
    Write-Host "Error Status: " $_.Exception.Response.StatusCode
    $errorContent = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($errorContent)
    Write-Host "Error Body: " $reader.ReadToEnd()
}