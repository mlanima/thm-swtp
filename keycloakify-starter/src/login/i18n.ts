/* eslint-disable @typescript-eslint/no-unused-vars */
import { i18nBuilder } from '@keycloakify/angular/login';
import type { ThemeName } from '../kc.gen';

/** @see: https://docs.keycloakify.dev/features/i18n */
const { getI18n, ofTypeI18n } = i18nBuilder
  .withThemeName<ThemeName>()
  .withCustomTranslations({
    en: {
      loginAccountTitle: 'Login',
      loginSubtitle: 'Welcome back! Please enter your credentials to continue',
      usernameOrEmail: 'Email or Username',
      doLogIn: 'Login',
      noAccount: "Don't have an account? ",
      doRegister: 'Sign up here',
      backToLogin: '\u00AB Back to Login',

      stepChooseNickname: 'Choose nickname',
      stepChooseNicknameTitle: 'Choose your nickname',
      stepChooseNicknameSubtitle: 'This will be your unique identifier on the platform',
      stepSetPassword: 'Set Password',
      stepSetPasswordTitle: 'Set your password',
      stepSetPasswordSubtitle: 'Choose a strong password to secure your account',
      stepSetEmail: 'Set mail',
      stepSetEmailTitle: 'Set your email',
      stepSetEmailSubtitle: "We'll use this to send you important notifications",
      stepProfile: 'Profile',
      stepProfileTitle: 'Complete your profile',
      stepProfileSubtitle: 'Tell us a bit about yourself',
      doContinue: 'Continue',
    },
  })
  .build();

type I18n = typeof ofTypeI18n;

export { getI18n, type I18n };
