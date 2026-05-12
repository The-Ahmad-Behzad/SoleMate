import { useState } from 'react';
import { Upload, Sparkles } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card } from '../components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Label } from '../components/ui/label';
import { Badge } from '../components/ui/badge';

export function AIMatchPage() {
  const [selectedGender, setSelectedGender] = useState<'unisex' | 'male' | 'female'>('unisex');
  const [isAnalyzing, setIsAnalyzing] = useState(false);

  const handleAnalyze = () => {
    setIsAnalyzing(true);
    // TODO: Implement AI analysis
    setTimeout(() => setIsAnalyzing(false), 2000);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8 text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-accent-100 rounded-2xl mb-4">
            <Sparkles size={32} className="text-accent-600" />
          </div>
          <h1 className="text-3xl md:text-4xl font-bold text-gray-900 mb-2">
            AI Outfit Match
          </h1>
          <p className="text-gray-600 max-w-2xl mx-auto">
            Get personalized style recommendations powered by AI
          </p>
        </div>

        {/* Gender Selector */}
        <div className="mb-8">
          <Label className="block text-center mb-4">Select Gender</Label>
          <div className="flex justify-center gap-3">
            {(['unisex', 'male', 'female'] as const).map((gender) => (
              <Badge
                key={gender}
                variant={selectedGender === gender ? 'default' : 'outline'}
                className="cursor-pointer capitalize px-6 py-2"
                onClick={() => setSelectedGender(gender)}
              >
                {gender}
              </Badge>
            ))}
          </div>
        </div>

        {/* Main Content */}
        <Tabs defaultValue="outfit-to-shoe" className="space-y-6">
          <TabsList className="grid w-full grid-cols-2 max-w-md mx-auto">
            <TabsTrigger value="outfit-to-shoe">Outfit → Shoes</TabsTrigger>
            <TabsTrigger value="shoe-to-outfit">Shoe → Outfit</TabsTrigger>
          </TabsList>

          <TabsContent value="outfit-to-shoe" className="space-y-6">
            <Card className="p-8">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">
                Upload Your Outfit Photo
              </h3>
              <div className="border-2 border-dashed border-gray-300 rounded-lg p-12 text-center hover:border-accent-500 transition-colors cursor-pointer">
                <Upload size={48} className="mx-auto text-gray-400 mb-4" />
                <p className="text-gray-600 mb-2">
                  Click to upload or drag and drop
                </p>
                <p className="text-sm text-gray-500">
                  PNG, JPG up to 10MB
                </p>
              </div>
            </Card>

            <div className="text-center">
              <Button
                size="lg"
                onClick={handleAnalyze}
                disabled={isAnalyzing}
                className="bg-accent-500 hover:bg-accent-600"
              >
                {isAnalyzing ? (
                  <>
                    <Sparkles className="mr-2 animate-spin" size={20} />
                    Analyzing...
                  </>
                ) : (
                  <>
                    <Sparkles className="mr-2" size={20} />
                    Find Matching Shoes
                  </>
                )}
              </Button>
            </div>

            {/* Results Placeholder */}
            <Card className="p-8 bg-gray-100 border-0">
              <p className="text-center text-gray-500">
                Your AI-powered shoe recommendations will appear here
              </p>
            </Card>
          </TabsContent>

          <TabsContent value="shoe-to-outfit" className="space-y-6">
            <Card className="p-8">
              <h3 className="text-lg font-semibold text-gray-900 mb-4">
                Upload Shoe Photo
              </h3>
              <div className="border-2 border-dashed border-gray-300 rounded-lg p-12 text-center hover:border-accent-500 transition-colors cursor-pointer">
                <Upload size={48} className="mx-auto text-gray-400 mb-4" />
                <p className="text-gray-600 mb-2">
                  Click to upload or drag and drop
                </p>
                <p className="text-sm text-gray-500">
                  PNG, JPG up to 10MB
                </p>
              </div>
            </Card>

            <div className="text-center">
              <Button
                size="lg"
                onClick={handleAnalyze}
                disabled={isAnalyzing}
                className="bg-accent-500 hover:bg-accent-600"
              >
                {isAnalyzing ? (
                  <>
                    <Sparkles className="mr-2 animate-spin" size={20} />
                    Generating...
                  </>
                ) : (
                  <>
                    <Sparkles className="mr-2" size={20} />
                    Get Outfit Recommendations
                  </>
                )}
              </Button>
            </div>

            {/* Results Placeholder */}
            <Card className="p-8 bg-gray-100 border-0">
              <p className="text-center text-gray-500">
                Your personalized outfit recommendations will appear here
              </p>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
