import { useState } from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Card } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Heart, ShoppingBag } from 'lucide-react';
import { mockTryOnHistory } from '../../lib/mock-data';

export function ClosetPage() {
  const [activeTab, setActiveTab] = useState('shoes');

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl md:text-4xl font-bold text-gray-900 mb-2">
            My Closet
          </h1>
          <p className="text-gray-600">Your saved shoes and try-on sessions</p>
        </div>

        {/* Tabs */}
        <Tabs value={activeTab} onValueChange={setActiveTab}>
          <TabsList className="mb-8">
            <TabsTrigger value="shoes" className="flex items-center gap-2">
              <ShoppingBag size={18} />
              Saved Shoes
            </TabsTrigger>
            <TabsTrigger value="tryons" className="flex items-center gap-2">
              <Heart size={18} />
              Try-On History
            </TabsTrigger>
          </TabsList>

          <TabsContent value="shoes">
            <div className="text-center py-16">
              <Heart size={48} className="mx-auto text-gray-300 mb-4" />
              <h3 className="text-xl font-semibold text-gray-900 mb-2">
                No saved shoes yet
              </h3>
              <p className="text-gray-600 mb-6">
                Start adding your favorite shoes to your closet
              </p>
              <Button onClick={() => window.location.href = '/catalog'}>
                Explore Catalog
              </Button>
            </div>
          </TabsContent>

          <TabsContent value="tryons">
            {mockTryOnHistory.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {mockTryOnHistory.map((session) => (
                  <Card key={session.id} className="overflow-hidden">
                    <div className="aspect-square overflow-hidden bg-gray-100">
                      <img
                        src={session.snapshotUrl}
                        alt="Try-on snapshot"
                        className="w-full h-full object-cover"
                      />
                    </div>
                    <div className="p-4">
                      <h3 className="font-semibold text-gray-900 mb-1">
                        {session.shoeId.name}
                      </h3>
                      <p className="text-sm text-gray-500 mb-3">
                        {new Date(session.createdAt).toLocaleDateString()}
                      </p>
                      <Button variant="outline" size="sm" className="w-full">
                        Try Again
                      </Button>
                    </div>
                  </Card>
                ))}
              </div>
            ) : (
              <div className="text-center py-16">
                <ShoppingBag size={48} className="mx-auto text-gray-300 mb-4" />
                <h3 className="text-xl font-semibold text-gray-900 mb-2">
                  No try-on sessions yet
                </h3>
                <p className="text-gray-600 mb-6">
                  Start trying on shoes with AR to build your history
                </p>
                <Button onClick={() => window.location.href = '/try-on'}>
                  Start AR Try-On
                </Button>
              </div>
            )}
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
