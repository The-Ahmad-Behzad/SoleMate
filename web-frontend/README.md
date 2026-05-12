# SoleMate - AR Shoe Try-On Platform

A modern, minimalist web application for AR shoe try-on experiences. Built with React, TypeScript, Tailwind CSS, and designed with a premium, high-contrast aesthetic.

## Overview

SoleMate transforms the online shoe shopping experience by allowing users to visualize shoes on their feet using AR technology, get AI-powered outfit recommendations, and customize shoe designs.

## Features

### ✅ Implemented

1. **Landing Page**
   - Hero section with call-to-action
   - Feature grid showcasing main capabilities
   - Popular shoes carousel
   - Featured collection
   - Responsive design (mobile-first)

2. **AR Try-On Interface** (Priority Feature)
   - Camera viewport with AR controls
   - Shoe selection grid
   - Real-time status indicators
   - Quick actions (Add to Closet, View 3D, Match Outfit)
   - Snapshot saving functionality (API ready)
   - Mobile and desktop optimized layouts

3. **Catalog Page**
   - Product grid with filtering
   - Search functionality
   - Category badges
   - Favorite/wishlist functionality
   - Responsive grid layout

4. **My Closet**
   - Saved shoes tab
   - Try-on history tab
   - Empty states with CTAs

5. **AI Outfit Match**
   - Outfit-to-Shoe recommendations
   - Shoe-to-Outfit suggestions
   - Gender selector
   - Image upload interface

6. **Customize Shoes**
   - Base shoe selection
   - Reference image uploads
   - Color pickers (primary & secondary)
   - Design instructions textarea
   - Request submission flow

7. **Authentication**
   - Login/Register tabs
   - Guest mode option
   - Clean, centered layout

8. **Navigation**
   - Top navigation (desktop)
   - Bottom navigation (mobile)
   - Responsive menu system
   - User dropdown menu

## Design System

### Color Palette

- **Primary**: Deep Charcoal (#1A1D29)
- **Accent**: Electric Blue (#3B82F6)
- **Success**: Emerald (#10B981)
- **Warning**: Amber (#F59E0B)
- **Error**: Rose (#F43F5E)
- **Info**: Sky (#0EA5E9)

### Typography

- **Font**: Inter (300, 400, 500, 600, 700, 800)
- **Scale**: 12px - 48px (responsive)
- **High contrast** for excellent readability

### Spacing

- 8px grid system
- Consistent component spacing
- Mobile-first responsive breakpoints

## Tech Stack

- **Framework**: React 18.3.1
- **Router**: React Router 7.13.0
- **Styling**: Tailwind CSS 4.1.12
- **UI Components**: Radix UI primitives
- **Icons**: Lucide React
- **Animations**: Motion (Framer Motion)
- **Forms**: React Hook Form 7.55.0
- **Notifications**: Sonner
- **Build Tool**: Vite 6.3.5

## Project Structure

```
src/
├── app/
│   ├── components/
│   │   ├── ar/               # AR-specific components
│   │   │   ├── CameraViewport.tsx
│   │   │   ├── ShoeSelector.tsx
│   │   │   └── QuickActions.tsx
│   │   ├── layout/           # Layout components
│   │   │   └── MainLayout.tsx
│   │   ├── navigation/       # Navigation components
│   │   │   ├── BottomNavigation.tsx
│   │   │   └── TopNavigation.tsx
│   │   ├── shared/           # Shared components
│   │   │   └── ProductCard.tsx
│   │   └── ui/               # Base UI components (shadcn/ui)
│   ├── pages/                # Page components
│   │   ├── Landing.tsx
│   │   ├── Auth.tsx
│   │   ├── Catalog.tsx
│   │   ├── ARTryOn.tsx
│   │   ├── Closet.tsx
│   │   ├── AIMatch.tsx
│   │   └── Customize.tsx
│   └── App.tsx               # Root component
├── lib/
│   ├── api.ts                # API client
│   └── mock-data.ts          # Mock data for development
└── styles/
    ├── fonts.css             # Font imports
    ├── theme.css             # Design system tokens
    └── index.css             # Main CSS entry
```

## API Integration

The app is structured to connect to a Node.js BFF (Backend-for-Frontend). API endpoints are defined in `src/lib/api.ts`.

### Key Endpoints

#### Consumer API
- `GET /api/catalog` - Fetch all shoes
- `GET /api/catalog/:id` - Get shoe details
- `POST /api/tryon/save` - Save try-on session
- `GET /api/tryon/history` - Get user's try-on history
- `POST /api/outfit/recommend-shoes` - AI shoe recommendations
- `POST /api/outfit/recommend-outfit-for-shoe` - AI outfit suggestions
- `POST /api/skins/request-redesign` - Submit custom design request

#### Seller API (Future)
- `POST /api/seller/upload-url` - Get S3 upload URL
- `POST /api/seller/uploads/confirm` - Confirm upload
- `GET /api/skin-requests` - Fetch design requests
- `PATCH /api/skin-requests/:id` - Update request status

### Configuration

Update the `BASE_URL` in `src/lib/api.ts`:

```typescript
const BASE_URL = 'https://api.solemate.example.com';
```

## Development

This is a Figma Make project. The Vite dev server is already running.

### Mock Data

Mock data is provided in `src/lib/mock-data.ts` for development and testing:
- Mock shoes catalog
- Mock try-on history
- Mock AI recommendations
- Mock skin requests

## Next Steps

### Phase 2: AR Integration

1. **Native AR Implementation**
   - Integrate Snap AR SDK
   - Implement camera feed capture
   - 3D model rendering on camera view
   - Gesture controls
   - Real-time shoe placement

2. **Backend Integration**
   - Replace mock data with actual API calls
   - Implement authentication flow
   - Connect Firebase/Auth service
   - Error handling and loading states

3. **Seller Dashboard Integration**
   - Build seller-specific routes
   - Implement model upload workflow
   - Design request fulfillment UI
   - Analytics dashboard

4. **Advanced Features**
   - Real-time 3D viewer
   - Social sharing
   - User reviews and ratings
   - Payment integration
   - Order management

### Phase 3: Optimization

1. **Performance**
   - Code splitting
   - Image optimization
   - Lazy loading
   - Service worker for PWA

2. **Accessibility**
   - ARIA labels
   - Keyboard navigation
   - Screen reader support
   - Focus management

3. **Testing**
   - Unit tests (Vitest)
   - E2E tests (Playwright)
   - Component tests
   - API integration tests

## Design System Documentation

Full design system specification is available in `DESIGN_SYSTEM.md`, including:
- Complete color palette
- Typography scale
- Spacing system
- Border radius tokens
- Shadow system
- Component patterns
- Animation guidelines

## Browser Support

- Chrome 90+
- Safari 14+
- Firefox 88+
- Edge 90+

## License

Proprietary - SoleMate Platform

---

**Built with ❤️ using Figma Make**
