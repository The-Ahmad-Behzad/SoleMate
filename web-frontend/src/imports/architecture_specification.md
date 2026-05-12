# SoleMate Platform: UI & Feature Architecture Specification

This document provides a comprehensive technical specification of the SoleMate platform, covering both the Flutter Mobile App and the Seller Dashboard. It is designed for AI designers and frontend generators to recreate the platform with high fidelity.

---

## 1. System Overview

| Attribute | Flutter Mobile App | Seller Dashboard |
| :--- | :--- | :--- |
| **Primary Goal** | Consumer AR Try-On & Shopping | Inventory Management & Design Fulfillment |
| **Technology Stack** | Flutter, Dart, Firebase, Snap AR | React, Vite, TypeScript, Tailwind CSS |
| **Key Features** | AR Try-On, AI Outfit Match, Custom Skin Requests | GLB Upload, AR Metadata Config, Order Management |
| **Target User** | Shoe Shoppers / Fashion Enthusiasts | Shoe Sellers / 3D Designers |

---

## 2. Platform Maps

### 2.1 Complete Site Map

#### **Flutter Mobile App**
- **Splash Screen**
- **Landing Screen** (Onboarding & Hero)
- **Authentication**
  - Login
  - Register
- **Main Shell** (Bottom Navigation)
  - **AR Try-On Screen**
  - **My Closet Screen**
    - Shoes Tab
    - Outfits Tab
  - **AI Outfit Match Screen**
    - Shoe-to-Outfit Flow
    - Outfit-to-Shoe Flow
    - Mismatch Checker
  - **Customize Screen** (Skin Upload/Design)
- **Catalog Screen** (Product Browsing)

#### **Seller Dashboard**
- **Login / Register**
- **Dashboard** (Overview Stats)
- **My Models** (Inventory)
  - **Upload Model** (Wizard)
  - **AR Integration Request**
- **Skin Design Requests** (List)
  - **Request Detail** (Workflow Management)

---

### 2.2 Feature Distribution Map

| Feature | Mobile App | Seller Portal | Backend Service |
| :--- | :--- | :--- | :--- |
| **AR Renderer** | Native AR Core / Snap | N/A | S3 (Model Hosting) |
| **AI Recommendation** | Result Viewing | N/A | Outfit Matching API |
| **Skin Customization** | Drawing/Request Submission | Request Fulfillment | Skins API |
| **Catalog Management**| Browsing | GLB Upload | Product API |
| **User History** | Local & Cloud Sync | N/A | User Service |

---

## 3. Navigation & User Flows

### 3.1 Mobile Navigation Behavior
- **Primary**: Bottom Navigation Bar (4 Tabs).
- **Secondary**: Top AppBar with Logo (Home link) and User Profile/Logout.
- **Contextual**: Floating Action Buttons (FAB) for AR activation in catalog.
- **Deep Linking**: Catalog "Try On" button navigates to AR tab with pre-selected shoe.

### 3.2 Desktop Navigation Behavior (Seller Dashboard)
- **Primary**: Sidebar Navigation (Dashboard, Models, Requests).
- **Secondary**: Top Navbar with Search and Profile.
- **Responsive**: Sidebar collapses on mobile view; grid layouts switch from 3-4 columns to 1 column.

---

### 3.3 Core Workflows

#### **A. AR Try-On Flow**
1. **Trigger**: User selects "Try On" from Catalog or "Try Again" from Closet.
2. **Setup**: App requests Camera permissions.
3. **Selection**: User selects shoe from horizontal carousel or grid.
4. **Action**: User taps "Enable AR".
5. **Execution**: Flutter invokes MethodChannel to open Native AR View (Snap AR Lens).
6. **Interaction**: User views shoe on foot; can change shoes via AR overlay.
7. **Save**: User takes snapshot or taps "Save to Closet" (triggers API: `POST /tryon/save`).

#### **B. AI Outfit Recommendation Flow**
**Path 1: Shoe -> Outfit**
1. User selects shoe.
2. Taps "Rec Outfit".
3. AI analyzes shoe colors/style.
4. Returns descriptive style advice and matching clothing suggestions.

**Path 2: Outfit -> Shoe**
1. User uploads/takes photo of current outfit.
2. Taps "Analyze".
3. AI extracts dominant colors (Shirt/Pants).
4. Returns a ranked list of recommended shoes from the catalog.

#### **C. Custom Shoe Skin Upload Flow**
1. User selects "Customize" tab.
2. Selects a "Base Shoe" from the catalog.
3. Uploads reference images (textures, patterns).
4. **Canvas Interaction**: User draws/annotates directly on the uploaded images.
5. **Specification**: User selects Primary/Secondary colors via hex picker.
6. **Submission**: User adds text instructions and submits (`POST /skins/request-redesign`).
7. **Fulfillment**: Seller receives request, views drawings, and updates status to "In Progress".

---

## 4. Detailed Mobile Screen Breakdown

### 4.1 Landing Screen
- **Purpose**: Brand introduction and feature discovery.
- **Entry Points**: App Launch.
- **Layout**: Single-column vertical scroll.
- **Header**: Transparent, overlaps hero.
- **Components**:
  - **Hero Section**: 
    - Title: "SoleMate" (H1, Primary Foreground).
    - Subtitle: "AR Shoe Try-On Experience" (Secondary color).
    - Description: App value proposition.
    - CTAs: [Start AR Try-On] (Primary), [Learn More] (Outline).
    - Image: Circular hero shoe image with glow effect.
  - **Features Grid**: 4 `FeatureCard` items (AR, Closet, Outfit, Customize).
  - **Catalog Teasers**: Horizontal lists for "Popular Shoes" and "Recently Tried".
  - **CTA Section**: Centered text with [Get Started Now] button.
- **Navigation**:
  - [Start AR Try-On] -> AR Try-On Screen.
  - [View all] -> Catalog Screen.
  - [Feature Tap] -> Corresponding Main Shell Tab.
- **States**:
  - Loading: Circular progress during catalog fetch.
  - Empty: "No items to show" in teasers.

### 4.2 AR Try-On Screen
- **Purpose**: Core AR visualization.
- **Entry Points**: Bottom Nav (Tab 0), Landing Screen CTA, Catalog "Try On".
- **Layout**: 
  - Mobile: Stacked Camera -> Selection -> Actions.
  - Desktop: 2-column (Camera Left, Selection/Actions Right).
- **Header**: Logo, title "AR Try-On", [View all] text button, [Logout] popup.
- **Components**:
  - **Camera Card**: 
    - Preview viewport (Placeholder for native AR view).
    - Status Badge: [AR Active] / [AR Ready].
    - Buttons: [Reset], [Enable AR] (with loading state), [Save].
  - **Shoe Selection Card**: 
    - Refresh icon.
    - Grid of shoe thumbnails with selection border and checkmark.
  - **Quick Actions Card**:
    - [Add to Closet] (Primary).
    - [View in 3D] (Secondary).
    - [Match with Outfit] (Outline).
- **Flows**:
  - [Enable AR] -> MethodChannel Call -> Snap AR Lens View.
  - [Save] -> `POST /tryon/save`.
  - [Match with Outfit] -> Outfit Match Screen.
- **States**:
  - Loading: Progress bar while fetching catalog.
  - Empty: "No shoes found" icon with [Retry] button.

### 4.3 My Closet Screen
- **Purpose**: View saved shoes and AI-generated outfits.
- **Entry Points**: Bottom Nav (Tab 1).
- **Layout**: Top Tab Bar (Shoes/Outfits) + Search + Filter + Grid.
- **Components**:
  - **Tabs**: [Shoes] (Shopping bag), [Outfits] (Checkroom).
  - **Search Bar**: Text field with prefix search icon and clear suffix.
  - **Filter Badges**: Horizontal scroll of [All], [Favorites], [Recent].
  - **Shoe Grid**: `ProductCard` items with "Try Again" CTA.
  - **Outfit Grid**: Cards showing AI-generated look, category, and date.
- **Modals**:
  - **Filter Sheet**: Bottom sheet with [Max Price] chips, [Brand] chips, [Category] chips.
  - **Outfit Detail Dialog**: Shows recommendation description and list of "Recommended Shoes".
- **States**:
  - Empty (Shoes): "No favorites yet" with [Explore Catalog] CTA.
  - Empty (Outfits): "No Saved Outfits" with advice to use AI matching.
  - Loading: Spinner during API sync.

### 4.4 AI Outfit Match Screen
- **Purpose**: Stylist automation.
- **Entry Points**: Bottom Nav (Tab 2), AR Screen Quick Action.
- **Layout**: 
  - Gender Selector -> Current Item -> AI Preview -> Recommendations.
- **Components**:
  - **Gender Selector**: [Unisex, Male, Female] segments.
  - **Current Shoe Card**: 
    - Image preview of selected shoe or uploaded photo.
    - Buttons: [Rec Outfit] (Primary/Generate), [Change Shoe], [Upload Shoe].
  - **Outfit Preview Card**:
    - Image preview of uploaded outfit or AI-suggested look.
    - Buttons: [Generate/Analyze] (context-sensitive), [Save Outfit], [Try On].
  - **Mismatch Section**: [Upload Image] for checking style inconsistencies.
- **Modals**:
  - **Shoe Selector**: Bottom sheet grid of catalog items.
- **Flows**:
  - [Analyze Outfit] -> Image upload -> Gemini Vision -> Shoe Recs.
  - [Rec Outfit] -> Shoe Data -> GPT/Gemini -> Style Advice.

### 4.5 Customize Screen
- **Purpose**: Community-driven shoe redesign.
- **Entry Points**: Bottom Nav (Tab 3).
- **Layout**: Vertical scroll of form sections.
- **Components**:
  - **Shoe Base Card**: Selectable shoe from catalog.
  - **Image Manager**: Horizontal list of uploaded references with delete icon.
  - **Color Pickers**: Circular color palette for Primary and Secondary colors.
  - **Description**: Multiline text input for instructions.
  - **CTA**: [Send Request] (Full width).
- **Modals**:
  - **Drawing Canvas**: Full-screen modal with [Undo], [Delete], [Save] buttons; allows painting over image.
- **States**:
  - Sending: Button becomes disabled with "Sending Request..." text.

---

## 5. Detailed Seller Dashboard Breakdown

### 5.1 Dashboard Page (Overview)
- **Purpose**: Command center for sellers.
- **Entry Points**: Post-login Landing.
- **Components**:
  - **Greeting Header**: Personalized greeting + [Upload Model] CTA.
  - **Stat Cards**: 4-panel grid with semantic coloring for status.
  - **Recent Activity Tables**: Two-column layout (Desktop) or stacked (Mobile).
- **Navigation**:
  - [View all] -> Corresponding list pages.
  - [Table Row Click] -> Detail pages.

### 5.2 My Models Page
- **Purpose**: Inventory management.
- **Entry Points**: Sidebar "Models".
- **Components**:
  - **Filter Bar**: Search + Status dropdown.
  - **Model Grid**: Cards showing shoe thumbnail, brand, status badge, and date.
- **Navigation**:
  - [Model Card] -> Model Settings (Edit AR placement).

### 5.3 Upload Model Wizard
- **Purpose**: Onboarding 3D assets.
- **Layout**: Two-column (Form Left, Summary Sidebar Right).
- **Components**:
  - **Dropzone**: Dashed border area for GLB upload.
  - **Metadata Form**: Inputs for Shoe Name, Brand, Description.
  - **AR Matrix Sections**: Expandable panel for X, Y, Z coordinates for Scale/Pos/Rot.
  - **Summary Sidebar**: Real-time validation + S3 Upload Progress Bar.
- **States**:
  - Success: "Upload Complete" screen with [Request AR Integration] CTA.

### 5.4 Skin Design Requests Page
- **Purpose**: Order processing.
- **Entry Points**: Sidebar "Requests".
- **Components**:
  - **Status Pills**: Filtering tabs with live counts (e.g., [New: 5]).
  - **Request Grid**: Cards with user email, shoe name, and submission time.
- **States**:
  - Empty: "No requests found" with explanatory subtext.

### 5.5 Skin Request Detail Page
- **Purpose**: Fulfillment workflow.
- **Layout**: User Info & Images (Left) + Management Sidebar (Right).
- **Components**:
  - **Image Gallery**: Grid of user-uploaded reference images with zoom lightbox.
  - **Description Box**: Formatted text display of user instructions.
  - **Management Sidebar**:
    - Status Dropdown (Viewed -> Completed).
    - Seller Notes Textarea.
    - [Save Changes] Button.
    - [Email User] Button (opens external mail client).
- **Audit Trail**: Timestamps for creation, last view, and completion.

---

## 6. Component Inventory

| Category | Component Name | Attributes |
| :--- | :--- | :--- |
| **Layout** | `AppScaffold` | Multi-tab bottom nav, AppTheme integration |
| **Data Display** | `ProductCard` | Image, Title, Price, Action Button, Favorite Star |
| **Data Display** | `StatusBadge` | Semantic colors (Blue=New, Green=Done, Red=Rejected) |
| **Input** | `SmartSearch` | Regex-based price and text parsing |
| **Creative** | `DrawingCanvas` | Layered UI: [Image Layer] + [Painter Layer] |
| **Feedback** | `StatusPills` | Interactive count-based filter indicators |

---

## 7. Role-Based Access Map

| Page / Action | Guest | Registered User | Seller | Admin |
| :--- | :---: | :---: | :---: | :---: |
| **AR Try-On** | View Only | View & Save | View | View |
| **My Closet** | No | Yes | No | No |
| **Upload Model** | No | No | Yes | No |
| **Approve Model** | No | No | No | Yes |
| **Skin Requests** | No | Submit | Fulfill | Monitor |

---

## 8. API & Data Dependencies

- **Auth**: Firebase Auth (Token-based).
- **Models**: `GET /products`, `POST /seller/upload-url`.
- **Try-On**: `POST /tryon/save`, `GET /closet`.
- **AI Match**: `POST /outfit/analyze` (Gemini/Vision integration).
- **Custom Skins**: `POST /skins/request-redesign`.
- **Assets**: AWS S3 (Models), Firebase Storage (Images).

---

## 9. Visual Aesthetics (Premium Design)

- **Palette**: Olive Greens (`#7A9F6B`), Warm Browns (`#B8895F`), Dark Olive (`#5D7F4E`).
- **Surface**: Glassmorphism on AR overlays; 95% opacity with backdrop blur.
- **Shadows**: Custom "Elegant" shadow (15% opacity brown) and "Glow" shadow (30% accent).
- **Transitions**: 150ms cubic-bezier for all interactions.
- **Typography**: System Font Stack with Heavy Weights (700) for headlines.
