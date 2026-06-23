import { Observable } from 'rxjs';
import { LinkVisibility } from '../../models/link-visibility.model';

export interface LinkManager {
  id: string,
  label : string,
  url : string
  visibility?: LinkVisibility
}

export interface CreateLinkRequest {
  label: string,
  url: string,
  visibility?: LinkVisibility
}

export interface UpdateLinkRequest {
  label?: string,
  url?: string,
  visibility?: LinkVisibility
}

export interface LinkManagerDataSource<TLink extends LinkManager = LinkManager>{
  load(): Observable<TLink[]>;
  createLink(request: CreateLinkRequest): Observable<TLink>;
  updateLink(linkId: string, request: UpdateLinkRequest): Observable<TLink>;
  deleteLink(linkId: string): Observable<void>;
}
