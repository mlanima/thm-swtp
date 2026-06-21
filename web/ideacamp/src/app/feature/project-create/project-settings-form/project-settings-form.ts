import {Component, Output, EventEmitter, Input, OnChanges, SimpleChanges, inject, OnDestroy, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ProjectSettingsData, projectSettingsSchema} from '../schemas/project-create.schema';
import {ProjectCreateData} from '../schemas/project-create.schema';
import {FormErrors, mapZodErrors} from '../schemas/zod-error.helper';
import {catchError, debounceTime, distinctUntilChanged, of, Subject, Subscription, switchMap} from 'rxjs';
import {ProjectService} from '../../project-site/project.service';
import {generateProjectUrl} from '../project-url.utils';

type SettingsFormFields = 'projectUrl' | 'isPrivateProject';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
@Component({
  selector: 'app-project-settings-form',
  standalone: true,
  imports: [FormsModule, TranslatePipe],
  templateUrl: './project-settings-form.html',
})

/** Second step of the project creation wizard.
 * Gathers the project setting information and validates input
 * with the {@link projectSettingsSchema}
 */
export class ProjectSettingsForm implements OnChanges, OnDestroy {
  private readonly projectService = inject(ProjectService);
  private readonly projectUrlTerms = new Subject<string>();
  private readonly projectUrlSubscription: Subscription;
  private readonly translateService = inject(TranslateService);

  @Input() initialFormData?: Partial<ProjectCreateData>;
  @Output() next = new EventEmitter<ProjectSettingsData>();
  @Output() back = new EventEmitter<ProjectSettingsData>();

  formData = {projectUrl: '', isPrivateProject: false};
  errors: FormErrors<SettingsFormFields> = {};

  isCheckingProjectUrl = signal(false);
  projectUrlCheckFinished = signal(false);
  projectUrlExists = signal(false);
  projectUrlCheckError = signal<string | null>(null);

  constructor() {
    this.projectUrlSubscription = this.createProjectUrlSubscription();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['initialFormData'] && this.initialFormData) {
      const existingUrl = this.initialFormData.projectUrl ?? '';
      const projectName = this.initialFormData.name ?? '';

      this.formData = {
        projectUrl: '',
        isPrivateProject: this.initialFormData.isPrivateProject ?? false,
      };

      if (existingUrl) {
        this.formData.projectUrl = existingUrl;
        this.triggerUrlCheck(existingUrl);
      } else if (projectName) {
        const generatedUrl = generateProjectUrl(projectName) || 'projekt';
        this.formData.projectUrl = generatedUrl;
        this.triggerUrlCheck(generatedUrl);
      }
    }
  }

  ngOnDestroy() {
    this.projectUrlSubscription.unsubscribe();
  }

  togglePrivateProject() {
    this.formData.isPrivateProject = !this.formData.isPrivateProject;
  }

  submit() {
    if (this.isCheckingProjectUrl() || this.projectUrlExists()) {
      return;
    }

    const res = projectSettingsSchema.safeParse(this.formData);

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

  onProjectUrlChange(projectUrl: string): void {
    this.formData.projectUrl = projectUrl;
    this.triggerUrlCheck(projectUrl);
  }

  private triggerUrlCheck(projectUrl: string): void {
    const cleanedUrl = projectUrl.trim();

    this.projectUrlExists.set(false);
    this.projectUrlCheckError.set(null);

    if (cleanedUrl.length >= 3) {
      this.isCheckingProjectUrl.set(true);
      this.projectUrlCheckFinished.set(false);
    } else {
      this.resetProjectUrlCheck();
    }

    this.projectUrlTerms.next(cleanedUrl);
  }

  private createProjectUrlSubscription(): Subscription {
    return this.projectUrlTerms
      .pipe(
        debounceTime(400),
        distinctUntilChanged(),
        switchMap((projectUrl) => this.checkProjectUrl(projectUrl)),
      )
      .subscribe((exists) => {
        if (exists === null) {
          this.resetProjectUrlCheck();
          return;
        }

        this.projectUrlExists.set(exists);
        this.projectUrlCheckFinished.set(true);
        this.isCheckingProjectUrl.set(false);
      });
  }

  private checkProjectUrl(projectUrl: string) {
    const cleanedUrl = projectUrl.trim();

    if (cleanedUrl.length < 3) {
      return of(null);
    }

    return this.projectService.projectUrlExists(cleanedUrl).pipe(
      catchError(() => {
        this.projectUrlCheckError.set(this.translateService.instant('PROJECTCREATE.PROJECT_URL_CHECK_ERROR'));
        return of(null);
      }),
    );
  }

  private resetProjectUrlCheck(): void {
    this.isCheckingProjectUrl.set(false);
    this.projectUrlCheckFinished.set(false);
    this.projectUrlExists.set(false);
    this.projectUrlCheckError.set(null);
  }
}