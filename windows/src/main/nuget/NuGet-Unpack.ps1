# Unpack .nupkg files resolved by Maven (target/libs/nuget/packages.list) into a NuGet fallback folder.
# NuGet does not handle same-version (-SNAPSHOT) packages from a feed, see Mountain Duck lib/msbuild/NuGet-Unpack.ps1
[CmdletBinding()]
param (
    [Parameter(Mandatory = $true)]
    [string]
    $PackagesListPath
)

if (-not (Test-Path -Path $PackagesListPath -PathType Leaf)) {
    return
}

# Use the Windows tar: a GNU tar earlier in PATH (Git for Windows) treats C:\ as a remote host
$Tar = Join-Path $env:SystemRoot 'System32\tar.exe'
$PackagesPath = Join-Path ([System.IO.Path]::GetDirectoryName($PackagesListPath)) 'packages'

foreach ($line in Get-Content $PackagesListPath) {
    $line = $line.Trim().Replace(':\', '$\').Split(':')
    if ($line.Count -lt 6) {
        continue
    }
    $version = $line[$line.Count - 3]
    $path = $line[$line.Count - 1].Replace('$\', ':\')

    $PackageName = [System.IO.Path]::GetFileNameWithoutExtension((& $Tar tf "$path" '*.nuspec')).ToLowerInvariant()
    $TargetDirectory = Join-Path $PackagesPath "$PackageName\$version"
    if (Test-Path $TargetDirectory) {
        if ((Get-Item $TargetDirectory).LastWriteTime -ge (Get-Item $path).LastWriteTime) {
            continue
        }
        Get-ChildItem $TargetDirectory | Remove-Item -Recurse -Force
    }
    else {
        New-Item -Force -Type Directory $TargetDirectory | Out-Null
    }
    & $Tar xf $path -C $TargetDirectory
    Set-Content (Join-Path $TargetDirectory '.nupkg.metadata') '{ "contentHash": "" }'
}
