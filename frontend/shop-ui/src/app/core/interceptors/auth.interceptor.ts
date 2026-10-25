import { HttpInterceptorFn } from '@angular/common/http';
import { inject, Injector } from '@angular/core';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Never add Authorization header to Keycloak OIDC endpoints
  if (req.url.includes('/realms/shop-realm/')) {
    return next(req);
  }

  const injector = inject(Injector);
  let token: string | null = null;
  try {
    const authService = injector.get(AuthService);
    token = authService.getAccessToken();
  } catch {
    // Prevent DI circular dependency during initial instantiation
  }

  if (token && !req.headers.has('Authorization')) {
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(authReq);
  }

  return next(req);
};
