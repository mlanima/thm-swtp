import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [

  {
    path: 'profile',
    renderMode: RenderMode.Client,
  },

  {
    path: 'contact-requests',
    renderMode: RenderMode.Client,
  },

  {
    path: 'searchProject',
    renderMode: RenderMode.Client,
  },

  {
    path: 'project',
    renderMode: RenderMode.Client,
  },

  {
    path: 'success',
    renderMode: RenderMode.Client,
  },

  {
    path: '**',
    renderMode: RenderMode.Prerender,
  }
];
