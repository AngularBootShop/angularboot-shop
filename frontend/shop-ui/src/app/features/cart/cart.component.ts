import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CartService } from '../../core/services/cart.service';
import { AuthService } from '../../core/services/auth.service';
import { OrderManagementService } from '../../core/api/order/api/orderManagement.service';
import { OrderCreateRequest } from '../../core/api/order/model/orderCreateRequest';
import { OrderResponse } from '../../core/api/order/model/orderResponse';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.css'
})
export class CartComponent {
  readonly cartService = inject(CartService);
  readonly authService = inject(AuthService);
  private readonly orderApi = inject(OrderManagementService);

  readonly placingOrder = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly confirmedOrder = signal<OrderResponse | null>(null);

  checkout(): void {
    if (!this.authService.isAuthenticated()) {
      this.authService.login();
      return;
    }

    const items = this.cartService.items();
    if (items.length === 0) {
      return;
    }

    this.placingOrder.set(true);
    this.error.set(null);

    // Generate unique client idempotency key per checkout attempt
    const idempotencyKey = crypto.randomUUID();

    const request: OrderCreateRequest = {
      items: items.map(i => ({
        productId: i.productId,
        quantity: i.quantity
      }))
    };

    this.orderApi.createOrder(idempotencyKey, request).subscribe({
      next: (order: OrderResponse) => {
        this.placingOrder.set(false);
        this.cartService.clearCart();
        this.confirmedOrder.set(order);
      },
      error: (err: any) => {
        this.placingOrder.set(false);
        console.error('Order creation failed:', err);
        const detail = err.error?.detail || err.error?.message || err.message;
        this.error.set('Order placement failed: ' + detail);
      }
    });
  }
}
