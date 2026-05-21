import { TestBed } from '@angular/core/testing';

import { SidebarService } from './sidebar.service';

describe('SidebarService', () => {
  let service: SidebarService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SidebarService);
    service.close();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should be closed by default', () => {
    expect(service.isOpen()).toBe(false);
  });

  it('should open the sidebar', () => {
    service.open();

    expect(service.isOpen()).toBe(true);
  });

  it('should close the sidebar', () => {
    service.open();
    service.close();

    expect(service.isOpen()).toBe(false);
  });
});

