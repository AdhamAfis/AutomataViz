const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { runInNewContext } = require('node:vm');

test('gallery keeps selection, image, caption and full-size links synchronized', () => {
  const element = (dataset = {}) => ({
    dataset, attrs: {}, listeners: {}, hidden: true,
    setAttribute(key, value) { this.attrs[key] = value; },
    addEventListener(event, callback) { this.listeners[event] = callback; }
  });
  const buttons = ['workspace', 'learning', 'alternatives', 'playback', 'rejection'].map(view => element({ view }));
  const link = element({ show: 'learning' });
  const nodes = Object.fromEntries([
    '#copy-command', '#gallery-image', '#gallery-full', '.full-size',
    '#gallery-caption', '.gallery-choices'
  ].map(key => [key, element()]));
  const document = {
    querySelector: selector => nodes[selector],
    querySelectorAll: selector => selector === '[data-view]' ? buttons : [link]
  };
  runInNewContext(readFileSync(`${__dirname}/../dist/site.js`, 'utf8'), { document });
  assert.equal(nodes['.gallery-choices'].hidden, false);
  for (const button of buttons) {
    button.listeners.click();
    assert.equal(button.attrs['aria-pressed'], 'true');
    assert.equal(buttons.filter(b => b.attrs['aria-pressed'] === 'true').length, 1);
    assert.equal(nodes['#gallery-image'].src, nodes['#gallery-full'].href);
    assert.equal(nodes['.full-size'].href, nodes['#gallery-image'].src);
    assert.ok(nodes['#gallery-image'].alt.length > 20);
    assert.ok(nodes['#gallery-caption'].textContent.length > 20);
    assert.ok(nodes['#gallery-image'].width > 0 && nodes['#gallery-image'].height > 0);
  }
  link.listeners.click();
  assert.equal(buttons[1].attrs['aria-pressed'], 'true');
  assert.equal(nodes['#gallery-image'].src, 'assets/learning-mac.png');
});
