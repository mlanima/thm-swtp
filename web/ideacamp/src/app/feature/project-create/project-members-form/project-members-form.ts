import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, inject, SimpleChanges} from '@angular/core';
import { Subject, Subscription, catchError, debounceTime, distinctUntilChanged, finalize, of, switchMap} from 'rxjs';
import {FormsModule } from '@angular/forms'

import { ProjectInviteMember } from '../../../models/project-invite-member.model'
import { SearchService } from '../../search/services/search.service'
import {UserSearchResult} from '../../search/models/user-search-result.model'
import {UserProfileService} from '../../../services/user-profile.service'


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
  private readonly userProfileService = inject(UserProfileService);
  currentUserKeycloakId: string | null = null;



  @Input() initialMembers: ProjectInviteMember[] = [];
  @Output() next = new EventEmitter<ProjectInviteMember[]>();
  @Output() back = new EventEmitter<ProjectInviteMember[]>();

  members: ProjectInviteMember[] = [];

  isMemberDialogOpen = false;
  searchQuery = '';
  isSearching = false;
  hasSearched = false;

  searchResults : UserSearchResult[] = [];
  selectedUser : UserSearchResult | null = null;


  /** Initializes the current user lookup and the user search subscription. */
  constructor(){
    this.loadCurrentUserProfile();
    this.searchSubscription =  this.createSearchSubscription();
  }

  /** Updates the member list when a new member is added. */
  ngOnChanges(changes: SimpleChanges) {
    if (changes['initialMembers']) {
      this.members = [...this.initialMembers];
    }
  }

  /** Cleans up the search subscription*/
  ngOnDestroy() {
    this.searchSubscription.unsubscribe();
  }

  /** Opens the add-member dialog and resets the search state. */
  openDialog() {
    this.isMemberDialogOpen = true;
    this.resetDialogSearch()
  }

  /** Closes the add-member dialog and resets the search state. */
  closeDialog() {
    this.isMemberDialogOpen = false;
    this.resetDialogSearch()
  }

  /** Updates the search query and starts the user search. */
  searchUser(query: string){
    this.searchQuery = query;
    this.searchTerms.next(query);
  }

  /** Stores the selected user for the confirmation to add the user*/
  selectUser(user: UserSearchResult){
    this.selectedUser = user;
  }

  /** Adds the selected user to the invite list if not added yet. */
  addSelectedUser(){
    if(!this.selectedUser){
      return;
    }

    const member = this.toProjectInviteMember(this.selectedUser);
    if(!this.isMemberAlreadyAdded(member.keycloakId)){
      this.members = [...this.members,member];
    }
    this.closeDialog();
  }

  /** Removes the selected member from the invitation list. */
  removeMember(keycloakId: string){
    this.members = this.members.filter(m => m.keycloakId !== keycloakId);
  }
  isAlreadyAdded(user : UserSearchResult): boolean {
    return this.isMemberAlreadyAdded(user.keycloakId);
  }

  /** Emits the selected members and moves to the next wizard step. */
  submit() {
    this.next.emit(this.members);
  }

  /** Emits the selected members and moves back to the previous wizard step. */
  goBack() {
    this.back.emit(this.members)
  }



  private resetDialogSearch(){
    this.searchQuery = '';
    this.searchResults = [];
    this.selectedUser = null;
    this.hasSearched = false;
    this.isSearching = false;
  }

  private toProjectInviteMember(user : UserSearchResult) : ProjectInviteMember{
    return {
      keycloakId: user.keycloakId,
      username: user.username,
      title: user.title,
      location: user.location
    };
  }

  private isMemberAlreadyAdded(keycloakId : string) {
    return this.members.some(mem => mem.keycloakId === keycloakId);
  }

  private loadCurrentUserProfile(){
    this.userProfileService.getMyProfile().subscribe({
      next : profile => {
        this.currentUserKeycloakId = profile.keycloakId;
      },
      error: () => {
        this.currentUserKeycloakId = null;
      },
    });
  }

  private searchUsers(query: string){
    const cleanedQuery = query.trim();
    this.selectedUser = null;
    this.searchResults = [];
    this.hasSearched = cleanedQuery.length > 0;

    if(cleanedQuery.length < 2){
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
  }

  private isCurrentUser(user : UserSearchResult): boolean {
    return user.keycloakId === this.currentUserKeycloakId;
  }

  private filterSearchResults(users : UserSearchResult[]) : UserSearchResult[] {
    const cleanedQuery = this.searchQuery.trim().toLowerCase();

    return users.filter(user => !this.isCurrentUser(user) && user.username.toLowerCase().includes(cleanedQuery));
  }

  private createSearchSubscription() {
    return this.searchTerms.pipe(debounceTime(250), distinctUntilChanged(), switchMap(query => this.searchUsers(query)))
      .subscribe((user: UserSearchResult[]) => {
        this.searchResults = this.filterSearchResults(user);
      });
  }

}
