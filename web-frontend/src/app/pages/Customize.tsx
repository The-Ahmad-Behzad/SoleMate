import { useState } from 'react';
import { Upload, Palette } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';

export function CustomizePage() {
  const [primaryColor, setPrimaryColor] = useState('#000033');
  const [secondaryColor, setSecondaryColor] = useState('#FF00FF');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    // TODO: Implement submission to API
    setTimeout(() => setIsSubmitting(false), 2000);
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8 text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-success-100 rounded-2xl mb-4">
            <Palette size={32} className="text-success-600" />
          </div>
          <h1 className="text-3xl md:text-4xl font-bold text-gray-900 mb-2">
            Customize Your Shoes
          </h1>
          <p className="text-gray-600 max-w-2xl mx-auto">
            Design custom shoe skins and request unique colorways
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Base Shoe Selection */}
          <Card className="p-6">
            <Label className="text-lg font-semibold mb-4 block">
              Select Base Shoe
            </Label>
            <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-accent-500 transition-colors cursor-pointer">
              <p className="text-gray-600">Click to select from catalog</p>
            </div>
          </Card>

          {/* Reference Images */}
          <Card className="p-6">
            <Label className="text-lg font-semibold mb-4 block">
              Upload Reference Images
            </Label>
            <div className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center hover:border-accent-500 transition-colors cursor-pointer">
              <Upload size={40} className="mx-auto text-gray-400 mb-3" />
              <p className="text-gray-600 mb-1">
                Click to upload or drag and drop
              </p>
              <p className="text-sm text-gray-500">
                PNG, JPG up to 10MB (max 5 images)
              </p>
            </div>
          </Card>

          {/* Color Pickers */}
          <Card className="p-6">
            <Label className="text-lg font-semibold mb-4 block">
              Choose Colors
            </Label>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <Label htmlFor="primary-color">Primary Color</Label>
                <div className="flex gap-3">
                  <Input
                    id="primary-color"
                    type="color"
                    value={primaryColor}
                    onChange={(e) => setPrimaryColor(e.target.value)}
                    className="w-20 h-12 cursor-pointer"
                  />
                  <Input
                    type="text"
                    value={primaryColor}
                    onChange={(e) => setPrimaryColor(e.target.value)}
                    className="flex-1"
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="secondary-color">Secondary Color</Label>
                <div className="flex gap-3">
                  <Input
                    id="secondary-color"
                    type="color"
                    value={secondaryColor}
                    onChange={(e) => setSecondaryColor(e.target.value)}
                    className="w-20 h-12 cursor-pointer"
                  />
                  <Input
                    type="text"
                    value={secondaryColor}
                    onChange={(e) => setSecondaryColor(e.target.value)}
                    className="flex-1"
                  />
                </div>
              </div>
            </div>
          </Card>

          {/* Description */}
          <Card className="p-6">
            <Label htmlFor="description" className="text-lg font-semibold mb-4 block">
              Design Instructions
            </Label>
            <Textarea
              id="description"
              placeholder="Describe your design idea in detail. For example: 'Please add a galaxy pattern to the sole with purple and blue stars...'"
              rows={6}
              className="resize-none"
            />
          </Card>

          {/* Submit Button */}
          <div className="flex justify-center">
            <Button
              type="submit"
              size="lg"
              disabled={isSubmitting}
              className="bg-success-500 hover:bg-success-600 px-8"
            >
              {isSubmitting ? 'Sending Request...' : 'Send Design Request'}
            </Button>
          </div>
        </form>

        {/* Info Card */}
        <Card className="mt-8 p-6 bg-accent-50 border-accent-200">
          <h3 className="font-semibold text-gray-900 mb-2">
            How it works
          </h3>
          <ul className="space-y-2 text-sm text-gray-700">
            <li>1. Select a base shoe from our catalog</li>
            <li>2. Upload reference images or sketches</li>
            <li>3. Choose your preferred colors</li>
            <li>4. Describe your vision in detail</li>
            <li>5. Our designers will review and create your custom shoe</li>
          </ul>
        </Card>
      </div>
    </div>
  );
}
