import { Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  template: `
    <div class="fixed right-4 top-4 z-[9999] flex flex-col gap-2">
      @for (toast of toastService.toasts(); track toast.id) {
        <div
          role="alert"
          class="animate-slide-in flex max-w-sm items-start gap-3 rounded-xl border px-4 py-3 text-sm shadow-lg"
          [class]="toast.type === 'error' ? 'border-red-200 bg-red-50 text-red-800' :
                   toast.type === 'success' ? 'border-green-200 bg-green-50 text-green-800' :
                   toast.type === 'warning' ? 'border-amber-200 bg-amber-50 text-amber-800' :
                   'border-blue-200 bg-blue-50 text-blue-800'"
        >
          <span class="mt-0.5 shrink-0">
            @if (toast.type === 'error') {
              <i class="pi pi-times-circle text-red-500"></i>
            } @else if (toast.type === 'success') {
              <i class="pi pi-check-circle text-green-500"></i>
            } @else if (toast.type === 'warning') {
              <i class="pi pi-exclamation-triangle text-amber-500"></i>
            } @else {
              <i class="pi pi-info-circle text-blue-500"></i>
            }
          </span>
          <span class="flex-1">{{ toast.message }}</span>
          <button
            class="shrink-0 opacity-60 hover:opacity-100"
            (click)="toastService.dismiss(toast.id)"
            aria-label="Close"
          >
            <i class="pi pi-times text-xs"></i>
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    @keyframes slide-in {
      from { transform: translateX(100%); opacity: 0; }
      to   { transform: translateX(0);    opacity: 1; }
    }
    .animate-slide-in { animation: slide-in 0.25s ease-out; }
  `],
})
export class ToastComponent {
  protected readonly toastService = inject(ToastService);
}
