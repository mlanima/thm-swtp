import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class SidebarService {
  isOpen = signal(false);
  pendingInvitationsCount = signal(0);
  unreadThesisNotificationsCount = signal(0);

  private unreadThesisNotificationsSequence = 0;

  /** Reserves a sequence number for an in-flight update to unreadThesisNotificationsCount. */
  nextUnreadThesisNotificationsSequence(): number {
    return ++this.unreadThesisNotificationsSequence;
  }

  /** Applies the count only if no newer update has been reserved since, so a slower stale response can't overwrite a more recent one. */
  setUnreadThesisNotificationsCount(count: number, sequence: number) {
    if (sequence === this.unreadThesisNotificationsSequence) {
      this.unreadThesisNotificationsCount.set(count);
    }
  }

  open() {
    this.isOpen.set(true);
  }

  close() {
    this.isOpen.set(false);
  }
}
