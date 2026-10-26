"""Validate release metadata and package the already-tested Maven artifact."""
from hashlib import sha256
from pathlib import Path
import os
import re
import shutil
import sys
import xml.etree.ElementTree as ET
from zipfile import ZipFile, is_zipfile

VERSION = re.compile(r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?')


def metadata(root, ref):
    version = ET.parse(root / 'pom.xml').getroot().findtext('{http://maven.apache.org/POM/4.0.0}version')
    if version and version.endswith('-SNAPSHOT'):
        if ref.startswith('refs/tags/'):
            raise ValueError('Snapshot versions cannot be tagged as releases')
        return {'publish': 'false'}
    match = VERSION.fullmatch(version or '')
    if not match or (match[4] and any(part.isdigit() and len(part) > 1 and part[0] == '0' for part in match[4].split('.'))):
        raise ValueError('Use a semantic release version such as 1.2.0 or 1.2.0-rc.1 in pom.xml')
    tag = f'v{version}'
    if ref.startswith('refs/tags/') and ref != f'refs/tags/{tag}':
        raise ValueError(f'Tag does not match pom.xml; expected {tag}')
    return {'publish': 'true', 'version': version, 'tag': tag, 'prerelease': str(bool(match[4])).lower()}


def package(root, version):
    if not VERSION.fullmatch(version):
        raise ValueError('Invalid release version')
    source = root / 'target' / f'AutomataViz-{version}-jar-with-dependencies.jar'
    if not is_zipfile(source):
        raise ValueError('Build the dependency-bundled JAR before packaging')
    with ZipFile(source) as jar:
        properties = jar.read('META-INF/maven/com.dfavisualizer/AutomataViz/pom.properties').decode()
        if not re.search(rf'^version={re.escape(version)}$', properties, re.MULTILINE):
            raise ValueError('The JAR version does not match the release')
    output = root / 'release'
    output.mkdir(exist_ok=True)
    target = output / source.name
    shutil.copyfile(source, target)
    (output / 'SHA256SUMS.txt').write_text(f'{sha256(target.read_bytes()).hexdigest()}  {target.name}\n')
    instructions = (f'AutomataViz {version}\n\nRequires a desktop Java 11 or newer runtime.\n'
                    f'Dependencies are included. Maven is not needed to run.\n\n'
                    f'Run: java -jar {target.name}\n\n'
                    'Verify the download on Linux: sha256sum -c SHA256SUMS.txt\n'
                    'On macOS: shasum -a 256 -c SHA256SUMS.txt\n')
    (output / 'README.txt').write_text(instructions)
    changelog = (root / 'CHANGELOG.md').read_text()
    section = re.search(rf'^## {re.escape(version)}(?:[ \t][^\n]*)?\n(.*?)(?=^## |\Z)', changelog, re.MULTILINE | re.DOTALL)
    notes = (section[1].strip() + '\n\n') if section else ''
    (output / 'RELEASE_NOTES.md').write_text(notes + instructions)
    return output


if __name__ == '__main__':
    root = Path(__file__).resolve().parents[1]
    if sys.argv[1:] == ['metadata']:
        values = metadata(root, os.environ['GITHUB_REF'])
        with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
            for key, value in values.items():
                output.write(f'{key}={value}\n')
    elif len(sys.argv) == 3 and sys.argv[1] == 'package':
        package(root, sys.argv[2])
    else:
        raise SystemExit('Usage: release.py metadata | package VERSION')
