from hashlib import sha256
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit
import re
import shutil
import sys

WEBSITE = Path(__file__).resolve().parents[1]
OUTPUT = WEBSITE / 'build'


class Page(HTMLParser):
    def __init__(self, text):
        super().__init__()
        self.ids = set()
        self.links = []
        self.feed(text)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if attrs.get('id'):
            if attrs['id'] in self.ids:
                raise ValueError(f"Duplicate HTML id: {attrs['id']}")
            self.ids.add(attrs['id'])
        for key in ('href', 'src'):
            if attrs.get(key):
                self.links.append(attrs[key])


def local_file(root, url):
    parts = urlsplit(url)
    if parts.scheme in ('https', 'http', 'mailto', 'data'):
        return None
    if parts.scheme or parts.netloc or parts.path.startswith('/'):
        raise ValueError(f'Use project-relative URLs for GitHub Pages: {url}')
    path = (root / unquote(parts.path or 'index.html')).resolve()
    if not path.is_relative_to(root.resolve()):
        raise ValueError(f'URL leaves the published directory: {url}')
    if not path.is_file():
        raise ValueError(f'Missing local file: {url}')
    return path


def check(root):
    text = (root / 'index.html').read_text()
    page = Page(text)
    for url in page.links:
        path = local_file(root, url)
        parts = urlsplit(url)
        if path == (root / 'index.html').resolve() and parts.fragment:
            if unquote(parts.fragment) not in page.ids:
                raise ValueError(f'Missing anchor: {url}')
    script = (root / 'site.js').read_text()
    for asset in re.findall(r"src:\s*'([^']+)'", script):
        local_file(root, asset)
    jar = root / 'downloads/AutomataViz.jar'
    checksum = re.search(r'class="checksum">([a-f0-9]{64})<', text)
    if not checksum or checksum[1] != sha256(jar.read_bytes()).hexdigest():
        raise ValueError('The displayed checksum does not match the downloadable JAR')
    size = re.search(r'data-download-size>([^<]+)<', text)
    expected_size = f'{jar.stat().st_size / 1_000_000:.1f} MB'
    if not size or size[1] != expected_size:
        raise ValueError('The displayed download size does not match the JAR')


def build():
    jars = list((WEBSITE.parent / 'target').glob('*-jar-with-dependencies.jar'))
    if len(jars) != 1:
        raise ValueError('Run mvn clean package first; expected one dependency-bundled JAR in target/')
    jar = jars[0]
    if not jar.read_bytes().startswith(b'PK\x03\x04'):
        raise ValueError('The download is not a JAR/ZIP archive')
    text = (WEBSITE / 'dist/index.html').read_text()
    checksum = sha256(jar.read_bytes()).hexdigest()
    text, hashes = re.subn(r'(class="checksum">)[a-f0-9]{64}(<)',
                          lambda m: m[1] + checksum + m[2], text)
    text, sizes = re.subn(r'(data-download-size>)[^<]+(<)',
                         lambda m: m[1] + f'{jar.stat().st_size / 1_000_000:.1f} MB' + m[2], text)
    if hashes != 1 or sizes != 1:
        raise ValueError('Expected exactly one checksum and download-size field in index.html')
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    shutil.copytree(WEBSITE / 'dist', OUTPUT,
                    ignore=shutil.ignore_patterns('downloads', '.DS_Store'))
    (OUTPUT / 'downloads').mkdir()
    shutil.copyfile(jar, OUTPUT / 'downloads/AutomataViz.jar')
    (OUTPUT / 'index.html').write_text(text)
    (OUTPUT / '.nojekyll').touch()
    check(OUTPUT)
    print(f'Built and verified {OUTPUT}')


if __name__ == '__main__':
    try:
        if sys.argv[1:] == ['build']:
            build()
        elif sys.argv[1:] == ['check']:
            check(OUTPUT)
            print('Published files, project-relative URLs, anchors, gallery assets, and JAR metadata verified')
        else:
            raise ValueError('Usage: python3 scripts/site.py build|check')
    except (ValueError, OSError) as error:
        sys.exit(str(error))
