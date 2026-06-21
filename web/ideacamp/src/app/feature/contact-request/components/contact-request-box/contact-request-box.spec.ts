import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTranslateTesting } from '../../../../testing/translate-testing.provider';
import { ContactRequestBox } from './contact-request-box';

describe('ContactRequestBox', () => {
  let component: ContactRequestBox;
  let fixture: ComponentFixture<ContactRequestBox>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ContactRequestBox],
      providers: [
        provideTranslateTesting(),
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ContactRequestBox);
    component = fixture.componentInstance;
    // Provide a minimal input so the template can render during the test.
    component.request = {
      id: 'r1',
      senderId: 's1',
      senderName: 'Alice',
      projectId: 'p1',
      projectName: 'Project X',
      message: 'Hello',
      status: 'Open',
      date: '2026-01-01',
    };

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
