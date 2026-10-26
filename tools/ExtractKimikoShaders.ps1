param([Parameter(Mandatory=$true)][string]$Source,[Parameter(Mandatory=$true)][string]$Destination)
$ErrorActionPreference='Stop'
$text=[IO.File]::ReadAllText((Resolve-Path -LiteralPath $Source))
$methods=@{}
foreach($m in [regex]::Matches($text,'private final String (s\d+)\(\).*?new String\[\]\{(.*?)\};\s*return',[Text.RegularExpressions.RegexOptions]::Singleline)){
    $lines=@([regex]::Matches($m.Groups[2].Value,'"(?:\\.|[^"\\])*"') | ForEach-Object {ConvertFrom-Json $_.Value})
    $methods[$m.Groups[1].Value]=($lines -join "`n")
}
$root=[IO.Path]::GetFullPath($Destination)
foreach($entry in [regex]::Matches($text,'sources\.put\("([^"]+)", INSTANCE\.(s\d+)\(\)\)')){
    $name=$entry.Groups[1].Value;$method=$entry.Groups[2].Value
    if(!$methods.ContainsKey($method)){throw "Missing shader method $method"}
    $target=[IO.Path]::GetFullPath((Join-Path $root $name))
    if(!$target.StartsWith($root+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Invalid output path'}
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target)) | Out-Null
    [IO.File]::WriteAllText($target,$methods[$method],[Text.UTF8Encoding]::new($false))
    Write-Output $name
}
