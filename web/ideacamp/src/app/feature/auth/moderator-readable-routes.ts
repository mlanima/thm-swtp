export function isModeratorReadableRoute(url: string): boolean {
  const path = getPathWithoutQueryAndFragment(url);

  return isProjectDetailPage(path) || isUserProfilePage(path);
}

function getPathWithoutQueryAndFragment(url: string): string {
  return url.split('?')[0].split('#')[0];
}

function isProjectDetailPage(path: string): boolean {
  return hasExactlyOnePathParameter(path, '/project/');
}

function isUserProfilePage(path: string): boolean {
  return hasExactlyOnePathParameter(path, '/profiles/');
}

function hasExactlyOnePathParameter(path: string, prefix: string): boolean {
  if (!path.startsWith(prefix)) {
    return false;
  }

  const rest = path.slice(prefix.length);
  return rest.length > 0 && !rest.includes('/');
}
