# SoleMate - Design System & Theme Specifications

> **Application:** AR Shoe Try-On & Shopping Platform  
> **Version:** 1.0  
> **Last Updated:** 2025-10-24  
> **Framework:** React + Vite + Tailwind CSS + TypeScript

---

## 📱 Table of Contents

1. [Design Philosophy](#design-philosophy)
2. [Color System](#color-system)
3. [Typography](#typography)
4. [Spacing & Layout](#spacing--layout)
5. [Component Architecture](#component-architecture)
6. [Page-by-Page Breakdown](#page-by-page-breakdown)
7. [Responsive Design](#responsive-design)
8. [Animations & Transitions](#animations--transitions)
9. [Visual Effects](#visual-effects)
10. [Icon System](#icon-system)

---

## 🎨 Design Philosophy

### Core Principles
- **Earthy & Natural**: Olive greens and warm browns create a sophisticated, organic feel
- **Clean & Modern**: Minimalist layouts with ample whitespace
- **AR-First**: Design emphasizes visual try-on experiences
- **Mobile-Optimized**: Touch-friendly interactions and responsive layouts

### Brand Identity
- **Primary Color**: Olive Green (#7A9F6B / HSL 85 25% 45%)
- **Secondary Color**: Warm Brown (#B8895F / HSL 30 40% 50%)
- **Accent Color**: Dark Olive (#5D7F4E / HSL 85 30% 40%)

---



## 🌈 Color System

### Light Mode Colors (Default)

#### Base Colors
```css
Background:       HSL(0, 0%, 98%)       /* #FAFAFA - Off-white */
Foreground:       HSL(30, 35%, 25%)     /* #403121 - Dark brown */
```

#### Surface Colors
```css
Card:             HSL(0, 0%, 100%)      /* #FFFFFF - Pure white */
Card Foreground:  HSL(30, 35%, 25%)     /* #403121 - Dark brown */
Popover:          HSL(0, 0%, 100%)      /* #FFFFFF */
Popover Foreground: HSL(30, 35%, 25%)   /* #403121 */
```

#### Brand Colors
```css
Primary:          HSL(85, 25%, 45%)     /* #7A9F6B - Olive green */
Primary Foreground: HSL(0, 0%, 98%)     /* #FAFAFA - Off-white */

Secondary:        HSL(30, 40%, 50%)     /* #B8895F - Warm brown */
Secondary Foreground: HSL(0, 0%, 98%)   /* #FAFAFA */

Accent:           HSL(85, 30%, 40%)     /* #5D7F4E - Dark olive */
Accent Foreground: HSL(0, 0%, 98%)      /* #FAFAFA */
```

#### Neutral Colors
```css
Muted:            HSL(30, 20%, 90%)     /* #EBE5DE - Light tan */
Muted Foreground: HSL(30, 20%, 45%)     /* #8A7A66 - Medium brown */
```

#### UI Utilities
```css
Border:           HSL(30, 15%, 85%)     /* #DDD5CC - Light border */
Input:            HSL(30, 15%, 85%)     /* #DDD5CC - Input border */
Ring:             HSL(85, 25%, 45%)     /* #7A9F6B - Focus ring (same as primary) */
```

#### Semantic Colors
```css
Destructive:      HSL(0, 84.2%, 60.2%)  /* #E53935 - Red */
Destructive Foreground: HSL(0, 0%, 98%) /* #FAFAFA */
```

### Dark Mode Colors

#### Base Colors
```css
Background:       HSL(30, 20%, 12%)     /* #1F1914 - Very dark brown */
Foreground:       HSL(0, 0%, 95%)       /* #F2F2F2 - Off-white */
```

#### Surface Colors
```css
Card:             HSL(30, 25%, 18%)     /* #2D2419 - Dark card */
Card Foreground:  HSL(0, 0%, 95%)       /* #F2F2F2 */
Popover:          HSL(30, 25%, 18%)     /* #2D2419 */
Popover Foreground: HSL(0, 0%, 95%)     /* #F2F2F2 */
```

#### Brand Colors (Same as Light)
```css
Primary:          HSL(85, 25%, 45%)     /* #7A9F6B */
Primary Foreground: HSL(0, 0%, 98%)     /* #FAFAFA */

Secondary:        HSL(30, 40%, 50%)     /* #B8895F */
Secondary Foreground: HSL(0, 0%, 98%)   /* #FAFAFA */

Accent:           HSL(85, 30%, 40%)     /* #5D7F4E */
Accent Foreground: HSL(0, 0%, 98%)      /* #FAFAFA */
```

#### Neutral Colors
```css
Muted:            HSL(30, 20%, 25%)     /* #3D3329 - Dark muted */
Muted Foreground: HSL(0, 0%, 65%)       /* #A6A6A6 - Light gray */
```

#### UI Utilities
```css
Border:           HSL(30, 20%, 25%)     /* #3D3329 - Dark border */
Input:            HSL(30, 20%, 25%)     /* #3D3329 - Input border */
Ring:             HSL(85, 25%, 45%)     /* #7A9F6B - Focus ring */
```

#### Semantic Colors
```css
Destructive:      HSL(0, 62.8%, 30.6%)  /* #7D2927 - Dark red */
Destructive Foreground: HSL(0, 0%, 98%) /* #FAFAFA */
```

---

## ✍️ Typography

### Font Family
```css
System Font Stack (Default):
font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, 
             "Helvetica Neue", Arial, sans-serif;
```

### Font Sizes (Tailwind Scale)
```
text-xs:    0.75rem  (12px)
text-sm:    0.875rem (14px)
text-base:  1rem     (16px)
text-lg:    1.125rem (18px)
text-xl:    1.25rem  (20px)
text-2xl:   1.5rem   (24px)
text-3xl:   1.875rem (30px)
text-4xl:   2.25rem  (36px)
text-5xl:   3rem     (48px)
text-6xl:   3.75rem  (60px)
```

### Font Weights
```
font-normal:    400
font-medium:    500
font-semibold:  600
font-bold:      700
```

### Typography Usage by Component Type

#### Headlines
```
H1 (Hero):      text-5xl md:text-6xl font-bold
H1 (Page):      text-4xl font-bold
H2 (Section):   text-3xl md:text-4xl font-bold
H3 (Card):      text-xl md:text-lg font-semibold
```

#### Body Text
```
Paragraph:      text-base (16px)
Description:    text-muted-foreground
Small text:     text-sm text-muted-foreground
```

#### Buttons
```
Large:          text-base font-medium
Default:        text-sm font-medium
```

---

## 📐 Spacing & Layout

### Container System
```css
container: {
  center: true,
  padding: "2rem" (32px on desktop, responsive on mobile)
}

Max widths:
- max-w-7xl:  1280px (main content areas)
- max-w-6xl:  1152px (2-column layouts)
- max-w-3xl:  768px  (CTA sections)
- max-w-2xl:  672px  (centered text content)
```

### Spacing Scale (Tailwind)
```
px-4:  1rem    (16px) - Mobile horizontal padding
px-8:  2rem    (32px) - Desktop padding
py-4:  1rem    (16px) - Small vertical spacing
py-6:  1.5rem  (24px) - Card padding
py-8:  2rem    (32px) - Medium vertical spacing
py-12: 3rem    (48px) - Large vertical spacing
py-16: 4rem    (64px) - Section spacing (mobile)
py-20: 5rem    (80px) - Section spacing (desktop)

gap-3:  0.75rem (12px) - Button groups
gap-4:  1rem    (16px) - Card grids (mobile)
gap-6:  1.5rem  (24px) - Card grids (desktop)
gap-8:  2rem    (32px) - Section gaps
gap-12: 3rem    (48px) - Major section gaps
```

### Border Radius
```css
--radius: 0.75rem (12px)

Border radius scale:
rounded-lg:  12px (default, from CSS variable)
rounded-md:  10px (calculated: --radius - 2px)
rounded-sm:  8px  (calculated: --radius - 4px)
rounded-full: 9999px (circular)
```

### Aspect Ratios
```
aspect-square:   1:1  (Product images, color swatches)
aspect-[4/5]:    4:5  (AR camera view)
aspect-[3/4]:    3:4  (Outfit cards)
aspect-[16/9]:   16:9 (Outfit preview)
```

---

## 🧩 Component Architecture

### Core UI Components (shadcn/ui)

All components use the semantic color tokens. Never use direct color values.

#### Button Variants
```tsx
// Default (Primary)
<Button>Text</Button>
className: "bg-primary text-primary-foreground hover:bg-primary/90"

// Secondary
<Button variant="secondary">Text</Button>
className: "bg-secondary text-secondary-foreground hover:bg-secondary/90"

// Outline
<Button variant="outline">Text</Button>
className: "border border-input bg-background hover:bg-accent"

// Sizes
size="lg":  h-11 px-8
size="default": h-10 px-4
size="sm":  h-9 px-3
```

#### Card Component
```tsx
<Card className="p-6 bg-card shadow-[var(--shadow-elegant)]">
  {/* Content */}
</Card>

// Hover effect for interactive cards:
className: "hover:shadow-[var(--shadow-glow)] transition-all"
```

#### Badge Component
```tsx
// Default (Accent)
<Badge>Text</Badge>
className: "bg-accent text-accent-foreground"

// Outline
<Badge variant="outline">Text</Badge>
className: "border border-border"
```

### Navigation Component

#### Structure
```
Fixed Header (z-50)
├── Container (max-width with padding)
│   ├── Logo (h-14 / 56px)
│   ├── Desktop Nav Links (hidden on mobile)
│   │   ├── AR Try-On
│   │   ├── My Closet
│   │   ├── Outfit Match
│   │   └── Customize
│   └── Sign In Button
```

#### Styling
```css
Background: bg-primary/95 backdrop-blur-sm
Height: h-16 (64px)
Border: border-b border-border

Active Link: text-foreground (dark brown)
Inactive Link: text-primary-foreground/80 (light, 80% opacity)
Hover: text-primary-foreground
```

---

## 📄 Page-by-Page Breakdown

### 1. Landing Page (Index.tsx)

#### Layout Structure
```
Navigation (fixed header)
├── Hero Section
│   ├── Background: --gradient-hero with pattern overlay
│   ├── Grid: lg:grid-cols-2
│   │   ├── Left: Headline + CTA buttons
│   │   └── Right: Hero image (with glow effect)
│   └── Spacing: pt-24 pb-16
├── Features Section
│   ├── Heading (centered)
│   ├── Grid: md:grid-cols-2 lg:grid-cols-4
│   │   └── Feature Cards (4 total)
│   └── Spacing: py-20
└── CTA Section
    ├── Background: bg-primary/5
    ├── Centered content (max-w-3xl)
    └── Spacing: py-20
```

#### Theme Application

**Hero Section:**
```css
Background: var(--gradient-hero)
Pattern Overlay: SVG pattern with opacity-20
Blur Effect: bg-secondary/20 blur-3xl (on hero image)

Headline:
  - Primary text: text-primary-foreground
  - Secondary text (span): text-secondary
  - Size: text-5xl md:text-6xl font-bold

CTA Buttons:
  - Primary: bg-secondary hover:bg-secondary/90 + shadow-[var(--shadow-glow)]
  - Secondary: variant="outline" with border-primary-foreground/20
```

**Feature Cards:**
```css
Card: bg-card shadow-[var(--shadow-elegant)]
Hover: shadow-[var(--shadow-glow)] transition-all

Icon Container:
  - bg-accent/10
  - Hover: bg-accent/20
  - Icon color: text-accent

Title: text-xl font-semibold text-foreground
Description: text-muted-foreground
```

**CTA Section:**
```css
Background: bg-primary/5 (5% opacity olive green)
Button: bg-secondary hover:bg-secondary/90
```

#### Responsive Behavior
```
Mobile (<768px):
  - Hero: Single column, stacked
  - Buttons: flex-col (stacked)
  - Features: Single column grid

Tablet (768px-1024px):
  - Features: 2 columns

Desktop (>1024px):
  - Hero: 2 columns
  - Features: 4 columns
```

---

### 2. AR Try-On Page (TryOn.tsx)

#### Layout Structure
```
Navigation (fixed header)
└── Main Container
    ├── Page Header (centered)
    └── Grid: lg:grid-cols-2
        ├── Left: AR Camera Card
        │   ├── Camera View (aspect-[4/5])
        │   └── Action Buttons (Reset, Save)
        └── Right Column
            ├── Shoe Selection Card
            │   └── Grid: grid-cols-2 (4 shoes)
            └── Quick Actions Card
                ├── Add to Closet
                ├── View in 3D
                └── Match with Outfit
```

#### Theme Application

**Page Header:**
```css
Title: text-4xl font-bold text-foreground
Subtitle: text-muted-foreground
Spacing: mb-8
```

**Camera Card:**
```css
Card: p-8 bg-card shadow-[var(--shadow-elegant)]
Camera View:
  - Background: bg-muted
  - Gradient overlay: bg-gradient-to-br from-primary/5 to-accent/5
  - Icon: text-muted-foreground

Enable Button: bg-secondary hover:bg-secondary/90
```

**Shoe Selection:**
```css
Grid items:
  - Background: bg-muted
  - Hover: hover:ring-2 hover:ring-secondary
  - Transition: transition-all
  - Inner gradient: from-primary/10 to-accent/10
```

**Quick Actions:**
```css
Buttons:
  - Primary: bg-accent hover:bg-accent/90
  - Secondary: variant="outline"
  - Width: w-full
  - Spacing: space-y-3
```

#### Responsive Behavior
```
Mobile: Single column (camera on top, selections below)
Desktop: 2 columns side-by-side
```

---

### 3. My Closet Page (Closet.tsx)

#### Layout Structure
```
Navigation (fixed header)
└── Main Container
    ├── Page Header
    ├── Filter Badges (All, Favorites, Recent)
    └── Grid: grid-cols-1 md:grid-cols-2 lg:grid-cols-4
        └── Shoe Cards (8 items)
            ├── Image (aspect-square)
            │   └── Favorite Star (conditional)
            └── Card Footer
                ├── Shoe Name
                ├── Try-On Date
                └── Action Buttons (Try Again, Delete)
```

#### Theme Application

**Filter Badges:**
```css
Active: bg-accent text-accent-foreground
Inactive: variant="outline"
Spacing: gap-3 mb-6
```

**Shoe Cards:**
```css
Card: bg-card shadow-[var(--shadow-elegant)]
Hover: hover:shadow-[var(--shadow-glow)] transition-all

Image Area:
  - Gradient: bg-gradient-to-br from-primary/10 to-accent/10
  - Hover overlay: bg-primary/0 group-hover:bg-primary/5

Favorite Star:
  - Color: fill-secondary text-secondary
  - Position: absolute top-3 right-3

Footer:
  - Padding: p-4
  - Title: font-semibold text-foreground
  - Date: text-sm text-muted-foreground

Buttons:
  - Try Again: bg-accent hover:bg-accent/90
  - Delete: border with hover:bg-destructive hover:text-destructive-foreground
```

#### Grid Responsiveness
```
Mobile (default):   1 column
Tablet (md):        2 columns
Desktop (lg):       4 columns
Gap: gap-6 (24px between cards)
```

---

### 4. Outfit Match Page (OutfitMatch.tsx)

#### Layout Structure
```
Navigation (fixed header)
└── Main Container
    ├── Page Header (centered)
    ├── Top Grid: lg:grid-cols-3
    │   ├── Current Shoe Card (1 column)
    │   └── Outfit Preview Card (2 columns)
    └── Suggestions Section
        ├── Section Header
        └── Grid: md:grid-cols-2 lg:grid-cols-4
            └── Outfit Cards (4 items)
```

#### Theme Application

**Current Shoe Card:**
```css
Card: p-6 bg-card shadow-[var(--shadow-elegant)]
Header: flex items-center gap-2 with ShoppingBag icon
Preview: gradient from-primary/10 to-accent/10
Button: variant="outline" w-full
```

**Outfit Preview Card:**
```css
Span: lg:col-span-2
Card: p-6 bg-card shadow-[var(--shadow-elegant)]
Header: flex items-center gap-2 with Sparkles icon
Preview: aspect-[16/9] with gradient from-primary/5 to-accent/5

Buttons:
  - Save: bg-secondary hover:bg-secondary/90 flex-1
  - Try: variant="outline" flex-1
  - Layout: flex gap-3
```

**Outfit Suggestion Cards:**
```css
Card: p-6 bg-card shadow-[var(--shadow-elegant)]
Hover: hover:shadow-[var(--shadow-glow)] transition-all cursor-pointer

Preview:
  - Aspect: aspect-[3/4]
  - Gradient: from-primary/10 to-accent/10
  - Hover: group-hover:scale-105 transition-transform

Content:
  - Title: font-semibold text-foreground
  - Items: text-sm text-muted-foreground (bulleted list)
```

#### Responsive Layout
```
Mobile: All cards single column, full width
Tablet (md): Suggestions in 2 columns
Desktop (lg): Top section 1:2 ratio, Suggestions 4 columns
```

---

### 5. Customize Page (Customize.tsx)

#### Layout Structure
```
Navigation (fixed header)
└── Main Container
    ├── Page Header (centered)
    └── Grid: lg:grid-cols-2
        ├── Left: 3D Preview Card
        │   ├── Preview (aspect-square)
        │   └── Save Button
        └── Right: Customization Options
            ├── Color Selection Card
            ├── Texture Selection Card
            └── Shine Adjustment Card
```

#### Theme Application

**3D Preview Card:**
```css
Card: p-8 bg-card shadow-[var(--shadow-elegant)]
Preview:
  - Gradient: from-primary/5 to-accent/5
  - Aspect: aspect-square
  - Text: text-muted-foreground

Button: bg-secondary hover:bg-secondary/90 w-full
```

**Color Selection:**
```css
Card: p-6 bg-card shadow-[var(--shadow-elegant)]
Header: text-lg font-semibold text-foreground mb-4

Grid: grid-cols-4 gap-3
Color Buttons:
  - Size: aspect-square
  - Border: border-2 border-border
  - Hover: hover:border-accent transition-all
  - Background: inline style with color.hex
  - Selected: Check icon with text-accent
```

**Texture Selection:**
```css
Grid: grid-cols-2 gap-3
Buttons:
  - Padding: p-4
  - Border: border-2
  - Selected: border-accent bg-accent/10
  - Default: border-border hover:border-accent
  - Text: font-medium text-foreground
```

**Shine Adjustment:**
```css
Slider: Custom component with track and thumb
Labels: text-sm text-muted-foreground
Layout: flex justify-between
Range: 0-100 (Matte to Glossy)
```

#### Responsive Behavior
```
Mobile: Single column (preview on top)
Desktop (lg): 2 columns side-by-side
```

---

## 📱 Responsive Design

### Breakpoints (Tailwind)
```
sm:  640px   (Small tablets)
md:  768px   (Tablets)
lg:  1024px  (Small laptops)
xl:  1280px  (Desktops)
2xl: 1536px  (Large screens)
```

### Mobile-First Approach

All styles are mobile-first, with larger screens using responsive prefixes:

```css
/* Mobile (default): Full width, stacked */
className="text-xl grid-cols-1"

/* Tablet and above: Larger text, 2 columns */
className="text-xl md:text-2xl md:grid-cols-2"

/* Desktop: Even larger, 4 columns */
className="text-xl md:text-2xl lg:text-3xl lg:grid-cols-4"
```

### Touch Targets
```
Minimum touch target size: 44x44px (iOS guidelines)

Buttons:
  - Default height: h-10 (40px) + padding
  - Large buttons: h-11 (44px) + padding
  - Icon buttons: p-3 (minimum 48px total)
```

### Navigation Adaptations
```
Desktop (md and above):
  - Horizontal nav links visible
  - Logo on left, links center, button right

Mobile (default):
  - Only logo and Sign In button visible
  - Hidden: .hidden .md:flex (nav links)
  - Could implement mobile menu drawer (not currently in code)
```

---

## ✨ Animations & Transitions

### CSS Variables for Animations
```css
/* Defined in index.css */
--gradient-hero: linear-gradient(135deg, hsl(85 25% 45%), hsl(85 30% 35%))
--gradient-card: linear-gradient(180deg, hsl(0 0% 100%), hsl(0 0% 98%))
--shadow-elegant: 0 10px 40px -10px hsl(30 35% 25% / 0.15)
--shadow-glow: 0 0 30px hsl(85 25% 45% / 0.3)
```

### Transition Classes
```css
transition-all: All properties, 150ms cubic-bezier
transition-colors: Color properties only
transition-transform: Transform properties only
```

### Animation Usage by Component

#### Cards
```css
/* Hover elevation */
hover:shadow-[var(--shadow-glow)] transition-all

/* Hover scale */
group-hover:scale-105 transition-transform
```

#### Buttons
```css
/* State changes */
hover:bg-primary/90 transition-colors

/* Icon rotation (not used but common pattern) */
group-hover:rotate-12 transition-transform
```

#### Images
```css
/* Fade in on load */
animate-fade-in

/* Drop shadow */
drop-shadow-2xl
```

### Keyframe Animations (Available)
```css
/* From tailwind.config.ts */
@keyframes accordion-down {
  from: { height: 0 }
  to: { height: var(--radix-accordion-content-height) }
}

@keyframes accordion-up {
  from: { height: var(--radix-accordion-content-height) }
  to: { height: 0 }
}

/* Usage */
animation: "accordion-down 0.2s ease-out"
animation: "accordion-up 0.2s ease-out"
```

---

## 🎭 Visual Effects

### Gradients

#### Hero Background
```css
background: var(--gradient-hero)
/* Light: linear-gradient(135deg, hsl(85 25% 45%), hsl(85 30% 35%)) */
/* Dark:  linear-gradient(135deg, hsl(85 25% 35%), hsl(85 30% 25%)) */

/* Pattern overlay (SVG data URI) */
background-image: url('data:image/svg+xml;base64,...')
opacity: 0.2
```

#### Card Backgrounds
```css
/* Subtle card gradient */
background: var(--gradient-card)
/* Light: linear-gradient(180deg, hsl(0 0% 100%), hsl(0 0% 98%)) */
/* Dark:  linear-gradient(180deg, hsl(30 25% 18%), hsl(30 20% 15%)) */
```

#### Element Gradients
```css
/* Product preview placeholders */
bg-gradient-to-br from-primary/10 to-accent/10

/* Overlay effects */
bg-gradient-to-br from-primary/5 to-accent/5
```

### Shadows

#### Elevation System
```css
/* Card resting state */
shadow-[var(--shadow-elegant)]
/* Light: 0 10px 40px -10px hsl(30 35% 25% / 0.15) */
/* Dark:  0 10px 40px -10px hsl(0 0% 0% / 0.5) */

/* Interactive hover state */
shadow-[var(--shadow-glow)]
/* Light: 0 0 30px hsl(85 25% 45% / 0.3) */
/* Dark:  0 0 30px hsl(85 25% 45% / 0.4) */
```

### Blur Effects

#### Backdrop Blur
```css
/* Navigation bar */
backdrop-blur-sm (4px blur)

/* Glass morphism effects */
bg-primary/95 backdrop-blur-sm
```

#### Glow Effects
```css
/* Hero image glow */
<div className="absolute inset-0 bg-secondary/20 blur-3xl rounded-full"></div>
```

### Opacity Patterns

#### Hover States
```css
/* Inactive to active */
text-primary-foreground/80  /* 80% opacity */
hover:text-primary-foreground /* 100% opacity */

/* Overlay reveals */
bg-primary/0 group-hover:bg-primary/5
```

---

## 🔍 Icon System

### Library: Lucide React

#### Installation
```bash
npm install lucide-react
```

#### Icon Usage
```tsx
import { Camera, ShoppingBag, Sparkles } from "lucide-react";

<Camera className="h-5 w-5 text-accent" />
```

### Icon Sizes
```
h-4 w-4:  16px (Small inline icons)
h-5 w-5:  20px (Button icons)
h-6 w-6:  24px (Feature card icons)
h-12 w-12: 48px (Large placeholder icons)
h-16 w-16: 64px (Hero icons)
```

### Icons Used by Page

#### Navigation
```tsx
Camera       // AR Try-On
ShoppingBag  // My Closet
Sparkles     // Outfit Match
Palette      // Customize
```

#### Landing Page
```tsx
ArrowRight   // CTA buttons
Footprints   // AR Try-On feature
Sparkles     // Outfit matching feature
Palette      // Customize feature
ShoppingBag  // Closet feature
```

#### Try-On Page
```tsx
Camera       // Camera view placeholder
RefreshCw    // Reset button
Download     // Save button
```

#### Closet Page
```tsx
Star         // Favorite indicator
Trash2       // Delete button
```

#### Outfit Match Page
```tsx
ShoppingBag  // Current shoe section
Sparkles     // Outfit preview section
```

#### Customize Page
```tsx
Check        // Selected color indicator
```

### Icon Color Patterns
```css
/* Accent colored (primary feature) */
text-accent

/* Muted (placeholder/inactive) */
text-muted-foreground

/* Destructive (delete actions) */
text-destructive

/* Inherit from parent */
(no color class, inherits from button/link)
```

---

## 🧪 Design Tokens Reference

### Quick Copy-Paste Values

#### Primary Color Palette
```
Olive Green:  #7A9F6B  HSL(85, 25%, 45%)
Warm Brown:   #B8895F  HSL(30, 40%, 50%)
Dark Olive:   #5D7F4E  HSL(85, 30%, 40%)
```

#### Neutral Palette (Light)
```
Background:   #FAFAFA  HSL(0, 0%, 98%)
Foreground:   #403121  HSL(30, 35%, 25%)
Muted:        #EBE5DE  HSL(30, 20%, 90%)
Border:       #DDD5CC  HSL(30, 15%, 85%)
```

#### Neutral Palette (Dark)
```
Background:   #1F1914  HSL(30, 20%, 12%)
Foreground:   #F2F2F2  HSL(0, 0%, 95%)
Muted:        #3D3329  HSL(30, 20%, 25%)
Border:       #3D3329  HSL(30, 20%, 25%)
```

#### Semantic Colors
```
Success:      (Use accent/primary)
Warning:      (Not defined, use HSL(45, 100%, 50%) if needed)
Error:        #E53935  HSL(0, 84.2%, 60.2%)
```

---

## 📋 Component Checklist

### Required Components for Full App

#### Core UI (shadcn/ui - already included)
- [x] Button
- [x] Card
- [x] Badge
- [x] Slider
- [x] All other shadcn components

#### Custom Components Needed
- [ ] Mobile Navigation Drawer/Menu
- [ ] Camera Permission Handler
- [ ] 3D Shoe Renderer (Three.js integration)
- [ ] AR View Component (AR.js or similar)
- [ ] Image Upload Handler
- [ ] Product Card (reusable)
- [ ] Loading Skeletons
- [ ] Empty States

#### Pages (all implemented)
- [x] Landing/Index
- [x] AR Try-On
- [x] My Closet
- [x] Outfit Match
- [x] Customize
- [x] 404 Not Found

---

## 🎯 Implementation Notes for Flutter/Dart

### Color Conversion
```dart
// Example: Converting HSL to Flutter Color
// Primary: HSL(85, 25%, 45%)

Color primary = Color.fromRGBO(122, 159, 107, 1.0);
// Or using HSL package:
Color primary = HSLColor.fromAHSL(1.0, 85, 0.25, 0.45).toColor();
```

### Layout Equivalents
```dart
// Tailwind "container mx-auto px-4"
Container(
  width: double.infinity,
  padding: EdgeInsets.symmetric(horizontal: 16),
  child: ConstrainedBox(
    constraints: BoxConstraints(maxWidth: 1280),
    child: child,
  ),
)

// Tailwind "grid lg:grid-cols-4 gap-6"
GridView.builder(
  gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
    crossAxisCount: MediaQuery.of(context).size.width > 1024 ? 4 : 2,
    crossAxisSpacing: 24,
    mainAxisSpacing: 24,
  ),
)
```

### Shadow Equivalents
```dart
// shadow-[var(--shadow-elegant)]
BoxShadow(
  color: Color(0x26403121), // HSL(30, 35%, 25%) at 15% opacity
  blurRadius: 40,
  offset: Offset(0, 10),
  spreadRadius: -10,
)

// shadow-[var(--shadow-glow)]
BoxShadow(
  color: Color(0x4D7A9F6B), // HSL(85, 25%, 45%) at 30% opacity
  blurRadius: 30,
  offset: Offset(0, 0),
)
```

### Gradient Equivalents
```dart
// bg-gradient-to-br from-primary/10 to-accent/10
LinearGradient(
  begin: Alignment.topLeft,
  end: Alignment.bottomRight,
  colors: [
    Color(0x1A7A9F6B), // Primary at 10% opacity
    Color(0x1A5D7F4E), // Accent at 10% opacity
  ],
)
```

### Responsive Breakpoints
```dart
// Equivalent to Tailwind md: (768px) and lg: (1024px)
double screenWidth = MediaQuery.of(context).size.width;
int columns = screenWidth > 1024 ? 4 : screenWidth > 768 ? 2 : 1;
```

---

## 📦 Assets & Resources

### Images
```
src/assets/hero-shoe.jpg  - Main hero image
src/assets/logo.png       - App logo (56px height recommended)
```

### Image Optimization Guidelines
- **Hero images**: 1920x1080px max, optimized for web
- **Product images**: 800x800px square
- **Logo**: SVG or PNG with transparency, multiple sizes
- **Format**: WebP with JPEG fallback for best performance

---

## 🔐 Accessibility Notes

### Color Contrast
All color combinations meet WCAG AA standards:
- Foreground on Background: 4.5:1 minimum
- Primary Foreground on Primary: 4.5:1 minimum
- Buttons maintain contrast in all states

### Interactive Elements
- All buttons have min 44x44px touch targets
- Focus states use ring-2 ring-ring
- Alt text on all images
- Semantic HTML structure (header, main, section, nav)

### Screen Reader Support
```tsx
// Icon buttons
<button aria-label="Delete shoe">
  <Trash2 className="h-4 w-4" />
</button>

// Decorative content
<span className="sr-only">Accessibility label</span>
```

---

## 📝 Design System Checklist

### When Implementing Each Page

- [ ] Use semantic color tokens (never direct colors)
- [ ] Apply --shadow-elegant to resting cards
- [ ] Add --shadow-glow on hover for interactive cards
- [ ] Use proper spacing scale (4, 6, 8, 12, 16, 20)
- [ ] Include mobile-first responsive breakpoints
- [ ] Add transition-all to hover effects
- [ ] Use aspect-ratio utilities for media
- [ ] Include proper icon sizes (h-4 to h-6 typically)
- [ ] Add aria-labels to icon-only buttons
- [ ] Test dark mode variant

---

## 🚀 Getting Started

1. **Start with colors**: Implement the HSL color system first
2. **Build base components**: Card, Button, Badge using semantic tokens
3. **Create layout system**: Container, grid, spacing utilities
4. **Implement pages**: One at a time, following the structure above
5. **Add interactions**: Hover states, transitions, animations
6. **Test responsive**: Mobile, tablet, desktop breakpoints
7. **Verify dark mode**: All components in both themes

---

## 📖 Additional Resources

- **Design System**: All colors use HSL format for easy manipulation
- **Component Library**: shadcn/ui (React) - translate to Flutter equivalents
- **Icons**: Lucide React - use equivalent Flutter icon library
- **Font**: System font stack - use platform defaults in Flutter
- **Spacing**: 8px base unit (0.5rem increments)

---

**End of Design Specifications**

*Last updated: 2025-10-24*
*Version: 1.0*
*For: SoleMate AR Shoe Shopping Application*
