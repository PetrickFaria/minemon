"""Exact, narrow exception for the requested Arbok UV texture adaptation."""
import hashlib

ARBOK_TEXTURE_PATH = "assets/minemon/textures/pokemon/arbok_cobblemon_1_8_2/arbok.png"
ARBOK_TEXTURE_SHA256 = "ac0a5028891be22ffa2673ff5272f2a1a7bd0bee49e299c52fc9946c10a801ea"


def is_approved_prior_asset_delta(path, content):
    """Accept only the reviewed Arbok atlas bytes; all other old assets stay exact."""
    return path == ARBOK_TEXTURE_PATH and hashlib.sha256(content).hexdigest() == ARBOK_TEXTURE_SHA256
