import { Component, Input, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { JoinRequest } from '../../models/project-settings.model';

@Component({
  selector: 'app-join-requests-tab',
  standalone: true,
  imports: [NgClass],
  templateUrl: './join-requests-tab.html',
})
export class JoinRequestsTab {
  @Input() projectId = '';

  requests = signal<JoinRequest[]>([
    {
      id: '1',
      userId: 'u1',
      name: 'Maria Schmidt',
      email: 'm.schmidt@example.com',
      initials: 'MS',
      avatarColor: 'bg-lime-500',
      message: '"I would love to contribute my UX expertise."',
      requestDate: 'May 29, 2026',
    },
    {
      id: '2',
      userId: 'u2',
      name: 'Luca Bianchi',
      email: 'luca.b@example.com',
      initials: 'LB',
      avatarColor: 'bg-slate-600',
      message: '"Interested in helping with the roadmap planning."',
      requestDate: 'May 28, 2026',
    },
    {
      id: '3',
      userId: 'u3',
      name: 'Sophie Müller',
      email: 'sophie.m@example.com',
      initials: 'SM',
      avatarColor: 'bg-slate-500',
      message: '"Happy to support with documentation and testing."',
      requestDate: 'May 27, 2026',
    },
  ]);

  approve(id: string): void {
    this.requests.update(list => list.filter(r => r.id !== id));
  }

  decline(id: string): void {
    this.requests.update(list => list.filter(r => r.id !== id));
  }
}