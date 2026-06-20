import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  template: `
    <footer class="border-t border-slate-200 bg-slate-100 px-6 py-4">
      <div class="mx-auto flex max-w-5xl flex-wrap items-center gap-x-5 gap-y-1 text-xs text-slate-400">
        <span>&copy; {{ currentYear }} ideaCamp &middot; THM Gießen</span>
        <a routerLink="/impressum" class="text-slate-400 hover:text-lime-600 transition-colors">Impressum</a>
        <a href="mailto:ideacamp.thm@protonmail.com" class="text-slate-400 hover:text-lime-600 transition-colors">Kontakt</a>
      </div>
    </footer>
  `,
})
export class FooterComponent {
  readonly currentYear = new Date().getFullYear();
}
