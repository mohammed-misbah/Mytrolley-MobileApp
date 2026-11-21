$ErrorActionPreference = 'Stop'

# Builds a 100-product rich catalogue (data + images) for DigitalOcean Spaces.
# Sources public product data from DummyJSON. Personal/reference use only.
# Nothing in this script touches the Android app, Django backend or database.

$root = Join-Path (Get-Location).Path 'catalogue'
$imagesRoot = Join-Path $root 'images'
New-Item -ItemType Directory -Force -Path $imagesRoot | Out-Null

$currencyRate = 3.6725  # USD -> AED
$sourceCategories = @(
    'groceries',
    'kitchen-accessories',
    'smartphones',
    'mobile-accessories',
    'laptops',
    'tablets',
    'home-decoration'
)

$products = @()
foreach ($category in $sourceCategories) {
    $url = "https://dummyjson.com/products/category/$category`?limit=0"
    $response = Invoke-RestMethod -Uri $url -TimeoutSec 60
    $products += $response.products
    Write-Output "Fetched $category : $($response.products.Count)"
}

if ($products.Count -ne 100) {
    throw "Expected 100 products, got $($products.Count)"
}

function Get-AppCategory($product) {
    $tags = ($product.tags -join ' ').ToLowerInvariant()
    if ($product.category -in @('smartphones', 'mobile-accessories', 'laptops', 'tablets')) { return 'Electronics' }
    if ($tags -match 'fruits') { return 'Fruits' }
    if ($tags -match 'vegetables') { return 'Vegetables' }
    if ($tags -match 'dairy|eggs') { return 'Dairy & Eggs' }
    if ($tags -match 'meat|seafood') { return 'Chicken' }
    if ($tags -match 'beverages|coffee') { return 'Beverages' }
    if ($tags -match 'bakery|bread') { return 'Bakery' }
    return 'Groceries'
}

function Get-Diet($product) {
    $tags = ($product.tags -join ' ').ToLowerInvariant()
    if ($tags -match 'meat|seafood|chicken|beef') { return 'Non-veg' }
    return 'Veg'
}

function Get-Storage($product) {
    $category = Get-AppCategory $product
    if ($category -in @('Dairy & Eggs', 'Chicken', 'Fruits', 'Vegetables')) {
        return 'Keep refrigerated below 4 C. Consume before the best-before date.'
    }
    if ($category -eq 'Electronics') {
        return 'Store in a dry place away from direct sunlight and moisture.'
    }
    return 'Store in a cool, dry place away from direct sunlight.'
}

function Get-ShelfLife($product) {
    $category = Get-AppCategory $product
    switch ($category) {
        'Fruits' { return '5-7 days when refrigerated' }
        'Vegetables' { return '5-10 days when refrigerated' }
        'Dairy & Eggs' { return '7-21 days when refrigerated' }
        'Chicken' { return 'Use within 3 days of purchase (frozen: 3 months)' }
        'Bakery' { return '2-4 days at ambient temperature' }
        'Groceries' { return '6-12 months unopened' }
        'Beverages' { return '6-12 months unopened' }
        'Electronics' { return 'Not applicable' }
        default { return '6-12 months unopened' }
    }
}

function Get-Origin($product) {
    $origins = @('UAE', 'Saudi Arabia', 'India', 'Turkey', 'Egypt', 'China', 'Pakistan', 'Jordan')
    return $origins[($product.id % $origins.Count)]
}

function ConvertTo-Slug($text) {
    $slug = $text.ToLowerInvariant() -replace '[^a-z0-9]+', '-'
    return $slug.Trim('-')
}

$stores = @(
    @{ id = 1; name = 'MyTrolley Abu Dhabi'; code = 'MT-AUH-003'; factor = 1.00 },
    @{ id = 2; name = 'MyTrolley Ajman';      code = 'MT-AJM-004'; factor = 0.96 },
    @{ id = 3; name = 'MyTrolley Dubai';      code = 'MT-DXB-001'; factor = 1.04 },
    @{ id = 4; name = 'MyTrolley Sharjah';    code = 'MT-SHJ-002'; factor = 0.98 }
)

$categoryNames = $products | ForEach-Object { Get-AppCategory $_ } | Sort-Object -Unique
$categories = @()
$categoryId = 1
foreach ($name in $categoryNames) {
    $categories += @{
        id = $categoryId
        name = $name
        slug = ConvertTo-Slug $name
        description = "$name products selected for the MyTrolley catalogue."
        productCount = @($products | Where-Object { (Get-AppCategory $_) -eq $name }).Count
    }
    $categoryId++
}

$outputProducts = @()
$imageCount = 0
$failedImages = 0

foreach ($product in $products) {
    $appCategory = Get-AppCategory $product
    $categoryRecord = $categories | Where-Object { $_.name -eq $appCategory } | Select-Object -First 1
    $basePrice = [math]::Round([double]$product.price * $currencyRate, 2)
    $productDir = Join-Path $imagesRoot ([string]$product.id)
    New-Item -ItemType Directory -Force -Path $productDir | Out-Null

    $images = @()
    $candidates = @()
    if ($product.images) { $candidates += $product.images }
    if ($product.thumbnail) { $candidates += $product.thumbnail }

    $position = 1
    foreach ($url in $candidates) {
        if ($position -gt 3) { break }
        $extension = if ($url -match '\.(\w+)$') { $Matches[1] } else { 'webp' }
        $fileName = "$position.$extension"
        $destination = Join-Path $productDir $fileName
        try {
            Invoke-WebRequest -Uri $url -OutFile $destination -TimeoutSec 30 -ErrorAction Stop
            $images += @{
                id = ($product.id * 10 + $position)
                url = "images/$($product.id)/$fileName"
                altText = "$($product.title) - view $position"
                displayOrder = $position
                isPrimary = ($position -eq 1)
            }
            $imageCount++
            $position++
        } catch {
            $failedImages++
        }
    }

    $attributes = @(
        @{ name = 'Brand'; value = if ($product.brand) { $product.brand } else { 'MyTrolley Selection' } },
        @{ name = 'SKU'; value = [string]$product.sku },
        @{ name = 'Diet'; value = Get-Diet $product },
        @{ name = 'Country of origin'; value = Get-Origin $product },
        @{ name = 'Storage'; value = Get-Storage $product },
        @{ name = 'Shelf life'; value = Get-ShelfLife $product },
        @{ name = 'Pack size'; value = "$($product.weight) kg / units" },
        @{ name = 'Customer rating'; value = "$($product.rating) / 5" }
    )

    $storeOffers = @()
    foreach ($store in $stores) {
        $storeOffers += @{
            store = @{ id = $store.id; name = $store.name; code = $store.code }
            price = ('{0:N2}' -f ([double]$basePrice * [double]$store.factor)).Replace(',', '')
            quantity = [int]([math]::Round([double]$product.stock * (0.4 + ($store.id * 0.15))))
            isAvailable = $true
        }
    }

    $outputProducts += @{
        id = $product.id
        name = $product.title
        slug = ConvertTo-Slug $product.title
        description = $product.description
        category = @{ id = $categoryRecord.id; name = $appCategory; slug = $categoryRecord.slug }
        isActive = $true
        brand = if ($product.brand) { $product.brand } else { 'MyTrolley Selection' }
        tags = $product.tags
        attributes = $attributes
        images = $images
        stores = $storeOffers
        startingPrice = ('{0:N2}' -f $basePrice).Replace(',', '')
        source = @{ api = 'DummyJSON'; sourceId = $product.id; category = $product.category }
    }
}

$manifest = @{
    generatedAt = (Get-Date).ToString('o')
    purpose = 'Personal / reference catalogue for MyTrolley. Not the live production catalogue.'
    currency = 'AED'
    source = 'Public DummyJSON product API; third-party images for personal use.'
    stores = $stores | ForEach-Object { @{ id = $_.id; name = $_.name; code = $_.code } }
    categories = $categories
    products = $outputProducts
}

$manifest | ConvertTo-Json -Depth 12 | Set-Content -Path (Join-Path $root 'catalogue.json') -Encoding UTF8

$readme = @"
# MyTrolley personal-use catalogue

- **100 products**, rich data, studio-style product images.
- ``catalogue.json`` is the manifest; images are under ``images/<product-id>/``.
- Prices are in AED and are **illustrative**, not live MyTrolley prices.
- Source: public DummyJSON product API (third-party images, personal use only).
- This dataset is **not** connected to the Android app, Django backend or database.
- Generated: $((Get-Date).ToString('o'))
"@
Set-Content -Path (Join-Path $root 'README.md') -Value $readme -Encoding UTF8

Write-Output "DONE: products=$($outputProducts.Count), images=$imageCount, failedImageAttempts=$failedImages"
Write-Output "Output: $root"
