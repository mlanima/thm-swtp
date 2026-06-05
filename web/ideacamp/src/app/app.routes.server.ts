import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [

  {
    path: 'profiles/:username',
    renderMode: RenderMode.Client,
  },

  {
    path: 'contact-requests',
    renderMode: RenderMode.Client,
  },

  {
    path: 'search',
    renderMode: RenderMode.Client,
  },

  {
    path: 'createProject',
    renderMode: RenderMode.Client,
  },

  {
    path: 'project/:id',
    renderMode: RenderMode.Client,
  },

  {
    path: 'success',
    renderMode: RenderMode.Client,
  },

  {
    path: 'my-projects',
    renderMode: RenderMode.Client,
  },

  {
    path: 'project/:projectUrl/settings',
    renderMode: RenderMode.Client,
  },

  {
    path: '**',
    renderMode: RenderMode.Prerender,
  }
];
