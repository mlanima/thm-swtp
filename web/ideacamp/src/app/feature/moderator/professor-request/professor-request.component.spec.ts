import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs'
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ProfessorRequestComponent } from './professor-request.component';
import { ModeratorProfessorRequestService } from './service/moderator-professor-request.service';

describe('ProfessorRequestComponent', () => {
  let component: ProfessorRequestComponent;
  let fixture: ComponentFixture<ProfessorRequestComponent>;

  const professorRequestServiceMock = {
    getRequests: () =>
      of({
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
      }),
    acceptRequest: () => of({}),
    rejectRequest: () => of({}),
  };

  const translateServiceMock = {
    instant: (key: string) => key,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfessorRequestComponent],
      providers: [
        {
          provide: ModeratorProfessorRequestService,
          useValue: professorRequestServiceMock,
        },
        {
          provide: TranslateService,
          useValue: translateServiceMock,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProfessorRequestComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
