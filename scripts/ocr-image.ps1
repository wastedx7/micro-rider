Add-Type -AssemblyName System.Runtime.WindowsRuntime

# Load WinRT types
$null = [Windows.Media.Ocr.OcrEngine,Windows.Foundation,ContentType=WindowsRuntime]
$null = [Windows.Graphics.Imaging.BitmapDecoder,Windows.Foundation,ContentType=WindowsRuntime]
$null = [Windows.Storage.Streams.RandomAccessStream,Windows.Storage,ContentType=WindowsRuntime]
$null = [Windows.Globalization.Language,Windows.Globalization,ContentType=WindowsRuntime]

# WinRT helper to await async operations
$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]

Function Await($WinRtTask, $ResultType) {
    $asTask = $asTaskGeneric.MakeGenericMethod($ResultType)
    $netTask = $asTask.Invoke($null, @($WinRtTask))
    $netTask.Wait(-1) | Out-Null
    $netTask.Result
}

# Load image
$file = Get-Item -Path "images\image.png"
$stream = [System.IO.File]::OpenRead($file.FullName)

# Use FileRandomAccessStream
$fileStream = [System.IO.WindowsRuntimeStreamExtensions]::AsRandomAccessStream($stream)
Write-Output "File stream converted"

# Decode image
$decoder = Await ([Windows.Graphics.Imaging.BitmapDecoder]::CreateAsync($fileStream)) ([Windows.Graphics.Imaging.BitmapDecoder])
Write-Output "Image decoded: $($decoder.PixelWidth)x$($decoder.PixelHeight)"

# Get software bitmap
$softwareBitmap = Await ($decoder.GetSoftwareBitmapAsync()) ([Windows.Graphics.Imaging.SoftwareBitmap])
Write-Output "Software bitmap obtained"

# Create OCR engine
$engine = [Windows.Media.Ocr.OcrEngine]::TryCreateFromUserProfileLanguages()
if ($engine -eq $null) {
    $engine = [Windows.Media.Ocr.OcrEngine]::TryCreateFromLanguage([Windows.Globalization.Language]::new("en-US"))
}
Write-Output "OCR engine created"

# Run OCR
$ocrResult = Await ($engine.RecognizeAsync($softwareBitmap)) ([Windows.Media.Ocr.OcrResult])
Write-Output "========== OCR RESULT START =========="
$lineNumber = 1
foreach ($line in $ocrResult.Lines) {
    $x = [math]::Round($line.Words[0].BoundingRect.X)
    $y = [math]::Round($line.Words[0].BoundingRect.Y)
    Write-Output "L$lineNumber (x=$x, y=$y): $($line.Text)"
    $lineNumber++
}
Write-Output "========== OCR RESULT END =========="

# Cleanup
$stream.Close()
