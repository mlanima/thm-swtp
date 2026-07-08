import { describe, it, expect } from 'vitest';
import { renderGithubReadme } from './github-readme-renderer';

const CURRENT_PATH = '/project/thm-swtp';

describe('renderGithubReadme', () => {
  it('renders basic markdown to HTML', () => {
    const html = renderGithubReadme('# Title\n\nSome **bold** text.', 'mlanima', 'thm-swtp', 'main', CURRENT_PATH);

    expect(html).toContain('<h1');
    expect(html).toContain('Title');
    expect(html).toContain('<strong>bold</strong>');
  });

  it('renders GFM tables and task lists', () => {
    const markdown = '| A | B |\n| - | - |\n| 1 | 2 |\n\n- [x] done\n- [ ] todo';
    const html = renderGithubReadme(markdown, 'mlanima', 'thm-swtp', 'main', CURRENT_PATH);

    expect(html).toContain('<table>');
    expect(html).toContain('type="checkbox"');
  });

  it('rewrites a relative image path to an absolute raw.githubusercontent.com URL', () => {
    const html = renderGithubReadme('![logo](docs/logo.png)', 'mlanima', 'thm-swtp', 'main', CURRENT_PATH);

    expect(html).toContain('src="https://raw.githubusercontent.com/mlanima/thm-swtp/main/docs/logo.png"');
  });

  it('rewrites a relative link to an absolute github.com blob URL', () => {
    const html = renderGithubReadme('[docs](docs/guide.md)', 'mlanima', 'thm-swtp', 'develop', CURRENT_PATH);

    expect(html).toContain('href="https://github.com/mlanima/thm-swtp/blob/develop/docs/guide.md"');
  });

  it('leaves absolute URLs untouched', () => {
    const html = renderGithubReadme(
      '![external](https://example.com/img.png) [site](https://example.com)',
      'mlanima',
      'thm-swtp',
      'main',
      CURRENT_PATH,
    );

    expect(html).toContain('src="https://example.com/img.png"');
    expect(html).toContain('href="https://example.com"');
  });

  it('rewrites a bare "#fragment" link to include the current page path', () => {
    // <base href="/"> makes a bare "#fragment" resolve against the app root and navigate
    // away instead of scrolling in place, so the current path must be made explicit.
    const html = renderGithubReadme('[back to top](#readme-top)', 'mlanima', 'thm-swtp', 'main', CURRENT_PATH);

    expect(html).toContain('href="/project/thm-swtp#readme-top"');
  });

  it('gives headings GitHub-style slug ids so table-of-contents links actually resolve', () => {
    const markdown = '# Usage\n\nSome text.\n\n[Jump to usage](#usage)';
    const html = renderGithubReadme(markdown, 'mlanima', 'thm-swtp', 'main', CURRENT_PATH);

    expect(html).toContain('id="usage"');
    expect(html).toContain(`href="${CURRENT_PATH}#usage"`);
  });

  it('slugifies a multi-word heading the same way GitHub does', () => {
    const html = renderGithubReadme('## Getting Started', 'mlanima', 'thm-swtp', 'main', CURRENT_PATH);

    expect(html).toContain('id="getting-started"');
  });

  it('leaves a mailto link untouched', () => {
    const html = renderGithubReadme('[mail](mailto:test@example.com)', 'mlanima', 'thm-swtp', 'main', CURRENT_PATH);

    expect(html).toContain('href="mailto:test@example.com"');
  });

  it('strips a script tag injected via markdown', () => {
    const html = renderGithubReadme('<script>alert(1)</script>\n\n# Title', 'mlanima', 'thm-swtp', 'main', CURRENT_PATH);

    expect(html).not.toContain('<script');
    expect(html).not.toContain('alert(1)');
  });

  it('strips an inline event-handler XSS payload', () => {
    const html = renderGithubReadme('<img src=x onerror="alert(1)">', 'mlanima', 'thm-swtp', 'main', CURRENT_PATH);

    expect(html).not.toContain('onerror');
    expect(html).not.toContain('alert(1)');
  });

  it('falls back to "main" when defaultBranch is empty', () => {
    const html = renderGithubReadme('![logo](logo.png)', 'mlanima', 'thm-swtp', '', CURRENT_PATH);

    expect(html).toContain('/mlanima/thm-swtp/main/logo.png');
  });
});
