Minecraft 1.19.2 vanilla bone texture, retained solely as the exact recoloring reference.
Source mirror: https://github.com/InventivetalentDev/minecraft-assets/blob/1.19.2/assets/minecraft/textures/item/bone.png
Git blob: d30c7e3207362600780f7fd0acae6b4c07cf9a24
Original artwork belongs to Mojang. Client smoke validation compares every RGBA pixel with the Minecraft 1.19.2 resource.

Model measurement coordinates follow GeckoLib 3 (Minecraft 1.19 branch, commit 6b17247dc9d6ae701e54e2145e309af8ffb4f6b1):
- Forge/.../geo/render/GeoBuilder.java and built/GeoCube.java reflect X and negate X/Y rotation.
- Forge/.../util/RenderUtils.java composes Z/Y/X rotations.
- Forge/.../util/json/JsonKeyFrameUtils.java and core/controller/AnimationController.java apply the same signs to numeric and Molang animation rotations.
Source: https://github.com/bernie-g/geckolib/tree/6b17247dc9d6ae701e54e2145e309af8ffb4f6b1/Forge
The measurement generator remains in raw geometry coordinates, using the equivalent Rz(-z) Ry(y) Rx(-x) transform.
