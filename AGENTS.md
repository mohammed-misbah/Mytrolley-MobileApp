# MYTROLLEY ANDROID — AGENTS.md
## Permanent Project Protection Instructions

This file establishes mandatory development rules for all AI agents and human developers working on the MyTrolley native Android application. These rules protect the verified functional baseline and ensure the upcoming UI/UX redesign preserves all existing functionality.

---

## 1. PROJECT OBJECTIVE

MyTrolley is a native Android product exploration application built using:
- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Retrofit** + **OkHttp** + **Gson** for Django REST Framework backend integration
- **Coil** for image loading
- **Django REST Framework** + **PostgreSQL** backend (external, not in this repo)

**Goal:** Achieve a premium, professional, commercial-quality Android UI/UX using an approved Figma master design reference, while preserving all existing functionality throughout the visual redesign.

---

## 2. PROTECTED FUNCTIONAL BASELINE (STAGE 1 VERIFIED)

The following functionality is verified working and must be preserved:

### Backend Connectivity
- Working public catalogue backend connectivity; default base URL is the production `https://products.mytrolley.ae/api/v1/`, with `http://10.0.2.2:8000/api/v1/` available for local Django development via `-PmytrolleyApiUrl=...`
- Android API 37 compatibility
- Android local network runtime permission handling (`ACCESS_LOCAL_NETWORK`)
- Existing Retrofit and OkHttp configuration (15s connect / 20s read timeouts)
- Existing Gson DTO mapping for all public catalogue endpoints
- Existing Coil image loading configuration

### Catalogue Repository & Data Layer
- Existing public catalogue repository (`CatalogRepository`)
- Active store loading (`/public/stores/`)
- Store selection with dropdown UI
- Active category loading (`/public/categories/`)
- Category product counts from API
- Real product listing (`/public/products/?store={id}`)
- Product search (`?search=`)
- Category filtering (`?category=`)
- Product details (`/public/products/{id}/?store={id}`)
- Real product images (`/public/images/{id}/`)
- Product attributes from detail endpoint
- Store-specific prices (never fall back to another store's price)
- Store-specific stock quantities
- Product availability status

### UI State Management
- Loading states (circular progress indicators)
- Empty states ("No products are available at this store.")
- Error handling with Retry buttons (`retry()`, `retryDetail()`)
- Navigation and state management (HomeViewModel with StateFlow)
- Store switching updates applicable products and prices

### Build & Test Baseline
- **Gradle build:** SUCCESS (AGP 9.5.1, Kotlin 2.2.10, Compose BOM 2026.02.01)
- **Unit tests:** 5/5 PASS (`ExampleUnitTest`, `CatalogPublicDtoTest`)
- **Target SDK:** 37, **Min SDK:** 24

---

## 3. ABSOLUTE PROTECTION RULES

The following rules are **non-negotiable** and apply to all development work:

1. **Never modify the existing React frontend files, design or functionality.**
2. **Never modify Django backend files.**
3. **Never modify PostgreSQL database records or schema.**
4. **Never modify backend models, serializers, views, URLs, migrations or settings.**
5. **Never modify existing REST API contracts.**
6. **Never modify authentication or authorization.**
7. **Never change existing Android networking configuration without explicit approval.**
8. **Never remove or weaken Android network permissions or security configuration.**
9. **Never modify existing API calls, DTOs, repositories or business logic for visual convenience.**
10. **Never remove existing functionality.**
11. **Never replace live API data with mock data, fake products or hardcoded prices.**
12. **Never expose credentials, secrets or API keys.**
13. **Never overwrite or revert previous completed development work without approval.**
14. **Never introduce additional dependencies without approval.**
15. **Never change SDK versions or build configuration without approval.**
16. **Never delete existing tests or weaken test assertions.**
17. **Never perform destructive Git operations or unauthorized file deletion.**
18. **Never modify files outside the authorized Android project directory.**
19. **Never add customer registration, authentication, checkout, payments or unrelated features without explicit approval.**
20. **If a protected functional change appears necessary, stop and explain the reason before implementing it.**

---

## 4. ALLOWED VISUAL CHANGES

After the user approves the master Figma reference and design specification, the following visual changes are permitted:

- Jetpack Compose theme
- Material 3 colour scheme
- Typography and font weights
- Spacing and padding
- Shapes and corner radii
- Borders and shadows
- Presentation-only Compose components
- Screen layout and visual hierarchy
- Product card appearance
- Category card appearance
- Search field styling
- Store selector presentation
- Product detail presentation
- Navigation presentation while preserving existing behavior
- Loading, empty and error state appearance
- Responsive Android layouts
- Accessibility improvements that preserve existing functionality

**Do not begin these changes until explicitly authorized.**

---

## 5. MASTER DESIGN REFERENCE RULES

- Follow **one approved Figma mobile application design language**.
- Wait for the user to provide the Figma link and reference screenshots.
- Do not independently select an unrelated design.
- Extract and document the design system before implementation.
- Distinguish exact reference properties from estimated properties.
- Define colours, typography, spacing, component dimensions and visual states.
- Adapt the reference to MyTrolley's actual product exploration functionality.
- Preserve the existing MyTrolley identity and logo unless explicitly instructed otherwise.
- Do not introduce unnecessary features simply because they appear in the reference.
- Reuse approved Compose components across all screens.
- Do not make arbitrary design decisions.

---

## 6. SCREEN-BY-SCREEN IMPLEMENTATION RULES

Never redesign the entire application in a single implementation task.

Follow this order, subject to user approval:

1. Approved design system
2. Shared Compose presentation components
3. Home screen
4. Category browsing
5. Product listing
6. Product details
7. Search experience
8. Store selection
9. Loading, empty and error states
10. Responsive layouts and accessibility
11. Final regression testing

**Implement only the currently authorized screen or component group.**
**Stop after each implementation and wait for user approval.**

---

## 7. MANDATORY VISUAL VERIFICATION

After every approved visual implementation:

1. Build the Android application.
2. Install and launch it on the existing Android emulator.
3. Capture an actual emulator screenshot using ADB or available Android inspection tools.
4. Compare the rendered result with the approved Figma reference when genuine image inspection is supported.
5. Identify differences in typography, spacing, positioning, dimensions, alignment, colour and hierarchy.
6. Correct significant visual differences.
7. Capture another actual screenshot.
8. Report any remaining differences.

**If the selected AI model cannot genuinely inspect image contents, do not fabricate screenshot analysis.**
Provide the actual screenshot file paths and request manual visual review by the user or ChatGPT.
**Never declare visual completion based only on successful compilation.**

---

## 8. MANDATORY FUNCTIONAL VERIFICATION

After every implementation stage:

- Run relevant existing Gradle build tasks.
- Run existing unit tests.
- Verify actual Android runtime behavior.
- Confirm real backend data is still displayed.
- Verify affected product browsing functionality.
- Confirm existing network permissions remain functional.
- Inspect repository differences or compare against a protected baseline.
- Report every modified file.
- Confirm React frontend, Django backend and PostgreSQL remain untouched.
- Report failures or untested functionality accurately.

**Do not falsely claim that tests or visual checks have passed.**

---

## 9. SAFETY AND BACKUP RULES

**Stage 1 confirmed:** The Android project is **not currently a Git repository** (no `.git` directory).

Therefore:

- Respect the user's separate project backup.
- **Do not initialize Git automatically.**
- Recommend a safe local version-control or snapshot strategy for user approval.
- Preserve existing files before every approved modification.
- Never access or modify protected backup directories.
- Do not use automatic bulk replacements or destructive commands.

### Recommended Backup Strategy (for user approval)
1. **Manual zip snapshot** before each authorized screen implementation
2. **File-level copy** of modified files to a dated backup directory
3. **Gradle wrapper + source** preservation (already versioned in wrapper)
4. **User-maintained external backup** (acknowledged by Stage 1 report)

---

## 10. CURRENT AUTHORIZED TASK

**Stage 2 Only:** Create or update this `AGENTS.md` file in the project root.

**No other modifications are authorized at this stage.**

---

## 11. STAGE 2 COMPLETION REPORT

### File Created/Updated
- **Path:** `C:\Users\user\AndroidStudioProjects\MyTrolley\AGENTS.md`

### Protection Rules Included
✅ All 20 Absolute Protection Rules (Section 3)
✅ Allowed Visual Changes scope (Section 4)
✅ Master Design Reference Rules (Section 5)
✅ Screen-by-Screen Implementation Order (Section 6)
✅ Mandatory Visual Verification Process (Section 7)
✅ Mandatory Functional Verification Process (Section 8)
✅ Safety and Backup Rules with Recommended Strategy (Section 9)
✅ Protected Functional Baseline documented (Section 2)

### Files Modified During Stage 2
- `AGENTS.md` (created new)

### Confirmation: No Existing Application Source Code Changed
**CONFIRMED.** No Kotlin source files, Compose UI components, Android resources, Gradle configuration, manifests, backend files, frontend files, or database content were modified.

### Limitations of AGENTS.md
- AGENTS.md is a **behavioral instruction file** for AI agents; it cannot enforce compliance programmatically.
- It relies on the agent reading and following instructions at conversation start.
- It does not replace code reviews, CI checks, or Git hooks.
- Visual verification depends on the AI model's actual image inspection capability (currently limited to file paths only).
- Backup strategy is recommended but not implemented; requires user action.

### Readiness for Stage 3
**READY.** The AGENTS.md file is created with all mandatory protection rules. Stage 3 (Android Visual Inspection Setup) can proceed upon user approval.

---

*Generated during Stage 2: Permanent Project Protection*
*Date: 2026-11-19*
*Project: MyTrolley Android (com.mytrolley.app)*