import { Component, signal } from '@angular/core';

import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './shared/header/header.component';
import { SidebarComponent } from './shared/sidebar/sidebar.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent, SidebarComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('ideacamp');

  isSidebarOpen = signal(false);

  toggleSidebar() {
    this.isSidebarOpen.update((value) => !value);
  }
}

