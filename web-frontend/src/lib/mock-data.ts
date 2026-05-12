/**
 * Mock data for development and testing
 * This provides placeholder data while the backend is being connected
 */

export const mockShoes = [
  {
    id: '1',
    name: 'Air Max 270',
    brand: 'Nike',
    price: 150.0,
    thumbnailUrl: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400',
    modelUrl: 'https://s3.example.com/models/air-max-270.glb',
    category: 'Running',
    colors: ['Black', 'White', 'Volt'],
    description: 'Premium cushioning for daily comfort.',
  },
  {
    id: '2',
    name: 'Yeezy Boost 350',
    brand: 'Adidas',
    price: 220.0,
    thumbnailUrl: 'https://images.unsplash.com/photo-1600185365926-3a2ce3cdb9eb?w=400',
    modelUrl: 'https://s3.example.com/models/yeezy-boost.glb',
    category: 'Lifestyle',
    colors: ['Cream', 'Black', 'Zebra'],
    description: 'Iconic streetwear design with ultimate comfort.',
  },
  {
    id: '3',
    name: 'Jordan 1 Retro',
    brand: 'Nike',
    price: 170.0,
    thumbnailUrl: 'https://images.unsplash.com/photo-1511556532299-8f662fc26c06?w=400',
    modelUrl: 'https://s3.example.com/models/jordan-1.glb',
    category: 'Basketball',
    colors: ['Chicago', 'Bred', 'Royal'],
    description: 'Legendary basketball shoe with timeless style.',
  },
  {
    id: '4',
    name: 'Chuck Taylor All Star',
    brand: 'Converse',
    price: 65.0,
    thumbnailUrl: 'https://images.unsplash.com/photo-1607522370275-f14206abe5d3?w=400',
    modelUrl: 'https://s3.example.com/models/chuck-taylor.glb',
    category: 'Casual',
    colors: ['White', 'Black', 'Red'],
    description: 'Classic canvas sneaker for everyday wear.',
  },
  {
    id: '5',
    name: 'UltraBoost 22',
    brand: 'Adidas',
    price: 180.0,
    thumbnailUrl: 'https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?w=400',
    modelUrl: 'https://s3.example.com/models/ultraboost.glb',
    category: 'Running',
    colors: ['White', 'Black', 'Solar Yellow'],
    description: 'Responsive running shoe with Boost technology.',
  },
  {
    id: '6',
    name: 'Air Force 1',
    brand: 'Nike',
    price: 110.0,
    thumbnailUrl: 'https://images.unsplash.com/photo-1549298916-b41d501d3772?w=400',
    modelUrl: 'https://s3.example.com/models/air-force-1.glb',
    category: 'Lifestyle',
    colors: ['White', 'Black', 'Triple White'],
    description: 'The iconic basketball shoe that started it all.',
  },
];

export const mockTryOnHistory = [
  {
    id: '1',
    shoeId: {
      name: 'Air Max 270',
      thumbnailUrl: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400',
    },
    snapshotUrl: 'https://images.unsplash.com/photo-1460353581641-37baddab0fa2?w=400',
    createdAt: '2024-05-10T15:00:00Z',
  },
  {
    id: '2',
    shoeId: {
      name: 'Yeezy Boost 350',
      thumbnailUrl: 'https://images.unsplash.com/photo-1600185365926-3a2ce3cdb9eb?w=400',
    },
    snapshotUrl: 'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=400',
    createdAt: '2024-05-09T12:30:00Z',
  },
];

export const mockOutfitRecommendation = {
  styleAdvice: 'This shoe pairs perfectly with slim-fit indigo jeans and a neutral oversized hoodie.',
  suggestedColors: ['navy', 'beige', 'white'],
  outfitCategory: 'Casual',
};

export const mockShoeRecommendations = {
  detectedStyle: 'Streetwear',
  detectedColors: ['black', 'grey'],
  suggestedShoeColor: 'white',
  recommendations: [
    {
      id: '2',
      name: 'Yeezy Boost 350',
      brand: 'Adidas',
      matchScore: 0.95,
      thumbnailUrl: 'https://images.unsplash.com/photo-1600185365926-3a2ce3cdb9eb?w=400',
    },
    {
      id: '6',
      name: 'Air Force 1',
      brand: 'Nike',
      matchScore: 0.88,
      thumbnailUrl: 'https://images.unsplash.com/photo-1549298916-b41d501d3772?w=400',
    },
  ],
};

export const mockSkinRequests = {
  requests: [
    {
      id: '1',
      userEmail: 'user@example.com',
      shoeName: 'Air Max 270',
      status: 'new',
      createdAt: '2024-05-12T10:00:00Z',
      description: 'Please add a galaxy pattern to the sole.',
      primaryColor: '#000033',
      secondaryColor: '#FF00FF',
      imageUrls: ['https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400'],
    },
  ],
  total: 1,
  statusCounts: { new: 1, viewed: 0, in_progress: 0, completed: 0 },
};
