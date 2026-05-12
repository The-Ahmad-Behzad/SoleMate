import { RefreshCw, Check } from 'lucide-react';
import { Card } from '../ui/card';
import { Button } from '../ui/button';
import { ScrollArea } from '../ui/scroll-area';

interface Shoe {
  id: string;
  name: string;
  brand: string;
  thumbnailUrl: string;
}

interface ShoeSelectorProps {
  shoes: Shoe[];
  selectedShoeId?: string;
  onSelectShoe: (id: string) => void;
  onRefresh: () => void;
  isLoading?: boolean;
}

export function ShoeSelector({
  shoes,
  selectedShoeId,
  onSelectShoe,
  onRefresh,
  isLoading = false,
}: ShoeSelectorProps) {
  return (
    <Card className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-gray-900">Select Shoe</h3>
        <Button
          variant="ghost"
          size="icon"
          onClick={onRefresh}
          disabled={isLoading}
          className="h-8 w-8"
        >
          <RefreshCw
            size={18}
            className={isLoading ? 'animate-spin' : ''}
          />
        </Button>
      </div>

      <ScrollArea className="h-[400px] pr-4">
        <div className="grid grid-cols-2 gap-4">
          {shoes.map((shoe) => {
            const isSelected = selectedShoeId === shoe.id;

            return (
              <button
                key={shoe.id}
                onClick={() => onSelectShoe(shoe.id)}
                className={`relative aspect-square rounded-lg overflow-hidden transition-all ${
                  isSelected
                    ? 'ring-4 ring-accent-500 ring-offset-2'
                    : 'ring-2 ring-gray-200 hover:ring-gray-300'
                }`}
              >
                <img
                  src={shoe.thumbnailUrl}
                  alt={shoe.name}
                  className="w-full h-full object-cover"
                />
                {isSelected && (
                  <div className="absolute inset-0 bg-accent-500/20 flex items-center justify-center">
                    <div className="w-8 h-8 bg-accent-500 rounded-full flex items-center justify-center">
                      <Check size={20} className="text-white" />
                    </div>
                  </div>
                )}
                <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/80 to-transparent p-2">
                  <p className="text-xs text-white font-medium truncate">
                    {shoe.name}
                  </p>
                </div>
              </button>
            );
          })}
        </div>
      </ScrollArea>

      {shoes.length === 0 && (
        <div className="text-center py-12 text-gray-500">
          <p className="mb-2">No shoes available</p>
          <Button variant="outline" size="sm" onClick={onRefresh}>
            Retry
          </Button>
        </div>
      )}
    </Card>
  );
}
