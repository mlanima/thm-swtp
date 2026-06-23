import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink, TranslatePipe],
  template: `
    <footer class="border-t border-slate-200 bg-slate-100 px-6 py-4">
      <div class="mx-auto flex max-w-5xl flex-wrap items-center gap-x-5 gap-y-1 text-xs text-slate-400">
        <span>&copy; {{ currentYear }} {{ 'IMPRESSUM.FOOTER' | translate }}</span>
        <a routerLink="/impressum" class="text-slate-400 hover:text-lime-600 transition-colors">{{ 'FOOTER.IMPRESSUM' | translate }}</a>
        <a href="mailto:ideacamp.thm@protonmail.com" class="text-slate-400 hover:text-lime-600 transition-colors">{{ 'FOOTER.CONTACT' | translate }}</a>
      </div>
    </footer>
  `,
})
export class FooterComponent {
  readonly currentYear = new Date().getFullYear();
}
