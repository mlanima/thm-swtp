import { Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import {ContactRequestBox} from '../../components/contact-request-box/contact-request-box'
import {ContactRequest} from '../../components/contact-request.model';
@Component({
  selector: 'app-contact-request',
  standalone: true,
  imports: [ContactRequestBox, TranslatePipe],
  templateUrl: './contact-requests.html',
})
export class ContactRequests {
  requests: ContactRequest[] = [
    {
      id: 'abc123',
      senderId : '1',
      senderName: 'Dummy User',
      projectId : 'pro1234',
      projectName: 'Softwaretechnik Realisierung',
      message: 'Hallo, ich möchte deinem Projekt gerne beitreten.',
      status: 'Open',
      date: '21.05.2026',
    },
    {
      id: 'def456',
      senderId : '2',
      senderName: 'Chris',
      projectId: 'pro1234',
      projectName: 'Softwaretechnik Realisierung',
      message: 'Hi, hast du noch Platz in deinem Projekt? Ich möchte gerne beitreten. Ich bin Backend-Entwickler.',
      status: 'Accepted',
      date: '20.05.2026',
    },
    {
      id: 'ghi789',
      senderId : '3',
      senderName: 'dummy user2',
      projectId: 'pro1234',
      projectName: 'Softwaretechnik Realisierung',
      message: 'Hi, hast du noch Platz in deinem Projekt? Ich möchte gerne beitreten. Ich bin Frontend-Entwickler.',
      status: 'Rejected',
      date: '20.05.2026',
    }
  ]

  get openRequests() {
    return this.requests.filter(req => req.status === 'Open');
  }

  get processedRequests() {
    return this.requests.filter(req => req.status !== 'Open')
  }
}
