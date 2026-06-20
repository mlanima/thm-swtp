import { Component, Input,Output, EventEmitter } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import {ContactRequest} from '../contact-request.model'

@Component({
  selector: 'app-contact-request-box',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './contact-request-box.html',
})
export class ContactRequestBox {
  @Input({required: true}) request!: ContactRequest;

  @Output() accept = new EventEmitter<string>();
  @Output() reject = new EventEmitter<string>();


  get switchStatusStyle(){
    switch(this.request.status){
      case 'Accepted':
        return 'bg-green-100 text-green-700';
      case 'Rejected':
        return 'bg-red-100 text-red-700';
      case 'Open':
        return'bg-yellow-100 text-yellow-700';
      default:
        return;
    }
  }
  get openRequest(){
    return this.request.status === 'Open';
  }
}
