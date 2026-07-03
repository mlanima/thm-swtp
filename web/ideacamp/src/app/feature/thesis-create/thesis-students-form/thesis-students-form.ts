import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, ViewChild } from '@angular/core';
import {FormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { ProjectInviteMember } from '../../../models/project-invite-member.model';
import {UserSearchResult} from '../../search/models/user-search-result.model';
import { UserSearchPick } from '../../../shared/user-search-pick/user-search-pick';
import { UserProfileService } from '../../../services/user-profile.service';


@Component({
  selector: 'app-thesis-students-form',
  standalone : true,
  imports: [FormsModule, UserSearchPick, TranslatePipe],
  templateUrl: './thesis-students-form.html',
})

/** Third step of the thesis creation wizard.
 */
export class ThesisStudentsForm implements OnChanges {
  private readonly userProfileService = inject(UserProfileService);

  @ViewChild('userSearchPick') userSearchPick?: UserSearchPick;

  @Input() initialStudents: ProjectInviteMember[] = [];
  @Output() next = new EventEmitter<ProjectInviteMember[]>();
  @Output() back = new EventEmitter<ProjectInviteMember[]>();

  currentUserKeycloakId: string | null = null;

  students: ProjectInviteMember[] = [];
  isStudentDialogOpen = false;
  selectedUser : UserSearchResult | null = null;


  /** Initializes the current user lookup and the user search subscription. */
  constructor(){
    this.loadCurrentUserProfile();
  }

  /** Updates the student list when a new student is added. */
  ngOnChanges(changes: SimpleChanges) {
    if (changes['initialStudents']) {
      this.students = [...this.initialStudents];
    }
  }

  /** Opens the add-student dialog and resets the search state. */
  openDialog() {
    this.isStudentDialogOpen = true;
    this.selectedUser = null;
  }

  /** Closes the add-student dialog and resets the search state. */
  closeDialog() {
    this.isStudentDialogOpen = false;
    this.selectedUser = null;
  }

  /** Stores the selected user for the confirmation to add the user*/
  selectUser(user: UserSearchResult){
    this.selectedUser = user;
  }

  /** Adds the selected user to the student list if not added yet. */
  addSelectedUser(){
    if(!this.selectedUser){
      return;
    }

    const student = this.toProjectInviteMember(this.selectedUser);
    if(!this.isStudentAlreadyAdded(student.keycloakId)){
      this.students = [...this.students, student];
    }
    this.closeDialog();
  }

  /** Removes the selected student from the list. */
  removeStudent(keycloakId: string){
    this.students = this.students.filter(s => s.keycloakId !== keycloakId);
  }

  /** Emits the selected students and moves to the next wizard step. */
  submit() {
    this.next.emit(this.students);
  }

  /** Emits the selected students and moves back to the previous wizard step. */
  goBack() {
    this.back.emit(this.students)
  }

  get excludedUserIds(): string[] {
    return [...(this.currentUserKeycloakId ? [this.currentUserKeycloakId] : []),
      ...this.students.map((student => student.keycloakId)),
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

  private isStudentAlreadyAdded(keycloakId : string) {
    return this.students.some(student => student.keycloakId === keycloakId);
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
