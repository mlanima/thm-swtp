import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, inject, SimpleChanges} from '@angular/core';
import { Subject, Subscription, catchError, debounceTime, distinctUntilChanged, finalize, of, switchMap} from 'rxjs';
import {FormsModule } from '@angular/forms'

import { ProjectInviteMember } from '../../../models/project-invite-member.model'
import { SearchService } from '../../search/services/search.service'
import {UserSearchResult} from '../../search/models/user-search-result.model'


@Component({
  selector: 'app-project-members-form',
  standalone : true,
  imports: [FormsModule],
  templateUrl: './project-members-form.html',
})

/** Third step of the project creation wizard.*/
export class ProjectMembersForm implements OnChanges, OnDestroy {

  private readonly searchService = inject(SearchService);
  private readonly searchTerms = new Subject<string>();
  private readonly searchSubscription: Subscription;


  @Input() initialMembers: ProjectInviteMember[] = [];
  @Output() next = new EventEmitter<ProjectInviteMember[]>();
  @Output() back = new EventEmitter<ProjectInviteMember[]>();

  members: ProjectInviteMember[] = [];

  isMemberDialogOpen = false;
  searchQuery = '';
  isSearching = false;
  isSearchFinished = false;

  searchResults : UserSearchResult[] = [];
  selectedUser : UserSearchResult | null = null;


  constructor(){
    this.searchSubscription = this.searchTerms.pipe(debounceTime(300), distinctUntilChanged(), switchMap(query => {
        const cleanedQuery = query.trim();
        this.selectedUser = null;
        this.searchResults = [];
        this.isSearchFinished = cleanedQuery.length > 0;

        if(!cleanedQuery){
          this.isSearching = false;
          return of([]);
        }

        this.isSearching = true;
        return this.searchService.searchUsers(cleanedQuery).pipe(
          catchError(() => of([])),
          finalize(() => {
            this.isSearching = false;
          })
        );
      })
    ).subscribe(users => {
      this.searchResults = users;
    });
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['initialMembers']) {
      this.members = [...this.initialMembers];
    }
  }

  ngOnDestroy() {
    this.searchSubscription.unsubscribe();
  }

  openDialog() {
    this.isMemberDialogOpen = true;
    this.searchQuery = '';
    this.searchResults = [];
    this.selectedUser = null;
    this.isSearchFinished = false;
    this.isSearching = false;
  }

  closeDialog() {
    this.isMemberDialogOpen = false;
    this.searchQuery = '';
    this.searchResults = [];
    this.selectedUser = null;
    this.isSearchFinished = false;
    this.isSearching = false;
  }

  searchUser(query: string){
    this.searchQuery = query;
    this.searchTerms.next(query);
  }

  selectUser(user: UserSearchResult){
    this.selectedUser = user;
  }

  addSelectedUser(){
    if(!this.selectedUser){
      return;
    }

    const member : ProjectInviteMember = {
      keycloakId: this.selectedUser.keycloakId,
      username: this.selectedUser.username,
      title: this.selectedUser.title,
      location: this.selectedUser.location
    };

    const alreadyAdded = this.members.some(existingMember => existingMember.keycloakId === member.keycloakId);

    if(!alreadyAdded){
      this.members = [...this.members,member];
    }
    this.closeDialog();
  }

  removeMember(keycloakId: string){
    this.members = this.members.filter(m => m.keycloakId !== keycloakId);
  }

  submit() {
    this.next.emit(this.members);
  }

  goBack() {
    this.back.emit(this.members)
  }

  isAlreadyAdded(user : UserSearchResult): boolean {
    return this.members.some(m => m.keycloakId === user.keycloakId)
  }

}
