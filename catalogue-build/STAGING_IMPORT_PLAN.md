# MyTrolley — Staging Catalogue Import Plan (for approval)

**Status: PLAN ONLY. No database records, backend files, Android data files or settings were changed.**
Prepared: 2026-11-21. Reference dataset: `catalogue/v1/` on DigitalOcean Spaces.

---

## 1. Objective and hard constraints

Import the 100-product catalogue (data + images) currently hosted at
`https://mytrolley-product-management-media.sgp1.digitaloceanspaces.com/catalogue/v1/`
into an **isolated development/staging** Django database so the existing MyTrolley
Android app can display it for demonstration and review.

Non-negotiable rules carried forward from `AGENTS.md` and your instructions:

- **Never touch the live production database** without explicit written approval.
- Preserve every existing product, store, category, API contract and business rule.
- Never overwrite or delete existing data. Additive inserts only.
- Keep the Android app on its existing Retrofit/DTO/API architecture.
- Clearly identify demo products and illustrative prices in the app itself.
- Dry-run validation must pass before any import.
- Back up first; provide a tested rollback path.

---

## 2. Recommended target architecture

```
Spaces catalogue/v1/  ──(download/import)──▶  NEW isolated PostgreSQL DB
                                                  │  mytrolley_staging
                                                  ▼
                                        Django (existing, unchanged code)
                                        catalog.settings.development
                                        /api/v1/public/... endpoints
                                                  │
                                                  ▼
                                  Android demo build (existing app code)
                          -PmytrolleyApiUrl=http://<staging-host>:8000/api/v1/
```

Why this is the safe path:

- The Android app already supports a base-URL override (`-PmytrolleyApiUrl=...`), so its
  networking, DTOs, repository, ViewModels and UI stay untouched.
- No backend model/serializer/view/URL change is required.
- The live production DB and live API remain completely untouched.

**A separate staging DB is mandatory.** Do not point this work at `DB_NAME` from the
current `.env` until its value is confirmed to be non-production.

---

## 3. Backend facts established (read-only inspection)

Backend root: `C:\Users\user\OneDrive\Documents\MyTrolleyApp\backend`

### Models (`catalog/models.py`)
| Model | Key fields / constraints relevant to import |
|---|---|
| `Category` | `name` unique (DB + `Lower("name")` CI constraint), `slug` unique, `parent` self-FK PROTECT, `is_active`. `save()` generates slug; **`bulk_create()` would leave empty slugs and collide**. |
| `Store` | `code` unique + CI unique; `save()` uppercases code. Match existing stores by code; do not create stores. |
| `Product` | `name`, `slug` unique, `category` FK PROTECT, `is_active`, `deleted_at` (soft delete). `objects` hides soft-deleted rows; `all_objects` is unfiltered. `save()` generates slug; **`bulk_create()` needs explicit slugs**. |
| `ProductAttribute` | FK `product`, `name`, `value` (required), unique `(product, name)` + CI unique per product. |
| `ProductImage` | FK `product`, `image` ImageField, `alt_text`, `display_order`, `is_primary`. `save()` enforces one primary per product; **`bulk_create()` bypasses this**. |
| `ProductStore` | FK `product` + `store`, `price` Decimal(12,4) `>= 0`, `quantity` PositiveInt, `is_available`, unique `(product, store)`. `save()` calls `full_clean()` and quantizes price to **2 decimals**. There is **no global price field** — every price is store-specific. |

### Public API visibility rule (from `catalog/public_views.py`)
A product appears publicly only if:
- `is_active=True` and `deleted_at IS NULL`
- category (and all ancestors) `is_active=True`
- ≥1 `ProductStore` with `is_available=True` whose `store.is_active=True`

Endpoints (unchanged): `GET /api/v1/public/stores/`, `/categories/`, `/products/`,
`/products/<pk>/`, `/images/<id>/`. Product image URLs are **proxy URLs**
(`/api/v1/public/images/<id>/`), not raw storage URLs.

### Settings / storage
- `manage.py` defaults to `config.settings.development`; WSGI/ASGI default to production.
- `DATABASES` from env: `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_HOST`, `DB_PORT`.
- `MEDIA_ROOT = BASE_DIR`; `USE_SPACES_STORAGE=true` switches default storage to
  `storages.backends.s3.S3Storage` with `location=""` and signed URLs.
- `config.settings.test` forces **local** filesystem storage (safe for dry runs).
- No staging settings module exists. No product-import command exists.
- `benchmarks/benchmark_api.py --seed` **deletes all catalogue rows** and is guarded to
  databases whose name contains `bench`/`test`. It must never be used for this import.

---

## 4. Mapping specification (Spaces JSON → Django models)

Source manifest: `catalogue/v1/catalogue.json` (100 products, 7 categories, 4 stores).

| Source field | Target | Notes |
|---|---|---|
| `stores[].code` | `Store.code` | Match existing stores only (all 4 codes already match live codes). No inserts. |
| `categories[]` | `Category` | Map to existing where possible; create only genuinely missing categories in staging (see §5). Never modify existing. |
| `products[].name` | `Product.name` | Prefix with `[DEMO] ` so sample data is unmistakable in the UI. |
| `products[].slug` | `Product.slug` | Prefix with `demo-`; guarantees no collision with real slugs. |
| `products[].description` | `Product.description` | Extended with a demonstration-data notice. |
| `products[].is_active` | `Product.is_active` | `true`. `deleted_at` left `NULL`. |
| `products[].attributes[]` | `ProductAttribute` | 8 source attributes + `Data source` and `Price type` markers. |
| `products[].images[]` | `ProductImage` | Create in `display_order`; exactly one `is_primary=True` (first image). |
| `products[].stores[]` | `ProductStore` | Match store by code; `price` quantized to 2 decimals; `quantity`, `is_available=true`. |
| `products[].startingPrice` | — | Not stored; derived by API from `ProductStore` rows. |

### Demonstration-data labelling (required)
- Product name: `[DEMO] <source name>`
- Slug: `demo-<source slug>`
- Extra attributes:
  - `Data source = Demonstration catalogue (public DummyJSON data), personal/reference use`
  - `Price type = Illustrative only — not a real MyTrolley price`
- Category description of any newly created category appends `(demonstration category)`.

---

## 5. Category mapping proposal (needs your decision)

Live API categories (8): Bakery, Beverages, Chicken, Dairy & Eggs, Fruits & Vegetables,
Personal Care, Rice & Grains, Snacks.

| Source category | Proposed staging target | Rationale |
|---|---|---|
| Fruits | existing **Fruits & Vegetables** | reuse |
| Vegetables | existing **Fruits & Vegetables** | reuse |
| Beverages | existing **Beverages** | reuse |
| Chicken | existing **Chicken** | reuse |
| Dairy & Eggs | existing **Dairy & Eggs** | reuse |
| Groceries (43) | **new staging category** `Home & Kitchen` *or* split into Rice & Grains / Snacks | source items are kitchenware/home goods + packaged food |
| Electronics (38) | **new staging category** `Electronics` | no equivalent exists |

Alternative: map all Groceries/Electronics into existing `Snacks` / `Rice & Grains`
to avoid creating categories — but that misrepresents the products. Recommendation:
create the two new categories **in staging only**.

---

## 6. Image strategy (important)

Your requirement is to use the existing Spaces images. The backend's `ProductImage.image`
is an **ImageField**, and the public API emits a proxy URL, not a raw storage URL.
Therefore we cannot simply paste an absolute `https://...digitaloceanspaces.com/...`
string into `image` — `PublicImageView` calls `default_storage.open()` on that value.

Two safe options:

| Option | What happens | Pros / cons |
|---|---|---|
| **A. Local staging media (recommended)** | Download the 242 images into staging `MEDIA_ROOT/products/<new_product_id>/images/<uuid>.webp`; `image` holds the storage key; API proxy serves them. | No risk to live bucket; works with `config.settings.test`/local storage. Images originate from the Spaces URLs. |
| **B. Spaces via django-storages** | Copy objects into a **staging-only prefix** (e.g. `staging/products/...`) and set `USE_SPACES_STORAGE=true` with a `location` prefix. | Uses Spaces directly, but `STORAGES.location=""` today would write to the live bucket root — must be changed/parameterised first (backend settings change, needs approval). |

Recommendation: **Option A** for the demonstration. Option B can be done later with an
explicitly approved settings change. In both cases the Android app sees the same
`/api/v1/public/images/<id>/` API shape — no app changes.

---

## 7. Dry-run validation report (already executed — read-only)

| Check | Result |
|---|---|
| Local manifest parses; product count | **PASS** — 100 products, 7 categories, 4 stores |
| Required fields / image files present | **PASS** — 0 issues; all 242 images exist locally |
| Product slug uniqueness within dataset | **PASS** — 100/100 unique |
| Price format / range | **PASS** — AED 2.90 – 7,344.96 (illustrative) |
| Store codes | **PASS** — all 4 match existing live store codes |
| Live API duplicate check (all 4 stores, 7 live products) | **PASS** — 0 exact name collisions |
| Public Spaces manifest reachable | **PASS** — HTTP 200, parses to 100 products |
| Sample primary images reachable | **PASS** — 10/10 HTTP 200 |
| Backend model constraint review | **PASS with caveats** — slugs, one-primary-image, price rounding and CI uniqueness must be handled explicitly by the importer (§4) |
| `makemigrations --check --dry-run` | **NOT RUN** — requires connecting to a target DB; deferred until staging DB exists |
| Staging DB existence / connectivity | **PENDING** — needs staging DB credentials |
| Full 242-image HTTP check | **PENDING** — will run in the importer's dry-run phase |

**Conclusion:** data is structurally sound; no blocker found. Import must still be gated
on staging DB creation, backup, and a full importer dry-run.

---

## 8. Import design (additive, idempotent, guarded)

Implementation choice (needs approval):
- **Preferred:** a new Django management command `catalog/management/commands/import_spaces_catalogue.py`
  with `--dry-run` (default) and `--apply` flags. This modifies a backend file.
- **Alternative (no backend files):** a one-off script executed via
  `python manage.py shell < <script>` where the script lives in this Android project's
  `catalogue-build/` directory. Uses the same ORM logic, leaves backend untouched.

Guards built into the importer:

1. Refuse to run unless `DB_NAME` contains `staging`, `dev`, `demo` or `test`.
2. `--dry-run` is the default; writes require explicit `--apply`.
3. Wrap each product in `transaction.atomic()`; failure rolls back that product.
4. Duplicate prevention before INSERT:
   - `Product.all_objects.filter(slug=demo_slug).exists()` → skip
   - `Category` lookup by `Lower(name)` → reuse or create only if absent
   - `ProductAttribute` unique per `(product, Lower(name))`
   - `ProductStore` unique per `(product, store)`
5. **Never call `.delete()`, `.update()` on existing rows, `get_or_create` with defaults
   that overwrite, or `bulk_create` without slugs.** Insert-only.
6. Use `.save()` (not `bulk_create`) for `Category`, `Product`, `ProductImage`,
   `ProductStore` so model-level validation/slug/primary-image/price-rounding apply.
7. Log every skipped duplicate and every created object with source IDs.

---

## 9. Backup and rollback plan

### Before import
```powershell
# Staging DB only — replace with the confirmed staging credentials
$env:PGPASSWORD = '<staging-db-password>'
pg_dump -Fc --no-owner --no-acl -h <host> -p 5432 -U <user> -d mytrolley_staging `
  -f "C:\Users\user\AndroidStudioProjects\MyTrolley\catalogue-build\backups\mytrolley_staging_PRE_IMPORT_<timestamp>.dump"
```
Also record baseline counts: `Category`, `Store`, `Product.all_objects`,
`ProductAttribute`, `ProductImage`, `ProductStore`.

### Targeted rollback (preferred, staging only)
Because all imported rows are marked, rollback is surgical and does not touch real data:
- Delete `ProductImage` / `ProductAttribute` / `ProductStore` where
  `product__slug LIKE 'demo-%'`, then `Product.all_objects.filter(slug__startswith='demo-')`.
- Delete only categories created by this import (record their IDs in the import log).
- Verify baseline counts are restored.

### Full rollback
Restore the pre-import dump into a fresh database:
```powershell
createdb -h <host> -U <user> mytrolley_staging_rollback
pg_restore -h <host> -U <user> -d mytrolley_staging_rollback <backup.dump>
```
Then repoint the staging backend to the restored DB. Production is never involved.

---

## 10. Post-import verification plan (after your approval)

**Backend**
- `python manage.py check` and `makemigrations --check --dry-run` on staging.
- Import `--dry-run` report: 100 would-create, 0 conflicts, 242 images planned.
- Import `--apply`; then API smoke tests:
  - `/public/stores/` → 4 stores, unchanged product counts for real products
  - `/public/categories/` → existing 8 categories intact + new staging categories
  - `/public/products/?store=<id>&page_size=100` → 100 `[DEMO]` products per store
  - `/public/products/<id>/?store=<id>` → attributes, 1 primary image, 4 store prices
  - `/public/products/?search=demo` and `?category=<id>` → correct results
  - Re-run existing backend test suite (`pytest`) — must stay green.

**Android (demo build, existing app code)**
Build with the staging API override (needs your approval):
```
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug -PmytrolleyApiUrl=http://<staging-host>:8000/api/v1/
```
Then verify and screenshot each Market Hall screen:
1. Home: recommended cards, hero, photographic aisle tiles
2. Products/Catalogue: 100 demo products, prices, stock, add buttons
3. Category pages per mapped category
4. Product detail: gallery, attributes, store/availability picker, compare stores
5. Search: name search returns demo products
6. Filters: diet filter and price/name sort
7. Store selection: all 4 stores, prices change per store
8. Confirm existing real products and the production API are unaffected when not using the demo build.

Screenshots will be captured with ADB and reported with actual file paths (AGENTS.md §7).

---

## 11. Decisions needed before I execute anything

1. **Staging DB** — name, host, user, and confirmation it is not production. Who creates it?
2. **Category strategy** — reuse existing 8 + create `Electronics` and `Home & Kitchen` in staging (recommended), or force-map into existing categories only?
3. **Image strategy** — Option A local staging media (recommended) or Option B Spaces prefix (requires an approved backend settings change)?
4. **Importer form** — new backend management command (requires approval to modify a backend file) or one-off script in `catalogue-build/` (no backend files changed)?
5. **Android demo build** — approve the `-PmytrolleyApiUrl=...` override for a demo APK.
6. **Confirmation** that the live production database and live API must remain untouched throughout.

---

## 12. Current state — what has and has not happened

- ✅ Read-only inspection of Django backend and read-only queries of the live public API.
- ✅ Local catalogue validation and Spaces upload (completed earlier, separate task).
- ✅ This plan + dry-run report written inside the Android project directory.
- ❌ No database created, backed up, or modified.
- ❌ No backend file created or changed.
- ❌ No Android source, Gradle, API URL or build config changed.
- ❌ No import executed — dry-run or otherwise.

**Awaiting your approval and the decisions in §11.**
