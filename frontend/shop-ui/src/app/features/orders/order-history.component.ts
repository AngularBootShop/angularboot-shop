import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderManagementService } from '../../core/api/order/api/orderManagement.service';
import { OrderResponse } from '../../core/api/order/model/orderResponse';
import { PagedResponseOrderResponse } from '../../core/api/order/model/pagedResponseOrderResponse';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './order-history.component.html',
  styleUrl: './order-history.component.css'
})
export class OrderHistoryComponent implements OnInit {
  private readonly orderApi = inject(OrderManagementService);
  readonly authService = inject(AuthService);

  readonly orders = signal<OrderResponse[]>([]);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly currentPage = signal<number>(0);
  readonly totalPages = signal<number>(1);

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.loadOrders();
    }
  }

  loadOrders(): void {
    this.loading.set(true);
    this.error.set(null);

    const pageable = {
      page: this.currentPage(),
      size: 10
    };

    this.orderApi.getOrders(pageable).subscribe({
      next: (res: PagedResponseOrderResponse) => {
        this.orders.set(res.content || []);
        this.totalPages.set(res.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: any) => {
        console.error('Failed to load orders:', err);
        this.error.set('Failed to retrieve orders from Order Service.');
        this.loading.set(false);
      }
    });
  }

  changePage(newPage: number): void {
    this.currentPage.set(newPage);
    this.loadOrders();
  }
}
