import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'products', pathMatch: 'full' },
  {
    path: 'products',
    loadComponent: () =>
      import('./features/catalog/product-list.component').then(m => m.ProductListComponent)
  },
  {
    path: 'admin/products/new',
    loadComponent: () =>
      import('./features/catalog/product-form.component').then(m => m.ProductFormComponent)
  },
  {
    path: 'admin/products/:id',
    loadComponent: () =>
      import('./features/catalog/product-form.component').then(m => m.ProductFormComponent)
  },
  {
    path: 'cart',
    loadComponent: () =>
      import('./features/cart/cart.component').then(m => m.CartComponent)
  },
  {
    path: 'orders',
    loadComponent: () =>
      import('./features/orders/order-history.component').then(m => m.OrderHistoryComponent)
  },
  { path: '**', redirectTo: 'products' }
];
