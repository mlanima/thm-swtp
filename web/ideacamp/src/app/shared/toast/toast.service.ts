import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  type: 'error' | 'success' | 'warning' | 'info';
  message: string;
}

let nextId = 0;

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);

  private show(type: Toast['type'], message: string): void {
    const id = nextId++;
    this.toasts.update(list => [...list, { id, type, message }]);
    const duration = type === 'error' ? 6000 : type === 'warning' ? 4000 : 3000;
    setTimeout(() => this.dismiss(id), duration);
  }

  error(message: string): void { this.show('error', message); }
  success(message: string): void { this.show('success', message); }
  warning(message: string): void { this.show('warning', message); }
  info(message: string): void { this.show('info', message); }

  dismiss(id: number): void {
    this.toasts.update(list => list.filter(t => t.id !== id));
  }

  clear(): void {
    this.toasts.set([]);
  }
}
