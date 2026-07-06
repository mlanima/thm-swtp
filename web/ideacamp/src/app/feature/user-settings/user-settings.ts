import { Component, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { TranslatePipe } from '@ngx-translate/core';
import { ProfessorRequestTab } from './tabs/professor-request-tab/professor-request-tab';
import { ContactTab } from './tabs/contact-tab/contact-tab';
import { ImpressumTab } from './tabs/impressum-tab/impressum-tab';
import { IntegrationsTab } from './tabs/integrations-tab/integrations-tab';

type Tab = 'professor-request' | 'contact' | 'impressum' | 'integrations';

@Component({
  selector: 'app-user-settings',
  standalone: true,
  imports: [NgClass, TranslatePipe, ProfessorRequestTab, ContactTab, ImpressumTab, IntegrationsTab],
  templateUrl: './user-settings.html',
})
export class UserSettings {
  activeTab = signal<Tab>('professor-request');

  readonly tabs: { id: Tab; label: string; icon: string }[] = [
    { id: 'professor-request', label: 'USER_SETTINGS.TABS.PROFESSOR_REQUEST', icon: 'pi-graduation-cap' },
    { id: 'integrations', label: 'USER_SETTINGS.TABS.INTEGRATIONS', icon: 'pi-github' },
    { id: 'contact', label: 'USER_SETTINGS.TABS.CONTACT', icon: 'pi-envelope' },
    { id: 'impressum', label: 'USER_SETTINGS.TABS.IMPRESSUM', icon: 'pi-file' },
  ];

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
  }
}
