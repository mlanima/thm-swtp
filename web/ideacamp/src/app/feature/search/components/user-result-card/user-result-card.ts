import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UserSearchResult } from '../../models/user-search-result.model';

@Component({
  selector: 'app-user-result-card',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './user-result-card.html',
})
export class UserResultCard {
  readonly user = input.required<UserSearchResult>();

  get initials(): string {
    return this.user().username.slice(0, 2).toUpperCase();
  }
}
