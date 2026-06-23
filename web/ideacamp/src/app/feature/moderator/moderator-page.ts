import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-moderator-page',
  standalone: true,
  imports: [TranslatePipe],
  template: `
    <section class="min-h-screen grid place-items-center bg-gradient-to-b from-gray-50 to-white p-8">
      <div class="w-full max-w-xl rounded-3xl border border-gray-200 bg-white p-10 text-center shadow-xl">
        <div class="mx-auto mb-5 grid h-16 w-16 place-items-center rounded-full bg-indigo-100 text-3xl text-indigo-600">
          <i class="pi pi-shield"></i>
        </div>

        <h1 class="text-4xl font-bold leading-tight text-gray-900">
          {{ 'MODERATOR.TITLE' | translate }}
        </h1>

        <p class="mx-auto mt-4 max-w-md leading-7 text-gray-600">
          {{ 'MODERATOR.DESCRIPTION' | translate }}
        </p>
      </div>
    </section>
  `,
})
export class ModeratorPage {}
