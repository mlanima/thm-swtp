import { Component, signal, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
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
  protected readonly title = signal('ideacamp');

  isSidebarOpen = signal(false);

  ngOnInit(): void {
    this.languageService.initLanguage();
  }

  toggleSidebar() {
    this.isSidebarOpen.update((value) => !value);
  }
}
