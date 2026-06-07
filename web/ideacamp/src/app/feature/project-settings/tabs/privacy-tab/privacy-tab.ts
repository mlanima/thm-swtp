import { Component, Input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-privacy-tab',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './privacy-tab.html',
})
export class PrivacyTab {
  @Input() projectId = '';

  isPublic = signal(true);
  allowJoinRequests = signal(true);
}