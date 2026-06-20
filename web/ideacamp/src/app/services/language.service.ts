import { isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export type AppLanguage = 'de' | 'en';

@Injectable({
  providedIn: 'root'
})
export class LanguageService {
  private readonly translateService = inject(TranslateService);
  private readonly platformId = inject(PLATFORM_ID);

  private readonly storageKey = 'language';
  private readonly fallbackLanguage: AppLanguage = 'de';

  readonly currentLanguage = signal<AppLanguage>(this.fallbackLanguage);

  initLanguage(): void {
    const language = this.getStoredLanguage();

    this.translateService.setFallbackLang(this.fallbackLanguage);
    this.translateService.use(language);
    this.currentLanguage.set(language);
  }

  changeLanguage(language: AppLanguage): void {
    this.translateService.use(language);
    this.currentLanguage.set(language);

    if (this.isBrowser()) {
      localStorage.setItem(this.storageKey, language);
    }
  }

  private getStoredLanguage(): AppLanguage {
    if (!this.isBrowser()) {
      return this.fallbackLanguage;
    }

    const savedLanguage = localStorage.getItem(this.storageKey);

    return this.isSupportedLanguage(savedLanguage)
      ? savedLanguage
      : this.fallbackLanguage;
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId);
  }

  private isSupportedLanguage(language: string | null | undefined): language is AppLanguage {
    return language === 'de' || language === 'en';
  }
}
