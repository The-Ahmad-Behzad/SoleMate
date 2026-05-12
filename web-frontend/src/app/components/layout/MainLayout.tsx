import { Outlet } from 'react-router';
import { TopNavigation } from '../navigation/TopNavigation';
import { BottomNavigation } from '../navigation/BottomNavigation';

export function MainLayout() {
  return (
    <div className="min-h-screen bg-white flex flex-col">
      <TopNavigation />

      <main className="flex-1 pb-20 md:pb-0">
        <Outlet />
      </main>

      <BottomNavigation />
    </div>
  );
}
