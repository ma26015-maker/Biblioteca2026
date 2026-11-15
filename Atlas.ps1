$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
Write-Host ''
Write-Host 'Biblioteca - MongoDB Atlas'
Write-Host 'Copia el enlace de Atlas > Connect > Drivers > Java.'
Write-Host 'Deja <db_password> o <password> en el enlace.'
Write-Host ''
$plantilla = (Read-Host 'Pega el enlace sin contraseña').Trim()
if ($plantilla -notmatch '^mongodb\+srv://') {
    throw 'La direccion debe comenzar con mongodb+srv://'
}
if (-not ($plantilla.Contains('<db_password>') -or $plantilla.Contains('<password>'))) {
    throw 'La direccion debe mantener <db_password> o <password>, sin poner la contraseña en este campo.'
}
$secreta = Read-Host 'Escribe o pega la contraseña de la base de datos' -AsSecureString
$puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secreta)
try {
    $texto = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    $codificada = [Uri]::EscapeDataString($texto)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
}
$env:MONGO_URI = $plantilla.Replace('<db_password>', $codificada).Replace('<password>', $codificada)
$texto = $null
$codificada = $null
$secreta = $null
try {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        throw 'Java no se encuentra. Comprueba java -version y la instalación de JDK 21.'
    }
    $jar = Join-Path $PSScriptRoot 'target\Biblioteca.jar'
    if (-not (Test-Path -LiteralPath $jar)) {
        if (Get-Command mvn -ErrorAction SilentlyContinue) {
            Write-Host 'Compilando proyecto con Maven...'
            & mvn clean package
            if ($LASTEXITCODE -ne 0) { throw 'La compilación falló.' }
        } else {
            throw 'No existe target\Biblioteca.jar. En NetBeans utiliza Clean and Build en el proyecto y vuelve a abrir Atlas.bat. Como alternativa, instala Apache Maven.'
        }
    }
    Write-Host ''
    Write-Host 'Comprobando acceso a MongoDB Atlas...'
    & java -cp $jar biblioteca.ComprobarConexion
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo establecer la conexión. Revisa el usuario, contraseña, IP permitida e Internet.' }
    Write-Host 'Iniciando Biblioteca...'
    & java -jar $jar
    if ($LASTEXITCODE -ne 0) { throw 'La aplicación terminó con un error.' }
} finally {
    Remove-Item Env:MONGO_URI -ErrorAction SilentlyContinue
}
