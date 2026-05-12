# SoleMate Design System
**Version 1.0** | Minimalist Light Theme

A modern, high-contrast design system for the SoleMate AR shoe try-on platform. Built for clarity, accessibility, and premium aesthetics.

---

## Design Philosophy

**Core Principles:**
- **Minimalist**: Clean layouts with purposeful whitespace
- **High Contrast**: Excellent readability from any distance
- **Premium**: Fashion-tech aesthetic inspired by Nike, Apple, StockX
- **Fast**: Lightweight, smooth interactions
- **Accessible**: WCAG AA compliant contrast ratios

---

## Color System

### Primary Palette

```css
/* Primary - Deep Charcoal for authority and sophistication */
--color-primary-50: #F7F7F8;
--color-primary-100: #EFEFF1;
--color-primary-200: #DCDDE2;
--color-primary-300: #B8BBC4;
--color-primary-400: #8B8F9B;
--color-primary-500: #5F6370;
--color-primary-600: #1A1D29;  /* Main primary */
--color-primary-700: #13151E;
--color-primary-800: #0D0E14;
--color-primary-900: #07080A;

/* Accent - Electric Blue for interactive elements */
--color-accent-50: #EFF6FF;
--color-accent-100: #DBEAFE;
--color-accent-200: #BFDBFE;
--color-accent-300: #93C5FD;
--color-accent-400: #60A5FA;
--color-accent-500: #3B82F6;  /* Main accent */
--color-accent-600: #2563EB;
--color-accent-700: #1D4ED8;
--color-accent-800: #1E40AF;
--color-accent-900: #1E3A8A;
```

### Semantic Colors

```css
/* Success - Emerald */
--color-success-50: #ECFDF5;
--color-success-500: #10B981;
--color-success-600: #059669;

/* Warning - Amber */
--color-warning-50: #FFFBEB;
--color-warning-500: #F59E0B;
--color-warning-600: #D97706;

/* Error - Rose */
--color-error-50: #FFF1F2;
--color-error-500: #F43F5E;
--color-error-600: #E11D48;

/* Info - Sky */
--color-info-50: #F0F9FF;
--color-info-500: #0EA5E9;
--color-info-600: #0284C7;
```

### Neutrals (High Contrast)

```css
/* Surface colors */
--color-white: #FFFFFF;
--color-gray-50: #F9FAFB;
--color-gray-100: #F3F4F6;
--color-gray-200: #E5E7EB;
--color-gray-300: #D1D5DB;
--color-gray-400: #9CA3AF;
--color-gray-500: #6B7280;
--color-gray-600: #4B5563;
--color-gray-700: #374151;
--color-gray-800: #1F2937;
--color-gray-900: #111827;
--color-black: #000000;
```

### Background System

```css
--bg-primary: var(--color-white);
--bg-secondary: var(--color-gray-50);
--bg-tertiary: var(--color-gray-100);
--bg-elevated: var(--color-white);
--bg-overlay: rgba(0, 0, 0, 0.4);
```

### Text System

```css
--text-primary: var(--color-gray-900);      /* Highest contrast */
--text-secondary: var(--color-gray-700);
--text-tertiary: var(--color-gray-500);
--text-inverse: var(--color-white);
--text-link: var(--color-accent-600);
```

---

## Typography

### Font Families

```css
/* Primary: Inter - Clean, modern, highly legible */
--font-sans: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif;

/* Monospace: For technical content */
--font-mono: 'JetBrains Mono', 'Fira Code', monospace;
```

### Type Scale (Responsive)

```css
/* Headings */
--text-h1: 3rem;        /* 48px - Hero titles */
--text-h2: 2.25rem;     /* 36px - Section titles */
--text-h3: 1.875rem;    /* 30px - Card titles */
--text-h4: 1.5rem;      /* 24px - Subsections */
--text-h5: 1.25rem;     /* 20px - Small headings */
--text-h6: 1.125rem;    /* 18px - Labels */

/* Body */
--text-xl: 1.25rem;     /* 20px - Large body */
--text-lg: 1.125rem;    /* 18px - Medium body */
--text-base: 1rem;      /* 16px - Default body */
--text-sm: 0.875rem;    /* 14px - Small text */
--text-xs: 0.75rem;     /* 12px - Captions */
```

### Font Weights

```css
--font-light: 300;
--font-normal: 400;
--font-medium: 500;
--font-semibold: 600;
--font-bold: 700;
--font-extrabold: 800;
```

### Line Heights

```css
--leading-tight: 1.25;      /* Headings */
--leading-snug: 1.375;      /* Subheadings */
--leading-normal: 1.5;      /* Body text */
--leading-relaxed: 1.625;   /* Long-form content */
--leading-loose: 2;         /* Spacious layouts */
```

### Typography Usage

```css
/* H1 - Hero */
font-size: var(--text-h1);
font-weight: var(--font-extrabold);
line-height: var(--leading-tight);
letter-spacing: -0.02em;

/* H2 - Section */
font-size: var(--text-h2);
font-weight: var(--font-bold);
line-height: var(--leading-tight);
letter-spacing: -0.01em;

/* Body - Default */
font-size: var(--text-base);
font-weight: var(--font-normal);
line-height: var(--leading-normal);
```

---

## Spacing System

### Base Scale (8px grid)

```css
--spacing-0: 0;
--spacing-0.5: 0.125rem;   /* 2px */
--spacing-1: 0.25rem;      /* 4px */
--spacing-2: 0.5rem;       /* 8px */
--spacing-3: 0.75rem;      /* 12px */
--spacing-4: 1rem;         /* 16px */
--spacing-5: 1.25rem;      /* 20px */
--spacing-6: 1.5rem;       /* 24px */
--spacing-8: 2rem;         /* 32px */
--spacing-10: 2.5rem;      /* 40px */
--spacing-12: 3rem;        /* 48px */
--spacing-16: 4rem;        /* 64px */
--spacing-20: 5rem;        /* 80px */
--spacing-24: 6rem;        /* 96px */
--spacing-32: 8rem;        /* 128px */
```

### Layout Spacing

```css
--gap-section: var(--spacing-24);     /* Between major sections */
--gap-component: var(--spacing-8);    /* Between components */
--gap-element: var(--spacing-4);      /* Between elements */
--gap-tight: var(--spacing-2);        /* Tight grouping */
```

---

## Border System

### Radius

```css
--radius-none: 0;
--radius-sm: 0.25rem;      /* 4px - Badges, pills */
--radius-base: 0.5rem;     /* 8px - Buttons, inputs */
--radius-md: 0.75rem;      /* 12px - Cards */
--radius-lg: 1rem;         /* 16px - Modals */
--radius-xl: 1.5rem;       /* 24px - Feature cards */
--radius-2xl: 2rem;        /* 32px - Hero elements */
--radius-full: 9999px;     /* Circular */
```

### Border Widths

```css
--border-0: 0;
--border-1: 1px;
--border-2: 2px;
--border-4: 4px;
```

### Border Colors

```css
--border-light: var(--color-gray-200);
--border-base: var(--color-gray-300);
--border-strong: var(--color-gray-400);
--border-accent: var(--color-accent-500);
```

---

## Shadows

### Elevation System

```css
/* Subtle elevation */
--shadow-xs: 0 1px 2px 0 rgba(0, 0, 0, 0.05);

/* Card elevation */
--shadow-sm: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 
             0 1px 2px -1px rgba(0, 0, 0, 0.1);

/* Hover states */
--shadow-base: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 
               0 2px 4px -2px rgba(0, 0, 0, 0.1);

/* Modals, dropdowns */
--shadow-md: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 
             0 4px 6px -4px rgba(0, 0, 0, 0.1);

/* Popovers */
--shadow-lg: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 
             0 8px 10px -6px rgba(0, 0, 0, 0.1);

/* Dialogs */
--shadow-xl: 0 25px 50px -12px rgba(0, 0, 0, 0.25);

/* Focus states */
--shadow-focus: 0 0 0 3px rgba(59, 130, 246, 0.3);
```

---

## Component Patterns

### Buttons

#### Primary Button
```css
background: var(--color-primary-600);
color: var(--color-white);
padding: var(--spacing-3) var(--spacing-6);
border-radius: var(--radius-base);
font-weight: var(--font-semibold);
font-size: var(--text-base);
box-shadow: var(--shadow-sm);
transition: all 150ms cubic-bezier(0.4, 0, 0.2, 1);

/* Hover */
background: var(--color-primary-700);
box-shadow: var(--shadow-base);
transform: translateY(-1px);
```

#### Accent Button
```css
background: var(--color-accent-500);
color: var(--color-white);
/* Same structure as primary */
```

#### Outline Button
```css
background: transparent;
color: var(--color-primary-600);
border: var(--border-2) solid var(--color-primary-600);
```

#### Ghost Button
```css
background: transparent;
color: var(--color-primary-600);
border: none;
```

#### Sizes
```css
/* Small */
padding: var(--spacing-2) var(--spacing-4);
font-size: var(--text-sm);

/* Medium (default) */
padding: var(--spacing-3) var(--spacing-6);
font-size: var(--text-base);

/* Large */
padding: var(--spacing-4) var(--spacing-8);
font-size: var(--text-lg);
```

### Cards

```css
background: var(--bg-elevated);
border: var(--border-1) solid var(--border-light);
border-radius: var(--radius-md);
box-shadow: var(--shadow-sm);
padding: var(--spacing-6);

/* Hover state */
box-shadow: var(--shadow-base);
border-color: var(--border-base);
transition: all 150ms ease;
```

### Input Fields

```css
background: var(--color-white);
border: var(--border-2) solid var(--border-base);
border-radius: var(--radius-base);
padding: var(--spacing-3) var(--spacing-4);
font-size: var(--text-base);
color: var(--text-primary);

/* Focus */
border-color: var(--color-accent-500);
box-shadow: var(--shadow-focus);
outline: none;

/* Error */
border-color: var(--color-error-500);
```

### Badges

```css
display: inline-flex;
align-items: center;
padding: var(--spacing-1) var(--spacing-3);
border-radius: var(--radius-full);
font-size: var(--text-xs);
font-weight: var(--font-semibold);
text-transform: uppercase;
letter-spacing: 0.05em;

/* Status variants */
/* New */
background: var(--color-info-50);
color: var(--color-info-600);

/* Success */
background: var(--color-success-50);
color: var(--color-success-600);

/* Warning */
background: var(--color-warning-50);
color: var(--color-warning-600);

/* Error */
background: var(--color-error-50);
color: var(--color-error-600);
```

---

## Layout Patterns

### Container Widths

```css
--container-sm: 640px;      /* Mobile */
--container-md: 768px;      /* Tablet */
--container-lg: 1024px;     /* Laptop */
--container-xl: 1280px;     /* Desktop */
--container-2xl: 1536px;    /* Large desktop */
```

### Breakpoints

```css
--breakpoint-sm: 640px;
--breakpoint-md: 768px;
--breakpoint-lg: 1024px;
--breakpoint-xl: 1280px;
--breakpoint-2xl: 1536px;
```

### Grid System

```css
/* 12-column grid */
display: grid;
grid-template-columns: repeat(12, 1fr);
gap: var(--spacing-6);

/* Responsive columns */
/* Mobile: 1 column */
/* Tablet: 2 columns */
/* Desktop: 3-4 columns */
```

---

## Animation & Transitions

### Duration

```css
--duration-fast: 150ms;
--duration-base: 200ms;
--duration-slow: 300ms;
--duration-slower: 500ms;
```

### Easing

```css
--ease-in: cubic-bezier(0.4, 0, 1, 1);
--ease-out: cubic-bezier(0, 0, 0.2, 1);
--ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
--ease-spring: cubic-bezier(0.68, -0.55, 0.265, 1.55);
```

### Common Transitions

```css
/* All properties */
transition: all var(--duration-base) var(--ease-out);

/* Specific properties */
transition: background-color var(--duration-base) var(--ease-out),
            box-shadow var(--duration-base) var(--ease-out),
            transform var(--duration-base) var(--ease-out);
```

---

## Navigation Components

### Bottom Navigation (Mobile)

```css
position: fixed;
bottom: 0;
width: 100%;
background: var(--bg-elevated);
border-top: var(--border-1) solid var(--border-light);
box-shadow: var(--shadow-lg);
padding: var(--spacing-2) var(--spacing-4);
display: flex;
justify-content: space-around;
align-items: center;
z-index: 50;

/* Nav item */
.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-1);
  padding: var(--spacing-2);
  color: var(--text-tertiary);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
}

.nav-item.active {
  color: var(--color-accent-600);
}
```

### Top Navigation (Desktop)

```css
position: sticky;
top: 0;
background: var(--bg-elevated);
border-bottom: var(--border-1) solid var(--border-light);
box-shadow: var(--shadow-sm);
padding: var(--spacing-4) var(--spacing-6);
display: flex;
justify-content: space-between;
align-items: center;
z-index: 40;
```

### Sidebar Navigation (Desktop Seller Dashboard)

```css
width: 280px;
height: 100vh;
background: var(--bg-secondary);
border-right: var(--border-1) solid var(--border-light);
padding: var(--spacing-6);
display: flex;
flex-direction: column;
gap: var(--spacing-2);

/* Sidebar item */
.sidebar-item {
  padding: var(--spacing-3) var(--spacing-4);
  border-radius: var(--radius-base);
  color: var(--text-secondary);
  font-weight: var(--font-medium);
  transition: all var(--duration-fast) var(--ease-out);
}

.sidebar-item:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.sidebar-item.active {
  background: var(--color-accent-50);
  color: var(--color-accent-600);
}
```

---

## AR Try-On Specific Components

### Camera Viewport

```css
aspect-ratio: 3 / 4;
background: var(--color-gray-900);
border-radius: var(--radius-lg);
overflow: hidden;
position: relative;
box-shadow: var(--shadow-xl);
```

### AR Status Badge

```css
position: absolute;
top: var(--spacing-4);
left: var(--spacing-4);
background: rgba(0, 0, 0, 0.7);
backdrop-filter: blur(10px);
color: var(--color-white);
padding: var(--spacing-2) var(--spacing-4);
border-radius: var(--radius-full);
font-size: var(--text-sm);
font-weight: var(--font-semibold);
```

### Shoe Selection Thumbnail

```css
width: 80px;
height: 80px;
border-radius: var(--radius-base);
border: var(--border-2) solid var(--border-light);
cursor: pointer;
transition: all var(--duration-fast) var(--ease-out);

/* Selected state */
border-color: var(--color-accent-500);
box-shadow: 0 0 0 3px var(--color-accent-100);
transform: scale(1.05);
```

---

## Accessibility

### Focus States

```css
/* Keyboard focus */
outline: var(--border-2) solid var(--color-accent-500);
outline-offset: 2px;
```

### Contrast Ratios

- **Large text (18px+)**: Minimum 3:1
- **Normal text**: Minimum 4.5:1
- **Interactive elements**: Minimum 3:1

All color combinations in this system meet WCAG AA standards.

### Touch Targets

```css
/* Minimum touch target size */
min-width: 44px;
min-height: 44px;
```

---

## Component States

### Interactive States

```css
/* Default */
opacity: 1;

/* Hover */
opacity: 0.9;
transform: translateY(-1px);

/* Active (pressed) */
transform: translateY(0);
opacity: 0.95;

/* Disabled */
opacity: 0.5;
cursor: not-allowed;
pointer-events: none;

/* Loading */
position: relative;
color: transparent;
/* Add spinner overlay */
```

---

## Icons

**Icon Library**: Lucide React  
**Icon Sizes**: 16px, 20px, 24px, 32px, 48px  
**Icon Weight**: 2px stroke

```css
/* Default icon */
width: 24px;
height: 24px;
stroke-width: 2;
color: currentColor;
```

---

## Implementation Notes

### Tailwind CSS Configuration

This design system is implemented using Tailwind CSS v4 with custom CSS variables defined in `/src/styles/theme.css`.

### Font Loading

Inter font should be imported via Google Fonts or self-hosted:

```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');
```

### Dark Mode (Future)

This system is designed with light mode priority. Dark mode can be added by creating alternate CSS variable values with `@media (prefers-color-scheme: dark)`.

---

## Usage Examples

### Hero Section

```jsx
<section className="bg-white py-24 px-6">
  <div className="max-w-4xl mx-auto text-center">
    <h1 className="text-5xl font-extrabold text-gray-900 tracking-tight mb-4">
      Try Shoes in AR
    </h1>
    <p className="text-xl text-gray-700 mb-8 leading-relaxed">
      Experience the future of online shopping
    </p>
    <button className="bg-primary-600 text-white px-8 py-4 rounded-lg font-semibold shadow-md hover:bg-primary-700 hover:shadow-lg transition-all">
      Start AR Try-On
    </button>
  </div>
</section>
```

### Product Card

```jsx
<div className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm hover:shadow-md transition-all">
  <img src="shoe.jpg" alt="Shoe" className="w-full aspect-square object-cover rounded-lg mb-4" />
  <h3 className="text-lg font-semibold text-gray-900 mb-1">Air Max 270</h3>
  <p className="text-sm text-gray-500 mb-3">Nike</p>
  <div className="flex items-center justify-between">
    <span className="text-xl font-bold text-gray-900">$150</span>
    <button className="bg-accent-500 text-white px-4 py-2 rounded-lg font-medium hover:bg-accent-600">
      Try On
    </button>
  </div>
</div>
```

---

**End of Design System Specification**
