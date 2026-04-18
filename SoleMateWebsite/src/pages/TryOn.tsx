import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Camera, RefreshCw, Download, ExternalLink } from "lucide-react";
import { useState, useEffect } from "react";
import api from "@/config/api";
import { toast } from "sonner";
import { Link } from "react-router-dom";

interface Product {
  _id: string;
  name: string;
  brand: string;
  thumbnailUrl?: string;
  price: number;
}

const TryOn = () => {
  const [products, setProducts] = useState<Product[]>([]);
  const [selectedShoe, setSelectedShoe] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchCatalog = async () => {
      try {
        const res = await api.get<Product[]>('/catalog');
        setProducts(res.data);
        if (res.data.length > 0) setSelectedShoe(res.data[0]);
      } catch (err) {
        console.error("Failed to fetch catalog:", err);
        toast.error("Could not load shoe catalog");
      } finally {
        setLoading(false);
      }
    };
    fetchCatalog();
  }, []);

  const openARSession = () => {
    // Open Snap WebAR Experience in a new tab
    window.open("https://lens.snap.com/experience/4c2ccb84-4e89-4ad8-90b3-a7bfb5d19f01", "_blank");
  };

  return (
    <div className="min-h-screen bg-background">
      <main className="container mx-auto px-4 pt-24 pb-12">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-8">
            <h1 className="text-4xl font-bold text-foreground mb-3">AR Virtual Try-On</h1>
            <p className="text-muted-foreground">See how shoes look on your feet in real-time</p>
          </div>

          <div className="grid lg:grid-cols-2 gap-8">
            {/* AR Camera View */}
            <Card className="p-8 bg-card shadow-[var(--shadow-elegant)]">
              <div className="aspect-[4/5] bg-muted rounded-lg flex items-center justify-center mb-6 relative overflow-hidden">
                <div className="absolute inset-0 bg-gradient-to-br from-primary/5 to-accent/5"></div>
                
                {selectedShoe && selectedShoe.thumbnailUrl ? (
                  <div className="absolute inset-0 flex items-center justify-center p-8">
                    <img 
                      src={selectedShoe.thumbnailUrl} 
                      alt={selectedShoe.name}
                      className="max-w-full max-h-full object-contain drop-shadow-2xl animate-fade-in"
                    />
                  </div>
                ) : null}

                <div className="relative z-10 text-center bg-background/40 backdrop-blur-md p-6 rounded-2xl border border-white/20">
                  <Camera className="h-12 w-12 mx-auto mb-3 text-foreground" />
                  <p className="text-foreground font-medium mb-4">Ready to try on {selectedShoe?.name || 'shoes'}?</p>
                  <Button 
                    onClick={openARSession}
                    className="bg-secondary hover:bg-secondary/90 text-secondary-foreground px-8 py-6 text-lg font-semibold h-auto"
                  >
                    Launch WebAR
                    <ExternalLink className="ml-2 h-5 w-5" />
                  </Button>
                </div>
              </div>

              <div className="flex gap-3">
                <Button variant="outline" className="flex-1">
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Reset
                </Button>
                <Button variant="outline" className="flex-1">
                  <Download className="h-4 w-4 mr-2" />
                  Save
                </Button>
              </div>
            </Card>

            {/* Shoe Selection */}
            <div className="space-y-6">
              <Card className="p-6 bg-card shadow-[var(--shadow-elegant)] h-fit">
                <h3 className="font-semibold text-lg mb-4 text-foreground">Select a Shoe</h3>
                {loading ? (
                  <div className="grid grid-cols-2 gap-4">
                    {[1, 2, 3, 4].map(i => (
                      <div key={i} className="aspect-square bg-muted animate-pulse rounded-lg" />
                    ))}
                  </div>
                ) : (
                  <div className="grid grid-cols-2 gap-4">
                    {products.map((shoe) => (
                      <button
                        key={shoe._id}
                        onClick={() => setSelectedShoe(shoe)}
                        className={`aspect-square rounded-lg transition-all p-3 flex flex-col items-center justify-center border-2 ${
                          selectedShoe?._id === shoe._id 
                            ? "border-secondary bg-secondary/5 scale-[1.02]" 
                            : "border-transparent bg-muted hover:border-secondary/50"
                        }`}
                      >
                        {shoe.thumbnailUrl ? (
                          <img src={shoe.thumbnailUrl} alt={shoe.name} className="w-full h-2/3 object-contain mb-2" />
                        ) : (
                          <div className="w-full h-2/3 bg-gradient-to-br from-primary/10 to-accent/10 rounded mb-2"></div>
                        )}
                        <span className="text-xs font-medium text-foreground text-center truncate w-full px-1">{shoe.name}</span>
                      </button>
                    ))}
                  </div>
                )}
              </Card>

              <Card className="p-6 bg-card shadow-[var(--shadow-elegant)]">
                <h3 className="font-semibold text-lg mb-4 text-foreground">Quick Actions</h3>
                <div className="space-y-3">
                  <Button className="w-full bg-accent hover:bg-accent/90 text-accent-foreground">
                    Add to Closet
                  </Button>
                  <Button variant="outline" className="w-full">
                    View in 3D
                  </Button>
                  <Link to="/outfit-match" className="block w-full">
                    <Button variant="outline" className="w-full">
                      Match with Outfit
                    </Button>
                  </Link>
                </div>
              </Card>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
};

export default TryOn;
