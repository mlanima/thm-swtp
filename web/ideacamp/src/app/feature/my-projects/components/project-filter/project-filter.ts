import {
  AfterViewChecked,
  Component,
  input,
  output,
  ViewChild,
  ElementRef,
  AfterViewInit,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-project-filter',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './project-filter.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectFilter implements AfterViewInit, AfterViewChecked {
  readonly activeFilter = input.required<'my' | 'all'>();
  readonly filterChange = output<'my' | 'all'>();

  @ViewChild('btnAll') btnAll!: ElementRef<HTMLButtonElement>;
  @ViewChild('btnMy') btnMy!: ElementRef<HTMLButtonElement>;
  @ViewChild('pillContainer') pillContainer!: ElementRef<HTMLDivElement>;

  readonly pillLeft = signal(0);
  readonly pillWidth = signal(0);

  private previousFilter: 'my' | 'all' | null = null;

  ngAfterViewInit(): void {
    this.updatePillPosition();
  }

  ngAfterViewChecked(): void {
    if (this.previousFilter !== this.activeFilter()) {
      this.previousFilter = this.activeFilter();
      this.updatePillPosition();
    }
  }

  setFilter(filter: 'my' | 'all'): void {
    this.filterChange.emit(filter);
  }

  private updatePillPosition(): void {
    const container = this.pillContainer?.nativeElement;
    const activeBtn =
      this.activeFilter() === 'all' ? this.btnAll?.nativeElement : this.btnMy?.nativeElement;
    if (!container || !activeBtn) {
      return;
    }
    const containerRect = container.getBoundingClientRect();
    const btnRect = activeBtn.getBoundingClientRect();
    this.pillLeft.set(btnRect.left - containerRect.left);
    this.pillWidth.set(btnRect.width);
  }
}
