import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ProfileSaveErrorService {
  readonly message = signal<string>('');
  readonly errorCode = signal<string>('');

  set(msg: string, code?: string): void {
    this.message.set(msg);
    this.errorCode.set(code ?? '');
  }

  clear(): void {
    this.message.set('');
    this.errorCode.set('');
  }
}
