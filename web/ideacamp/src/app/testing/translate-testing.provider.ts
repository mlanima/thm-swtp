import { provideTranslateService } from '@ngx-translate/core';

export function provideTranslateTesting() {
  return provideTranslateService({
    fallbackLang: 'de',
    lang: 'de',
  });
}
