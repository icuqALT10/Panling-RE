Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$textureRoot = Join-Path $projectRoot 'src\main\resources\assets\panlingre\textures'

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
                if ($pixels[$row + ($x * 4) + 3] -gt 16) {
                    if ($x -lt $minX) { $minX = $x }
                    if ($x -gt $maxX) { $maxX = $x }
                    if ($y -lt $minY) { $minY = $y }
                    if ($y -gt $maxY) { $maxY = $y }
                }
            }
        }

        if ($maxX -lt $minX -or $maxY -lt $minY) {
            throw "No visible pixels found in image."
        }

        return [System.Drawing.Rectangle]::new($minX, $minY, $maxX - $minX + 1, $maxY - $minY + 1)
    }
    finally {
        $Bitmap.UnlockBits($data)
    }
}

function Save-ItemTexture {
    param(
        [string] $InputPath,
        [string] $OutputPath,
        [string] $PreviewPath
    )

    $source = [System.Drawing.Bitmap]::FromFile($InputPath)
    try {
        $bounds = Get-AlphaBounds $source
        $cropped = $source.Clone($bounds, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $scale = [Math]::Min(28.0 / $cropped.Width, 30.0 / $cropped.Height)
            $targetWidth = [Math]::Max(1, [int][Math]::Round($cropped.Width * $scale))
            $targetHeight = [Math]::Max(1, [int][Math]::Round($cropped.Height * $scale))
            $targetX = [int][Math]::Floor((32 - $targetWidth) / 2.0)
            $targetY = 31 - $targetHeight

            $output = [System.Drawing.Bitmap]::new(32, 32, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            try {
                $graphics = [System.Drawing.Graphics]::FromImage($output)
                try {
                    $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighSpeed
                    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
                    $graphics.Clear([System.Drawing.Color]::Transparent)
                    $destination = [System.Drawing.Rectangle]::new($targetX, $targetY, $targetWidth, $targetHeight)
                    $graphics.DrawImage(
                        $cropped,
                        $destination,
                        0,
                        0,
                        $cropped.Width,
                        $cropped.Height,
                        [System.Drawing.GraphicsUnit]::Pixel
                    )
                }
                finally {
                    $graphics.Dispose()
                }

                $temporaryOutput = "$OutputPath.new.png"
                $output.Save($temporaryOutput, [System.Drawing.Imaging.ImageFormat]::Png)
                Move-Item -LiteralPath $temporaryOutput -Destination $OutputPath -Force

                $preview = [System.Drawing.Bitmap]::new(320, 320, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
                try {
                    $previewGraphics = [System.Drawing.Graphics]::FromImage($preview)
                    try {
                        $previewGraphics.Clear([System.Drawing.Color]::FromArgb(255, 30, 30, 32))
                        $previewGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                        $previewGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                        $previewGraphics.DrawImage($output, [System.Drawing.Rectangle]::new(0, 0, 320, 320))
                    }
                    finally {
                        $previewGraphics.Dispose()
                    }
                    $preview.Save($PreviewPath, [System.Drawing.Imaging.ImageFormat]::Png)
                }
                finally {
                    $preview.Dispose()
                }

                Write-Output "$(Split-Path $OutputPath -Leaf): crop=$($bounds.Width)x$($bounds.Height), placed=${targetWidth}x${targetHeight} at ($targetX,$targetY)"
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

function Save-GuiTexture {
    param(
        [string] $GuiPath,
        [string] $InventoryPath,
        [string] $PreviewPath
    )

    $gui = [System.Drawing.Bitmap]::FromFile($GuiPath)
    $inventory = [System.Drawing.Bitmap]::FromFile($InventoryPath)
    try {
        $graphics = [System.Drawing.Graphics]::FromImage($gui)
        try {
            $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
            $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
            $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half

            $baseBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 68, 66, 71))
            $lightBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 91, 88, 95))
            $shadowBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(255, 37, 35, 40))
            try {
                $graphics.FillRectangle($baseBrush, 0, 0, 352, 166)
                $graphics.FillRectangle($lightBrush, 0, 0, 352, 2)
                $graphics.FillRectangle($lightBrush, 0, 0, 2, 166)
                $graphics.FillRectangle($shadowBrush, 0, 164, 352, 2)
                $graphics.FillRectangle($shadowBrush, 350, 0, 2, 166)
            }
            finally {
                $baseBrush.Dispose()
                $lightBrush.Dispose()
                $shadowBrush.Dispose()
            }

            $slotSource = [System.Drawing.Rectangle]::new(14, 166, 36, 36)
            foreach ($slotX in @(50, 104, 158, 212, 266)) {
                $graphics.DrawImage(
                    $inventory,
                    [System.Drawing.Rectangle]::new($slotX, 54, 36, 36),
                    $slotSource,
                    [System.Drawing.GraphicsUnit]::Pixel
                )
            }
        }
        finally {
            $graphics.Dispose()
        }

        $temporaryGui = "$GuiPath.new.png"
        $gui.Save($temporaryGui, [System.Drawing.Imaging.ImageFormat]::Png)

        $preview = [System.Drawing.Bitmap]::new(176, 166, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        try {
            $previewGraphics = [System.Drawing.Graphics]::FromImage($preview)
            try {
                $previewGraphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
                $previewGraphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
                $previewGraphics.DrawImage(
                    $gui,
                    [System.Drawing.Rectangle]::new(0, 0, 176, 166),
                    [System.Drawing.Rectangle]::new(0, 0, 352, 332),
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
    }
    finally {
        $gui.Dispose()
        $inventory.Dispose()
    }

    Move-Item -LiteralPath $temporaryGui -Destination $GuiPath -Force
}

$itemTextureDirectory = Join-Path $textureRoot 'item'
$guiTexturePath = Join-Path $textureRoot 'gui\fu_zhi_bag.png'
$inventoryTexturePath = Get-ChildItem `
    -LiteralPath (Join-Path $projectRoot 'run\resourcepacks') `
    -Recurse `
    -Filter 'inventory.png' |
    Where-Object { $_.FullName -like '*\assets\minecraft\textures\gui\container\inventory.png' } |
    Select-Object -First 1 -ExpandProperty FullName

if (-not $inventoryTexturePath) {
    throw 'Could not locate the resource-pack inventory texture.'
}

Save-ItemTexture `
    -InputPath (Join-Path $PSScriptRoot 'empty_alpha.png') `
    -OutputPath (Join-Path $itemTextureDirectory 'fu_zhi_bao.png') `
    -PreviewPath (Join-Path $PSScriptRoot 'empty_32_preview.png')

Save-ItemTexture `
    -InputPath (Join-Path $PSScriptRoot 'filled_alpha.png') `
    -OutputPath (Join-Path $itemTextureDirectory 'fu_zhi_bao_filled.png') `
    -PreviewPath (Join-Path $PSScriptRoot 'filled_32_preview.png')

Save-GuiTexture `
    -GuiPath $guiTexturePath `
    -InventoryPath $inventoryTexturePath `
    -PreviewPath (Join-Path $PSScriptRoot 'gui_preview.png')
