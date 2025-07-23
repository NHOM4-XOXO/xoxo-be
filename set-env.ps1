# set-env.ps1
# Script này sẽ đọc file .env và set từng biến thành biến môi trường cho phiên PowerShell hiện tại

Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*#') { return } # Bỏ qua dòng comment
    if ($_ -match '^\s*$') { return } # Bỏ qua dòng trống
    if ($_ -match '^\s*([^=]+)\s*=\s*(.*)\s*$') {
        $key = $matches[1].Trim()
        $value = $matches[2].Trim('"').Trim("'")
        [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
        Write-Host "Set $key=$value"
    }
}