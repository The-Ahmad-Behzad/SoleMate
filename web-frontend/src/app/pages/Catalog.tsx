import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router';
import { Search, SlidersHorizontal } from 'lucide-react';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import { ProductCard } from '../components/shared/ProductCard';
import { Badge } from '../components/ui/badge';
import { api } from '../../lib/api';
import { toast } from 'sonner';

const categories = ['All', 'Running', 'Lifestyle', 'Basketball', 'Casual'];

export function CatalogPage() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');
  const [favorites, setFavorites] = useState<Set<string>>(new Set());
  const [shoes, setShoes] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    fetchShoes();
  }, []);

  const fetchShoes = async () => {
    setIsLoading(true);
    try {
      const data = await api.getCatalog();
      setShoes(data);
    } catch (error) {
      toast.error('Failed to load shoe catalog');
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleTryOn = (id: string) => {
    navigate(`/try-on?shoe=${id}`);
  };

  const handleFavorite = (id: string) => {
    setFavorites((prev) => {
      const newFavorites = new Set(prev);
      if (newFavorites.has(id)) {
        newFavorites.delete(id);
      } else {
        newFavorites.add(id);
      }
      return newFavorites;
    });
  };

  const filteredShoes = shoes.filter((shoe) => {
    const name = shoe.name || '';
    const brand = shoe.brand || '';
    const matchesSearch =
      name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      brand.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory =
      selectedCategory === 'All' || shoe.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl md:text-4xl font-bold text-gray-900 mb-2">
            Shoe Catalog
          </h1>
          <p className="text-gray-600">
            Browse our collection and try them on with AR
          </p>
        </div>

        {/* Search and Filters */}
        <div className="mb-8 space-y-4">
          <div className="flex gap-4">
            <div className="flex-1 relative">
              <Search
                size={20}
                className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400"
              />
              <Input
                type="text"
                placeholder="Search shoes or brands..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10"
              />
            </div>
            <Button variant="outline" size="icon" onClick={fetchShoes} disabled={isLoading}>
              <SlidersHorizontal size={20} className={isLoading ? 'animate-spin' : ''} />
            </Button>
          </div>

          {/* Category Filters */}
          <div className="flex gap-2 overflow-x-auto pb-2">
            {categories.map((category) => (
              <Badge
                key={category}
                variant={selectedCategory === category ? 'default' : 'outline'}
                className="cursor-pointer whitespace-nowrap"
                onClick={() => setSelectedCategory(category)}
              >
                {category}
              </Badge>
            ))}
          </div>
        </div>

        {/* Results Count */}
        <div className="mb-6">
          <p className="text-sm text-gray-600">
            {isLoading ? 'Loading shoes...' : `Showing ${filteredShoes.length} ${filteredShoes.length === 1 ? 'shoe' : 'shoes'}`}
          </p>
        </div>

        {/* Product Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredShoes.map((shoe) => {
            const id = shoe.id || shoe._id;
            return (
              <ProductCard
                key={id}
                id={id}
                name={shoe.name}
                brand={shoe.brand}
                price={shoe.price}
                thumbnailUrl={shoe.thumbnailUrl}
                onTryOn={handleTryOn}
                onFavorite={handleFavorite}
                isFavorite={favorites.has(id)}
              />
            );
          })}
        </div>

        {/* Empty State */}
        {!isLoading && filteredShoes.length === 0 && (
          <div className="text-center py-16">
            <p className="text-gray-500 text-lg mb-4">No shoes found</p>
            <Button
              variant="outline"
              onClick={() => {
                setSearchQuery('');
                setSelectedCategory('All');
              }}
            >
              Clear filters
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
