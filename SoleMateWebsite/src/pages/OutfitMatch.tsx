import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Sparkles, ShoppingBag, Upload, Trash2, Camera } from "lucide-react";
import { useState, useEffect, useRef } from "react";
import api from "@/config/api";
import { toast } from "sonner";
import { useAuth } from "@/contexts/AuthContext";

interface Product {
  _id: string;
  name: string;
  brand: string;
  price: number;
  thumbnailUrl?: string;
  modelUrl?: string;
  category?: string;
}

const OutfitMatch = () => {
  const { user, loginWithGoogle } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  
  const [outfitImage, setOutfitImage] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [recommendations, setRecommendations] = useState<Product[]>([]);
  const [aiDescription, setAiDescription] = useState<string>("");

  useEffect(() => {
    const fetchCatalog = async () => {
      try {
        const res = await api.get<Product[]>('/catalog');
        setProducts(res.data);
      } catch (err) {
        console.error("Failed to load catalog:", err);
      } finally {
        setLoading(false);
      }
    };
    fetchCatalog();
  }, []);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files ? e.target.files[0] : null;
    if (file) {
      setOutfitImage(file);
      setPreviewUrl(URL.createObjectURL(file));
      setRecommendations([]); 
      setAiDescription("");
    }
  };

  const handleAnalyze = async () => {
    if (!user) {
      toast.error("Please login to analyze your outfit");
      return;
    }

    if (!outfitImage) {
      toast.error("Please upload an outfit image first");
      return;
    }

    setIsAnalyzing(true);
    try {
      // 1. Get recommendations based on simulated colors (matching mobile app logic)
      const simulatedColors = ['white', 'grey', 'black'];
      const recRes = await api.post('/outfit/recommend', { colors: simulatedColors });
      
      // Keep exactly 2 as requested
      const topMatches = recRes.data.slice(0, 2);
      setRecommendations(topMatches);

      // 2. Upload and save the analysis
      const formData = new FormData();
      formData.append('outfitImage', outfitImage);
      formData.append('dominantColors', JSON.stringify(simulatedColors));
      formData.append('category', 'AI Stylist Selection');
      formData.append('description', 'This AI-generated look focuses on neutral tones to highlight your footwear choice.');
      formData.append('recommendedShoeIds', JSON.stringify(topMatches.map((m: any) => m._id)));

      const analyzeRes = await api.post('/outfit/analyze', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });

      setAiDescription(analyzeRes.data.description || "This combination offers a sophisticated balance of tones.");
      toast.success("Analysis complete!");
    } catch (err: any) {
      console.error("Analysis failed:", err);
      toast.error(err.message || "Failed to analyze outfit");
    } finally {
      setIsAnalyzing(false);
    }
  };

  const clearSelection = () => {
    setOutfitImage(null);
    setPreviewUrl(null);
    setRecommendations([]);
    setAiDescription("");
  };

  return (
    <div className="min-h-screen bg-background pt-24 pb-12">
      <div className="container mx-auto px-4">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-12">
            <h1 className="text-4xl md:text-5xl font-black text-foreground mb-4 tracking-tight">OUTFIT MATCHING</h1>
            <p className="text-lg text-muted-foreground max-w-2xl mx-auto">Analyze your outfit and get the top 2 matching shoe suggestions from our AI stylist.</p>
          </div>

          <div className="grid lg:grid-cols-2 gap-12">
            {/* Left Column: Upload */}
            <Card className="p-8 bg-card shadow-[var(--shadow-elegant)] border-none relative overflow-hidden group">
              <div className="absolute inset-0 bg-gradient-to-br from-primary/5 to-accent/5 opacity-50"></div>
              <div className="relative z-10">
                <div className="flex items-center gap-3 mb-8">
                   <div className="p-2 bg-accent/20 rounded-xl">
                      <Camera className="h-6 w-6 text-accent" />
                   </div>
                   <h3 className="font-bold text-2xl">Upload Your Outfit</h3>
                </div>

                <div 
                  onClick={() => !outfitImage && fileInputRef.current?.click()}
                  className={`aspect-square rounded-3xl border-2 border-dashed transition-all flex flex-col items-center justify-center relative overflow-hidden cursor-pointer ${
                    previewUrl ? 'border-transparent ring-4 ring-primary/20' : 'border-border hover:border-accent bg-background/50 hover:bg-accent/5'
                  }`}
                >
                  {previewUrl ? (
                    <>
                      <img src={previewUrl} className="w-full h-full object-cover" alt="Outfit Preview" />
                      <div className="absolute inset-0 bg-black/40 opacity-0 hover:opacity-100 transition-opacity flex items-center justify-center">
                         <Button variant="destructive" onClick={(e) => { e.stopPropagation(); clearSelection(); }} className="rounded-full">
                            <Trash2 className="h-5 w-5 mr-2" /> Remove
                         </Button>
                      </div>
                    </>
                  ) : (
                    <div className="text-center p-8">
                      <Upload className="h-12 w-12 text-accent mx-auto mb-4" />
                      <p className="text-lg font-bold mb-1">Click to upload photo</p>
                      <p className="text-sm text-muted-foreground">JPG, PNG or WEBP</p>
                    </div>
                  )}
                  <input type="file" ref={fileInputRef} className="hidden" accept="image/*" onChange={handleFileSelect} />
                </div>

                <div className="mt-8">
                  {!user ? (
                     <Card className="p-6 bg-secondary/5 border border-secondary/20 rounded-2xl flex flex-col items-center gap-4">
                        <p className="font-bold text-secondary-foreground text-center">Please login to analyze your outfit</p>
                        <Button onClick={loginWithGoogle} className="w-full bg-secondary text-secondary-foreground h-12 rounded-xl">
                          Sign in with Google
                        </Button>
                     </Card>
                  ) : (
                    <Button 
                      disabled={!outfitImage || isAnalyzing} 
                      onClick={handleAnalyze}
                      className="w-full bg-secondary hover:bg-secondary/90 text-secondary-foreground h-14 text-lg font-black rounded-2xl shadow-xl transition-transform active:scale-95"
                    >
                      {isAnalyzing ? "ANALYZING STYLE..." : "ANALYZE OUTFIT"}
                    </Button>
                  )}
                </div>
              </div>
            </Card>

            {/* Right Column: Results */}
            <div className="flex flex-col gap-6">
              <Card className="p-8 bg-card shadow-[var(--shadow-elegant)] border-none h-full flex flex-col">
                <div className="flex items-center gap-3 mb-8">
                   <div className="p-2 bg-accent/20 rounded-xl">
                      <Sparkles className="h-6 w-6 text-accent" />
                   </div>
                   <h3 className="font-bold text-2xl">AI Suggestions</h3>
                </div>

                {recommendations.length > 0 ? (
                  <div className="flex-1 flex flex-col">
                    <div className="grid grid-cols-2 gap-6 mb-8">
                      {recommendations.map((shoe) => (
                        <div key={shoe._id} className="group">
                          <div className="aspect-square bg-muted rounded-3xl mb-4 p-6 relative overflow-hidden flex items-center justify-center">
                            <img src={shoe.thumbnailUrl} className="w-full h-full object-contain group-hover:scale-110 transition-transform duration-500" alt={shoe.name} />
                            <div className="absolute top-3 right-3">
                               <Badge className="bg-accent text-accent-foreground font-black italic">TOP MATCH</Badge>
                            </div>
                          </div>
                          <h4 className="font-bold text-base truncate mb-1">{shoe.name}</h4>
                          <p className="text-[10px] text-muted-foreground tracking-widest uppercase font-black">{shoe.brand}</p>
                        </div>
                      ))}
                    </div>

                    <div className="bg-accent/5 p-6 rounded-3xl border border-accent/10 mt-auto">
                       <p className="text-[10px] font-black tracking-widest text-accent mb-2 uppercase">Stylist's Note</p>
                       <p className="text-foreground leading-relaxed italic opacity-80">
                         "{aiDescription}"
                       </p>
                    </div>
                  </div>
                ) : (
                  <div className="flex-1 flex flex-col items-center justify-center text-center opacity-30 py-20">
                    <ShoppingBag className="h-16 w-16 mb-4" />
                    <p className="text-xl font-medium">Capture or upload an outfit<br />to see AI matches here.</p>
                  </div>
                )}
                
                {recommendations.length > 0 && (
                   <div className="mt-8 pt-8 border-t flex gap-4">
                      <Button variant="outline" className="flex-1 h-12 rounded-xl">Save Results</Button>
                      <Button className="flex-1 h-12 rounded-xl bg-accent text-accent-foreground">Try Best Match</Button>
                   </div>
                )}
              </Card>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OutfitMatch;

