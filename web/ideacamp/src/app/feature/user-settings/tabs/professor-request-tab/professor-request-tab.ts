import { Component, ElementRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { z } from 'zod';
import {
  ProfessorRequestResponse,
  ProfessorRequestService,
} from '../../../professor-request/services/professor-request.service';
import { AuthService } from '../../../auth/auth.service';
import { FormErrors, mapZodErrors } from '../../../project-create/schemas/zod-error.helper';
import { ActivatedRoute, Router } from '@angular/router';
import { DatePipe } from '@angular/common';
import { UserProfileService } from '../../../../services/user-profile.service'

const professorRequestSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, 'PROFESSOR_REQUEST.VALIDATION.EMAIL_REQUIRED')
    .email('PROFESSOR_REQUEST.VALIDATION.EMAIL_INVALID'),
  text: z
    .string()
    .trim()
    .min(1, 'PROFESSOR_REQUEST.VALIDATION.TEXT_REQUIRED')
    .max(1000, 'PROFESSOR_REQUEST.VALIDATION.TEXT_MAX'),
});

type FormFields = keyof z.infer<typeof professorRequestSchema>;

@Component({
  selector: 'app-professor-request-tab',
  standalone: true,
  imports: [FormsModule, TranslatePipe, DatePipe],
  templateUrl: './professor-request-tab.html',
})
export class ProfessorRequestTab implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly requestService = inject(ProfessorRequestService);
  private readonly translate = inject(TranslateService);
  private readonly userProfileService = inject(UserProfileService);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly verificationToken = signal<string | null>(null);
  readonly verificationSuccess = signal(false);

  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');
  readonly showSuccess = signal(false);
  readonly existingRequest = signal<ProfessorRequestResponse | null>(null);

  readonly userName = signal('');
  readonly userEmail = signal('');
  readonly userText = signal('');

  readonly formErrors = signal<FormErrors<FormFields>>({});

  readonly profTextRef = viewChild<ElementRef<HTMLTextAreaElement>>('profText');

  readonly isProfessor = signal(false);

  autoResize(): void {
    const el = this.profTextRef()?.nativeElement;
    if (!el) return;
    el.style.height = '0';
    el.style.height = el.scrollHeight + 2 + 'px';
  }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.verificationToken.set(params.get('verifyToken'));
    });

    this.authService.waitUntilAuthReady().then(() => {
      const user = this.authService.user();

      if (!user) {
        this.isLoading.set(false);
        return;
      }

      this.loadUserAndRequest(user.id, user.username);
    });
  }

  submitApplication(): void {
    const result = professorRequestSchema.safeParse({
      email: this.userEmail(),
      text: this.userText(),
    });

    if (!result.success) {
      this.formErrors.set(mapZodErrors<FormFields>(result.error));
      return;
    }

    this.formErrors.set({});
    this.isSubmitting.set(true);
    this.errorMessage.set('');

    this.requestService
      .create({
        email: result.data.email ?? '',
        text: result.data.text,
      })
      .subscribe({
        next: (created) => {
          this.existingRequest.set(created);
          this.isSubmitting.set(false);
          this.showSuccess.set(true);
        },
        error: (error) => {
          if (error.status === 400) {
            this.formErrors.set({
              email: 'PROFESSOR_REQUEST.VALIDATION.EMAIL_THM_REQUIRED',
            });
          } else {
            this.errorMessage.set(this.translate.instant('PROFESSOR_REQUEST.ERROR_SEND'));
          }

          this.isSubmitting.set(false);
        },
      });
  }

  closeSuccess(): void {
    this.showSuccess.set(false);
  }

  verifyEmail(): void {
    const token = this.verificationToken();

    if (!token) {
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');

    this.requestService.verifyEmail(token).subscribe({
      next: (verifiedRequest) => {
        this.existingRequest.set(verifiedRequest);
        this.verificationSuccess.set(true);
        this.verificationToken.set(null);
        this.isSubmitting.set(false);

        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { verifyToken: null },
          queryParamsHandling: 'merge',
          replaceUrl: true,
        });
      },
      error: () => {
        this.errorMessage.set(this.translate.instant('PROFESSOR_REQUEST.ERROR_VERIFY_EMAIL'));
        this.isSubmitting.set(false);
      },
    });
  }

  private loadUserAndRequest(userId: string, username: string): void {
    this.userName.set(username);

    this.userProfileService.getMyProfile().subscribe({
      next: (profile) => {
        this.isProfessor.set(profile.isProfessor);
      },
    });

    this.requestService.getMyRequest(userId).subscribe({
      next: (existing) => {
        this.existingRequest.set(existing);

        if (!existing) {
          this.userEmail.set('');
          this.userText.set('');
        }

        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translate.instant('PROFESSOR_REQUEST.ERROR_LOAD'));
        this.isLoading.set(false);
      },
    });
  }
}
