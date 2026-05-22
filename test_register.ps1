$body = @{
    userAccount = "drjohn"
    userPassword = "12345678"
    checkPassword = "12345678"
    userName = "John Doe"
    userRole = "DOCTOR"
    department = "Internal Medicine"
    title = "Attending Physician"
    licenseNo = "MED123456789"
    consultationFee = 50.00
    phone = "13800138006"
    email = "john@test.com"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/register" -Method "POST" -Headers @{"Content-Type"="application/json"} -Body $body -UseBasicParsing
    Write-Host "Success: " $response.Content
} catch {
    Write-Host "Error Status: " $_.Exception.Response.StatusCode
    $errorContent = $_.Exception.Response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($errorContent)
    Write-Host "Error Body: " $reader.ReadToEnd()
}