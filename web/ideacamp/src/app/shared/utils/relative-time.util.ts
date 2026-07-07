import { TranslateService } from '@ngx-translate/core';

export function timeAgo(dateStr: string, translateService: TranslateService): string {
  const now = Date.now();
  const date = new Date(dateStr).getTime();
  const diffMs = now - date;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return translateService.instant('MYPROJECTS.TIME.JUST_NOW');
  if (diffMins < 60) return translateService.instant('MYPROJECTS.TIME.MINUTES_AGO', {count: diffMins});
  if (diffHours < 24) return translateService.instant('MYPROJECTS.TIME.HOURS_AGO', {count: diffHours});
  if (diffDays === 1) return translateService.instant('MYPROJECTS.TIME.YESTERDAY');
  if (diffDays < 30) return translateService.instant('MYPROJECTS.TIME.DAYS_AGO', {count: diffDays});
  return new Date(dateStr).toLocaleDateString();
}
