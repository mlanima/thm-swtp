import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { z } from 'zod';
import {
  ProfessorRequestResponse,
  ProfessorRequestService,
} from '../../../professor-request/services/professor-request.service';
import { AuthService } from '../../../auth/auth.service';
import { FormErrors, mapZodErrors } from '../../../project-create/schemas/zod-error.helper';

const professorRequestSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Bitte gib deinen Namen ein.')
    .max(100, 'Name darf höchstens 100 Zeichen lang sein.'),
  email: z
    .string()
    .trim()
    .max(200, 'E-Mail darf höchstens 200 Zeichen lang sein.')
    .optional()
    .or(z.literal(''))
    .pipe(
      z
        .string()
        .email('Bitte gib eine gültige E-Mail-Adresse ein.')
        .optional()
        .or(z.literal('')),
    ),
  text: z
    .string()
    .trim()
    .min(1, 'Bitte gib eine Begründung ein.')
    .max(1000, 'Begründung darf höchstens 1000 Zeichen lang sein.'),
});

type FormFields = keyof z.infer<typeof professorRequestSchema>;

@Component({
  selector: 'app-professor-request-tab',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './professor-request-tab.html',
})
export class ProfessorRequestTab implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly requestService = inject(ProfessorRequestService);

  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly errorMessage = signal('');
  readonly showSuccess = signal(false);
  readonly existingRequest = signal<ProfessorRequestResponse | null>(null);

  readonly userName = signal('');
  readonly userEmail = signal('');
  readonly userText = signal('');

  readonly formErrors = signal<FormErrors<FormFields>>({});

  ngOnInit(): void {
    this.authService.waitUntilAuthReady().then(() => {
      const user = this.authService.user();
      if (!user) {
        this.errorMessage.set('Benutzer konnte nicht geladen werden.');
        this.isLoading.set(false);
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
          this.isLoading.set(false);
        },
      });
    });
  }

  submitApplication(): void {
    const result = professorRequestSchema.safeParse({
      name: this.userName(),
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
        name: result.data.name,
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
          this.errorMessage.set(
            'Deine Anfrage konnte nicht gesendet werden. Bitte versuche es später erneut.',
          );
          this.isSubmitting.set(false);
        },
      });
  }

  closeSuccess(): void {
    this.showSuccess.set(false);
  }
}
