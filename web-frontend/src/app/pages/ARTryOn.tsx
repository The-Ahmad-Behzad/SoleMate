import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { Camera, Info } from 'lucide-react';
import { Card } from '../components/ui/card';
import { toast } from 'sonner';
import { CameraViewport } from '../components/ar/CameraViewport';
import { ShoeSelector } from '../components/ar/ShoeSelector';
import { QuickActions } from '../components/ar/QuickActions';
import { api } from '../../lib/api';
import { snapAR } from '../../lib/SnapARService';

export function ARTryOnPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const preselectedShoeId = searchParams.get('shoe');

  const [isARActive, setIsARActive] = useState(false);
  const [shoes, setShoes] = useState<any[]>([]);
  const [selectedShoeId, setSelectedShoeId] = useState<string | undefined>(
    preselectedShoeId || undefined
  );
  const [isLoadingShoes, setIsLoadingShoes] = useState(false);

  const currentShoe = shoes.find((shoe) => shoe.id === selectedShoeId || shoe._id === selectedShoeId);

  useEffect(() => {
    fetchShoes();
  }, []);

  const fetchShoes = async () => {
    setIsLoadingShoes(true);
    try {
      const data = await api.getCatalog();
      setShoes(data);
      if (!selectedShoeId && data.length > 0) {
        setSelectedShoeId(data[0].id || data[0]._id);
      }
    } catch (error) {
      toast.error('Failed to load shoe catalog');
      console.error(error);
    } finally {
      setIsLoadingShoes(false);
    }
  };

  const handleEnableAR = () => {
    if (!selectedShoeId) {
      toast.error('Please select a shoe first');
      return;
    }

    setIsARActive(!isARActive);

    if (!isARActive) {
      toast.success('AR Session Started');
    } else {
      toast.info('AR Session Ended');
    }
  };

  const handleReset = () => {
    toast.info('AR view reset');
    // Re-apply current lens
    if (selectedShoeId) {
      snapAR.applyLens();
    }
  };

  const handleSaveSnapshot = async () => {
    if (!isARActive) {
      toast.error('Enable AR first to save snapshot');
      return;
    }

    try {
      const snapshotBlob = await snapAR.takeSnapshot();
      const formData = new FormData();
      formData.append('shoeId', selectedShoeId!);
      formData.append('snapshot', snapshotBlob, `snapshot_${Date.now()}.png`);
      
      await api.saveTryOn(formData);
      toast.success('Snapshot saved to your closet!');
    } catch (error) {
      toast.error('Failed to save snapshot');
      console.error(error);
    }
  };

  const handleSelectShoe = (id: string) => {
    setSelectedShoeId(id);
    if (isARActive) {
      toast.info('Shoe changed in AR view');
      // In a real multi-lens scenario, we would call snapAR.applyLens(lensIdMap[id])
      // For now, we use the default lens
      snapAR.applyLens();
    }
  };

  const handleRefreshShoes = () => {
    fetchShoes();
    toast.success('Catalog refreshed');
  };

  const handleAddToCloset = async () => {
    if (!selectedShoeId) {
      toast.error('No shoe selected');
      return;
    }
    
    try {
      const formData = new FormData();
      formData.append('shoeId', selectedShoeId);
      await api.saveTryOn(formData);
      toast.success('Added to your closet!');
    } catch (error) {
      toast.error('Failed to add to closet');
    }
  };

  const handleView3D = () => {
    if (!selectedShoeId) {
      toast.error('No shoe selected');
      return;
    }
    toast.info('3D viewer coming soon');
  };

  const handleMatchOutfit = () => {
    if (!selectedShoeId) {
      toast.error('No shoe selected');
      return;
    }
    navigate(`/ai-match?shoe=${selectedShoeId}`);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-12 h-12 bg-accent-100 rounded-xl flex items-center justify-center">
              <Camera size={24} className="text-accent-600" />
            </div>
            <div>
              <h1 className="text-3xl font-bold text-gray-900">AR Try-On</h1>
              <p className="text-gray-600">
                See how shoes look on your feet in real-time
              </p>
            </div>
          </div>
        </div>

        {/* Main AR Interface */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
          {/* Camera Viewport - Left Column (2/3) */}
          <div className="lg:col-span-2">
            <CameraViewport
              isARActive={isARActive}
              onEnableAR={handleEnableAR}
              onReset={handleReset}
              onSaveSnapshot={handleSaveSnapshot}
              currentShoe={currentShoe ? {
                id: currentShoe.id || currentShoe._id,
                name: currentShoe.name,
                brand: currentShoe.brand
              } : undefined}
            />
          </div>

          {/* Controls - Right Column (1/3) */}
          <div className="space-y-6">
            {/* Shoe Selector */}
            <ShoeSelector
              shoes={shoes.map(s => ({
                id: s.id || s._id,
                name: s.name,
                brand: s.brand,
                thumbnailUrl: s.thumbnailUrl,
                price: s.price
              }))}
              selectedShoeId={selectedShoeId}
              onSelectShoe={handleSelectShoe}
              onRefresh={handleRefreshShoes}
              isLoading={isLoadingShoes}
            />

            {/* Quick Actions */}
            <QuickActions
              onAddToCloset={handleAddToCloset}
              onView3D={handleView3D}
              onMatchOutfit={handleMatchOutfit}
              disabled={!selectedShoeId}
            />
          </div>
        </div>

        {/* Info Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Card className="p-6 bg-accent-50 border-accent-200">
            <div className="flex items-start gap-3">
              <Info size={20} className="text-accent-600 mt-1 flex-shrink-0" />
              <div>
                <h3 className="font-semibold text-gray-900 mb-2">
                  How to Use AR Try-On
                </h3>
                <ol className="space-y-1 text-sm text-gray-700">
                  <li>1. Select a shoe from the grid on the right</li>
                  <li>2. Tap "Enable AR" to start the camera</li>
                  <li>3. Point your camera at your feet</li>
                  <li>4. See the shoe in real-time on your feet</li>
                  <li>5. Save snapshots to your closet</li>
                </ol>
              </div>
            </div>
          </Card>

          <Card className="p-6 bg-info-50 border-info-200">
            <div className="flex items-start gap-3">
              <Camera size={20} className="text-info-600 mt-1 flex-shrink-0" />
              <div>
                <h3 className="font-semibold text-gray-900 mb-2">
                  Camera Permissions
                </h3>
                <p className="text-sm text-gray-700 mb-3">
                  This feature requires camera access to show shoes on your feet.
                  Your privacy is important - camera feed is processed locally
                  and not stored.
                </p>
                <p className="text-xs text-gray-600">
                  Note: Integrated with Snap AR SDK for web. 
                  Ensure you have granted camera permissions in your browser.
                </p>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
