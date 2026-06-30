import {
  ChangeDetectionStrategy,
  Component,
  forwardRef,
  inject,
  input,
  type OnDestroy,
  type OnInit,
  signal,
  type TemplateRef,
  type Type,
  viewChild,
} from '@angular/core';
import { KcSanitizePipe } from '@keycloakify/angular/lib/pipes/kc-sanitize';
import { USE_DEFAULT_CSS } from '@keycloakify/angular/lib/tokens/use-default-css';
import { ComponentReference } from '@keycloakify/angular/login/classes/component-reference';
import { type UserProfileFormFieldsComponent } from '@keycloakify/angular/login/components/user-profile-form-fields';
import { KcClassDirective } from '@keycloakify/angular/login/directives/kc-class';
import type { I18n } from '../../i18n';
import type { KcContext } from '@keycloakify/angular/login/KcContext';
import { LOGIN_CLASSES } from '@keycloakify/angular/login/tokens/classes';
import { LOGIN_I18N } from '@keycloakify/angular/login/tokens/i18n';
import { KC_LOGIN_CONTEXT } from '@keycloakify/angular/login/tokens/kc-context';
import type { ClassKey } from 'keycloakify/login/lib/kcClsx';

interface Step {
  label: string;
  title: string;
  subtitle: string;
  fields: string[];
}

@Component({
  selector: 'kc-register',
  templateUrl: 'register.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [KcClassDirective, KcSanitizePipe],
  providers: [
    {
      provide: ComponentReference,
      useExisting: forwardRef(() => RegisterComponent),
    },
  ],
})
export class RegisterComponent extends ComponentReference implements OnInit, OnDestroy {
  kcContext = inject<Extract<KcContext, { pageId: 'register.ftl' }>>(KC_LOGIN_CONTEXT);
  i18n = inject<I18n>(LOGIN_I18N);

  override doUseDefaultCss = inject<boolean>(USE_DEFAULT_CSS);
  override classes = inject<Partial<Record<ClassKey, string>>>(LOGIN_CLASSES);

  documentTitle: string | undefined;
  bodyClassName: string | undefined;

  displayRequiredFields = false;
  displayInfo = false;
  displayMessage = !this.kcContext?.messagesPerField?.existsError('global');

  headerNode = viewChild<TemplateRef<HTMLElement>>('headerNode');
  infoNode = viewChild<TemplateRef<HTMLElement>>('infoNode');
  socialProvidersNode = viewChild<TemplateRef<HTMLElement>>('socialProvidersNode');

  areTermsAccepted = signal(false);
  userProfileFormFields = input<Type<UserProfileFormFieldsComponent>>();

  currentStep = signal(0);

  steps: Step[] = [];

  private fieldValues: Record<string, string> = {};

  fieldErrors = signal<Record<string, string>>({});
  touchedFields = signal<Set<string>>(new Set());

  private validators: Record<string, (value: string) => string | null> = {
    username: (value) => {
      if (!value?.trim()) return 'Username is required';
      if (value.trim().length < 3) return 'Username must be at least 3 characters';
      return null;
    },
    password: (value) => {
      if (!value) return 'Password is required';
      if (value.length < 8) return 'Password must be at least 8 characters';
      return null;
    },
    'password-confirm': (value) => {
      if (!value) return 'Please confirm your password';
      if (value !== this.fieldValues['password']) return 'Passwords do not match';
      return null;
    },
    email: (value) => {
      if (!value?.trim()) return 'Email is required';
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())) return 'Invalid email format';
      return null;
    },
    firstName: (value) => {
      if (!value?.trim()) return 'First name is required';
      return null;
    },
    lastName: (value) => {
      if (!value?.trim()) return 'Last name is required';
      return null;
    },
  };

  getFieldError(field: string): string | null {
    return this.fieldErrors()[field] ?? null;
  }

  isFieldValid(field: string): boolean {
    const validator = this.validators[field];
    if (!validator) return true;
    return validator(this.fieldValues[field] ?? '') === null;
  }

  isCurrentStepValid(): boolean {
    const step = this.steps[this.currentStep()];
    if (!step) return true;
    return step.fields.every((f) => this.isFieldValid(f));
  }

  private validateField(field: string): void {
    const validator = this.validators[field];
    if (!validator) return;
    const error = validator(this.fieldValues[field] ?? '');
    this.fieldErrors.update((errors) => {
      const next = { ...errors };
      if (error) {
        next[field] = error;
      } else {
        delete next[field];
      }
      return next;
    });
  }

  private markTouched(field: string): void {
    this.touchedFields.update((touched) => {
      const next = new Set(touched);
      next.add(field);
      return next;
    });
  }

  get isLastStep(): boolean {
    return this.currentStep() === this.steps.length - 1;
  }

  get isFirstStep(): boolean {
    return this.currentStep() === 0;
  }

  getUsernameValue(): string {
    return this.fieldValues['username'] ?? '';
  }

  getEmailValue(): string {
    return this.fieldValues['email'] ?? '';
  }

  getFirstNameValue(): string {
    return this.fieldValues['firstName'] ?? '';
  }

  getLastNameValue(): string {
    return this.fieldValues['lastName'] ?? '';
  }

  getPasswordValue(): string {
    return this.fieldValues['password'] ?? '';
  }

  getPasswordConfirmValue(): string {
    return this.fieldValues['password-confirm'] ?? '';
  }

  getStepIndex(fieldName: string): number {
    const profile = this.kcContext.profile;
    const hasUsername =
      profile.attributesByName['username'] !== undefined && profile.attributesByName['username'] !== null;
    const hasPassword = this.kcContext.passwordRequired;

    let idx = 0;
    if (fieldName === 'username') return hasUsername ? idx : -1;
    if (hasUsername) idx++;
    if (fieldName === 'password') return hasPassword ? idx : -1;
    if (hasPassword) idx++;
    if (fieldName === 'email') return idx;
    idx++;
    if (fieldName === 'firstName' || fieldName === 'lastName') return idx;
    return -1;
  }

  private onFieldInput(field: string, event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.fieldValues[field] = value;
    this.markTouched(field);
    this.validateField(field);
  }

  onUsernameInput(event: Event): void {
    this.onFieldInput('username', event);
  }

  onEmailInput(event: Event): void {
    this.onFieldInput('email', event);
  }

  onFirstNameInput(event: Event): void {
    this.onFieldInput('firstName', event);
  }

  onLastNameInput(event: Event): void {
    this.onFieldInput('lastName', event);
  }

  onPasswordInput(event: Event): void {
    this.onFieldInput('password', event);
  }

  onPasswordConfirmInput(event: Event): void {
    this.onFieldInput('password-confirm', event);
  }

  ngOnInit(): void {
    this.steps = this.buildSteps();

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (window as any)['onSubmitRecaptcha'] = () => {
      // @ts-expect-error: from native code
      document.getElementById('kc-register-form').requestSubmit();
    };
  }

  ngOnDestroy(): void {
    // eslint-disable-next-line
    delete (window as any)['onSubmitRecaptcha'];
  }

  nextStep(): void {
    if (!this.isCurrentStepValid()) return;
    // Touch all fields on the current step so errors show
    const step = this.steps[this.currentStep()];
    if (step) {
      for (const field of step.fields) {
        this.markTouched(field);
      }
    }
    if (this.currentStep() < this.steps.length - 1) {
      this.currentStep.update((s) => s + 1);
    }
  }

  prevStep(): void {
    if (this.currentStep() > 0) {
      this.currentStep.update((s) => s - 1);
    }
  }

  goToStep(step: number): void {
    if (step >= 0 && step < this.steps.length) {
      this.currentStep.set(step);
    }
  }

  isStepAccessible(stepIndex: number): boolean {
    return stepIndex <= this.currentStep();
  }

  private buildSteps(): Step[] {
    const profile = this.kcContext.profile;
    const hasUsername =
      profile.attributesByName['username'] !== undefined && profile.attributesByName['username'] !== null;
    const hasPassword = this.kcContext.passwordRequired;

    const steps: Step[] = [];

    if (hasUsername) {
      steps.push({
        label: this.i18n.msgStr('stepChooseNickname'),
        title: this.i18n.msgStr('stepChooseNicknameTitle'),
        subtitle: this.i18n.msgStr('stepChooseNicknameSubtitle'),
        fields: ['username'],
      });
    }

    if (hasPassword) {
      steps.push({
        label: this.i18n.msgStr('stepSetPassword'),
        title: this.i18n.msgStr('stepSetPasswordTitle'),
        subtitle: this.i18n.msgStr('stepSetPasswordSubtitle'),
        fields: ['password', 'password-confirm'],
      });
    }

    steps.push({
      label: this.i18n.msgStr('stepSetEmail'),
      title: this.i18n.msgStr('stepSetEmailTitle'),
      subtitle: this.i18n.msgStr('stepSetEmailSubtitle'),
      fields: ['email'],
    });

    steps.push({
      label: this.i18n.msgStr('stepProfile'),
      title: this.i18n.msgStr('stepProfileTitle'),
      subtitle: this.i18n.msgStr('stepProfileSubtitle'),
      fields: ['firstName', 'lastName'],
    });

    return steps;
  }
}
