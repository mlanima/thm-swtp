import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { ManagedReport, ReportStatus } from '../../models/report.model';

@Component({
  selector: 'app-report-action-menu',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './report-action-menu.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportActionMenuComponent {
  readonly report = input.required<ManagedReport>();

  readonly viewReport = output<ManagedReport>();
  readonly updateStatus = output<{ report: ManagedReport; status: ReportStatus }>();

  readonly menuButton = viewChild<ElementRef<HTMLButtonElement>>('menuButton');

  isOpen = signal(false);
  menuTop = signal(0);
  menuLeft = signal(0);

  toggleMenu(): void {
    if (this.isOpen()) {
      this.closeMenu();
      return;
    }

    this.openMenu();
  }

  openMenu(): void {
    const button = this.menuButton()?.nativeElement;

    if (!button) {
      return;
    }

    const rect = button.getBoundingClientRect();
    const menuWidth = 192;

    this.menuTop.set(rect.bottom + 8);
    this.menuLeft.set(rect.right - menuWidth);

    this.isOpen.set(true);
  }

  closeMenu(): void {
    this.isOpen.set(false);
  }

  emitView(): void {
    this.viewReport.emit(this.report());
    this.closeMenu();
  }

  emitStatus(status: ReportStatus): void {
    this.updateStatus.emit({
      report: this.report(),
      status,
    });
    this.closeMenu();
  }

  canSetInReview(): boolean {
    return this.report().status === 'OPEN';
  }

  canCloseReport(): boolean {
    return this.report().status === 'OPEN' || this.report().status === 'IN_REVIEW';
  }
}
