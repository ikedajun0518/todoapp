# solr/Setup-SolrSchema.ps1
[CmdletBinding()]
param(
  [string]$BaseUrl    = "http://localhost:8983/solr",
  [string]$Collection = "todoapp"
)

$ErrorActionPreference = "Stop"

function Invoke-JsonPost {
  param([string]$Uri, [hashtable]$Body)
  $json = $Body | ConvertTo-Json -Depth 10
  return Invoke-RestMethod -Method Post -Uri $Uri -ContentType "application/json" -Body $json
}

function Test-FieldExists {
  param([string]$FieldName)
  $uri = "$BaseUrl/$Collection/schema/fields/$FieldName"
  try {
    $null = Invoke-RestMethod -Method Get -Uri $uri -ContentType "application/json"
    return $true
  } catch {
    return $false
  }
}

function Ensure-Field {
  param([hashtable]$FieldDef)
  $name = $FieldDef.name
  if (Test-FieldExists -FieldName $name) {
    Write-Host "Field '$name' already exists. Skip."
    return
  }
  $schemaUrl = "$BaseUrl/$Collection/schema"
  Write-Host "Adding field '$name' ..."
  Invoke-JsonPost -Uri $schemaUrl -Body @{ "add-field" = @($FieldDef) } | Out-Null
  Write-Host "Added."
}

$fields = @(
  @{ name="type";          type="string";  stored=$true },
  @{ name="goal_id_l";     type="plong";   stored=$true },
  @{ name="goal_name_ja";  type="text_ja"; stored=$true },
  @{ name="task_name_ja";  type="text_ja"; stored=$true },
  @{ name="updated_at_dt"; type="pdate";   stored=$true }
)

foreach ($f in $fields) {
  Ensure-Field -FieldDef $f
}

Write-Host "Schema setup finished for collection '$Collection'."
