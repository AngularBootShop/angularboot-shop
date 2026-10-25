import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideOAuthClient } from 'angular-oauth2-oidc';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { correlationIdInterceptor } from './core/interceptors/correlation-id.interceptor';
import { BASE_PATH as CATALOG_BASE_PATH } from './core/api/catalog';
import { BASE_PATH as ORDER_BASE_PATH } from './core/api/order';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor, correlationIdInterceptor])
    ),
    provideOAuthClient(),
    { provide: CATALOG_BASE_PATH, useValue: environment.apiBaseUrl },
    { provide: ORDER_BASE_PATH, useValue: environment.apiBaseUrl }
  ]
};
