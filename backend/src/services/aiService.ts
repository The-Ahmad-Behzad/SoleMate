import { ProductModel } from '../models/Product.js';

const AI_BASE_URL = 'https://cooperative-essence-production-7eb2.up.railway.app';

export class AIService {
    private static instance: AIService;

    private constructor() { }

    public static getInstance(): AIService {
        if (!AIService.instance) {
            AIService.instance = new AIService();
        }
        return AIService.instance;
    }

    /**
     * Gets shoe recommendations based on an outfit image.
     */
    public async getRecommendationsFromImage(imageBuffer: Buffer, filename: string): Promise<any> {
        console.log(`[AIService] Requesting AI shoe recommendations for image: ${filename}`);

        try {
            const formData = new FormData();
            const blob = new Blob([new Uint8Array(imageBuffer)]);
            formData.append('file', blob, filename);


            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 60000);

            const response = await fetch(`${AI_BASE_URL}/recommend`, {
                method: 'POST',
                body: formData,
                signal: controller.signal
            });

            clearTimeout(timeoutId);

            if (!response.ok) {
                console.error(`[AIService] AI API Error: ${response.status} ${response.statusText}`);
                throw new Error(`AI API Error: ${response.status}`);
            }

            const data: any = await response.json();
            console.log('[AIService] AI /recommend Response received successfully.');

            // data.recommendations is usually the list of shoe objects from Python side
            return {
                style: data.detected_style,
                colors: data.detected_colors,
                recommendations: data.recommendations || []
            };

        } catch (error) {
            console.error('[AIService] Call to AI /recommend failed:', error);
            throw error;
        }
    }

    /**
     * Gets an outfit description based on a shoe image.
     */
    public async getOutfitRecommendationForShoe(imageBuffer: Buffer, filename: string): Promise<any> {
        console.log(`[AIService] Requesting AI outfit recommendation for shoe: ${filename}`);

        try {
            const formData = new FormData();
            const blob = new Blob([new Uint8Array(imageBuffer)]);
            formData.append('file', blob, filename);


            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 60000);

            const response = await fetch(`${AI_BASE_URL}/recommend_outfit`, {
                method: 'POST',
                body: formData,
                signal: controller.signal
            });

            clearTimeout(timeoutId);

            if (!response.ok) {
                console.error(`[AIService] AI API Error: ${response.status} ${response.statusText}`);
                throw new Error(`AI API Error: ${response.status}`);
            }

            const data: any = await response.json();
            console.log('[AIService] AI /recommend_outfit Response received successfully.');

            return data; // Usually contains textual output
        } catch (error) {
            console.error('[AIService] Call to AI /recommend_outfit failed:', error);
            throw error;
        }
    }

    /**
     * Checks for style harmony between outfit and shoes in an image.
     */
    public async validateOutfitImage(imageBuffer: Buffer, filename: string): Promise<any> {
        console.log(`[AIService] Requesting style harmony check for image: ${filename}`);

        try {
            const formData = new FormData();
            const blob = new Blob([new Uint8Array(imageBuffer)]);
            formData.append('file', blob, filename);

            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 60000);

            const response = await fetch(`${AI_BASE_URL}/validate_outfit_image`, {
                method: 'POST',
                body: formData,
                signal: controller.signal
            });

            clearTimeout(timeoutId);

            if (!response.ok) {
                console.error(`[AIService] AI API Error: ${response.status} ${response.statusText}`);
                throw new Error(`AI API Error: ${response.status}`);
            }

            const data: any = await response.json();
            console.log('[AIService] AI /validate_outfit_image Response received successfully.');

            return data;
        } catch (error) {
            console.error('[AIService] Call to AI /validate_outfit_image failed:', error);
            throw error;
        }
    }

    /**
     * Legacy method for color-based recommendations (fallback)
     */
    public async getRecommendationsByColors(colors: string[]): Promise<any[]> {
        console.log(`[AIService] Requesting fallback recommendations for colors: ${colors.join(', ')}`);
        try {
            // Find products in DB that match any of these colors
            const products = await ProductModel.find({
                $or: [
                    { primaryColor: { $in: colors } },
                    { secondaryColor: { $in: colors } }
                ]
            }).limit(5).lean();

            return products.map(shoe => ({
                id: (shoe as any)._id.toString(),
                name: (shoe as any).name,
                brand: (shoe as any).brand,
                price: (shoe as any).price,
                category: (shoe as any).category,
                thumbnailUrl: (shoe as any).thumbnailUrl,
                modelUrl: (shoe as any).modelUrl
            }));
        } catch (error) {
            console.error('[AIService] Fallback recommendations failed:', error);
            return [];
        }
    }

    public async analyzeOutfitImage(imageUrl: string): Promise<string[]> {
        // This is now handled by getRecommendationsFromImage in a single pass
        return ['black', 'blue', 'grey'];
    }
}

