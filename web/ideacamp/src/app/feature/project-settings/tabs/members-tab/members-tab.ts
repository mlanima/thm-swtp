import { Component, Input, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { ProjectMember } from '../../models/project-settings.model';

@Component({
  selector: 'app-members-tab',
  standalone: true,
  imports: [NgClass],
  templateUrl: './members-tab.html',
})
export class MembersTab {
  @Input() projectId = '';

  members = signal<ProjectMember[]>([
    {
      id: 'u1',
      name: 'Example User',
      email: 'example@thm.de',
      initials: 'EU',
      avatarColor: 'bg-lime-500',
      role: 'Owner',
      joinedDate: 'Jan 1, 2026',
    },
    {
      id: 'u2',
      name: 'Felix Wagner',
      email: 'f.wagner@thm.de',
      initials: 'FW',
      avatarColor: 'bg-slate-500',
      role: 'Member',
      joinedDate: 'Mar 3, 2026',
    },
    {
      id: 'u3',
      name: 'Carla Rossi',
      email: 'c.rossi@thm.de',
      initials: 'CR',
      avatarColor: 'bg-rose-400',
      role: 'Member',
      joinedDate: 'Apr 20, 2026',
    },
  ]);

  memberToRemove = signal<ProjectMember | null>(null);

  openRemoveModal(member: ProjectMember): void {
    this.memberToRemove.set(member);
  }

  closeRemoveModal(): void {
    this.memberToRemove.set(null);
  }

  confirmRemove(): void {
    const member = this.memberToRemove();
    if (!member) return;
    this.members.update(list => list.filter(m => m.id !== member.id));
    this.memberToRemove.set(null);
  }
}