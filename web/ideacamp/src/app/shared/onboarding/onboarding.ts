import { Component, output, signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

interface OnboardingStep {
  icon: string;
  titleKey: string;
  textKey: string;
}

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './onboarding.html',
})
export class Onboarding {
  readonly completed = output<void>();
  readonly remindLater = output<void>();

  readonly currentStep = signal(0);

  readonly steps: OnboardingStep[] = [
    {
      icon: 'pi pi-th-large',
      titleKey: 'ONBOARDING.STEPS.DASHBOARD.TITLE',
      textKey: 'ONBOARDING.STEPS.DASHBOARD.TEXT',
    },
    {
      icon: 'pi pi-bars',
      titleKey: 'ONBOARDING.STEPS.SIDEBAR.TITLE',
      textKey: 'ONBOARDING.STEPS.SIDEBAR.TEXT',
    },
    {
      icon: 'pi pi-search',
      titleKey: 'ONBOARDING.STEPS.SEARCH.TITLE',
      textKey: 'ONBOARDING.STEPS.SEARCH.TEXT',
    },
    {
      icon: 'pi pi-folder',
      titleKey: 'ONBOARDING.STEPS.MY_PROJECTS.TITLE',
      textKey: 'ONBOARDING.STEPS.MY_PROJECTS.TEXT',
    },
    {
      icon: 'pi pi-heart',
      titleKey: 'ONBOARDING.STEPS.FAVORITES.TITLE',
      textKey: 'ONBOARDING.STEPS.FAVORITES.TEXT',
    },
    {
      icon: 'pi pi-user',
      titleKey: 'ONBOARDING.STEPS.PROFILE_SETTINGS.TITLE',
      textKey: 'ONBOARDING.STEPS.PROFILE_SETTINGS.TEXT',
    },
  ];

  get isFirstStep(): boolean {
    return this.currentStep() === 0;
  }

  get isLastStep(): boolean {
    return this.currentStep() === this.steps.length - 1;
  }

  next(): void {
    if (this.isLastStep) {
      this.completed.emit();
      return;
    }

    this.currentStep.update((step) => step + 1);
  }

  previous(): void {
    if (this.isFirstStep) {
      return;
    }

    this.currentStep.update((step) => step - 1);
  }

  remindLaterClicked(): void {
    this.remindLater.emit();
  }
}
