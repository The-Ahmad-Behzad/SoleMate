

export class AIService {
    private static instance: AIService;

    private constructor() { }

    public static getInstance(): AIService {
        if (!AIService.instance) {
            AIService.instance = new AIService();
        }
        return AIService.instance;
    }

    public async getRecommendations(colors: string[]): Promise<any[]> {
        console.log(`[AIService] Requesting AI recommendations for colors: ${colors.join(', ')}`);

        try {
            // Using native fetch to call the Render API.
            // Using AbortController to handle timeouts (Render free tier wakes up slowly).
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 60000); // 60s timeout

            const response = await fetch('https://solemate-outfit-recommendation.onrender.com/recommend', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ colors }),
                signal: controller.signal
            });

            clearTimeout(timeoutId);

            if (!response.ok) {
                console.error(`[AIService] AI API Error: ${response.status} ${response.statusText}`);
                throw new Error(`AI API Error: ${response.status}`);
            }

            const data: any = await response.json();
            console.log('[AIService] AI API Response received successfully.');
            
            // Map the returned objects into our Product format.
            // Assuming the python api returns a list of shoes in `data.recommendations` or `data` directly.
            let recommendationsList: any[] = [];
            if (Array.isArray(data)) {
                recommendationsList = data;
            } else if (data && Array.isArray(data.recommendations)) {
                recommendationsList = data.recommendations;
            } else if (data && Array.isArray(data.result)) {
                recommendationsList = data.result;
            } else if (data && typeof data === 'object') {
                recommendationsList = [data]; // Last resort wrap
            }

            if (!recommendationsList.length) {
                throw new Error('AI Response empty or unparseable array format');
            }

            // Map AI fields to Flutter Product shape
            return recommendationsList.map(shoe => ({
                _id: shoe.id || shoe._id || `ai_mock_${Math.random()}`,
                name: shoe.name || shoe.title || 'AI Recommended Shoe',
                brand: shoe.brand || 'SoleMate',
                price: shoe.price || 129.99,
                category: shoe.category || 'Casual',
                thumbnailUrl: shoe.image_url || shoe.thumbnailUrl || shoe.imagePath || 'assets/images/shoes/nike_journey_run.png',
                modelUrl: shoe.model_url || shoe.modelUrl || 'models/shoes/nike_journey_run_left.glb',
                colors: shoe.colors || colors
            }));

        } catch (error) {
            console.warn('[AIService] Call to deployed AI endpoint failed or timed out. Falling back to local mock.', error);
            
            // Simulating a minor delay
            await new Promise(resolve => setTimeout(resolve, 800));

            return [
                {
                    _id: 'mock_shoe_1',
                    name: 'Air Zoom Runner',
                    brand: 'SoleMate',
                    price: 129.99,
                    category: 'Running', 
                    thumbnailUrl: 'assets/images/shoes/nike_journey_run.png', 
                    modelUrl: 'models/shoes/nike_journey_run_left.glb', 
                    colors: colors.length ? colors : ['black', 'white']
                },
                {
                    _id: 'mock_shoe_2',
                    name: 'Classic Leather',
                    brand: 'SoleMate',
                    category: 'Casual', 
                    price: 89.99,
                    thumbnailUrl: 'assets/images/shoes/puma_winter_shoe.png',
                    modelUrl: 'models/shoes/puma_winter_shoe_left.glb',
                    colors: ['white', 'beige']
                }
            ];
        }
    }

    /**
     * Placeholder for outfit analysis (Color Extraction).
     */
    public async analyzeOutfitImage(imageUrl: string): Promise<string[]> {
        // Mock color extraction
        return ['black', 'blue', 'grey'];
    }
}
