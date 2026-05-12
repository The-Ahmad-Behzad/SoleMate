import { Camera, Heart, Sparkles, Palette } from 'lucide-react';
import { Link, useLocation } from 'react-router';

const navigationItems = [
  {
    name: 'AR Try-On',
    path: '/try-on',
    icon: Camera,
  },
  {
    name: 'My Closet',
    path: '/closet',
    icon: Heart,
  },
  {
    name: 'AI Match',
    path: '/ai-match',
    icon: Sparkles,
  },
  {
    name: 'Customize',
    path: '/customize',
    icon: Palette,
  },
];

export function BottomNavigation() {
  const location = useLocation();

  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 shadow-lg z-50 md:hidden">
      <div className="flex justify-around items-center px-4 py-2">
        {navigationItems.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname === item.path;

          return (
            <Link
              key={item.path}
              to={item.path}
              className="flex flex-col items-center gap-1 py-2 px-3 transition-colors"
            >
              <Icon
                size={24}
                className={isActive ? 'text-accent-600' : 'text-gray-500'}
                strokeWidth={2}
              />
              <span
                className={`text-xs font-medium ${
                  isActive ? 'text-accent-600' : 'text-gray-500'
                }`}
              >
                {item.name}
              </span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
