import { Heart, Box, Sparkles } from 'lucide-react';
import { Card } from '../ui/card';
import { Button } from '../ui/button';

interface QuickActionsProps {
  onAddToCloset: () => void;
  onView3D: () => void;
  onMatchOutfit: () => void;
  disabled?: boolean;
}

export function QuickActions({
  onAddToCloset,
  onView3D,
  onMatchOutfit,
  disabled = false,
}: QuickActionsProps) {
  return (
    <Card className="p-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4">
        Quick Actions
      </h3>
      <div className="space-y-3">
        <Button
          onClick={onAddToCloset}
          disabled={disabled}
          className="w-full justify-start bg-accent-500 hover:bg-accent-600 text-white"
        >
          <Heart size={18} className="mr-3" />
          Add to Closet
        </Button>
        <Button
          onClick={onView3D}
          disabled={disabled}
          variant="outline"
          className="w-full justify-start"
        >
          <Box size={18} className="mr-3" />
          View in 3D
        </Button>
        <Button
          onClick={onMatchOutfit}
          disabled={disabled}
          variant="outline"
          className="w-full justify-start"
        >
          <Sparkles size={18} className="mr-3" />
          Match with Outfit
        </Button>
      </div>
    </Card>
  );
}
