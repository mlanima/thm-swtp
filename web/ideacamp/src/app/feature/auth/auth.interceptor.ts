import { Injectable, inject } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../../enviroments/enviroment.dev';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private auth = inject(AuthService);

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.auth.getAccessToken();

    // Only attach token for our API backend
    if (token && req.url.startsWith(environment.apiUrl)) {
      const userId = this.auth.user()?.id ?? '';
      const cloned = req.clone({
        setHeaders: { Authorization: `Bearer ${token}`, 'X-User-Id': userId },
      });
      return next.handle(cloned);
    }

    return next.handle(req);
  }

}

