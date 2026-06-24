import { Component, signal, inject, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { HeaderComponent } from './shared/header/header.component';
import { SidebarComponent } from './shared/sidebar/sidebar.component';
import { FooterComponent } from './shared/footer/footer';
import { LanguageService } from './services/language.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent, SidebarComponent, FooterComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly languageService = inject(LanguageService);
  private readonly router = inject(Router);
  protected readonly title = signal('ideacamp');

  isSidebarOpen = signal(false);
  currentUrl = signal('');

  ngOnInit(): void {
    this.languageService.initLanguage();

    this.currentUrl.set(this.router.url);

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(event => {
        this.currentUrl.set(event.urlAfterRedirects);
      });
  }

  hideLayout(): boolean {
    return this.currentUrl().startsWith('/account-banned');
  }

  toggleSidebar() {
    this.isSidebarOpen.update((value) => !value);
  }
}
