"""Exact, narrow exception for the requested Arbok UV texture adaptation."""
import hashlib

ARBOK_TEXTURE_PATH = "assets/minemon/textures/pokemon/arbok_cobblemon_1_8_2/arbok.png"
ARBOK_TEXTURE_SHA256 = "798735ad1679330d0289a70e27ce33e11c6365aa5df2ad50980b4f2e721d0cea"


def is_approved_prior_asset_delta(path, content):
    """Accept only the reviewed Arbok atlas bytes; all other old assets stay exact."""
    return path == ARBOK_TEXTURE_PATH and hashlib.sha256(content).hexdigest() == ARBOK_TEXTURE_SHA256
