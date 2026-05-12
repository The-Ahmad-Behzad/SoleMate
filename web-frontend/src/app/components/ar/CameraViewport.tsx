import { useState, useRef, useEffect } from 'react';
import { Camera, RefreshCw, Save } from 'lucide-react';
import { Button } from '../ui/button';
import { Badge } from '../ui/badge';
import { snapAR } from '../../../lib/SnapARService';
import { toast } from 'sonner';

interface CameraViewportProps {
  isARActive: boolean;
  onEnableAR: () => void;
  onReset: () => void;
  onSaveSnapshot: () => void;
  currentShoe?: { name: string; brand: string; id: string };
}

export function CameraViewport({
  isARActive,
  onEnableAR,
  onReset,
  onSaveSnapshot,
  currentShoe,
}: CameraViewportProps) {
  const [isLoading, setIsLoading] = useState(false);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    let isMounted = true;

    if (isARActive && canvasRef.current) {
      const initAR = async () => {
        try {
          console.log('CameraViewport: Starting AR initialization');
          await snapAR.initialize(canvasRef.current!);
          if (!isMounted) return;
          
          await snapAR.startCamera();
          if (!isMounted) return;
          
          await snapAR.applyLens(); // Uses default lens ID provided in service
          console.log('CameraViewport: AR successfully started');
        } catch (error) {
          if (isMounted) {
            toast.error('Failed to start AR camera. Please check permissions.');
            console.error('CameraViewport Error:', error);
          }
        }
      };
      initAR();
    } else {
      snapAR.stop();
    }

    return () => {
      isMounted = false;
      snapAR.stop();
    };
  }, [isARActive]);

  const handleToggleAR = async () => {
    if (!isARActive) {
      setIsLoading(true);
      // Brief delay to show loading state
      setTimeout(() => {
        setIsLoading(false);
        onEnableAR();
      }, 800);
    } else {
      onEnableAR();
    }
  };

  return (
    <div className="relative rounded-xl overflow-hidden shadow-xl">
      {/* Camera View */}
      <div className="aspect-[3/4] bg-gray-900 relative">
        {/* AR Status Badge */}
        <div className="absolute top-4 left-4 z-10">
          <Badge
            variant={isARActive ? 'default' : 'secondary'}
            className={`${
              isARActive
                ? 'bg-success-500 text-white'
                : 'bg-white/90 backdrop-blur-sm text-gray-900'
            } px-4 py-2 text-sm font-semibold shadow-lg`}
          >
            <div className={`w-2 h-2 rounded-full mr-2 ${isARActive ? 'bg-white animate-pulse' : 'bg-gray-400'}`} />
            {isARActive ? 'AR Active' : 'AR Ready'}
          </Badge>
        </div>

        {/* Current Shoe Info */}
        {currentShoe && (
          <div className="absolute top-4 right-4 z-10 bg-white/90 backdrop-blur-sm rounded-lg px-4 py-2 shadow-lg">
            <p className="text-xs text-gray-600">{currentShoe.brand}</p>
            <p className="text-sm font-semibold text-gray-900">{currentShoe.name}</p>
          </div>
        )}

        {/* Camera Preview / AR View */}
        <div className="absolute inset-0 flex items-center justify-center">
          {!isARActive ? (
            <div className="text-center text-white z-0">
              <Camera size={64} className="mx-auto mb-4 opacity-50" />
              <p className="text-lg font-medium mb-2">Camera Preview</p>
              <p className="text-sm text-gray-400">
                Enable AR to see shoes on your feet
              </p>
            </div>
          ) : (
            <canvas 
              ref={canvasRef} 
              className="w-full h-full object-cover"
            />
          )}
        </div>

        {/* Control Overlay */}
        <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/80 to-transparent p-6 z-10">
          <div className="flex items-center justify-center gap-3">
            <Button
              variant="ghost"
              size="icon"
              onClick={onReset}
              className="bg-white/20 backdrop-blur-sm hover:bg-white/30 text-white rounded-full w-12 h-12"
              disabled={!isARActive}
            >
              <RefreshCw size={20} />
            </Button>

            <Button
              onClick={handleToggleAR}
              disabled={isLoading}
              className={`${
                isARActive
                  ? 'bg-error-500 hover:bg-error-600'
                  : 'bg-accent-500 hover:bg-accent-600'
              } text-white px-8 py-6 rounded-full shadow-lg font-semibold min-w-[160px]`}
            >
              {isLoading ? (
                <>
                  <RefreshCw size={20} className="mr-2 animate-spin" />
                  Loading...
                </>
              ) : isARActive ? (
                <>
                  <Camera size={20} className="mr-2" />
                  Disable AR
                </>
              ) : (
                <>
                  <Camera size={20} className="mr-2" />
                  Enable AR
                </>
              )}
            </Button>

            <Button
              variant="ghost"
              size="icon"
              onClick={onSaveSnapshot}
              className="bg-white/20 backdrop-blur-sm hover:bg-white/30 text-white rounded-full w-12 h-12"
              disabled={!isARActive}
            >
              <Save size={20} />
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
