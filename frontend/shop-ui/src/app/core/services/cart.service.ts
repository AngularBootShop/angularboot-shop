import { Injectable, signal, computed } from '@angular/core';

export interface CartItem {
  productId: number;
  name: string;
  sku: string;
  price: number;
  quantity: number;
  stock: number;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private readonly STORAGE_KEY = 'shop_cart_items';

  readonly items = signal<CartItem[]>(this.loadInitialItems());

  readonly totalItems = computed(() =>
    this.items().reduce((sum, item) => sum + item.quantity, 0)
  );

  readonly totalPrice = computed(() =>
    this.items().reduce((sum, item) => sum + item.price * item.quantity, 0)
  );

  private loadInitialItems(): CartItem[] {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      return stored ? JSON.parse(stored) : [];
    } catch {
      return [];
    }
  }

  private persist(items: CartItem[]): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(items));
    } catch {
      // Ignore storage errors in restricted contexts
    }
  }

  addItem(product: { id?: number; name?: string; sku?: string; price?: number; stock?: number }): void {
    if (!product.id || (product.stock !== undefined && product.stock <= 0)) {
      return;
    }

    this.items.update(currentItems => {
      const existing = currentItems.find(item => item.productId === product.id);
      let updated: CartItem[];

      if (existing) {
        if (existing.quantity >= (product.stock || 999)) {
          return currentItems; // Cannot exceed stock
        }
        updated = currentItems.map(item =>
          item.productId === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item
        );
      } else {
        updated = [
          ...currentItems,
          {
            productId: product.id!,
            name: product.name || 'Unnamed Product',
            sku: product.sku || 'SKU-000',
            price: Number(product.price) || 0,
            quantity: 1,
            stock: product.stock ?? 999
          }
        ];
      }

      this.persist(updated);
      return updated;
    });
  }

  decrementItem(productId: number): void {
    this.items.update(currentItems => {
      const existing = currentItems.find(item => item.productId === productId);
      if (!existing) return currentItems;

      let updated: CartItem[];
      if (existing.quantity <= 1) {
        updated = currentItems.filter(item => item.productId !== productId);
      } else {
        updated = currentItems.map(item =>
          item.productId === productId
            ? { ...item, quantity: item.quantity - 1 }
            : item
        );
      }

      this.persist(updated);
      return updated;
    });
  }

  removeItem(productId: number): void {
    this.items.update(currentItems => {
      const updated = currentItems.filter(item => item.productId !== productId);
      this.persist(updated);
      return updated;
    });
  }

  clearCart(): void {
    this.items.set([]);
    this.persist([]);
  }
}
