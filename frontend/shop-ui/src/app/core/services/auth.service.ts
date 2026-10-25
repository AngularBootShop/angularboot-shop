import { Injectable, signal, computed, inject } from '@angular/core';
import { OAuthService, AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly oauthService = inject(OAuthService);

  readonly isAuthenticated = signal<boolean>(false);
  readonly username = signal<string>('');
  readonly roles = signal<string[]>([]);
  readonly token = signal<string | null>(null);

  readonly isAdmin = computed(() => {
    const userRoles = this.roles();
    return userRoles.includes('ROLE_ADMIN') || userRoles.includes('ADMIN');
  });

  constructor() {
    this.initOAuth();
  }

  private async initOAuth(): Promise<void> {
    const authConfig: AuthConfig = {
      issuer: environment.keycloak.issuer,
      redirectUri: environment.keycloak.redirectUri,
      clientId: environment.keycloak.clientId,
      responseType: environment.keycloak.responseType,
      scope: environment.keycloak.scope,
      requireHttps: environment.keycloak.requireHttps,
      showDebugInformation: !environment.production
    };

    this.oauthService.configure(authConfig);
    this.oauthService.setupAutomaticSilentRefresh();

    try {
      await this.oauthService.loadDiscoveryDocumentAndTryLogin();
      this.updateState();

      this.oauthService.events.subscribe(() => {
        this.updateState();
      });
    } catch (err) {
      console.warn('Keycloak OIDC discovery or login check failed (Keycloak may be offline):', err);
    }
  }

  private updateState(): void {
    const hasValidToken = this.oauthService.hasValidAccessToken();
    this.isAuthenticated.set(hasValidToken);

    if (hasValidToken) {
      this.token.set(this.oauthService.getAccessToken());
      const claims = this.oauthService.getIdentityClaims() as Record<string, any>;
      if (claims) {
        this.username.set(claims['preferred_username'] || claims['sub'] || 'User');
      }

      // Extract realm roles from access token
      const token = this.oauthService.getAccessToken();
      if (token) {
        try {
          const payload = JSON.parse(atob(token.split('.')[1]));
          const realmRoles = payload?.realm_access?.roles || [];
          this.roles.set(realmRoles);
        } catch {
          this.roles.set([]);
        }
      }
    } else {
      this.token.set(null);
      this.username.set('');
      this.roles.set([]);
    }
  }

  login(): void {
    this.oauthService.initCodeFlow();
  }

  logout(): void {
    this.oauthService.logOut();
    this.updateState();
  }

  getAccessToken(): string | null {
    return this.token() || this.oauthService.getAccessToken();
  }

  hasRole(role: string): boolean {
    const userRoles = this.roles();
    const normalized = role.toUpperCase().replace('ROLE_', '');
    return userRoles.some(r => r.toUpperCase().replace('ROLE_', '') === normalized);
  }
}
