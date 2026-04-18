import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Trash2, Star, Clock, ShoppingBag } from "lucide-react";
import { useState, useEffect } from "react";
import api from "@/config/api";
import { useAuth } from "@/contexts/AuthContext";
import { toast } from "sonner";
import { Link } from "react-router-dom";

interface TryOnRecord {
  _id: string;
  productId: {
    _id: string;
    name: string;
    thumbnailUrl?: string;
  };
  snapshotUrl?: string;
  createdAt: string;
}

const Closet = () => {
  const { user } = useAuth();
  const [history, setHistory] = useState<TryOnRecord[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchHistory = async () => {
    if (!user) return;
    try {
      const res = await api.get<TryOnRecord[]>('/tryons/history');
      setHistory(res.data);
    } catch (err) {
      console.error("Failed to fetch history:", err);
      toast.error("Could not load your closet");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHistory();
  }, [user]);

  const handleDelete = async (id: string) => {
    try {
      await api.delete(`/tryons/${id}`);
      setHistory(prev => prev.filter(item => item._id !== id));
      toast.success("Removed from closet");
    } catch (err) {
      toast.error("Failed to delete record");
    }
  };

  return (
    <div className="min-h-screen bg-background">
      
      <main className="container mx-auto px-4 pt-24 pb-12">
        <div className="max-w-7xl mx-auto">
          <div className="mb-8">
            <h1 className="text-4xl font-bold text-foreground mb-3">My Closet</h1>
            <p className="text-muted-foreground">Your try-on history and favorite shoes</p>
          </div>

          <div className="mb-6 flex gap-3">
            <Badge className="bg-accent text-accent-foreground">All ({history.length})</Badge>
            <Badge variant="outline">Favorites</Badge>
            <Badge variant="outline">Recent</Badge>
          </div>

          {loading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              {[1, 2, 3, 4].map(i => (
                <div key={i} className="h-64 bg-muted animate-pulse rounded-xl" />
              ))}
            </div>
          ) : history.length === 0 ? (
            <div className="text-center py-20 bg-muted/20 rounded-2xl border-2 border-dashed border-border">
              <ShoppingBag className="h-12 w-12 mx-auto mb-4 text-muted-foreground opacity-50" />
              <h3 className="text-xl font-semibold mb-2">Your closet is empty</h3>
              <p className="text-muted-foreground mb-6">Try on some shoes to see them saved here!</p>
              <Link to="/try-on">
                <Button className="bg-secondary hover:bg-secondary/90">Go to AR Try-On</Button>
              </Link>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              {history.map((record) => (
                <Card key={record._id} className="group overflow-hidden bg-card shadow-[var(--shadow-elegant)] hover:shadow-[var(--shadow-glow)] transition-all border-none">
                  <div className="aspect-square bg-gradient-to-br from-primary/10 to-accent/10 relative overflow-hidden">
                    {(record.snapshotUrl || record.productId?.thumbnailUrl) && (
                      <img 
                        src={record.snapshotUrl || record.productId.thumbnailUrl} 
                        alt={record.productId?.name} 
                        className="w-full h-full object-cover transition-transform group-hover:scale-110"
                      />
                    )}
                    <div className="absolute inset-0 bg-primary/0 group-hover:bg-primary/5 transition-colors"></div>
                  </div>
                  <div className="p-4">
                    <h3 className="font-semibold text-foreground mb-1 truncate">{record.productId?.name || 'Unknown Shoe'}</h3>
                    <div className="flex items-center text-xs text-muted-foreground mb-4">
                      <Clock className="h-3 w-3 mr-1" />
                      {new Date(record.createdAt).toLocaleDateString()}
                    </div>
                    <div className="flex gap-2">
                      <Link to="/try-on" className="flex-1">
                        <button className="w-full text-sm py-2 rounded bg-accent hover:bg-accent/90 text-accent-foreground transition-colors font-medium">
                          Try Again
                        </button>
                      </Link>
                      <button 
                        onClick={() => handleDelete(record._id)}
                        className="px-3 py-2 rounded border border-border hover:bg-destructive hover:text-destructive-foreground hover:border-destructive transition-colors text-muted-foreground"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                </Card>
              ))}
            </div>
          )}
        </div>
      </main>
    </div>
  );
};

export default Closet;
