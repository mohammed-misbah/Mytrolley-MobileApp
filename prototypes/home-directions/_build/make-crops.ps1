$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Resolve-Path (Join-Path $PSScriptRoot '..\..\..')
$srcDir = Join-Path $root 'app\src\main\res\drawable-nodpi'
$outDir = Join-Path $root 'prototypes\home-directions\assets'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# x, y, w, h are in source pixels (banners are 1200x558).
$crops = @(
  @{ n='eggplant';  s='banner_vegetables.png'; x=670; y=105; w=280; h=280 },
  @{ n='broccoli';  s='banner_vegetables.png'; x=900; y=45;  w=220; h=220 },
  @{ n='tomatoes';  s='banner_vegetables.png'; x=795; y=230; w=195; h=195 },
  @{ n='carrots';   s='banner_vegetables.png'; x=830; y=330; w=220; h=220 },
  @{ n='oranges';   s='banner_fruits.png';     x=620; y=120; w=300; h=300 },
  @{ n='grapes';    s='banner_fruits.png';     x=880; y=160; w=260; h=260 },
  @{ n='kiwi';      s='banner_fruits.png';     x=930; y=300; w=240; h=240 },
  @{ n='milk';      s='banner_dairy.png';      x=830; y=15;  w=240; h=320 },
  @{ n='cheese';    s='banner_dairy.png';      x=650; y=240; w=270; h=240 },
  @{ n='yogurt';    s='banner_dairy.png';      x=880; y=290; w=280; h=210 },
  @{ n='croissant'; s='banner_bakery.png';     x=630; y=210; w=370; h=260 },
  @{ n='sourdough'; s='banner_bakery.png';     x=835; y=90;  w=350; h=310 },
  @{ n='salmon';    s='banner_nonveg.png';     x=645; y=140; w=350; h=260 },
  @{ n='chicken';   s='banner_nonveg.png';     x=830; y=210; w=330; h=230 },
  @{ n='rice';      s='banner_groceries.png';  x=780; y=50;  w=250; h=350 },
  @{ n='lentils';   s='banner_groceries.png';  x=660; y=310; w=190; h=190 },
  @{ n='oats';      s='banner_groceries.png';  x=975; y=200; w=190; h=240 },
  @{ n='scoop';     s='banner_groceries.png';  x=770; y=390; w=280; h=170 },
  @{ n='headphones';s='banner_electronics.png';x=680; y=55;  w=320; h=320 },
  @{ n='speaker';   s='banner_electronics.png';x=895; y=315; w=270; h=200 }
)

$cache = @{}
foreach ($c in $crops) {
  if (-not $cache.ContainsKey($c.s)) {
    $cache[$c.s] = [System.Drawing.Image]::FromFile((Join-Path $srcDir $c.s))
  }
  $src = $cache[$c.s]
  $bmp = New-Object System.Drawing.Bitmap(($c.w * 2), ($c.h * 2))
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.DrawImage($src, (New-Object System.Drawing.Rectangle(0, 0, ($c.w * 2), ($c.h * 2))), (New-Object System.Drawing.Rectangle($c.x, $c.y, $c.w, $c.h)), [System.Drawing.GraphicsUnit]::Pixel)
  $g.Dispose()
  $bmp.Save((Join-Path $outDir ($c.n + '.png')), [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
}
foreach ($img in $cache.Values) { $img.Dispose() }

# Copy the real brand logo into the prototype surface.
Copy-Item (Join-Path $srcDir 'mytrolley_logo.png') (Join-Path $outDir 'mytrolley-logo.png') -Force

# Contact sheet for crop verification.
$cols = 5
$cell = 210
$rows = [math]::Ceiling($crops.Count / $cols)
$sheet = New-Object System.Drawing.Bitmap(($cols * $cell), ($rows * ($cell + 28)))
$g = [System.Drawing.Graphics]::FromImage($sheet)
$g.Clear([System.Drawing.Color]::FromArgb(250, 247, 240))
$font = New-Object System.Drawing.Font('Segoe UI', 10)
$brush = [System.Drawing.Brushes]::Black
for ($i = 0; $i -lt $crops.Count; $i++) {
  $c = $crops[$i]
  $cx = ($i % $cols) * $cell
  $cy = [math]::Floor($i / $cols) * ($cell + 28)
  $im = [System.Drawing.Image]::FromFile((Join-Path $outDir ($c.n + '.png')))
  $g.DrawImage($im, $cx + 10, $cy + 6, 190, 190)
  $g.DrawString($c.n, $font, $brush, $cx + 10, $cy + 198)
  $im.Dispose()
}
$g.Dispose()
$sheet.Save((Join-Path $outDir '_contact-sheet.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$sheet.Dispose()

Write-Output "Generated $($crops.Count) crops + logo + contact sheet in $outDir"
