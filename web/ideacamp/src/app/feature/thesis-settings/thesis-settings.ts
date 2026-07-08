import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NgClass } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { ThesisService } from '../thesis-site/thesis.service';
import { AuthService } from '../auth/auth.service';
import { ThesisSettingsStore } from './thesis-settings.store';
import { StudentsTab } from './tabs/students-tab/students-tab';
import { DangerZoneTab } from './tabs/danger-zone-tab/danger-zone-tab';

type Tab = 'students' | 'danger-zone';

@Component({
  selector: 'app-thesis-settings',
  standalone: true,
  imports: [NgClass, RouterLink, StudentsTab, DangerZoneTab, TranslatePipe],
  templateUrl: './thesis-settings.html',
})
export class ThesisSettings implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly thesisService = inject(ThesisService);
  private readonly authService = inject(AuthService);
  readonly store = inject(ThesisSettingsStore);

  activeTab = signal<Tab>('students');

  readonly tabs: { id: Tab; label: string; icon: string }[] = [
    { id: 'students', label: 'THESISSETTINGS.TABS.STUDENTS', icon: 'pi-user' },
    { id: 'danger-zone', label: 'THESISSETTINGS.TABS.DANGER_ZONE', icon: 'pi-trash' },
  ];

  ngOnInit(): void {
    const thesisUrl = this.route.snapshot.paramMap.get('thesisUrl');
    if (!thesisUrl) {
      this.store.setError('THESISSETTINGS.ERRORS.NO_THESIS_URL');
      this.store.setLoading(false);
      return;
    }

    this.thesisService.getThesisByUrl(thesisUrl).subscribe({
      next: (data) => {
        const user = this.authService.user();
        if (!user || user.id !== data.supervisorKeycloakId) {
          this.router.navigateByUrl(`/thesis/${data.thesisUrl}`);
          return;
        }
        this.store.setThesis(data);
        this.store.setLoading(false);
      },
      error: () => {
        this.store.setError('THESISSETTINGS.ERRORS.LOAD_THESIS');
        this.store.setLoading(false);
      },
    });
  }

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
  }
}
