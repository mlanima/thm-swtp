import { getDefaultPageComponent, type KcPage } from '@keycloakify/angular/login';
import { UserProfileFormFieldsComponent } from '@keycloakify/angular/login/components/user-profile-form-fields';
import { TemplateComponent } from '@keycloakify/angular/login/template';
import type { ClassKey } from 'keycloakify/login';
import type { KcContext } from './KcContext';
import { LoginComponent } from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';

import './login.css';
import './register.css';

export const classes = {
  kcHtmlClass: 'login-pf',
  kcLoginClass: 'login-pf-page',
  kcHeaderClass: 'login-pf-page-header',
  kcHeaderWrapperClass: 'login-pf-header-wrapper',
  kcFormCardClass: 'login-pf-card',
  kcFormHeaderClass: 'login-pf-header',
  kcFormGroupClass: 'form-group',
  kcLabelClass: 'kc-label',
  kcInputClass: 'form-control',
  kcInputGroup: 'kc-input-group',
  kcFormPasswordVisibilityButtonClass: 'kc-password-toggle',
  kcFormPasswordVisibilityIconShow: 'kc-icon-eye',
  kcFormPasswordVisibilityIconHide: 'kc-icon-eye-slash',
  kcInputErrorMessageClass: 'kc-input-error',
  kcFormSettingClass: 'login-pf-settings',
  kcFormOptionsWrapperClass: 'login-pf-options-wrapper',
  kcFormButtonsClass: 'kc-form-buttons',
  kcButtonClass: 'kc-btn',
  kcButtonPrimaryClass: 'kc-btn-primary',
  kcButtonBlockClass: 'kc-btn-block',
  kcButtonLargeClass: 'kc-btn-lg',
  kcSignUpClass: 'login-pf-signup',
  kcAlertClass: 'kc-alert',
  kcAlertTitleClass: 'kc-alert-title',
  kcFeedbackInfoIcon: 'kc-icon-info',
  kcFeedbackSuccessIcon: 'kc-icon-success',
  kcFeedbackWarningIcon: 'kc-icon-warning',
  kcFeedbackErrorIcon: 'kc-icon-error',
} satisfies Partial<Record<ClassKey, string>>;
export const doUseDefaultCss = false;
export const doMakeUserConfirmPassword = true;

export async function getKcPage(pageId: KcContext['pageId']): Promise<KcPage> {
  switch (pageId) {
    case 'login.ftl':
      return {
        PageComponent: LoginComponent,
        TemplateComponent,
        UserProfileFormFieldsComponent,
        doMakeUserConfirmPassword,
        doUseDefaultCss,
        classes,
      };
    case 'register.ftl':
      return {
        PageComponent: RegisterComponent,
        TemplateComponent,
        UserProfileFormFieldsComponent,
        doMakeUserConfirmPassword,
        doUseDefaultCss,
        classes,
      };
    default:
      return {
        PageComponent: await getDefaultPageComponent(pageId),
        TemplateComponent,
        UserProfileFormFieldsComponent,
        doMakeUserConfirmPassword,
        doUseDefaultCss,
        classes,
      };
  }
}
