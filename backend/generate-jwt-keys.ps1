$ErrorActionPreference = "Stop"

$secretsPath = Join-Path $PSScriptRoot "auth-service\secrets"

Write-Host "Creando carpeta de claves en:"
Write-Host $secretsPath

New-Item `
    -ItemType Directory `
    -Force `
    -Path $secretsPath |
    Out-Null

$privateKeyPath = Join-Path $secretsPath "private-key.pem"
$publicKeyPath = Join-Path $secretsPath "public-key.pem"

if (Test-Path $privateKeyPath) {
    Remove-Item $privateKeyPath -Force
}

if (Test-Path $publicKeyPath) {
    Remove-Item $publicKeyPath -Force
}

Write-Host "Generando clave privada RSA..."

docker run --rm `
    -v "${secretsPath}:/keys" `
    alpine/openssl `
    genpkey `
    -algorithm RSA `
    -out /keys/private-key.pem `
    -pkeyopt rsa_keygen_bits:2048

if ($LASTEXITCODE -ne 0) {
    throw "No se pudo generar la clave privada."
}

Write-Host "Generando clave pública RSA..."

docker run --rm `
    -v "${secretsPath}:/keys" `
    alpine/openssl `
    rsa `
    -pubout `
    -in /keys/private-key.pem `
    -out /keys/public-key.pem

if ($LASTEXITCODE -ne 0) {
    throw "No se pudo generar la clave pública."
}

Write-Host ""
Write-Host "Verificando archivos..."

if (
    !(Test-Path $privateKeyPath) -or
    !(Test-Path $publicKeyPath)
) {
    throw "No se generaron correctamente los archivos PEM."
}

$privateHeader =
    Get-Content $privateKeyPath -TotalCount 1

$publicHeader =
    Get-Content $publicKeyPath -TotalCount 1

if ($privateHeader -ne "-----BEGIN PRIVATE KEY-----") {
    throw "La clave privada no tiene formato PKCS#8 válido."
}

if ($publicHeader -ne "-----BEGIN PUBLIC KEY-----") {
    throw "La clave pública no tiene formato X.509 válido."
}

Write-Host ""
Write-Host "Claves generadas correctamente:"
Write-Host $privateKeyPath
Write-Host $publicKeyPath

Write-Host ""
Write-Host "Clave privada:"
Write-Host $privateHeader

Write-Host ""
Write-Host "Clave pública:"
Write-Host $publicHeader