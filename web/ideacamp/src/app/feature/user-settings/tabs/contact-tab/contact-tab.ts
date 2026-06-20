import { Component, signal } from '@angular/core';

@Component({
  selector: 'app-contact-tab',
  standalone: true,
  templateUrl: './contact-tab.html',
})
export class ContactTab {
  readonly copied = signal(false);

  async copyMail(): Promise<void> {
    try {
      await navigator.clipboard.writeText('ideacamp.thm@protonmail.com');
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    } catch {
      // clipboard not available
    }
  }
}
