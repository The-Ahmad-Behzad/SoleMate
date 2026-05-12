/**
 * API Client for SoleMate BFF
 * Placeholder implementation - replace BASE_URL with actual backend URL
 */

const BASE_URL = 'http://localhost:8080';

interface ApiError {
  message: string;
  status: number;
}

class ApiClient {
  private baseUrl: string;
  private token: string | null = null;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
    this.token = localStorage.getItem('auth_token');
  }

  setToken(token: string) {
    this.token = token;
    localStorage.setItem('auth_token', token);
  }

  clearToken() {
    this.token = null;
    localStorage.removeItem('auth_token');
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const headers: HeadersInit = {
      ...options.headers,
    };

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`;
    }

    if (!(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(`${this.baseUrl}${endpoint}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      const error: ApiError = {
        message: `HTTP ${response.status}: ${response.statusText}`,
        status: response.status,
      };
      throw error;
    }

    return response.json();
  }

  // Catalog Service
  async getCatalog() {
    return this.request<any[]>('/api/catalog');
  }

  async getShoeById(id: string) {
    return this.request<any>(`/api/catalog/${id}`);
  }

  // Try-On & Closet Service
  async saveTryOn(data: FormData) {
    return this.request<any>('/api/tryon/save', {
      method: 'POST',
      body: data,
    });
  }

  async getTryOnHistory() {
    return this.request<any[]>('/api/tryon/history');
  }

  // AI Outfit Match Service
  async recommendShoes(data: FormData) {
    return this.request<any>('/api/outfit/recommend-shoes', {
      method: 'POST',
      body: data,
    });
  }

  async recommendOutfitForShoe(data: FormData) {
    return this.request<any>('/api/outfit/recommend-outfit-for-shoe', {
      method: 'POST',
      body: data,
    });
  }

  // Custom Skin Service
  async requestRedesign(data: FormData) {
    return this.request<any>('/api/skins/request-redesign', {
      method: 'POST',
      body: data,
    });
  }

  // Seller API - Inventory Service
  async getUploadUrl(data: any) {
    return this.request<any>('/api/seller/upload-url', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async confirmUpload(data: any) {
    return this.request<any>('/api/seller/uploads/confirm', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  // Seller API - Design Request Service
  async getSkinRequests() {
    return this.request<any>('/api/skin-requests');
  }

  async updateSkinRequest(id: string, data: any) {
    return this.request<any>(`/api/skin-requests/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  }
}

export const api = new ApiClient(BASE_URL);
