from pathlib import Path
import shutil
run=Path('run');candidates=[p.parent for p in run.rglob('level.dat') if 'saves' not in p.parts]
if not candidates:raise SystemExit('Minecraft GameTest world not found for client render validation')
dst=run/'saves/minemon-validation';dst.parent.mkdir(exist_ok=True,parents=True)
if dst.exists():shutil.rmtree(dst)
shutil.copytree(candidates[0],dst,ignore=shutil.ignore_patterns('session.lock'))
print('Client validation world prepared from',candidates[0])

# Most fixtures fit inside two chunks, but the Gastly aura regression deliberately
# moves the camera 50 blocks away. Four chunks load the entity and the whole camera
# path while still avoiding the cost of Minecraft's default twelve-chunk view.
# Keep every species, animation phase, assertion and particle enabled.
options=run/'options.txt'
settings={}
if options.exists():
    for line in options.read_text().splitlines():
        if ':' in line:
            key,value=line.split(':',1)
            settings[key]=value
settings.update({'renderDistance':'4','simulationDistance':'5','maxFps':'60','enableVsync':'false','particles':'0'})
options.write_text(''.join(f'{key}:{value}\n' for key,value in settings.items()))
print('Headless render fixture configured: 4-chunk view, all particles, full species/phase coverage')
