import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ArrowRight, Footprints, Sparkles, Palette, ShoppingBag } from "lucide-react";
import { Link } from "react-router-dom";
import heroShoe from "@/assets/hero-shoe.jpg";
import { useState, useEffect } from "react";
import api from "@/config/api";

interface Product {
  _id: string;
  name: string;
  brand: string;
  thumbnailUrl?: string;
  price: number;
}

const Index = () => {
  const [featuredShoes, setFeaturedShoes] = useState<Product[]>([]);

  useEffect(() => {
    const fetchFeatured = async () => {
      try {
        const res = await api.get<Product[]>('/catalog');
        setFeaturedShoes(res.data.slice(0, 4));
      } catch (err) {
        console.error("Failed to fetch featured shoes:", err);
      }
    };
    fetchFeatured();
  }, []);

  const features = [
    {
      icon: Footprints,
      title: "AR Try-On",
      description: "See shoes on your feet in real-time using advanced AR technology",
      link: "/try-on",
    },
    {
      icon: Sparkles,
      title: "Outfit Matching",
      description: "Discover perfect combinations with your wardrobe",
      link: "/outfit-match",
    },
    {
      icon: Palette,
      title: "Customize Shoes",
      description: "Create unique designs with custom colors and textures",
      link: "/customize",
    },
    {
      icon: ShoppingBag,
      title: "My Closet",
      description: "Save and manage your favorite try-on history",
      link: "/closet",
    },
  ];

  return (
    <div className="min-h-screen bg-background">
      
      {/* Hero Section */}
      <section className="relative pt-24 pb-16 overflow-hidden">
        <div className="absolute inset-0 bg-[var(--gradient-hero)] opacity-95"></div>
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHZpZXdCb3g9IjAgMCA2MCA2MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZyBmaWxsPSJub25lIiBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxwYXRoIGQ9Ik0zNiAxOGMzLjMxNCAwIDYgMi42ODYgNiA2cy0yLjY4NiA2LTYgNi02LTIuNjg2LTYtNiAyLjY4Ni02IDYtNnoiIHN0cm9rZT0iI0ZGNkIzNSIgc3Ryb2tlLW9wYWNpdHk9Ii4xIi8+PC9nPjwvc3ZnPg==')] opacity-20"></div>
        
        <div className="container mx-auto px-4 relative z-10">
          <div className="grid lg:grid-cols-2 gap-12 items-center max-w-7xl mx-auto">
            <div className="text-center lg:text-left">
              <h1 className="text-5xl md:text-6xl font-bold text-primary-foreground mb-6 animate-fade-in">
                Step Into the Future of
                <span className="block text-secondary mt-2">Shoe Shopping</span>
              </h1>
              <p className="text-xl text-primary-foreground/90 mb-8 max-w-2xl">
                Try on shoes virtually with AR, match them with your outfits, and customize your perfect style—all from home
              </p>
              <div className="flex flex-col sm:flex-row gap-4 justify-center lg:justify-start">
                <Link to="/try-on">
                  <Button size="lg" className="bg-secondary hover:bg-secondary/90 text-secondary-foreground shadow-[var(--shadow-glow)]">
                    Start AR Try-On
                    <ArrowRight className="ml-2 h-5 w-5" />
                  </Button>
                </Link>
                <Link to="/closet">
                  <Button size="lg" variant="outline" className="border-primary-foreground/20 text-primary-foreground hover:bg-primary-foreground/10">
                    View My Closet
                  </Button>
                </Link>
              </div>
            </div>
            <div className="relative">
              <div className="absolute inset-0 bg-secondary/20 blur-3xl rounded-full"></div>
              <img 
                src={heroShoe} 
                alt="Modern athletic shoe with AR try-on technology" 
                className="relative z-10 w-full h-auto animate-fade-in drop-shadow-2xl"
              />
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 container mx-auto px-4">
        <div className="text-center mb-12">
          <h2 className="text-3xl md:text-4xl font-bold text-foreground mb-4">Powerful Features</h2>
          <p className="text-muted-foreground max-w-2xl mx-auto">
            Everything you need to make confident shoe purchases online
          </p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 max-w-7xl mx-auto">
          {features.map((feature, index) => (
            <Link key={index} to={feature.link}>
              <Card className="p-6 h-full bg-card shadow-[var(--shadow-elegant)] hover:shadow-[var(--shadow-glow)] transition-all group cursor-pointer">
                <div className="w-12 h-12 rounded-lg bg-accent/10 flex items-center justify-center mb-4 group-hover:bg-accent/20 transition-colors">
                  <feature.icon className="h-6 w-6 text-accent" />
                </div>
                <h3 className="text-xl font-semibold text-foreground mb-2">{feature.title}</h3>
                <p className="text-muted-foreground">{feature.description}</p>
              </Card>
            </Link>
          ))}
        </div>
      </section>

      {/* Dynamic Catalog Section */}
      <section className="py-20 bg-muted/30">
        <div className="container mx-auto px-4">
          <div className="flex flex-col md:flex-row justify-between items-start md:items-end mb-12 max-w-7xl mx-auto gap-4">
            <div>
              <h2 className="text-3xl font-bold text-foreground mb-2 font-outfit">Shop the Catalog</h2>
              <p className="text-muted-foreground">Premium footwear for every occasion</p>
            </div>
            <Link to="/try-on">
              <Button variant="ghost" className="text-accent hover:text-accent/80 p-0 h-auto">
                View All <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8 max-w-7xl mx-auto">
            {featuredShoes.length > 0 ? (
              featuredShoes.map((shoe) => (
                <Card key={shoe._id} className="group overflow-hidden bg-card border-none shadow-[var(--shadow-elegant)] hover:shadow-[var(--shadow-glow)] transition-all">
                  <div className="aspect-square bg-gradient-to-br from-primary/5 to-accent/5 p-8 relative overflow-hidden">
                    {shoe.thumbnailUrl ? (
                      <img 
                        src={shoe.thumbnailUrl} 
                        alt={shoe.name} 
                        className="w-full h-full object-contain transition-transform group-hover:scale-110 drop-shadow-xl"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center">
                        <ShoppingBag className="h-12 w-12 text-muted-foreground opacity-20" />
                      </div>
                    )}
                    <div className="absolute top-3 left-3">
                      <Badge className="bg-white/80 backdrop-blur-sm text-foreground hover:bg-white">{shoe.brand}</Badge>
                    </div>
                  </div>
                  <div className="p-4 text-center">
                    <h3 className="font-semibold text-foreground text-lg mb-1">{shoe.name}</h3>
                    <p className="text-accent font-bold mb-4">${shoe.price || '99'}</p>
                    <Link to="/try-on">
                      <Button size="sm" className="w-full bg-secondary hover:bg-secondary/90 text-secondary-foreground">
                        Try On Now
                      </Button>
                    </Link>
                  </div>
                </Card>
              ))
            ) : (
              // Loading placeholders
              [1, 2, 3, 4].map(i => (
                <div key={i} className="aspect-[3/4] bg-muted animate-pulse rounded-2xl" />
              ))
            )}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-primary/5">
        <div className="container mx-auto px-4">
          <div className="max-w-3xl mx-auto text-center">
            <h2 className="text-3xl md:text-4xl font-bold text-foreground mb-4">
              Ready to Transform Your Shopping?
            </h2>
            <p className="text-muted-foreground mb-8 text-lg">
              Join thousands of users who are experiencing the future of online shoe shopping
            </p>
            <Link to="/try-on">
              <Button size="lg" className="bg-secondary hover:bg-secondary/90 text-secondary-foreground">
                Get Started Now
                <ArrowRight className="ml-2 h-5 w-5" />
              </Button>
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Index;
