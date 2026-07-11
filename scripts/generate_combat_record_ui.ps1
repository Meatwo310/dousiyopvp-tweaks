param(
    [switch] $StatsIconsOnly
)

$ErrorActionPreference = 'Stop'

Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$guiRoot = Join-Path $root 'src\main\resources\assets\dpvptweaks\textures\gui\combat_record'
$pagesDir = Join-Path $guiRoot 'pages'
$overlayDir = Join-Path $guiRoot 'overlay'
$statsIconsDir = Join-Path $guiRoot 'icons\stats'

$canvasWidth = 1280
$canvasHeight = 840

$colors = @{
    Background = [System.Drawing.Color]::FromArgb(255, 5, 12, 17)
    BackgroundBottom = [System.Drawing.Color]::FromArgb(255, 8, 19, 25)
    Surface = [System.Drawing.Color]::FromArgb(255, 9, 20, 27)
    SurfaceRaised = [System.Drawing.Color]::FromArgb(255, 12, 27, 35)
    Panel = [System.Drawing.Color]::FromArgb(255, 11, 24, 31)
    PanelRaised = [System.Drawing.Color]::FromArgb(255, 15, 31, 40)
    Stroke = [System.Drawing.Color]::FromArgb(255, 39, 59, 69)
    StrokeBright = [System.Drawing.Color]::FromArgb(255, 67, 91, 102)
    Accent = [System.Drawing.Color]::FromArgb(255, 0, 203, 226)
    AccentFill = [System.Drawing.Color]::FromArgb(255, 8, 55, 66)
    Green = [System.Drawing.Color]::FromArgb(255, 91, 215, 70)
    Amber = [System.Drawing.Color]::FromArgb(255, 239, 184, 43)
    Red = [System.Drawing.Color]::FromArgb(255, 231, 67, 67)
}

function New-RoundedPath {
    param(
        [float] $X,
        [float] $Y,
        [float] $Width,
        [float] $Height,
        [float] $Radius
    )

    $diameter = [Math]::Min($Radius * 2.0, [Math]::Min($Width, $Height))
    $path = [System.Drawing.Drawing2D.GraphicsPath]::new()
    if ($diameter -le 0.0) {
        $path.AddRectangle([System.Drawing.RectangleF]::new($X, $Y, $Width, $Height))
        return $path
    }

    $path.AddArc($X, $Y, $diameter, $diameter, 180.0, 90.0)
    $path.AddArc($X + $Width - $diameter, $Y, $diameter, $diameter, 270.0, 90.0)
    $path.AddArc($X + $Width - $diameter, $Y + $Height - $diameter, $diameter, $diameter, 0.0, 90.0)
    $path.AddArc($X, $Y + $Height - $diameter, $diameter, $diameter, 90.0, 90.0)
    $path.CloseFigure()
    return $path
}

function Fill-RoundedRectangle {
    param($Graphics, $Color, [float] $X, [float] $Y, [float] $Width, [float] $Height, [float] $Radius)

    $path = New-RoundedPath $X $Y $Width $Height $Radius
    $brush = [System.Drawing.SolidBrush]::new($Color)
    try {
        $Graphics.FillPath($brush, $path)
    }
    finally {
        $brush.Dispose()
        $path.Dispose()
    }
}

function Stroke-RoundedRectangle {
    param($Graphics, $Color, [float] $Thickness, [float] $X, [float] $Y, [float] $Width, [float] $Height, [float] $Radius)

    $path = New-RoundedPath $X $Y $Width $Height $Radius
    $pen = [System.Drawing.Pen]::new($Color, $Thickness)
    try {
        $pen.Alignment = [System.Drawing.Drawing2D.PenAlignment]::Inset
        $Graphics.DrawPath($pen, $path)
    }
    finally {
        $pen.Dispose()
        $path.Dispose()
    }
}

function Draw-Panel {
    param(
        $Graphics,
        [float] $X,
        [float] $Y,
        [float] $Width,
        [float] $Height,
        $FillColor = $colors.Panel,
        $StrokeColor = $colors.Stroke,
        [float] $Radius = 12.0,
        $AccentColor = $null
    )

    Fill-RoundedRectangle $Graphics $FillColor $X $Y $Width $Height $Radius
    Stroke-RoundedRectangle $Graphics $StrokeColor 3.0 $X $Y $Width $Height $Radius
    if ($null -ne $AccentColor) {
        $pen = [System.Drawing.Pen]::new($AccentColor, 4.0)
        try {
            $Graphics.DrawLine($pen, $X + 16.0, $Y + $Height - 10.0, $X + $Width - 16.0, $Y + $Height - 10.0)
        }
        finally {
            $pen.Dispose()
        }
    }
}

function New-PageCanvas {
    $bitmap = [System.Drawing.Bitmap]::new($canvasWidth, $canvasHeight, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

    $gradient = [System.Drawing.Drawing2D.LinearGradientBrush]::new(
        [System.Drawing.Rectangle]::new(0, 0, $canvasWidth, $canvasHeight),
        $colors.Background,
        $colors.BackgroundBottom,
        90.0
    )
    try {
        $graphics.FillRectangle($gradient, 0, 0, $canvasWidth, $canvasHeight)
    }
    finally {
        $gradient.Dispose()
    }

    $scanlinePen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(12, 112, 153, 168), 1.0)
    try {
        for ($y = 2; $y -lt $canvasHeight; $y += 4) {
            $graphics.DrawLine($scanlinePen, 2, $y, $canvasWidth - 3, $y)
        }
    }
    finally {
        $scanlinePen.Dispose()
    }

    return @{ Bitmap = $bitmap; Graphics = $graphics }
}

function Draw-HeaderIcon {
    param($Graphics)

    Draw-Panel $Graphics 24 18 64 64 $colors.PanelRaised $colors.StrokeBright 10
    $accentPen = [System.Drawing.Pen]::new($colors.Accent, 3.0)
    try {
        $Graphics.DrawEllipse($accentPen, 39, 33, 34, 34)
        $Graphics.DrawLine($accentPen, 56, 25, 56, 40)
        $Graphics.DrawLine($accentPen, 56, 60, 56, 75)
        $Graphics.DrawLine($accentPen, 31, 50, 46, 50)
        $Graphics.DrawLine($accentPen, 66, 50, 81, 50)
        $centerBrush = [System.Drawing.SolidBrush]::new($colors.Accent)
        try {
            $Graphics.FillEllipse($centerBrush, 53, 47, 6, 6)
        }
        finally {
            $centerBrush.Dispose()
        }
    }
    finally {
        $accentPen.Dispose()
    }
}

function Draw-CommonFrame {
    param($Graphics)

    Stroke-RoundedRectangle $Graphics $colors.StrokeBright 3.0 2 2 1276 836 12

    $headerBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(220, 8, 18, 24))
    try {
        $Graphics.FillRectangle($headerBrush, 4, 4, 1272, 88)
    }
    finally {
        $headerBrush.Dispose()
    }
    $separatorPen = [System.Drawing.Pen]::new($colors.Stroke, 3.0)
    try {
        $Graphics.DrawLine($separatorPen, 4, 92, 1276, 92)
        $Graphics.DrawLine($separatorPen, 4, 174, 1276, 174)
    }
    finally {
        $separatorPen.Dispose()
    }

    Draw-HeaderIcon $Graphics
    Draw-Panel $Graphics 808 16 368 64 $colors.Surface $colors.Stroke 10
    $searchPen = [System.Drawing.Pen]::new($colors.StrokeBright, 4.0)
    try {
        $Graphics.DrawEllipse($searchPen, 826, 32, 20, 20)
        $Graphics.DrawLine($searchPen, 842, 49, 854, 61)
    }
    finally {
        $searchPen.Dispose()
    }

    Draw-Panel $Graphics 1192 16 64 64 ([System.Drawing.Color]::FromArgb(255, 62, 22, 24)) ([System.Drawing.Color]::FromArgb(255, 150, 54, 55)) 10
    $closePen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255, 220, 226, 228), 4.0)
    try {
        $Graphics.DrawLine($closePen, 1210, 34, 1238, 62)
        $Graphics.DrawLine($closePen, 1238, 34, 1210, 62)
    }
    finally {
        $closePen.Dispose()
    }

    foreach ($tabX in @(328, 552, 776, 1000)) {
        Draw-Panel $Graphics $tabX 112 216 56 $colors.Surface $colors.Stroke 9
    }

    Draw-Panel $Graphics 4 178 304 588 $colors.Surface $colors.Stroke 12
    Draw-Panel $Graphics 312 178 966 588 $colors.Surface $colors.Stroke 12
    Draw-Panel $Graphics 4 770 1272 66 $colors.Surface $colors.Stroke 10
}

function Draw-OverviewPage {
    param($Graphics)

    Draw-Panel $Graphics 336 192 912 96 $colors.PanelRaised $colors.Stroke 12

    Draw-Panel $Graphics 336 328 288 176 $colors.Panel $colors.Stroke 12 $colors.Accent
    Draw-Panel $Graphics 648 328 288 176 $colors.Panel $colors.Stroke 12 $colors.Green
    Draw-Panel $Graphics 960 328 288 176 $colors.Panel $colors.Stroke 12 $colors.Amber

    Draw-Panel $Graphics 336 544 216 176 $colors.Panel $colors.Stroke 12 $colors.Accent
    Draw-Panel $Graphics 568 544 216 176 $colors.Panel $colors.Stroke 12 $colors.Red
    Draw-Panel $Graphics 800 544 216 176 $colors.Panel $colors.Stroke 12 $colors.Green
    Draw-Panel $Graphics 1032 544 216 176 $colors.Panel $colors.Stroke 12 $colors.Red
}

function Draw-HistoryPage {
    param($Graphics)

    Draw-Panel $Graphics 336 248 912 404 $colors.Panel $colors.Stroke 10
    $headerBrush = [System.Drawing.SolidBrush]::new($colors.PanelRaised)
    $rowBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(140, 9, 21, 28))
    $separatorPen = [System.Drawing.Pen]::new($colors.Stroke, 2.0)
    try {
        $Graphics.FillRectangle($headerBrush, 340, 260, 904, 48)
        for ($i = 0; $i -lt 5; $i++) {
            $rowY = 312 + $i * 64
            if (($i % 2) -eq 1) {
                $Graphics.FillRectangle($rowBrush, 340, $rowY, 904, 64)
            }
            if ($i -gt 0) {
                $Graphics.DrawLine($separatorPen, 340, $rowY, 1244, $rowY)
            }
        }
    }
    finally {
        $headerBrush.Dispose()
        $rowBrush.Dispose()
        $separatorPen.Dispose()
    }
    Draw-Panel $Graphics 336 664 912 80 $colors.PanelRaised $colors.Stroke 10
}

function Draw-RankingPage {
    param($Graphics)

    Draw-Panel $Graphics 336 264 912 392 $colors.Panel $colors.Stroke 10
    $headerBrush = [System.Drawing.SolidBrush]::new($colors.PanelRaised)
    $rowBrush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(140, 9, 21, 28))
    $separatorPen = [System.Drawing.Pen]::new($colors.Stroke, 2.0)
    try {
        $Graphics.FillRectangle($headerBrush, 340, 268, 904, 48)
        for ($i = 0; $i -lt 6; $i++) {
            $rowY = 320 + $i * 56
            if (($i % 2) -eq 1) {
                $Graphics.FillRectangle($rowBrush, 340, $rowY, 904, 56)
            }
            if ($i -gt 0) {
                $Graphics.DrawLine($separatorPen, 340, $rowY, 1244, $rowY)
            }
        }
    }
    finally {
        $headerBrush.Dispose()
        $rowBrush.Dispose()
        $separatorPen.Dispose()
    }
    Draw-Panel $Graphics 336 680 912 64 $colors.PanelRaised $colors.Stroke 10
}

function Draw-SettingsPage {
    param($Graphics)

    Draw-Panel $Graphics 368 216 840 448 $colors.Panel $colors.Stroke 12
    foreach ($rowY in @(280, 376, 472, 568)) {
        Draw-Panel $Graphics 380 $rowY 816 72 $colors.PanelRaised $colors.Stroke 9
    }
}

function Save-Page {
    param([string] $Name, [scriptblock] $DrawPage)

    $canvas = New-PageCanvas
    $bitmap = $canvas.Bitmap
    $graphics = $canvas.Graphics
    try {
        Draw-CommonFrame $graphics
        & $DrawPage $graphics
        $bitmap.Save((Join-Path $pagesDir ($Name + '_bg.png')), [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

function Save-Overlay {
    param(
        [string] $Name,
        [int] $Width,
        [int] $Height,
        $FillColor,
        $StrokeColor,
        [float] $Radius,
        $BottomAccent = $null
    )

    $bitmap = [System.Drawing.Bitmap]::new($Width, $Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([System.Drawing.Color]::Transparent)
    try {
        Fill-RoundedRectangle $graphics $FillColor 1 1 ($Width - 2) ($Height - 2) $Radius
        Stroke-RoundedRectangle $graphics $StrokeColor 3.0 1 1 ($Width - 2) ($Height - 2) $Radius
        if ($null -ne $BottomAccent) {
            $pen = [System.Drawing.Pen]::new($BottomAccent, 4.0)
            try {
                $graphics.DrawLine($pen, 12, $Height - 5, $Width - 12, $Height - 5)
            }
            finally {
                $pen.Dispose()
            }
        }
        $bitmap.Save((Join-Path $overlayDir ($Name + '.png')), [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

function Save-CloseHover {
    $bitmap = [System.Drawing.Bitmap]::new(64, 64, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([System.Drawing.Color]::Transparent)
    try {
        Fill-RoundedRectangle $graphics ([System.Drawing.Color]::FromArgb(245, 104, 28, 31)) 1 1 62 62 10
        Stroke-RoundedRectangle $graphics ([System.Drawing.Color]::FromArgb(255, 232, 75, 75)) 3.0 1 1 62 62 10
        $pen = [System.Drawing.Pen]::new([System.Drawing.Color]::FromArgb(255, 244, 247, 248), 4.0)
        try {
            $graphics.DrawLine($pen, 18, 18, 46, 46)
            $graphics.DrawLine($pen, 46, 18, 18, 46)
        }
        finally {
            $pen.Dispose()
        }
        $bitmap.Save((Join-Path $overlayDir 'close_hover.png'), [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

function Save-PixelIcon {
    param(
        [string] $Name,
        [string[]] $Rows
    )

    if ($Rows.Count -ne 16 -or ($Rows | Where-Object { $_.Length -ne 16 }).Count -gt 0) {
        throw "Pixel icon '$Name' must be a 16x16 character map."
    }

    $palette = @{
        'R' = [System.Drawing.Color]::FromArgb(255, 231, 67, 67)
        'H' = [System.Drawing.Color]::FromArgb(255, 255, 119, 119)
    }
    $bitmap = [System.Drawing.Bitmap]::new(16, 16, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        for ($y = 0; $y -lt 16; $y++) {
            for ($x = 0; $x -lt 16; $x++) {
                $pixel = [string] $Rows[$y][$x]
                if ($palette.ContainsKey($pixel)) {
                    $bitmap.SetPixel($x, $y, $palette[$pixel])
                }
            }
        }
        $bitmap.Save((Join-Path $statsIconsDir ($Name + '.png')), [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $bitmap.Dispose()
    }
}

Save-PixelIcon 'deaths' @(
    '................',
    '................',
    '.....RRRRRR.....',
    '...RRRRRRRRRR...',
    '..RRRRRRRRRRRR..',
    '..RRRRRRRRRRRR..',
    '..RR..RRRR..RR..',
    '..RR..RRRR..RR..',
    '..RRRRR..RRRRR..',
    '...RRRRRRRRRR...',
    '....RRRRRRRR....',
    '....RR.RR.RR....',
    '....RR.RR.RR....',
    '................',
    '................',
    '................'
)

Save-PixelIcon 'losses' @(
    '................',
    '....RRRRRRRR....',
    '...R........R...',
    '..R....H.....R..',
    '..R.....H....R..',
    '..R....H.....R..',
    '...R....H...R...',
    '...R...H....R...',
    '....R...H..R....',
    '....R..H...R....',
    '.....R..H.R.....',
    '.....R.H..R.....',
    '......R..R......',
    '.......RR.......',
    '................',
    '................'
)

if ($StatsIconsOnly) {
    Write-Output 'Combat record stat icons generated.'
    return
}

Save-Page 'overview' { param($graphics) Draw-OverviewPage $graphics }
Save-Page 'history' { param($graphics) Draw-HistoryPage $graphics }
Save-Page 'ranking' { param($graphics) Draw-RankingPage $graphics }
Save-Page 'settings' { param($graphics) Draw-SettingsPage $graphics }

$normalFill = [System.Drawing.Color]::FromArgb(205, 11, 24, 31)
$hoverFill = [System.Drawing.Color]::FromArgb(235, 15, 36, 46)
$selectedFill = [System.Drawing.Color]::FromArgb(245, 8, 55, 66)

Save-Overlay 'tab_hover' 216 56 $hoverFill $colors.StrokeBright 9
Save-Overlay 'tab_selected_glow' 216 56 $selectedFill $colors.Accent 9 $colors.Accent

Save-Overlay 'mode_row_normal' 240 56 $normalFill $colors.Stroke 8
Save-Overlay 'mode_row_hover' 240 56 $hoverFill $colors.StrokeBright 8
Save-Overlay 'mode_row_selected' 240 56 $selectedFill $colors.Accent 8 $colors.Accent

Save-Overlay 'filter_row_normal' 240 56 $normalFill $colors.Stroke 8
Save-Overlay 'filter_row_hover' 240 56 $hoverFill $colors.StrokeBright 8
Save-Overlay 'filter_row_selected' 240 56 $selectedFill $colors.Accent 8 $colors.Accent

Save-Overlay 'sort_button_normal' 176 56 $normalFill $colors.Stroke 8
Save-Overlay 'sort_button_selected' 176 56 $selectedFill $colors.Accent 8 $colors.Accent

Save-Overlay 'history_row_hover' 912 64 ([System.Drawing.Color]::FromArgb(215, 15, 36, 46)) $colors.StrokeBright 7
Save-Overlay 'history_row_selected' 912 64 ([System.Drawing.Color]::FromArgb(235, 8, 55, 66)) $colors.Accent 7
Save-Overlay 'ranking_row_hover' 912 56 ([System.Drawing.Color]::FromArgb(215, 15, 36, 46)) $colors.StrokeBright 7
Save-Overlay 'ranking_row_selected' 912 56 ([System.Drawing.Color]::FromArgb(235, 8, 55, 66)) $colors.Accent 7

Save-Overlay 'search_focus' 368 64 ([System.Drawing.Color]::FromArgb(100, 8, 55, 66)) $colors.Accent 10
Save-CloseHover

Write-Output 'Combat record UI textures generated.'
