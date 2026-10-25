import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CartService } from '../../../core/services/cart.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <header class="header">
      <div class="container header-container">
        <a routerLink="/" class="brand">
          <span class="brand-logo">⚡</span>
          <span class="brand-name">SHOP<span class="accent">.IO</span></span>
        </a>

        <nav class="nav-links">
          <a routerLink="/products" routerLinkActive="active" class="nav-item">Catalog</a>
          <a routerLink="/orders" routerLinkActive="active" class="nav-item">Orders</a>
          @if (authService.isAdmin()) {
            <a routerLink="/admin/products" routerLinkActive="active" class="nav-item admin-badge">
              Admin
            </a>
          }
        </nav>

        <div class="header-actions">
          <a routerLink="/cart" class="cart-btn" aria-label="Shopping Cart">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="9" cy="21" r="1"></circle>
              <circle cx="20" cy="21" r="1"></circle>
              <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
            </svg>
            @if (cartService.totalItems() > 0) {
              <span class="cart-count">{{ cartService.totalItems() }}</span>
            }
          </a>

          @if (authService.isAuthenticated()) {
            <div class="user-profile">
              <span class="user-greeting">
                <span class="status-indicator"></span>
                {{ authService.username() }}
              </span>
              <button (click)="authService.logout()" class="btn btn-secondary btn-sm">
                Sign Out
              </button>
            </div>
          } @else {
            <button (click)="authService.login()" class="btn btn-primary btn-sm">
              Sign In
            </button>
          }
        </div>
      </div>
    </header>
  `,
  styles: [`
    .header {
      position: sticky;
      top: 0;
      z-index: 50;
      background: rgba(10, 13, 20, 0.85);
      backdrop-filter: blur(12px);
      border-bottom: 1px solid var(--border-subtle);
      padding: 0.875rem 0;
    }

    .header-container {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1.5rem;
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-weight: 800;
      font-size: 1.25rem;
      letter-spacing: -0.02em;
    }

    .brand-logo {
      font-size: 1.35rem;
    }

    .accent {
      color: var(--primary);
    }

    .nav-links {
      display: flex;
      align-items: center;
      gap: 1.25rem;
    }

    .nav-item {
      font-size: 0.9rem;
      font-weight: 500;
      color: var(--text-secondary);
      transition: color var(--transition-fast);
      position: relative;
    }

    .nav-item:hover, .nav-item.active {
      color: var(--text-primary);
    }

    .nav-item.active::after {
      content: '';
      position: absolute;
      bottom: -1rem;
      left: 0;
      right: 0;
      height: 2px;
      background: var(--primary);
      border-radius: var(--radius-full);
    }

    .admin-badge {
      background: rgba(99, 102, 241, 0.15);
      color: var(--primary);
      padding: 0.2rem 0.5rem;
      border-radius: var(--radius-sm);
      font-size: 0.75rem;
      border: 1px solid rgba(99, 102, 241, 0.3);
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .cart-btn {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: var(--radius-md);
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      color: var(--text-primary);
      transition: all var(--transition-fast);
    }

    .cart-btn:hover {
      border-color: var(--border-highlight);
      background: var(--bg-surface-elevated);
    }

    .cart-count {
      position: absolute;
      top: -4px;
      right: -4px;
      background: var(--primary);
      color: #ffffff;
      font-size: 0.7rem;
      font-weight: 700;
      min-width: 18px;
      height: 18px;
      border-radius: var(--radius-full);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 4px;
    }

    .user-profile {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .user-greeting {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      font-size: 0.85rem;
      font-weight: 500;
      color: var(--text-secondary);
    }

    .status-indicator {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: var(--emerald);
      box-shadow: 0 0 8px rgba(16, 185, 129, 0.6);
    }

    .btn-sm {
      padding: 0.45rem 0.85rem;
      font-size: 0.8rem;
    }
  `]
})
export class HeaderComponent {
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);
}
