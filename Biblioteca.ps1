param([switch]$Configurar)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$carpeta = Join-Path $env:LOCALAPPDATA 'Biblioteca'
$archivo = Join-Path $carpeta 'atlas.dat'
$jar = Join-Path $PSScriptRoot 'target\Biblioteca.jar'

function ComprobarJava {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
        throw 'No se encontro Java. Comprueba que JDK 21 este instalado y configurado.'
    }
}

function ComprobarJar {
    if (-not (Test-Path -LiteralPath $jar)) {
        if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
            throw 'Falta target\Biblioteca.jar. Abre este proyecto en NetBeans y ejecuta Clean and Build.'
        }
        Write-Host 'Compilando la aplicacion...'
        & mvn clean package
        if ($LASTEXITCODE -ne 0) {
            throw 'No se pudo compilar el proyecto. Revisa los errores de Maven.'
        }
    }
}

try {
    ComprobarJava
    ComprobarJar

    if ($Configurar -or -not (Test-Path -LiteralPath $archivo)) {
        Write-Host ''
        Write-Host 'Configuracion inicial de MongoDB Atlas'
        Write-Host 'Esta configuracion se guarda cifrada para tu usuario de Windows.'
        Write-Host 'Conserva <db_password> o <password> en el enlace de Atlas.'
        Write-Host ''

        $plantilla = (Read-Host 'Pega la direccion de Atlas sin la contrasena').Trim()
        if ($plantilla -notmatch '^mongodb\+srv://') {
            throw 'La direccion debe comenzar con mongodb+srv://'
        }
        if (-not ($plantilla.Contains('<db_password>') -or $plantilla.Contains('<password>'))) {
            throw 'Deja el texto <db_password> o <password> en la direccion.'
        }

        $clave = Read-Host 'Contrasena del usuario de MongoDB Atlas' -AsSecureString
        $puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($clave)
        try {
            $contrasena = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
            $codificada = [Uri]::EscapeDataString($contrasena)
        } finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
        }

        $uri = $plantilla.Replace('<db_password>', $codificada).Replace('<password>', $codificada)
        $contrasena = $null
        $codificada = $null
        $clave = $null
        $plantilla = $null
        $env:MONGO_URI = $uri

        try {
            Write-Host ''
            Write-Host 'Comprobando conexion con Atlas...'
            & java -cp $jar biblioteca.ComprobarConexion
            if ($LASTEXITCODE -ne 0) {
                throw 'No se pudo conectar. Revisa el usuario, la contrasena, el acceso IP y tu Internet.'
            }

            New-Item -ItemType Directory -Path $carpeta -Force | Out-Null
            $segura = ConvertTo-SecureString -String $uri -AsPlainText -Force
            $cifrada = ConvertFrom-SecureString -SecureString $segura
            [System.IO.File]::WriteAllText($archivo, $cifrada, [System.Text.Encoding]::UTF8)
            Write-Host 'Conexion verificada. La configuracion quedo guardada de forma cifrada.'
        } finally {
            Remove-Item Env:MONGO_URI -ErrorAction SilentlyContinue
            $uri = $null
            $segura = $null
            $cifrada = $null
        }
    }

    $cifrada = [System.IO.File]::ReadAllText($archivo).Trim()
    $segura = ConvertTo-SecureString -String $cifrada
    $puntero = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
    try {
        $env:MONGO_URI = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($puntero)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($puntero)
        $cifrada = $null
        $segura = $null
    }

    Write-Host 'Abriendo Biblioteca...'
    & java -jar $jar
    if ($LASTEXITCODE -ne 0) {
        throw 'La aplicacion termino con un error. Comprueba que tengas Internet y acceso a Atlas.'
    }
} catch {
    Write-Host ''
    Write-Host ('ERROR: ' + $_.Exception.Message) -ForegroundColor Red
    Write-Host 'Si cambiaste tu contrasena de Atlas, abre Configurar.bat de nuevo.'
    Read-Host 'Presiona Enter para cerrar'
} finally {
    Remove-Item Env:MONGO_URI -ErrorAction SilentlyContinue
}
