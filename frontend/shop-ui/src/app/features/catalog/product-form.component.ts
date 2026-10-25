import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ProductCatalogService } from '../../core/api/catalog/api/productCatalog.service';
import { ProductRequest } from '../../core/api/catalog/model/productRequest';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="container page-content animate-fade-in">
      <div class="form-wrapper glass-panel">
        <div class="form-header">
          <a routerLink="/products" class="back-link">← Back to Catalog</a>
          <h1 class="form-title">{{ isEditMode() ? 'Edit Product' : 'Add New Product' }}</h1>
          <p class="form-subtitle">Fill in the details below to publish to the catalog</p>
        </div>

        @if (error()) {
          <div class="error-banner">{{ error() }}</div>
        }

        <form [formGroup]="productForm" (ngSubmit)="onSubmit()" class="form-body">
          <div class="form-group">
            <label class="form-label">SKU (Stock Keeping Unit)</label>
            <input
              type="text"
              formControlName="sku"
              class="input-control"
              placeholder="e.g. LAPTOP-PRO-16"
            />
            @if (productForm.get('sku')?.touched && productForm.get('sku')?.invalid) {
              <span class="field-error">SKU is required (3-50 characters)</span>
            }
          </div>

          <div class="form-group">
            <label class="form-label">Product Name</label>
            <input
              type="text"
              formControlName="name"
              class="input-control"
              placeholder="e.g. UltraBook Pro 16-inch"
            />
            @if (productForm.get('name')?.touched && productForm.get('name')?.invalid) {
              <span class="field-error">Product name is required</span>
            }
          </div>

          <div class="form-group">
            <label class="form-label">Description</label>
            <textarea
              formControlName="description"
              class="input-control textarea-control"
              rows="3"
              placeholder="Product technical specs or description..."
            ></textarea>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label class="form-label">Price (USD)</label>
              <input
                type="number"
                step="0.01"
                formControlName="price"
                class="input-control"
                placeholder="0.00"
              />
              @if (productForm.get('price')?.touched && productForm.get('price')?.invalid) {
                <span class="field-error">Valid positive price required</span>
              }
            </div>

            <div class="form-group">
              <label class="form-label">Initial Stock</label>
              <input
                type="number"
                formControlName="stock"
                class="input-control"
                placeholder="0"
              />
              @if (productForm.get('stock')?.touched && productForm.get('stock')?.invalid) {
                <span class="field-error">Stock cannot be negative</span>
              }
            </div>
          </div>

          <div class="form-actions">
            <a routerLink="/products" class="btn btn-secondary">Cancel</a>
            <button
              type="submit"
              [disabled]="productForm.invalid || submitting()"
              class="btn btn-primary"
            >
              {{ submitting() ? 'Saving...' : (isEditMode() ? 'Update Product' : 'Create Product') }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .page-content {
      padding: 3rem 1.5rem;
      display: flex;
      justify-content: center;
    }

    .form-wrapper {
      width: 100%;
      max-width: 600px;
      padding: 2.5rem;
    }

    .form-header {
      margin-bottom: 2rem;
    }

    .back-link {
      display: inline-block;
      font-size: 0.85rem;
      color: var(--text-secondary);
      margin-bottom: 1rem;
      transition: color var(--transition-fast);
    }

    .back-link:hover {
      color: var(--primary);
    }

    .form-title {
      font-size: 1.75rem;
      font-weight: 800;
      letter-spacing: -0.02em;
    }

    .form-subtitle {
      color: var(--text-secondary);
      font-size: 0.9rem;
      margin-top: 0.25rem;
    }

    .form-body {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
      flex: 1;
    }

    .form-row {
      display: flex;
      gap: 1.25rem;
    }

    .form-label {
      font-size: 0.85rem;
      font-weight: 600;
      color: var(--text-secondary);
    }

    .textarea-control {
      resize: vertical;
    }

    .field-error {
      font-size: 0.75rem;
      color: var(--rose);
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
      gap: 1rem;
      margin-top: 1rem;
    }

    .error-banner {
      background: var(--rose-bg);
      border: 1px solid rgba(244, 63, 94, 0.3);
      color: var(--rose);
      padding: 0.75rem 1rem;
      border-radius: var(--radius-md);
      font-size: 0.85rem;
      margin-bottom: 1.5rem;
    }
  `]
})
export class ProductFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly catalogApi = inject(ProductCatalogService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly isEditMode = signal<boolean>(false);
  readonly submitting = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  private productId: number | null = null;

  productForm: FormGroup = this.fb.group({
    sku: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(500)]],
    price: [null, [Validators.required, Validators.min(0.01)]],
    stock: [0, [Validators.required, Validators.min(0)]]
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && idParam !== 'new') {
      this.productId = Number(idParam);
      this.isEditMode.set(true);
      this.loadProduct(this.productId);
    }
  }

  loadProduct(id: number): void {
    this.catalogApi.getProductById(id).subscribe({
      next: (product) => {
        this.productForm.patchValue({
          sku: product.sku,
          name: product.name,
          description: product.description,
          price: product.price,
          stock: product.stock
        });
      },
      error: (err) => {
        this.error.set('Failed to load product details: ' + (err.error?.detail || err.message));
      }
    });
  }

  onSubmit(): void {
    if (this.productForm.invalid) {
      return;
    }

    this.submitting.set(true);
    this.error.set(null);

    const request: ProductRequest = {
      sku: this.productForm.value.sku,
      name: this.productForm.value.name,
      description: this.productForm.value.description,
      price: Number(this.productForm.value.price),
      stock: Number(this.productForm.value.stock)
    };

    const action$ = this.isEditMode() && this.productId
      ? this.catalogApi.updateProduct(this.productId, request)
      : this.catalogApi.createProduct(request);

    action$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.router.navigate(['/products']);
      },
      error: (err) => {
        this.submitting.set(false);
        this.error.set('Failed to save product: ' + (err.error?.detail || err.message));
      }
    });
  }
}
