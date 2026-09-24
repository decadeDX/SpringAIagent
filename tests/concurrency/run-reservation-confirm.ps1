#requires -Version 7.0
[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Password = 'student01'
)

$ErrorActionPreference = 'Stop'
$chinaTimeZone = [TimeZoneInfo]::FindSystemTimeZoneById('China Standard Time')
$now = [TimeZoneInfo]::ConvertTime([DateTimeOffset]::UtcNow, $chinaTimeZone)
$date = $now.Date.AddDays(1)
while ($date.DayOfWeek -in @([DayOfWeek]::Saturday, [DayOfWeek]::Sunday)) {
    $date = $date.AddDays(1)
}
$startTime = [DateTimeOffset]::new($date.Year, $date.Month, $date.Day, 14, 0, 0, $now.Offset)
$endTime = $startTime.AddHours(2)
$draftBody = @{ labId = 'LAB-B402'; startTime = $startTime.ToString('yyyy-MM-ddTHH:mm:sszzz'); endTime = $endTime.ToString('yyyy-MM-ddTHH:mm:sszzz'); participantCount = 3 } | ConvertTo-Json -Compress

function Invoke-LabApi {
    param([string]$Method, [string]$Uri, [object]$Body, [string]$Token)
    $headers = @{ Accept = 'application/json' }
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    $arguments = @{ Method = $Method; Uri = $Uri; Headers = $headers; ContentType = 'application/json'; SkipHttpErrorCheck = $true }
    if ($null -ne $Body) { $arguments.Body = $Body }
    $response = Invoke-WebRequest @arguments
    [pscustomobject]@{ StatusCode = [int]$response.StatusCode; Json = ($response.Content | ConvertFrom-Json) }
}

$drafts = foreach ($number in 1..20) {
    $username = 'concurrency{0:D2}' -f $number
    $login = Invoke-LabApi -Method Post -Uri "$BaseUrl/api/auth/login" -Body (@{ username = $username; password = $Password } | ConvertTo-Json -Compress)
    if ($login.StatusCode -ne 200 -or $login.Json.code -ne 200) {
        throw "登录 $username 失败：HTTP $($login.StatusCode)，业务码 $($login.Json.code)。请先导入 seed-concurrent-users.sql。"
    }
    $token = $login.Json.data.accessToken
    $draft = Invoke-LabApi -Method Post -Uri "$BaseUrl/api/reservation-drafts" -Body $draftBody -Token $token
    if ($draft.StatusCode -ne 201 -or $draft.Json.code -ne 200) {
        throw "创建 $username 的草案失败：HTTP $($draft.StatusCode)，业务码 $($draft.Json.code)。"
    }
    [pscustomobject]@{ Username = $username; Token = $token; ActionId = $draft.Json.data.actionId; SessionId = $draft.Json.data.sessionId }
}

$ready = [System.Threading.CountdownEvent]::new($drafts.Count)
$start = [System.Threading.ManualResetEventSlim]::new($false)
$jobs = foreach ($draft in $drafts) {
    Start-ThreadJob -ArgumentList $draft, $BaseUrl, $ready, $start -ScriptBlock {
        param($draft, $baseUrl, $ready, $start)
        $ready.Signal()
        $start.Wait()
        $body = @{ sessionId = $draft.SessionId } | ConvertTo-Json -Compress
        try {
            $response = Invoke-WebRequest -Method Post -Uri "$baseUrl/api/actions/$($draft.ActionId)/confirm" -Headers @{ Accept = 'application/json'; Authorization = "Bearer $($draft.Token)" } -ContentType 'application/json' -Body $body -SkipHttpErrorCheck
            $json = $response.Content | ConvertFrom-Json
            [pscustomobject]@{ Username = $draft.Username; HttpStatus = [int]$response.StatusCode; Code = [int]$json.code; Message = $json.message }
        } catch {
            [pscustomobject]@{ Username = $draft.Username; HttpStatus = 0; Code = 0; Message = $_.Exception.Message }
        }
    }
}

$ready.Wait()
$start.Set()
$results = $jobs | Receive-Job -Wait -AutoRemoveJob | Sort-Object Username
$successes = @($results | Where-Object { $_.HttpStatus -eq 200 -and $_.Code -eq 200 })
$conflicts = @($results | Where-Object { $_.HttpStatus -eq 409 -and $_.Code -eq 40901 })
$results | Format-Table Username, HttpStatus, Code, Message -AutoSize
Write-Host "测试时段（上海）：$($startTime.ToString('yyyy-MM-ddTHH:mm:sszzz')) — $($endTime.ToString('yyyy-MM-ddTHH:mm:sszzz'))"
Write-Host "测试时段（UTC）：$($startTime.UtcDateTime.ToString('yyyy-MM-dd HH:mm:ss')) — $($endTime.UtcDateTime.ToString('yyyy-MM-dd HH:mm:ss'))"
Write-Host "成功：$($successes.Count)，冲突：$($conflicts.Count)，总请求：$($results.Count)"

if ($successes.Count -ne 1 -or $conflicts.Count -ne 19) {
    throw '并发断言失败：期望恰好 1 个成功和 19 个 RESERVATION_CONFLICT（40901）。'
}

Write-Host 'HTTP 并发断言通过。请用 verify-reservation-consistency.sql（替换其中 UTC 开始时间）保存数据库一致性证据。'
