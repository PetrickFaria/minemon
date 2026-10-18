from pathlib import Path
import hashlib
import os
import shutil
import subprocess
import tempfile
import zipfile

repo = Path.cwd()
parts = sorted((repo / '.bootstrap').glob('minemon-source.part-*'))
if not parts:
    raise SystemExit('Bootstrap archive chunks are missing')
archive = Path('/tmp/minemon-source.zip')
with archive.open('wb') as target:
    for part in parts:
        target.write(part.read_bytes())
digest = hashlib.sha256(archive.read_bytes()).hexdigest()
expected = '23387ac3acf6f3b044ff301f67c5a08b103f615d245a47a929b75f9588ee5f74'
if digest != expected:
    raise SystemExit(f'Source archive checksum mismatch: {digest}')
with tempfile.TemporaryDirectory() as temp:
    with zipfile.ZipFile(archive) as bundle:
        bad = bundle.testzip()
        if bad:
            raise SystemExit(f'Invalid source archive member: {bad}')
        bundle.extractall(temp)
    source = Path(temp) / 'Minemon'
    if not (source / 'build.gradle').is_file():
        raise SystemExit('The source archive did not contain the Minemon project')
    shutil.copytree(source, repo, dirs_exist_ok=True)
shutil.rmtree(repo / '.bootstrap')
subprocess.run(['git', 'config', 'user.name', 'github-actions[bot]'], check=True)
subprocess.run(['git', 'config', 'user.email', '41898282+github-actions[bot]@users.noreply.github.com'], check=True)
subprocess.run(['git', 'add', '-A'], check=True)
subprocess.run(['git', 'commit', '-m', 'Publish Minemon project sources'], check=True)
subprocess.run(['git', 'push', 'origin', f"HEAD:{os.environ.get('GITHUB_REF_NAME', 'main')}"], check=True)
