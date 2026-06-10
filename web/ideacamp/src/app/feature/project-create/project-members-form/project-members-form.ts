import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, ViewChild } from '@angular/core';
import {FormsModule } from '@angular/forms';

import { ProjectInviteMember } from '../../../models/project-invite-member.model';
import {UserSearchResult} from '../../search/models/user-search-result.model';
import { UserSearchPick } from '../../../shared/user-search-pick/user-search-pick';
import { UserProfileService } from '../../../services/user-profile.service';


@Component({
  selector: 'app-project-members-form',
  standalone : true,
  imports: [FormsModule, UserSearchPick],
  templateUrl: './project-members-form.html',
})

/** Third step of the project creation wizard.*/
export class ProjectMembersForm implements OnChanges {
  private readonly userProfileService = inject(UserProfileService);

  @ViewChild('userSearchPick') userSearchPick?: UserSearchPick;

  @Input() initialMembers: ProjectInviteMember[] = [];
  @Output() next = new EventEmitter<ProjectInviteMember[]>();
  @Output() back = new EventEmitter<ProjectInviteMember[]>();

  currentUserKeycloakId: string | null = null;

  members: ProjectInviteMember[] = [];
  isMemberDialogOpen = false;
  selectedUser : UserSearchResult | null = null;


  /** Initializes the current user lookup and the user search subscription. */
  constructor(){
    this.loadCurrentUserProfile();
  }

  /** Updates the member list when a new member is added. */
  ngOnChanges(changes: SimpleChanges) {
    if (changes['initialMembers']) {
      this.members = [...this.initialMembers];
    }
  }

  /** Opens the add-member dialog and resets the search state. */
  openDialog() {
    this.isMemberDialogOpen = true;
    this.selectedUser = null;
  }

  /** Closes the add-member dialog and resets the search state. */
  closeDialog() {
    this.isMemberDialogOpen = false;
    this.selectedUser = null;
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

  /** Emits the selected members and moves to the next wizard step. */
  submit() {
    this.next.emit(this.members);
  }

  /** Emits the selected members and moves back to the previous wizard step. */
  goBack() {
    this.back.emit(this.members)
  }

  get excludedUserIds(): string[] {
    return [...(this.currentUserKeycloakId ? [this.currentUserKeycloakId] : []),
      ...this.members.map((mem => mem.keycloakId)),
    ];
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

}
