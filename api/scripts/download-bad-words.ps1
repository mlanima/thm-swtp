$langs = @('ar', 'cs', 'da', 'de', 'en', 'eo', 'es', 'fa', 'fi', 'fil', 'fr', 'fr-CA-u-sd-caqc', 'hi', 'hu', 'it', 'ja', 'kab', 'ko', 'nl', 'no', 'pl', 'pt', 'ru', 'sv', 'th', 'tlh', 'tr', 'zh')
$dir = 'src/main/resources/bad-words'

New-Item -ItemType Directory -Path $dir -Force | Out-Null

$count = 0
foreach ($lang in $langs) {
    $out = Join-Path $dir $lang
    if (-not (Test-Path $out)) {
        Write-Host "Downloading $lang..."
        Invoke-WebRequest -Uri "https://raw.githubusercontent.com/LDNOOBW/List-of-Dirty-Naughty-Obscene-and-Otherwise-Bad-Words/master/$lang" -OutFile $out
        $count++
    }
}

Write-Host "Done. Downloaded $count language files."
