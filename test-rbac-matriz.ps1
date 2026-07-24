<#
.SYNOPSIS
  Prueba automatizada de la matriz RBAC del Portal Cliente de FleetTrackPro.

.DESCRIPTION
  - Inicia sesión con admin, despachador, mecanico y contador de EMP-001.
  - Construye datos válidos y únicos para probar todas las mutaciones protegidas.
  - Espera 2xx para acciones permitidas y 403 para acciones denegadas.
  - Imprime una tabla final y devuelve exit code 1 si encuentra discrepancias.

  IMPORTANTE:
  El backend no ofrece DELETE para Vehiculo, Conductor, Gasto, Ingreso ni los
  catálogos probados. Esos registros quedan identificados con prefijo RBAC.
  Viajes, mantenimientos y órdenes sí se limpian en modo best-effort.
#>

[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# 1. Credenciales: completa los cuatro usuarios de EMP-001 antes de ejecutar.
# ---------------------------------------------------------------------------
$Credentials = @{
    admin = @{
        Email = "admin@fleettrackpro.com"
        Password = "admin123"
    }
    despachador = @{
        Email = "despachador@fleettrackpro.com"
        Password = "desp123"
    }
    mecanico = @{
        Email = "taller@fleettrackpro.com"
        Password = "taller123"
    }
    contador = @{
        Email = "contabilidad@fleettrackpro.com"
        Password = "conta123"
    }
}

$Roles = @("admin", "despachador", "mecanico", "contador")
$Results = [System.Collections.Generic.List[object]]::new()
$Tokens = @{}
$Created = @{
    Viajes = [System.Collections.Generic.List[int]]::new()
    Mantenimientos = [System.Collections.Generic.List[int]]::new()
    Ordenes = [System.Collections.Generic.List[int]]::new()
}

function ConvertTo-JsonBody {
    param([object]$Body)
    if ($null -eq $Body) { return $null }
    return $Body | ConvertTo-Json -Depth 12 -Compress
}

function Invoke-Api {
    param(
        [Parameter(Mandatory)][string]$Method,
        [Parameter(Mandatory)][string]$Path,
        [string]$Token,
        [object]$Body
    )

    $headers = @{}
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    $params = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $headers
        SkipHttpErrorCheck = $true
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json"
        $params.Body = ConvertTo-JsonBody $Body
    }

    $response = Invoke-WebRequest @params
    $data = $null
    if ($response.Content) {
        try { $data = $response.Content | ConvertFrom-Json } catch { $data = $response.Content }
    }
    return [pscustomobject]@{
        Status = [int]$response.StatusCode
        Data = $data
        Raw = $response.Content
    }
}

function Login-Role {
    param([string]$Role)
    $credential = $Credentials[$Role]
    if (!$credential -or $credential.Email -like "COMPLETAR_*" -or
        $credential.Password -eq "COMPLETAR_PASSWORD") {
        throw "Completa las credenciales del rol '$Role' al inicio del script."
    }

    $response = Invoke-Api -Method POST -Path "/auth/login" -Body @{
        email = $credential.Email
        contrasenia = $credential.Password
    }
    if ($response.Status -ne 200 -or !$response.Data.token) {
        throw "Login fallido para $Role (HTTP $($response.Status)): $($response.Raw)"
    }
    if ($response.Data.rol.Trim().ToLowerInvariant() -ne $Role) {
        throw "El usuario configurado como $Role devolvió rol '$($response.Data.rol)'."
    }
    if ($response.Data.idEmpresa -ne "EMP-001") {
        throw "El usuario $Role pertenece a '$($response.Data.idEmpresa)', no a EMP-001."
    }
    $Tokens[$Role] = $response.Data.token
}

function Get-Catalog {
    param([string]$Path)
    $response = Invoke-Api -Method GET -Path $Path -Token $Tokens.admin
    if ($response.Status -ne 200) {
        throw "No se pudo cargar $Path (HTTP $($response.Status))."
    }
    return @($response.Data)
}

function Get-Id {
    param([object[]]$Items, [string]$Property)
    if (!$Items -or $null -eq $Items[0].$Property) {
        throw "El catálogo no contiene la propiedad requerida '$Property'."
    }
    return [int]$Items[0].$Property
}

function Add-Result {
    param(
        [string]$Role,
        [string]$Endpoint,
        [string]$Method,
        [string]$Expected,
        [int]$Actual,
        [bool]$Matches,
        [string]$Detail
    )
    $Results.Add([pscustomobject]@{
        Rol = $Role
        Endpoint = $Endpoint
        Metodo = $Method
        Esperado = $Expected
        Real = $Actual
        Coincide = if ($Matches) { "✅" } else { "❌" }
        Detalle = $Detail
    })
}

function Test-RbacCall {
    param(
        [string]$Role,
        [string]$Endpoint,
        [string]$Method,
        [bool]$Allowed,
        [scriptblock]$AllowedAction,
        [object]$DeniedBody = @{}
    )

    try {
        if ($Allowed) {
            $response = & $AllowedAction $Role
            $matches = $response.Status -ge 200 -and $response.Status -lt 300
            Add-Result $Role $Endpoint $Method "2xx" $response.Status $matches $response.Raw
        } else {
            $response = Invoke-Api -Method $Method -Path $Endpoint -Token $Tokens[$Role] -Body $DeniedBody
            $matches = $response.Status -eq 403
            Add-Result $Role $Endpoint $Method "403" $response.Status $matches $response.Raw
        }
    } catch {
        Add-Result $Role $Endpoint $Method $(if ($Allowed) { "2xx" } else { "403" }) 0 $false $_.Exception.Message
    }
}

# ---------------------------------------------------------------------------
# 2. Inicio de sesión y catálogos necesarios para bodies válidos.
# ---------------------------------------------------------------------------
Write-Host "Iniciando sesiones RBAC..." -ForegroundColor Cyan
foreach ($role in $Roles) { Login-Role $role }

$Catalog = @{
    Marca = Get-Id (Get-Catalog "/catalogos/marcas") "idMarca"
    Modelo = Get-Id (Get-Catalog "/catalogos/modelos") "idModelo"
    Color = Get-Id (Get-Catalog "/catalogos/colores") "idColor"
    TipoUnidad = Get-Id (Get-Catalog "/catalogos/tipos-unidad") "idTipoUnidad"
    Propiedad = Get-Id (Get-Catalog "/catalogos/propiedad-tipos") "idPropiedadTipo"
    TipoDocumento = Get-Id (Get-Catalog "/catalogos/tipos-documento") "idTipoDocumento"
    CategoriaLicencia = Get-Id (Get-Catalog "/catalogos/licencia-categorias") "idCategoria"
    TipoSangre = Get-Id (Get-Catalog "/catalogos/tipos-sangre") "idTipoSangre"
    EstadoLaboral = Get-Id (Get-Catalog "/catalogos/estados-laborales") "idEstadoLaboral"
    Prioridad = Get-Id (Get-Catalog "/catalogos/prioridades") "idPrioridad"
    TipoServicio = Get-Id (Get-Catalog "/catalogos/tipos-servicio") "idTipoServicio"
    Moneda = Get-Id (Get-Catalog "/catalogos/monedas") "idMoneda"
    MetodoPago = Get-Id (Get-Catalog "/catalogos/metodos-pago-saas") "idMetodoPago"
    Comprobante = Get-Id (Get-Catalog "/catalogos/comprobante-tipos") "idComprobanteTipo"
}

$categoriasGasto = Get-Catalog "/catalogos/gasto-categorias"
$categoriaCfp = $categoriasGasto | Where-Object { $_.tipoCosto -eq "CFP" } | Select-Object -First 1
if (!$categoriaCfp) { $categoriaCfp = $categoriasGasto | Select-Object -First 1 }
$Catalog.GastoCategoria = [int]$categoriaCfp.idGastoCategoria

$stamp = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds().ToString()
$suffix = $stamp.Substring($stamp.Length - 6)
$today = Get-Date
$isoNow = $today.ToString("yyyy-MM-ddTHH:mm:ss")
$isoLater = $today.AddHours(2).ToString("yyyy-MM-ddTHH:mm:ss")

$VehicleBody = @{
    placa = "R$suffix"
    idMarca = $Catalog.Marca
    idModelo = $Catalog.Modelo
    anio = $today.Year
    capacidadCarga = 1000
    idEstadoOperativo = 1
    idColor = $Catalog.Color
    idTipoUnidad = $Catalog.TipoUnidad
    idPropiedadTipo = $Catalog.Propiedad
    nroChasisVin = "RBACVIN$stamp"
    kilometrajeActual = 100
    soatVencimiento = $today.AddYears(1).ToString("yyyy-MM-dd")
    revisionTecnicaVencimiento = $today.AddYears(1).ToString("yyyy-MM-dd")
    valorCompra = 50000
    valorResidual = 5000
    vidaUtilAnios = 5
}

$dniNumber = ([long]$stamp % 90000000) + 10000000
$ConductorBody = @{
    numeroDocumento = $dniNumber.ToString()
    idTipoDocumento = $Catalog.TipoDocumento
    nombre = "RBAC"
    apellido = "Prueba-$suffix"
    licenciaNro = $dniNumber.ToString()
    telefono = "9$($suffix.PadLeft(8, '0'))".Substring(0, 9)
    email = "rbac-$stamp@example.test"
    puesto = "Prueba RBAC"
    idEstadoLaboral = $Catalog.EstadoLaboral
    idCategoria = $Catalog.CategoriaLicencia
    licenciaVencimiento = $today.AddYears(1).ToString("yyyy-MM-dd")
    idTipoSangre = $Catalog.TipoSangre
    contactoEmergencia = "999999999"
    costoHora = 10
}

function New-Vehicle {
    $body = $VehicleBody.Clone()
    $unique = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds().ToString()
    $body.placa = "T$($unique.Substring($unique.Length - 6))"
    $body.nroChasisVin = "RBACVIN$unique"
    return Invoke-Api POST "/vehiculos" $Tokens.admin $body
}

function New-Conductor {
    $body = $ConductorBody.Clone()
    $unique = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $dni = (($unique % 90000000) + 10000000).ToString()
    $body.numeroDocumento = $dni
    $body.licenciaNro = $dni
    $body.email = "rbac-$unique@example.test"
    return Invoke-Api POST "/conductores" $Tokens.admin $body
}

$baseVehicleResponse = Invoke-Api POST "/vehiculos" $Tokens.admin $VehicleBody
if ($baseVehicleResponse.Status -notin 200, 201) { throw "No se pudo crear vehículo base: $($baseVehicleResponse.Raw)" }
$VehicleId = [int]$baseVehicleResponse.Data.idVehiculo

$baseDriverResponse = Invoke-Api POST "/conductores" $Tokens.admin $ConductorBody
if ($baseDriverResponse.Status -notin 200, 201) { throw "No se pudo crear conductor base: $($baseDriverResponse.Raw)" }
$DriverId = [int]$baseDriverResponse.Data.idConductor

$OrderBody = @{
    idVehiculo = $VehicleId
    idConductor = $DriverId
    tipoIncidencia = "Prueba RBAC"
    descripcion = "Registro automatizado"
    direccion = "Dirección de prueba"
    idPrioridad = $Catalog.Prioridad
    idViajeEstado = 1
}

$TripBody = @{
    idVehiculo = $VehicleId
    idConductor = $DriverId
    origen = "Origen RBAC"
    destino = "Destino RBAC"
    fechaSalida = $isoNow
    fechaLlegadaEstimada = $isoLater
    idViajeEstado = 1
    kilometrajeSalida = 100
    volumenAtendidoM3 = 1
}

$MaintenanceBody = @{
    idVehiculo = $VehicleId
    idTipoServicio = $Catalog.TipoServicio
    descripcionFalla = "Prueba RBAC"
    costoReparacion = 10
    fechaEntrada = $isoNow
    kilometrajeEntrada = 100
    garantiaMeses = 0
    idMetodoPago = $Catalog.MetodoPago
    idMoneda = $Catalog.Moneda
    naturalezaMantenimiento = "Correctivo"
    repuestos = @()
}

$ExpenseBody = @{
    idVehiculo = $VehicleId
    idGastoCategoria = $Catalog.GastoCategoria
    monto = 10
    idMoneda = $Catalog.Moneda
    fechaGasto = $isoNow
    descripcion = "Prueba RBAC"
    idComprobanteTipo = $Catalog.Comprobante
    comprobanteNro = "RBAC-$stamp"
    kilometrajeRegistro = 100
    idMetodoPago = $Catalog.MetodoPago
}

function New-Order {
    $response = Invoke-Api POST "/ordenes-trabajo" $Tokens.admin $OrderBody
    if ($response.Status -in 200, 201) { $Created.Ordenes.Add([int]$response.Data.idOrdenTrabajo) }
    return $response
}

function New-Trip {
    $response = Invoke-Api POST "/viajes" $Tokens.admin $TripBody
    if ($response.Status -in 200, 201) { $Created.Viajes.Add([int]$response.Data.idViaje) }
    return $response
}

function New-Maintenance {
    $response = Invoke-Api POST "/mantenimientos" $Tokens.admin $MaintenanceBody
    if ($response.Status -in 200, 201) { $Created.Mantenimientos.Add([int]$response.Data.idMantenimiento) }
    return $response
}

function New-CompletedTrip {
    $trip = New-Trip
    if ($trip.Status -notin 200, 201) { return $trip }
    $id = [int]$trip.Data.idViaje
    $checklist = @{
        idViaje = $id
        nivelCombustibleProporcion = "1/2"
        estadoNeumaticos = "Bueno"
        tieneHerramientasEmergencia = "SI"
        lucesOperativas = "SI"
        observaciones = "Prueba RBAC"
        firmaDigitalConductorUrl = "data:text/plain,rbac"
    }
    $checkResponse = Invoke-Api POST "/checklist" $Tokens.admin $checklist
    if ($checkResponse.Status -notin 200, 201) { return $checkResponse }
    foreach ($state in @(2, 3)) {
        $stateResponse = Invoke-Api PUT "/viajes/$id/estado" $Tokens.admin @{ idEstado = $state }
        if ($stateResponse.Status -notin 200, 201) { return $stateResponse }
    }
    return Invoke-Api PUT "/viajes/$id/estado" $Tokens.admin @{
        idEstado = 4
        fechaLlegada = $isoLater
        kilometrajeLlegada = 150
    }
}

# ---------------------------------------------------------------------------
# 3. Matriz de permisos. Para cada rol se ejecutan todas las mutaciones.
# ---------------------------------------------------------------------------
$baseTripResponse = New-Trip
if ($baseTripResponse.Status -notin 200, 201) {
    throw "No se pudo crear el viaje base para las pruebas PUT: $($baseTripResponse.Raw)"
}
$BaseTripId = [int]$baseTripResponse.Data.idViaje

$Permissions = @{
    admin = @{
        Vehicle = $true; Driver = $true; DriverAccess = $true; Maintenance = $true; Trip = $true
        Order = $true; Expense = $true; Income = $true; Catalog = $true; Subscription = $true
    }
    despachador = @{
        Vehicle = $false; Driver = $false; DriverAccess = $false; Maintenance = $false; Trip = $true
        Order = $true; Expense = $false; Income = $false; Catalog = $false; Subscription = $false
    }
    mecanico = @{
        Vehicle = $false; Driver = $false; DriverAccess = $false; Maintenance = $true; Trip = $false
        Order = $true; Expense = $false; Income = $false; Catalog = $true; Subscription = $false
    }
    contador = @{
        Vehicle = $false; Driver = $false; DriverAccess = $false; Maintenance = $false; Trip = $false
        Order = $false; Expense = $true; Income = $true; Catalog = $false; Subscription = $false
    }
}

foreach ($role in $Roles) {
    Write-Host "Probando rol $role..." -ForegroundColor Cyan
    $p = $Permissions[$role]

    Test-RbacCall $role "/vehiculos" POST $p.Vehicle {
        param($r)
        $body = $VehicleBody.Clone()
        $u = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds().ToString()
        $body.placa = "V$($u.Substring($u.Length - 6))"
        $body.nroChasisVin = "RBACROLE$u"
        Invoke-Api POST "/vehiculos" $Tokens[$r] $body
    }
    Test-RbacCall $role "/vehiculos/$VehicleId" PUT $p.Vehicle {
        param($r) Invoke-Api PUT "/vehiculos/$VehicleId" $Tokens[$r] $VehicleBody
    }

    Test-RbacCall $role "/conductores" POST $p.Driver {
        param($r)
        $body = $ConductorBody.Clone()
        $u = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        $dni = (($u % 90000000) + 10000000).ToString()
        $body.numeroDocumento = $dni
        $body.licenciaNro = $dni
        $body.email = "rbac-role-$u@example.test"
        Invoke-Api POST "/conductores" $Tokens[$r] $body
    }
    Test-RbacCall $role "/conductores/$DriverId" PUT $p.Driver {
        param($r) Invoke-Api PUT "/conductores/$DriverId" $Tokens[$r] $ConductorBody
    }
    Test-RbacCall $role "/conductores/$DriverId/crear-acceso" POST $p.DriverAccess {
        param($r)
        $fixture = New-Conductor
        if ($fixture.Status -notin 200, 201) { return $fixture }
        $unique = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        Invoke-Api POST "/conductores/$($fixture.Data.idConductor)/crear-acceso" $Tokens[$r] @{
            email = "rbac-access-$unique@example.test"
            contrasenia = "RBAC-Prueba-$unique"
        }
    }

    Test-RbacCall $role "/mantenimientos" POST $p.Maintenance {
        param($r)
        $response = Invoke-Api POST "/mantenimientos" $Tokens[$r] $MaintenanceBody
        if ($response.Status -in 200, 201) { $Created.Mantenimientos.Add([int]$response.Data.idMantenimiento) }
        $response
    }
    Test-RbacCall $role "/mantenimientos/1" PUT $p.Maintenance {
        param($r)
        $fixture = New-Maintenance
        if ($fixture.Status -notin 200, 201) { return $fixture }
        Invoke-Api PUT "/mantenimientos/$($fixture.Data.idMantenimiento)" $Tokens[$r] $MaintenanceBody
    }
    Test-RbacCall $role "/mantenimientos/1" DELETE $p.Maintenance {
        param($r)
        $fixture = New-Maintenance
        if ($fixture.Status -notin 200, 201) { return $fixture }
        Invoke-Api DELETE "/mantenimientos/$($fixture.Data.idMantenimiento)" $Tokens[$r]
    }

    Test-RbacCall $role "/viajes" POST $p.Trip {
        param($r)
        $response = Invoke-Api POST "/viajes" $Tokens[$r] $TripBody
        if ($response.Status -in 200, 201) { $Created.Viajes.Add([int]$response.Data.idViaje) }
        $response
    }
    Test-RbacCall $role "/viajes/$BaseTripId" PUT $p.Trip {
        param($r)
        $fixture = New-Trip
        if ($fixture.Status -notin 200, 201) { return $fixture }
        Invoke-Api PUT "/viajes/$($fixture.Data.idViaje)" $Tokens[$r] $TripBody
    }
    Test-RbacCall $role "/viajes/$BaseTripId/estado" PUT $p.Trip {
        param($r)
        $fixture = New-Trip
        if ($fixture.Status -notin 200, 201) { return $fixture }
        Invoke-Api PUT "/viajes/$($fixture.Data.idViaje)/estado" $Tokens[$r] @{ idEstado = 2 }
    }
    Test-RbacCall $role "/viajes/$BaseTripId" DELETE $p.Trip {
        param($r)
        $fixture = New-Trip
        if ($fixture.Status -notin 200, 201) { return $fixture }
        Invoke-Api DELETE "/viajes/$($fixture.Data.idViaje)" $Tokens[$r]
    }

    Test-RbacCall $role "/ordenes-trabajo" POST $p.Order {
        param($r)
        $response = Invoke-Api POST "/ordenes-trabajo" $Tokens[$r] $OrderBody
        if ($response.Status -in 200, 201) { $Created.Ordenes.Add([int]$response.Data.idOrdenTrabajo) }
        $response
    }
    Test-RbacCall $role "/ordenes-trabajo/1" PUT $p.Order {
        param($r)
        $fixture = New-Order
        if ($fixture.Status -notin 200, 201) { return $fixture }
        Invoke-Api PUT "/ordenes-trabajo/$($fixture.Data.idOrdenTrabajo)" $Tokens[$r] $OrderBody
    }
    Test-RbacCall $role "/ordenes-trabajo/1" DELETE $p.Order {
        param($r)
        $fixture = New-Order
        if ($fixture.Status -notin 200, 201) { return $fixture }
        Invoke-Api DELETE "/ordenes-trabajo/$($fixture.Data.idOrdenTrabajo)" $Tokens[$r]
    }

    Test-RbacCall $role "/gastos" POST $p.Expense {
        param($r)
        $body = $ExpenseBody.Clone()
        $body.comprobanteNro = "RBAC-$r-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
        Invoke-Api POST "/gastos" $Tokens[$r] $body
    }
    Test-RbacCall $role "/gastos/1" PUT $p.Expense {
        param($r)
        $body = $ExpenseBody.Clone()
        $body.comprobanteNro = "RBAC-UP-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
        $fixture = Invoke-Api POST "/gastos" $Tokens.admin $body
        if ($fixture.Status -notin 200, 201) { return $fixture }
        Invoke-Api PUT "/gastos/$($fixture.Data.idGasto)" $Tokens[$r] $body
    }

    Test-RbacCall $role "/ingresos" POST $p.Income {
        param($r)
        $trip = New-CompletedTrip
        if ($trip.Status -notin 200, 201) { return $trip }
        Invoke-Api POST "/ingresos" $Tokens[$r] @{
            idViaje = $trip.Data.idViaje
            idVehiculo = $VehicleId
            montoCobrado = 100
            costoManoObraAsociado = 0
            idMoneda = $Catalog.Moneda
            fechaPago = $isoLater
            idMetodoPago = $Catalog.MetodoPago
        }
    }
    Test-RbacCall $role "/ingresos/1" PUT $p.Income {
        param($r)
        $trip = New-CompletedTrip
        if ($trip.Status -notin 200, 201) { return $trip }
        $body = @{
            idViaje = $trip.Data.idViaje
            idVehiculo = $VehicleId
            montoCobrado = 100
            costoManoObraAsociado = 0
            idMoneda = $Catalog.Moneda
            fechaPago = $isoLater
            idMetodoPago = $Catalog.MetodoPago
        }
        $fixture = Invoke-Api POST "/ingresos" $Tokens.admin $body
        if ($fixture.Status -notin 200, 201) { return $fixture }
        Invoke-Api PUT "/ingresos/$($fixture.Data.idIngreso)" $Tokens[$r] $body
    }

    Test-RbacCall $role "/catalogos/repuestos" POST $p.Catalog {
        param($r) Invoke-Api POST "/catalogos/repuestos" $Tokens[$r] @{
            codigoRepuesto = "RBAC-$r-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
            nombreRepuesto = "Repuesto prueba RBAC"
            costoReferencial = 1
        }
    }
    Test-RbacCall $role "/catalogos/repuestos/1" PUT $p.Catalog {
        param($r)
        $fixture = Invoke-Api POST "/catalogos/repuestos" $Tokens.admin @{
            codigoRepuesto = "RBAC-UP-$([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
            nombreRepuesto = "Repuesto prueba RBAC"
            costoReferencial = 1
        }
        if ($fixture.Status -notin 200, 201) { return $fixture }
        Invoke-Api PUT "/catalogos/repuestos/$($fixture.Data.idRepuesto)" $Tokens[$r] @{
            codigoRepuesto = $fixture.Data.codigoRepuesto
            nombreRepuesto = "Repuesto actualizado RBAC"
            costoReferencial = 2
        }
    }
    Test-RbacCall $role "/catalogos/talleres" POST $p.Catalog {
        param($r) Invoke-Api POST "/catalogos/talleres" $Tokens[$r] @{
            nombreTaller = "Taller RBAC $r $([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
            direccion = "Dirección RBAC"
            telefono = "999999999"
            ruc = "20$($stamp.Substring($stamp.Length - 9).PadLeft(9, '0'))"
        }
    }
    Test-RbacCall $role "/catalogos/talleres/1" PUT $p.Catalog {
        param($r)
        $fixture = Invoke-Api POST "/catalogos/talleres" $Tokens.admin @{
            nombreTaller = "Taller RBAC $([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())"
            direccion = "Dirección RBAC"
            telefono = "999999999"
        }
        if ($fixture.Status -notin 200, 201) { return $fixture }
        Invoke-Api PUT "/catalogos/talleres/$($fixture.Data.idTaller)" $Tokens[$r] @{
            nombreTaller = "Taller actualizado RBAC"
            direccion = "Dirección RBAC"
            telefono = "999999999"
        }
    }

    # Los pagos pueden devolver 409/422 por estado o ventana de facturación.
    # Para admin se exige 2xx porque la matriz funcional indica pago permitido;
    # cualquier regla de negocio que lo impida quedará visible como discrepancia.
    Test-RbacCall $role "/mi-suscripcion/pagar-adelantado?idEmpresa=EMP-001" POST $p.Subscription {
        param($r) Invoke-Api POST "/mi-suscripcion/pagar-adelantado?idEmpresa=EMP-001" $Tokens[$r] @{
            idMetodoPago = $Catalog.MetodoPago
            referenciaPago = "RBAC-$stamp"
        }
    }

    $subscription = Invoke-Api GET "/mi-suscripcion?idEmpresa=EMP-001" $Tokens.admin
    $invoiceId = $subscription.Data.idFactura
    if ($invoiceId) {
        Test-RbacCall $role "/mi-suscripcion/{idFactura}/pagar?idEmpresa=EMP-001" POST $p.Subscription {
            param($r) Invoke-Api POST "/mi-suscripcion/$invoiceId/pagar?idEmpresa=EMP-001" $Tokens[$r] @{
                idMetodoPago = $Catalog.MetodoPago
                referenciaPago = "RBAC-$stamp"
            }
        }
    } else {
        Add-Result $role "/mi-suscripcion/{idFactura}/pagar?idEmpresa=EMP-001" POST "Factura pendiente requerida" 0 $false "No existe factura pendiente para probar este endpoint."
    }
}

# ---------------------------------------------------------------------------
# 4. Limpieza segura y resumen.
# ---------------------------------------------------------------------------
Write-Host "Ejecutando limpieza best-effort..." -ForegroundColor Cyan
foreach ($id in @($Created.Ordenes)) {
    try { [void](Invoke-Api DELETE "/ordenes-trabajo/$id" $Tokens.admin) } catch {}
}
foreach ($id in @($Created.Mantenimientos)) {
    try { [void](Invoke-Api DELETE "/mantenimientos/$id" $Tokens.admin) } catch {}
}
foreach ($id in @($Created.Viajes)) {
    try { [void](Invoke-Api DELETE "/viajes/$id" $Tokens.admin) } catch {}
}

Write-Host ""
Write-Host "RESULTADO DE MATRIZ RBAC" -ForegroundColor Cyan
$Results |
    Select-Object Rol, Endpoint, Metodo, Esperado, Real, Coincide |
    Format-Table -AutoSize -Wrap

$failures = @($Results | Where-Object { $_.Coincide -eq "❌" })
if ($failures.Count -gt 0) {
    Write-Host ""
    Write-Host "⚠️ $($failures.Count) discrepancia(s) encontrada(s):" -ForegroundColor Red
    $failures | ForEach-Object {
        Write-Host "- [$($_.Rol)] $($_.Metodo) $($_.Endpoint): esperado $($_.Esperado), real $($_.Real). $($_.Detalle)" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "Los registros sin endpoint DELETE quedan identificados con prefijo RBAC. Bases: vehículo $VehicleId, conductor $DriverId." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "✅ Toda la matriz RBAC coincide con lo esperado." -ForegroundColor Green
Write-Host "Los registros sin endpoint DELETE quedan identificados con prefijo RBAC. Bases: vehículo $VehicleId, conductor $DriverId." -ForegroundColor Yellow
exit 0

