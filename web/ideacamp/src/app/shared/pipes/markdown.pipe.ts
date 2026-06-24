import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'markdown',
  standalone: true,
})
export class MarkdownPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value?.trim()) {
      return '';
    }

    const lines = this.escapeHtml(value).split(/\r?\n/);
    let html = '';
    let isListOpen = false;

    const closeList = () => {
      if (isListOpen) {
        html += '</ul>';
        isListOpen = false;
      }
    };

    for (const line of lines) {
      const trimmed = line.trim();

      if (!trimmed) {
        closeList();
        continue;
      }

      if (trimmed.startsWith('- ')) {
        if (!isListOpen) {
          html += '<ul class="mt-3 list-disc space-y-1 pl-5">';
          isListOpen = true;
        }

        html += `<li>${this.renderInline(trimmed.slice(2))}</li>`;
        continue;
      }

      closeList();

      if (trimmed.startsWith('### ')) {
        html += `<h4 class="mt-4 text-sm font-semibold text-slate-700">${this.renderInline(trimmed.slice(4))}</h4>`;
        continue;
      }

      if (trimmed.startsWith('## ')) {
        html += `<h3 class="mt-4 text-base font-semibold text-slate-700">${this.renderInline(trimmed.slice(3))}</h3>`;
        continue;
      }

      if (trimmed.startsWith('# ')) {
        html += `<h2 class="mt-4 text-lg font-semibold text-slate-700">${this.renderInline(trimmed.slice(2))}</h2>`;
        continue;
      }

      if (trimmed.startsWith('> ')) {
        html += `<blockquote class="mt-3 border-l-4 border-slate-300 pl-3 text-slate-500 italic">${this.renderInline(trimmed.slice(2))}</blockquote>`;
        continue;
      }

      html += `<p class="mt-3 text-sm leading-6 text-slate-500">${this.renderInline(trimmed)}</p>`;
    }

    closeList();

    return html;
  }

  private renderInline(value: string): string {
    return value
      .replace(/\*\*(.+?)\*\*/g, '<strong class="font-semibold text-slate-700">$1</strong>')
      .replace(/\*(.+?)\*/g, '<em class="italic">$1</em>')
      .replace(
        /\[([^\]]+)]\((https?:\/\/[^\s)]+|mailto:[^\s)]+)\)/g,
        '<a href="$2" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:underline">$1</a>',
      );
  }

  private escapeHtml(value: string): string {
    return value
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#039;');
  }
}
