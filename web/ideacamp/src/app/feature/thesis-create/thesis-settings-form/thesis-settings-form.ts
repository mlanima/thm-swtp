import {Component, Output, EventEmitter, Input, OnChanges, SimpleChanges, inject, OnDestroy, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ThesisSettingsData, thesisSettingsSchema, ThesisCreateData} from '../schemas/thesis-create.schema';
import {FormErrors, mapZodErrors} from '../../project-create/schemas/zod-error.helper';
import {catchError, debounceTime, distinctUntilChanged, of, Subject, Subscription, switchMap} from 'rxjs';
import {ThesisService} from '../../thesis-site/thesis.service';
import {generateProjectUrl} from '../../project-create/project-url.utils';

type SettingsFormFields = 'thesisUrl';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
@Component({
  selector: 'app-thesis-settings-form',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './thesis-settings-form.html',
})

/** Second step of the thesis creation wizard.
 * Gathers the thesis URL and validates input
 * with the {@link thesisSettingsSchema}
 */
export class ThesisSettingsForm implements OnChanges, OnDestroy {
  private readonly thesisService = inject(ThesisService);
  private readonly thesisUrlTerms = new Subject<string>();
  private readonly thesisUrlSubscription: Subscription;
  private readonly translateService = inject(TranslateService);

  @Input() initialFormData?: Partial<ThesisCreateData>;
  @Output() next = new EventEmitter<ThesisSettingsData>();
  @Output() back = new EventEmitter<ThesisSettingsData>();

  formData = {thesisUrl: ''};
  errors: FormErrors<SettingsFormFields> = {};

  isCheckingThesisUrl = signal(false);
  thesisUrlCheckFinished = signal(false);
  thesisUrlExists = signal(false);
  thesisUrlCheckError = signal<string | null>(null);

  constructor() {
    this.thesisUrlSubscription = this.createThesisUrlSubscription();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['initialFormData'] && this.initialFormData) {
      const existingUrl = this.initialFormData.thesisUrl ?? '';
      const thesisTitle = this.initialFormData.title ?? '';

      this.formData = {
        thesisUrl: '',
      };

      if (existingUrl) {
        this.formData.thesisUrl = existingUrl;
        this.triggerUrlCheck(existingUrl);
      } else if (thesisTitle) {
        const generatedUrl = generateProjectUrl(thesisTitle) || 'abschlussarbeit';
        this.formData.thesisUrl = generatedUrl;
        this.triggerUrlCheck(generatedUrl);
      }
    }
  }

  ngOnDestroy() {
    this.thesisUrlSubscription.unsubscribe();
  }

  submit() {
    if (this.isCheckingThesisUrl() || this.thesisUrlExists()) {
      return;
    }

    const res = thesisSettingsSchema.safeParse(this.formData);

    if (!res.success) {
      this.errors = mapZodErrors<SettingsFormFields>(res.error);
      return;
    }

    this.errors = {};
    this.next.emit(res.data);
  }

  goBack() {
    this.back.emit(this.formData);
  }

  onThesisUrlChange(thesisUrl: string): void {
    this.formData.thesisUrl = thesisUrl;
    this.triggerUrlCheck(thesisUrl);
  }

  private triggerUrlCheck(thesisUrl: string): void {
    const cleanedUrl = thesisUrl.trim();

    this.thesisUrlExists.set(false);
    this.thesisUrlCheckError.set(null);

    if (cleanedUrl.length >= 3) {
      this.isCheckingThesisUrl.set(true);
      this.thesisUrlCheckFinished.set(false);
    } else {
      this.resetThesisUrlCheck();
    }

    this.thesisUrlTerms.next(cleanedUrl);
  }

  private createThesisUrlSubscription(): Subscription {
    return this.thesisUrlTerms
      .pipe(
        debounceTime(400),
        distinctUntilChanged(),
        switchMap((thesisUrl) => this.checkThesisUrl(thesisUrl)),
      )
      .subscribe((exists) => {
        if (exists === null) {
          this.resetThesisUrlCheck();
          return;
        }

        this.thesisUrlExists.set(exists);
        this.thesisUrlCheckFinished.set(true);
        this.isCheckingThesisUrl.set(false);
      });
  }

  private checkThesisUrl(thesisUrl: string) {
    const cleanedUrl = thesisUrl.trim();

    if (cleanedUrl.length < 3) {
      return of(null);
    }

    return this.thesisService.thesisUrlExists(cleanedUrl).pipe(
      catchError(() => {
        this.thesisUrlCheckError.set(this.translateService.instant('THESISCREATE.THESIS_URL_CHECK_ERROR'));
        return of(null);
      }),
    );
  }

  private resetThesisUrlCheck(): void {
    this.isCheckingThesisUrl.set(false);
    this.thesisUrlCheckFinished.set(false);
    this.thesisUrlExists.set(false);
    this.thesisUrlCheckError.set(null);
  }
}
