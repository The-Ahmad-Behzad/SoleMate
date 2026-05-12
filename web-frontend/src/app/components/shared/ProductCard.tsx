import { Heart } from 'lucide-react';
import { Card } from '../ui/card';
import { Button } from '../ui/button';

interface ProductCardProps {
  id: string;
  name: string;
  brand: string;
  price: number;
  thumbnailUrl: string;
  onTryOn?: (id: string) => void;
  onFavorite?: (id: string) => void;
  isFavorite?: boolean;
}

export function ProductCard({
  id,
  name,
  brand,
  price,
  thumbnailUrl,
  onTryOn,
  onFavorite,
  isFavorite = false,
}: ProductCardProps) {
  return (
    <Card className="overflow-hidden hover:shadow-md transition-all group">
      <div className="relative aspect-square overflow-hidden bg-gray-50">
        <img
          src={thumbnailUrl}
          alt={name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
        />
        <button
          onClick={() => onFavorite?.(id)}
          className="absolute top-3 right-3 w-10 h-10 rounded-full bg-white/90 backdrop-blur-sm flex items-center justify-center hover:bg-white transition-colors shadow-md"
        >
          <Heart
            size={20}
            className={isFavorite ? 'fill-error-500 text-error-500' : 'text-gray-600'}
          />
        </button>
      </div>
      <div className="p-4">
        <p className="text-sm text-gray-500 mb-1">{brand}</p>
        <h3 className="text-base font-semibold text-gray-900 mb-3 line-clamp-1">{name}</h3>
        <div className="flex items-center justify-between">
          <span className="text-xl font-bold text-gray-900">${price}</span>
          <Button
            onClick={() => onTryOn?.(id)}
            className="bg-accent-500 hover:bg-accent-600 text-white"
          >
            Try On
          </Button>
        </div>
      </div>
    </Card>
  );
}
