import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ProductCatalogService } from '../../core/api/catalog/api/productCatalog.service';
import { ProductResponse } from '../../core/api/catalog/model/productResponse';
import { PagedResponseProductResponse } from '../../core/api/catalog/model/pagedResponseProductResponse';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.css'
})
export class ProductListComponent implements OnInit {
  private readonly catalogApi = inject(ProductCatalogService);
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);

  readonly products = signal<ProductResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly currentPage = signal<number>(0);
  readonly totalPages = signal<number>(1);

  searchKeyword: string = '';
  searchMinPrice: number | undefined = undefined;
  searchMaxPrice: number | undefined = undefined;
  searchInStockOnly: boolean = false;

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading.set(true);
    this.error.set(null);

    const pageable = {
      page: this.currentPage(),
      size: 12
    };

    this.catalogApi.getProducts(
      pageable,
      this.searchKeyword || undefined,
      this.searchMinPrice,
      this.searchMaxPrice
    ).subscribe({
      next: (res: PagedResponseProductResponse) => {
        this.products.set(res.content || []);
        this.totalPages.set(res.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: any) => {
        console.error('Failed to load products:', err);
        this.error.set('Failed to connect to Catalog Service. Please check if services are running.');
        this.loading.set(false);
      }
    });
  }

  applyFilters(): void {
    this.currentPage.set(0);
    this.loadProducts();
  }

  changePage(newPage: number): void {
    this.currentPage.set(newPage);
    this.loadProducts();
  }

  addToCart(product: ProductResponse): void {
    this.cartService.addItem(product);
  }

  deleteProduct(id: number): void {
    if (!confirm('Are you sure you want to delete this product?')) {
      return;
    }

    this.catalogApi.deleteProduct(id).subscribe({
      next: () => {
        this.loadProducts();
      },
      error: (err: any) => {
        alert('Failed to delete product: ' + (err.error?.detail || err.message));
      }
    });
  }
}
