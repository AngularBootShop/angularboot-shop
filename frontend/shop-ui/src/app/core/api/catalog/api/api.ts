export * from './internalInventoryManagement.service';
import { InternalInventoryManagementService } from './internalInventoryManagement.service';
export * from './productCatalog.service';
import { ProductCatalogService } from './productCatalog.service';
export const APIS = [InternalInventoryManagementService, ProductCatalogService];
