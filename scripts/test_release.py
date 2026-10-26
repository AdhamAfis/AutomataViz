from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile
from hashlib import sha256
import release


class ReleaseChecks(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def pom(self, version):
        (self.root / 'pom.xml').write_text(f'<project xmlns="http://maven.apache.org/POM/4.0.0"><version>{version}</version></project>')

    def test_stable_and_prerelease(self):
        for version, prerelease in [('1.2.0', 'false'), ('2.0.0-rc.1', 'true')]:
            self.pom(version)
            result = release.metadata(self.root, 'refs/heads/main')
            self.assertEqual(result['tag'], f'v{version}')
            self.assertEqual(result['prerelease'], prerelease)

    def test_snapshots_skip_and_mismatched_tags_fail(self):
        self.pom('1.3.0-SNAPSHOT')
        self.assertEqual(release.metadata(self.root, 'refs/heads/main'), {'publish': 'false'})
        with self.assertRaises(ValueError):
            release.metadata(self.root, 'refs/tags/v1.3.0')
        with self.assertRaises(ValueError):
            release.metadata(self.root, 'refs/tags/v1.3.0-SNAPSHOT')
        self.pom('1.2.0')
        with self.assertRaises(ValueError):
            release.metadata(self.root, 'refs/tags/v9.0.0')
        self.assertEqual(release.metadata(self.root, 'refs/tags/v1.2.0')['publish'], 'true')

    def test_invalid_versions_fail(self):
        for version in ['1.2', '01.2.0', '1.2.0-rc.01', '1.2.0\nmalicious=value']:
            self.pom(version)
            with self.assertRaises(ValueError):
                release.metadata(self.root, 'refs/heads/main')

    def test_packaging_checks_version_and_checksum(self):
        (self.root / 'target').mkdir()
        source = self.root / 'target/AutomataViz-1.2.0-jar-with-dependencies.jar'
        with ZipFile(source, 'w') as jar:
            jar.writestr('META-INF/maven/com.dfavisualizer/AutomataViz/pom.properties', 'version=1.2.0\n')
        (self.root / 'CHANGELOG.md').write_text('# Changelog\n\n## 1.2.0 — 2026-10-26\n\n- New lesson.\n\n## 1.1.0\n\n- Older.\n')
        output = release.package(self.root, '1.2.0')
        self.assertEqual((output / source.name).read_bytes(), source.read_bytes())
        self.assertEqual((output / 'SHA256SUMS.txt').read_text(), f'{sha256(source.read_bytes()).hexdigest()}  {source.name}\n')
        notes = (output / 'RELEASE_NOTES.md').read_text()
        self.assertIn('New lesson.', notes)
        self.assertNotIn('Older.', notes)
        self.assertIn(f'java -jar {source.name}', (output / 'README.txt').read_text())
        with ZipFile(source, 'w') as jar:
            jar.writestr('META-INF/maven/com.dfavisualizer/AutomataViz/pom.properties', 'version=9.0.0\n')
        with self.assertRaisesRegex(ValueError, 'version'):
            release.package(self.root, '1.2.0')


if __name__ == '__main__':
    unittest.main()
