import { marked } from 'marked';
import { gfmHeadingId } from 'marked-gfm-heading-id';
import DOMPurify from 'dompurify';

// Gives headings GitHub-style slug ids (e.g. "## Usage" -> id="usage") so a README's own
// table-of-contents links (typically plain "#usage", not GitHub's "#user-content-usage")
// resolve to something once the "#fragment" rewrite below turns them into a real navigation.
marked.use(gfmHeadingId());

// Anything with a URI scheme (http:, mailto:, tel:, ...) or a protocol-relative "//" prefix
// is already absolute and left untouched; everything else is resolved against the repo.
const ABSOLUTE_URL_PATTERN = /^([a-z][a-z0-9+.-]*:|\/\/)/i;

function isRelative(url: string): boolean {
  if (!url || url.startsWith('#')) {
    return false;
  }
  return !ABSOLUTE_URL_PATTERN.test(url);
}

function stripLeadingPathMarkers(path: string): string {
  return path.replace(/^\.\//, '').replace(/^\//, '');
}

function toRawUrl(path: string, repoOwner: string, repoName: string, branch: string): string {
  return `https://raw.githubusercontent.com/${repoOwner}/${repoName}/${branch}/${stripLeadingPathMarkers(path)}`;
}

// A srcset is a comma-separated list of "URL [descriptor]" candidates; each relative URL is
// rewritten individually, keeping its descriptor (e.g. "2x", "480w") intact.
function rewriteSrcset(srcset: string, repoOwner: string, repoName: string, branch: string): string {
  return srcset
    .split(',')
    .map((candidate) => candidate.trim())
    .filter((candidate) => candidate.length > 0)
    .map((candidate) => {
      const [url, ...descriptorParts] = candidate.split(/\s+/);
      const resolved = isRelative(url) ? toRawUrl(url, repoOwner, repoName, branch) : url;
      return [resolved, ...descriptorParts].join(' ');
    })
    .join(', ');
}

/**
 * Renders a GitHub repo's raw README markdown into safe, displayable HTML:
 * 1. `marked` converts markdown (incl. GFM tables/task-lists) to HTML.
 * 2. `DOMPurify` sanitizes it — the only trust boundary for repo-owner-controlled content.
 * 3. Relative image/link paths (which GitHub's own renderer would resolve for us) are
 *    rewritten to absolute raw.githubusercontent.com / github.com URLs.
 * 4. Bare "#fragment" links (e.g. "back to top") are rewritten to include `currentPath` —
 *    the app's `<base href="/">` makes the browser resolve a bare "#fragment" against the
 *    app root instead of the current page, so the path is made explicit. That turns the
 *    click into a native same-document fragment navigation; no click-handling JS needed.
 */
export function renderGithubReadme(
  markdown: string,
  repoOwner: string,
  repoName: string,
  defaultBranch: string,
  currentPath: string,
): string {
  const rawHtml = marked.parse(markdown, { async: false }) as string;
  const sanitizedHtml = DOMPurify.sanitize(rawHtml);

  const doc = new DOMParser().parseFromString(sanitizedHtml, 'text/html');
  const branch = defaultBranch || 'main';

  doc.querySelectorAll('img[src]').forEach((img) => {
    const src = img.getAttribute('src') ?? '';
    if (isRelative(src)) {
      img.setAttribute('src', toRawUrl(src, repoOwner, repoName, branch));
    }
  });

  // Inside a <picture>, a matched <source media="..."> wins over the <img> src and does not
  // fall back on load error — a relative srcset left pointing at the app origin breaks the
  // image (typical GitHub READMEs use this for light/dark logo variants).
  doc.querySelectorAll('img[srcset], source[srcset]').forEach((element) => {
    const srcset = element.getAttribute('srcset') ?? '';
    if (srcset) {
      element.setAttribute('srcset', rewriteSrcset(srcset, repoOwner, repoName, branch));
    }
  });

  doc.querySelectorAll('a[href]').forEach((anchor) => {
    const href = anchor.getAttribute('href') ?? '';

    if (href.startsWith('#') && href.length > 1) {
      anchor.setAttribute('href', `${currentPath}${href}`);
      return;
    }

    if (isRelative(href)) {
      anchor.setAttribute(
        'href',
        `https://github.com/${repoOwner}/${repoName}/blob/${branch}/${stripLeadingPathMarkers(href)}`,
      );
    }
  });

  return doc.body.innerHTML;
}
