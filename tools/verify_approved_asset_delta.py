"""Exact, narrow exception for the requested Arbok UV texture adaptation."""
import hashlib

ARBOK_TEXTURE_PATH = "assets/minemon/textures/pokemon/arbok_cobblemon_1_8_2/arbok.png"
ARBOK_TEXTURE_SHA256 = "d6f4c86838c0f0d9d169d692fceaf3cc8040295a921b1257b480605023ba50a2"


def is_approved_prior_asset_delta(path, content):
    """Accept only the reviewed Arbok atlas bytes; all other old assets stay exact."""
    return path == ARBOK_TEXTURE_PATH and hashlib.sha256(content).hexdigest() == ARBOK_TEXTURE_SHA256
