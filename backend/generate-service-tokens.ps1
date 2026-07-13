$ErrorActionPreference = "Stop"

function New-ServiceToken {

    $bytes = New-Object byte[] 48

    $rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider

    try {
        $rng.GetBytes($bytes)
    }
    finally {
        $rng.Dispose()
    }

    return [Convert]::ToBase64String($bytes).
        Replace("+", "-").
        Replace("/", "_").
        TrimEnd("=")
}

$tokens = [ordered]@{
    "AUTH_SERVICE_TOKEN"     = New-ServiceToken
    "CATALOG_SERVICE_TOKEN"  = New-ServiceToken
    "COMMERCE_SERVICE_TOKEN" = New-ServiceToken
    "SUPPORT_SERVICE_TOKEN"  = New-ServiceToken
    "AUDIT_SERVICE_TOKEN"    = New-ServiceToken
}

$envPath = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envPath)) {
    New-Item -Path $envPath -ItemType File | Out-Null
}

$content = Get-Content $envPath -Raw

foreach ($entry in $tokens.GetEnumerator()) {

    $name = $entry.Key
    $value = $entry.Value
    $newLine = "$name=$value"

    $pattern = "(?m)^" + [regex]::Escape($name) + "=.*$"

    if ($content -match $pattern) {
        $content = [regex]::Replace(
            $content,
            $pattern,
            $newLine
        )
    }
    else {

        if (
            -not [string]::IsNullOrWhiteSpace($content) -and
            -not $content.EndsWith("`n")
        ) {
            $content += "`r`n"
        }

        $content += "$newLine`r`n"
    }
}

Set-Content `
    -Path $envPath `
    -Value $content `
    -Encoding UTF8

Write-Host ""
Write-Host "Tokens generados correctamente:"
Write-Host ""

foreach ($entry in $tokens.GetEnumerator()) {
    Write-Host "$($entry.Key)=********"
}

Write-Host ""
Write-Host "Archivo actualizado:"
Write-Host $envPath