export function isModeratorReadableRoute(url: string): boolean {
  const path = getPathWithoutQueryAndFragment(url);

  return isProjectDetailPage(path) || isUserProfilePage(path);
}

function getPathWithoutQueryAndFragment(url: string): string {
  return url.split('?')[0].split('#')[0];
}

function isProjectDetailPage(path: string): boolean {
  return hasPathParameter(path, '/project/');
}

function isUserProfilePage(path: string): boolean {
  return hasPathParameter(path, '/profiles/');
}

function hasPathParameter(path: string, prefix: string): boolean {
  return path.startsWith(prefix) && path.length > prefix.length;
}
