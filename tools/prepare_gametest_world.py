"""Prepare a reproducible, dry superflat world for the real Forge GameTests."""
from pathlib import Path

run = Path('run')
run.mkdir(exist_ok=True)
world_name = 'minemon-gametest'
if (run / world_name / 'level.dat').exists():
    raise SystemExit('Refusing to reuse an existing GameTest world; use a clean run directory.')
# Forge's GameTest launch uses dedicated-server world settings. Without an
# explicit preset it creates random normal terrain around/beyond the templates.
# Arenas at the bottom of that world can flood, invalidating dry survival and
# native ground-run checks even with a stationary, full-health specimen.
properties = {
    'level-name': world_name,
    'level-type': 'minecraft:flat',
    'level-seed': '30220261024',
    'generate-structures': 'false',
    'spawn-animals': 'false',
    'spawn-monsters': 'false',
}
(run / 'server.properties').write_text(
    ''.join(f'{key}={value}\n' for key, value in properties.items()), encoding='utf-8')
print('GameTest world: new dry superflat preset; fixed seed; real AI/physics and all assertions retained.')
