from pathlib import Path
import importlib.util
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('site_build', Path(__file__).parents[1] / 'scripts/site.py')
site = importlib.util.module_from_spec(spec)
spec.loader.exec_module(site)


class SiteChecks(unittest.TestCase):
    def test_project_relative_links_keep_query_and_fragment(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'styles.css').write_text('')
            self.assertEqual(site.local_file(root, 'styles.css?v=8'), (root / 'styles.css').resolve())
            self.assertIsNone(site.local_file(root, 'https://example.com/'))
            self.assertIsNone(site.local_file(root, 'data:,'))

    def test_missing_or_project_escaping_links_fail(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            for url in ['missing.png', '/assets/image.png', '../outside.txt', '%2e%2e/outside.txt', '//example.com/path']:
                with self.subTest(url=url), self.assertRaises(ValueError):
                    site.local_file(root, url)

    def test_download_metadata_detects_a_changed_binary(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'downloads').mkdir()
            jar = root / 'downloads/AutomataViz.jar'
            jar.write_bytes(b'PK\x03\x04example')
            checksum = site.sha256(jar.read_bytes()).hexdigest()
            (root / 'site.js').write_text('')
            (root / 'index.html').write_text(
                f'<code class="checksum">{checksum}</code><span data-download-size>0.0 MB</span>')
            site.check(root)
            jar.write_bytes(b'a different download')
            with self.assertRaisesRegex(ValueError, 'checksum'):
                site.check(root)

    def test_broken_anchor_and_gallery_asset_fail(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'index.html').write_text('<a href="#missing">Broken</a>')
            with self.assertRaisesRegex(ValueError, 'anchor'):
                site.check(root)
            (root / 'index.html').write_text('')
            (root / 'site.js').write_text("const view = {src: 'assets/missing.png'};")
            with self.assertRaisesRegex(ValueError, 'Missing local file'):
                site.check(root)

    def test_duplicate_ids_fail(self):
        with self.assertRaises(ValueError):
            site.Page('<div id="a"></div><p id="a"></p>')


if __name__ == '__main__':
    unittest.main()
