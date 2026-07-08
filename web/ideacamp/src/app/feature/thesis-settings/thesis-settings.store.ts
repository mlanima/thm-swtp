import { Injectable, signal } from '@angular/core';
import { ThesisResponse } from '../../models/thesis.model';

@Injectable({ providedIn: 'root' })
export class ThesisSettingsStore {
  thesis = signal<ThesisResponse | null>(null);
  isLoading = signal(true);
  errorMessage = signal<string | null>(null);

  setThesis(data: ThesisResponse): void {
    this.thesis.set(data);
  }

  setLoading(loading: boolean): void {
    this.isLoading.set(loading);
  }

  setError(error: string | null): void {
    this.errorMessage.set(error);
  }
}
