document.querySelector('#copy-command').addEventListener('click', async () => {
  const status = document.querySelector('#copy-status');
  try {
    await navigator.clipboard.writeText(document.querySelector('#launch-command').textContent);
    status.textContent = 'Launch command copied.';
  } catch {
    const selection = window.getSelection();
    const range = document.createRange();
    range.selectNodeContents(document.querySelector('#launch-command'));
    selection.removeAllRanges(); selection.addRange(range);
    status.textContent = 'Command selected. Use your usual Copy shortcut.';
  }
});

const screenshotViews = {
  workspace: {
    src: 'assets/workspace-mac.png', width: 2624, height: 1864,
    alt: 'AutomataViz on macOS in the light theme, showing the DFA for (a|b)*aba',
    caption: 'Workspace: inspect the DFA for strings ending in aba.'
  },
  learning: {
    src: 'assets/learning-mac.png', width: 2224, height: 1704,
    alt: 'The macOS learning walkthrough showing NFA state q1 after reading a from input ab',
    caption: 'Learning walkthrough: read a, then take ε-closure before reading the next symbol.'
  },
  alternatives: {
    src: 'assets/alternatives-mac.png', width: 1744, height: 1104,
    alt: 'The macOS Regex alternatives dialog showing two expressions verified to recognize the same language',
    caption: 'Regex alternatives: compare expression length and readability with verified equivalence.'
  },
  playback: {
    src: 'assets/playback-mac.png', width: 2624, height: 1864,
    alt: 'Input playback paused after four of five symbols in abaab, with q2 active and the next b transition visible',
    caption: 'Input playback: four symbols consumed, with the next transition highlighted.'
  }
};
function showScreenshot(key) {
  const view = screenshotViews[key];
  if (!view) return;
  const picture = document.querySelector('#gallery-image');
  picture.src = view.src;
  picture.alt = view.alt;
  picture.width = view.width;
  picture.height = view.height;
  document.querySelector('#gallery-full').href = view.src;
  document.querySelector('#gallery-full').setAttribute('aria-label', `Open full-size ${key} screenshot`);
  document.querySelector('.full-size').href = view.src;
  document.querySelector('#gallery-caption').textContent = view.caption;
  for (const button of document.querySelectorAll('[data-view]')) {
    button.setAttribute('aria-pressed', String(button.dataset.view === key));
  }
}
document.querySelector('.gallery-choices').hidden = false;
for (const button of document.querySelectorAll('[data-view]')) {
  button.addEventListener('click', () => showScreenshot(button.dataset.view));
}
