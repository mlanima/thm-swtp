import { Component, EventEmitter, Input, Output} from '@angular/core';

@Component({
  selector: 'app-success-modal',
  standalone: true,
  templateUrl: './success-modal.html',
})
  export class SuccessModal {
  @Input() message = '';
  @Input() buttonText = '';

  @Output() closeModal = new EventEmitter<void>();

  close(): void {
    this.closeModal.emit();
  }
}
