import { TestBed } from '@angular/core/testing';
import { CartService } from './cart.service';

describe('CartService', () => {
  let service: CartService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(CartService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created with empty cart', () => {
    expect(service).toBeTruthy();
    expect(service.items().length).toBe(0);
    expect(service.totalItems()).toBe(0);
    expect(service.totalPrice()).toBe(0);
  });

  it('should add item to cart', () => {
    service.addItem({
      id: 1,
      name: 'Test Product',
      sku: 'TEST-001',
      price: 25.50,
      stock: 10
    });

    expect(service.items().length).toBe(1);
    expect(service.totalItems()).toBe(1);
    expect(service.totalPrice()).toBe(25.50);
  });

  it('should increment quantity when same item added', () => {
    const product = {
      id: 1,
      name: 'Test Product',
      sku: 'TEST-001',
      price: 20.00,
      stock: 5
    };

    service.addItem(product);
    service.addItem(product);

    expect(service.items().length).toBe(1);
    expect(service.totalItems()).toBe(2);
    expect(service.totalPrice()).toBe(40.00);
  });

  it('should not exceed stock limit', () => {
    const product = {
      id: 1,
      name: 'Limited Product',
      sku: 'TEST-002',
      price: 10.00,
      stock: 2
    };

    service.addItem(product);
    service.addItem(product);
    service.addItem(product); // Exceeds stock 2

    expect(service.totalItems()).toBe(2);
  });

  it('should decrement item quantity and remove when zero', () => {
    const product = {
      id: 1,
      name: 'Test Product',
      sku: 'TEST-001',
      price: 15.00,
      stock: 5
    };

    service.addItem(product);
    service.addItem(product);
    expect(service.totalItems()).toBe(2);

    service.decrementItem(1);
    expect(service.totalItems()).toBe(1);

    service.decrementItem(1);
    expect(service.totalItems()).toBe(0);
    expect(service.items().length).toBe(0);
  });

  it('should clear all items', () => {
    service.addItem({ id: 1, name: 'P1', sku: 'S1', price: 10, stock: 5 });
    service.addItem({ id: 2, name: 'P2', sku: 'S2', price: 20, stock: 5 });

    service.clearCart();

    expect(service.items().length).toBe(0);
    expect(service.totalItems()).toBe(0);
  });
});
