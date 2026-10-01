param(
    [switch]$Force
)

$ErrorActionPreference = "Stop"

function Fail($Message) {
    Write-Host "ERROR: $Message" -ForegroundColor Red
    exit 1
}

# Check Git availability
if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    Fail "Git is not available in PATH."
}

# Check whether we are inside a Git repository
$insideRepo = git rev-parse --is-inside-work-tree 2>$null
if ($LASTEXITCODE -ne 0 -or $insideRepo -ne "true") {
    Fail "Run this script from inside the Git repository."
}

$repoRoot = git rev-parse --show-toplevel
Set-Location $repoRoot

Write-Host "Repository: $repoRoot" -ForegroundColor Cyan
Write-Host ""

# Find files that are both tracked and ignored by the current .gitignore rules.
$files = @(git ls-files -ci --exclude-standard)

if ($LASTEXITCODE -ne 0) {
    Fail "Could not read tracked ignored files."
}

if ($files.Count -eq 0) {
    Write-Host "No tracked files currently match .gitignore." -ForegroundColor Green
    exit 0
}

Write-Host "The following tracked files now match .gitignore:" -ForegroundColor Yellow
$files | ForEach-Object { Write-Host "  $_" }
Write-Host ""
Write-Host "They will be removed ONLY from the Git index." -ForegroundColor Yellow
Write-Host "The files will remain on disk." -ForegroundColor Yellow
Write-Host ""

if (-not $Force) {
    $answer = Read-Host "Continue? [y/N]"
    if ($answer -notmatch '^(y|yes|t|tak)$') {
        Write-Host "Cancelled."
        exit 0
    }
}

$removed = 0
foreach ($file in $files) {
    git rm --cached --ignore-unmatch -- "$file"
    if ($LASTEXITCODE -ne 0) {
        Fail "Failed while removing from Git index: $file"
    }
    $removed++
}

Write-Host ""
Write-Host "Done. Removed $removed tracked ignored file(s) from the Git index." -ForegroundColor Green
Write-Host ""
Write-Host "Review changes:" -ForegroundColor Cyan
Write-Host "  git status"
Write-Host ""
Write-Host "Then commit them, for example:" -ForegroundColor Cyan
Write-Host '  git add .gitignore'
Write-Host '  git commit -m "Update .gitignore and remove ignored files from repository"'
