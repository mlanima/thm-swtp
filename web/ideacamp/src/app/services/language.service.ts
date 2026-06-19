import { isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject } from '@angular/core';
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

  initLanguage(): void {
    const language = this.getStoredLanguage();

    this.translateService.setFallbackLang(this.fallbackLanguage);
    this.translateService.use(language);
  }

  changeLanguage(language: AppLanguage): void {
    this.translateService.use(language);

    if (this.isBrowser()) {
      localStorage.setItem(this.storageKey, language);
    }
  }

  getCurrentLanguage(): AppLanguage {
    const currentLanguage = this.translateService.getCurrentLang();

    return this.isSupportedLanguage(currentLanguage)
      ? currentLanguage
      : this.getStoredLanguage();
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
