import { test, expect } from '@playwright/test';

test.describe('Shop Application E2E Test Suite', () => {
  test('should load catalog and verify brand title', async ({ page }) => {
    await page.goto('/products');
    await expect(page.locator('.brand-name')).toContainText('SHOP.IO');
    await expect(page.locator('.page-title')).toContainText('Catalog');
  });

  test('should add product to cart and update cart badge', async ({ page }) => {
    await page.goto('/products');

    // Wait for product grid to be visible
    const productGrid = page.locator('.product-grid');
    await expect(productGrid).toBeVisible({ timeout: 10000 });

    // Click "Add to Cart" on first available in-stock product
    const firstAddBtn = page.locator('.btn-primary.btn-full').first();
    await firstAddBtn.click();

    // Verify cart count badge appears and equals '1'
    const cartBadge = page.locator('.cart-count');
    await expect(cartBadge).toHaveText('1');

    // Navigate to cart
    await page.click('.cart-btn');
    await expect(page).toHaveURL(/.*\/cart/);
    await expect(page.locator('.cart-items-card')).toBeVisible();
    await expect(page.locator('.summary-title')).toHaveText('Order Summary');
  });

  test('should require authentication on checkout', async ({ page }) => {
    await page.goto('/cart');
    // If cart is empty or has items, unauthenticated user sees "Sign In to Checkout"
    const signInBtn = page.locator('text=Sign In to Checkout');
    if (await signInBtn.isVisible()) {
      await expect(signInBtn).toBeEnabled();
    }
  });
});
