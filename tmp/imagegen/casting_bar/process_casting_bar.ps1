Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'

function Get-AlphaBounds {
    param([System.Drawing.Bitmap] $Bitmap)

    $rect = [System.Drawing.Rectangle]::new(0, 0, $Bitmap.Width, $Bitmap.Height)
    $data = $Bitmap.LockBits(
        $rect,
        [System.Drawing.Imaging.ImageLockMode]::ReadOnly,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    try {
        $stride = [Math]::Abs($data.Stride)
        $pixels = [byte[]]::new($stride * $Bitmap.Height)
        [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $pixels, 0, $pixels.Length)
        $minX = $Bitmap.Width
        $minY = $Bitmap.Height
        $maxX = -1
        $maxY = -1

        for ($y = 0; $y -lt $Bitmap.Height; $y++) {
            $row = if ($data.Stride -gt 0) { $y * $stride } else { ($Bitmap.Height - 1 - $y) * $stride }
            for ($x = 0; $x -lt $Bitmap.Width; $x++) {
                if ($pixels[$row + $x * 4 + 3] -gt 16) {
                    if ($x -lt $minX) { $minX = $x }
                    if ($x -gt $maxX) { $maxX = $x }
                    if ($y -lt $minY) { $minY = $y }
                    if ($y -gt $maxY) { $maxY = $y }
                }
            }
        }
        if ($maxX -lt $minX -or $maxY -lt $minY) { throw 'No visible pixels found.' }
        return [System.Drawing.Rectangle]::new($minX, $minY, $maxX - $minX + 1, $maxY - $minY + 1)
    }
    finally {
        $Bitmap.UnlockBits($data)
    }
}

function Convert-CastingBar {
    param([string] $InputPath, [string] $OutputPath, [string] $PreviewPath)

    $source = [System.Drawing.Bitmap]::FromFile($InputPath)
    try {
        $bounds = Get-AlphaBounds $source
        $cropped = $source.Clone($bounds, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $output = [System.Drawing.Bitmap]::new(160, 22, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            try {
                $graphics = [System.Drawing.Graphics]::FromImage($output)
                try {
                    $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
                    $graphics.Clear([System.Drawing.Color]::Transparent)
                    $graphics.DrawImage(
                        $cropped,
                        [System.Drawing.Rectangle]::new(2, 2, 156, 18),
                        0, 0, $cropped.Width, $cropped.Height,
                        [System.Drawing.GraphicsUnit]::Pixel
                    )
                }
                finally {
                    $graphics.Dispose()
                }

                $temporary = "$OutputPath.new.png"
                $output.Save($temporary, [System.Drawing.Imaging.ImageFormat]::Png)
                Move-Item -LiteralPath $temporary -Destination $OutputPath -Force

                $preview = [System.Drawing.Bitmap]::new(720, 100, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
                try {
                    $previewGraphics = [System.Drawing.Graphics]::FromImage($preview)
                    try {
                        $previewGraphics.Clear([System.Drawing.Color]::FromArgb(255, 28, 28, 32))
                        $previewGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                        $previewGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                        $previewGraphics.DrawImage(
                            $output,
                            [System.Drawing.Rectangle]::new(0, 0, 720, 100),
                            0, 0, 160, 22,
                            [System.Drawing.GraphicsUnit]::Pixel
                        )
                    }
                    finally {
                        $previewGraphics.Dispose()
                    }
                    $preview.Save($PreviewPath, [System.Drawing.Imaging.ImageFormat]::Png)
                }
                finally {
                    $preview.Dispose()
                }

                Write-Output "$(Split-Path $OutputPath -Leaf): crop=$($bounds.Width)x$($bounds.Height) -> 160x22"
            }
            finally {
                $output.Dispose()
            }
        }
        finally {
            $cropped.Dispose()
        }
    }
    finally {
        $source.Dispose()
    }
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$hudDirectory = Join-Path $projectRoot 'src\main\resources\assets\panlingre\textures\gui\hud'

Convert-CastingBar `
    -InputPath (Join-Path $PSScriptRoot 'casting_empty_alpha.png') `
    -OutputPath (Join-Path $hudDirectory 'casting_empty.png') `
    -PreviewPath (Join-Path $PSScriptRoot 'casting_empty_preview.png')

Convert-CastingBar `
    -InputPath (Join-Path $PSScriptRoot 'casting_full_alpha.png') `
    -OutputPath (Join-Path $hudDirectory 'casting_full.png') `
    -PreviewPath (Join-Path $PSScriptRoot 'casting_full_preview.png')
