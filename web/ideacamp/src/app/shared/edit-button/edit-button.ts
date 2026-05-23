import { Component, Output, EventEmitter } from '@angular/core';

/** Button with Icon. Used to change User-profile fields.*/
@Component({
  selector: 'app-edit-button',
  standalone: true,
  imports: [],
  templateUrl: './edit-button.html',
})
export class EditButton {
  @Output() edit = new EventEmitter<void>();
}
