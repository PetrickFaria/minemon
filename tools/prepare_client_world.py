from pathlib import Path
import shutil
run=Path('run');candidates=[p.parent for p in run.rglob('level.dat') if 'saves' not in p.parts]
if not candidates:raise SystemExit('Minecraft GameTest world not found for client render validation')
dst=run/'saves/minemon-validation';dst.parent.mkdir(exist_ok=True,parents=True)
if dst.exists():shutil.rmtree(dst)
shutil.copytree(candidates[0],dst,ignore=shutil.ignore_patterns('session.lock'))
print('Client validation world prepared from',candidates[0])
