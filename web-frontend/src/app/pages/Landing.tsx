import { Link, useNavigate } from 'react-router';
import { Camera, Heart, Sparkles, Palette, ArrowRight } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card } from '../components/ui/card';
import { ProductCard } from '../components/shared/ProductCard';
import { mockShoes } from '../../lib/mock-data';

const features = [
  {
    icon: Camera,
    title: 'AR Try-On',
    description: 'See how shoes look on your feet in real-time with advanced AR technology',
    color: 'bg-accent-50 text-accent-600',
    link: '/try-on',
  },
  {
    icon: Heart,
    title: 'My Closet',
    description: 'Save your favorite shoes and try-on sessions in one organized place',
    color: 'bg-error-50 text-error-600',
    link: '/closet',
  },
  {
    icon: Sparkles,
    title: 'AI Outfit Match',
    description: 'Get personalized shoe recommendations based on your outfit',
    color: 'bg-warning-50 text-warning-600',
    link: '/ai-match',
  },
  {
    icon: Palette,
    title: 'Customize Shoes',
    description: 'Design custom shoe skins and request unique colorways',
    color: 'bg-success-50 text-success-600',
    link: '/customize',
  },
];

export function LandingPage() {
  const navigate = useNavigate();

  const handleTryOn = (id: string) => {
    navigate(`/try-on?shoe=${id}`);
  };

  const popularShoes = mockShoes.slice(0, 3);
  const recentShoes = mockShoes.slice(3, 6);

  return (
    <div className="min-h-screen bg-white">
      {/* Hero Section */}
      <section className="relative overflow-hidden bg-gradient-to-br from-gray-50 to-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 md:py-32">
          <div className="text-center max-w-4xl mx-auto">
            <div className="mb-8 flex justify-center">
              <div className="relative">
                <div className="w-32 h-32 bg-accent-100 rounded-full absolute -top-4 -left-4 blur-2xl opacity-50" />
                <div className="w-32 h-32 bg-primary-100 rounded-full absolute -bottom-4 -right-4 blur-2xl opacity-50" />
                <img
                  src="https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600"
                  alt="Hero shoe"
                  className="relative w-64 h-64 object-contain rounded-full"
                />
              </div>
            </div>

            <h1 className="text-5xl md:text-6xl font-extrabold text-gray-900 mb-6 tracking-tight">
              Try Shoes in AR
            </h1>
            <p className="text-xl md:text-2xl text-gray-700 mb-8 leading-relaxed max-w-3xl mx-auto">
              Experience the future of online shopping. See how shoes look on your feet before you buy.
            </p>

            <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
              <Button
                size="lg"
                onClick={() => navigate('/try-on')}
                className="bg-primary-600 hover:bg-primary-700 text-white px-8 py-6 text-lg font-semibold shadow-lg hover:shadow-xl transition-all"
              >
                <Camera className="mr-2" size={24} />
                Start AR Try-On
              </Button>
              <Button
                size="lg"
                variant="outline"
                onClick={() => navigate('/catalog')}
                className="border-2 border-primary-600 text-primary-600 hover:bg-primary-50 px-8 py-6 text-lg font-semibold"
              >
                Explore Catalog
                <ArrowRight className="ml-2" size={24} />
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4">
              Everything You Need
            </h2>
            <p className="text-lg text-gray-600 max-w-2xl mx-auto">
              Powerful features to help you find the perfect shoes
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature) => {
              const Icon = feature.icon;
              return (
                <Link key={feature.title} to={feature.link}>
                  <Card className="p-6 h-full hover:shadow-lg transition-all group cursor-pointer">
                    <div className={`w-12 h-12 rounded-lg ${feature.color} flex items-center justify-center mb-4 group-hover:scale-110 transition-transform`}>
                      <Icon size={24} strokeWidth={2} />
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">
                      {feature.title}
                    </h3>
                    <p className="text-sm text-gray-600 leading-relaxed">
                      {feature.description}
                    </p>
                  </Card>
                </Link>
              );
            })}
          </div>
        </div>
      </section>

      {/* Popular Shoes Section */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center mb-8">
            <h2 className="text-3xl font-bold text-gray-900">Popular Shoes</h2>
            <Link to="/catalog" className="text-accent-600 hover:text-accent-700 font-medium flex items-center gap-1">
              View all
              <ArrowRight size={20} />
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {popularShoes.map((shoe) => (
              <ProductCard
                key={shoe.id}
                id={shoe.id}
                name={shoe.name}
                brand={shoe.brand}
                price={shoe.price}
                thumbnailUrl={shoe.thumbnailUrl}
                onTryOn={handleTryOn}
              />
            ))}
          </div>
        </div>
      </section>

      {/* Recently Tried Section */}
      <section className="py-20 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center mb-8">
            <h2 className="text-3xl font-bold text-gray-900">Featured Collection</h2>
            <Link to="/catalog" className="text-accent-600 hover:text-accent-700 font-medium flex items-center gap-1">
              View all
              <ArrowRight size={20} />
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {recentShoes.map((shoe) => (
              <ProductCard
                key={shoe.id}
                id={shoe.id}
                name={shoe.name}
                brand={shoe.brand}
                price={shoe.price}
                thumbnailUrl={shoe.thumbnailUrl}
                onTryOn={handleTryOn}
              />
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-primary-600">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-6">
            Ready to Find Your Perfect Shoes?
          </h2>
          <p className="text-xl text-primary-100 mb-8">
            Join thousands of users already using AR to shop smarter
          </p>
          <Button
            size="lg"
            onClick={() => navigate('/try-on')}
            className="bg-white text-primary-600 hover:bg-gray-100 px-8 py-6 text-lg font-semibold shadow-lg"
          >
            Get Started Now
            <ArrowRight className="ml-2" size={24} />
          </Button>
        </div>
      </section>
    </div>
  );
}
