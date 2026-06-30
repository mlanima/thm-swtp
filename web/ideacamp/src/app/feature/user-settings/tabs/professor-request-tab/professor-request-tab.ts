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
  imports: [FormsModule, TranslatePipe],
  templateUrl: './professor-request-tab.html',
})
export class ProfessorRequestTab implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly requestService = inject(ProfessorRequestService);
  private readonly translate = inject(TranslateService);

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

  autoResize(): void {
    const el = this.profTextRef()?.nativeElement;
    if (!el) return;
    el.style.height = '0';
    el.style.height = el.scrollHeight + 2 + 'px';
  }

  ngOnInit(): void {
    this.authService.waitUntilAuthReady().then(() => {
      this.loadUserAndRequest();
    });
  }
  private loadUserAndRequest(): void {
    const user = this.authService.user();

    if (!user) {
      setTimeout(() => this.loadUserAndRequest(), 100);
      return;
    }

    this.userName.set(user.username);

    this.requestService.getMyRequest(user.id).subscribe({
      next: (existing) => {
        this.existingRequest.set(existing);

        if (!existing) {
          this.userName.set(user.username);
          this.userEmail.set('');
          this.userText.set('');
        }

        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set(this.translate.instant('PROFESSOR_REQUEST.ERROR_LOAD_USER'));
        this.isLoading.set(false);
      },
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
        error: () => {
          this.errorMessage.set(this.translate.instant('PROFESSOR_REQUEST.ERROR_SEND'));
          this.isSubmitting.set(false);
        },
      });
  }

  closeSuccess(): void {
    this.showSuccess.set(false);
  }
}
