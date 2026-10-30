"""GeckoLib coordinate and cube-inflation regression checks."""
import unittest
from generate_model_metrics import measure

class ModelMeasurementTest(unittest.TestCase):
 def test_native_x_rotation_and_animated_translation(self):
  geometry={"minecraft:geometry":[{"bones":[{"name":"body","pivot":[0,0,0],"cubes":[{"origin":[0,0,1],"size":[1,2,1]}]}]}]}
  clip={"bones":{"body":{"rotation":[90,0,0],"position":[0,16,0]}}}
  core,full=measure(geometry,clip)
  expected=[0,17/16,-2/16,1/16,18/16,0]
  for measured,wanted in zip(full,expected):self.assertAlmostEqual(wanted,measured)
  self.assertEqual(core,full)
 def test_cube_inflation_is_included(self):
  geometry={"minecraft:geometry":[{"bones":[{"name":"body","cubes":[{"origin":[0,0,0],"size":[16,16,16],"inflate":1}]}]}]}
  core,full=measure(geometry,{})
  self.assertEqual([-1/16]*3+[17/16]*3,full)
  self.assertEqual(core,full)

if __name__=="__main__":unittest.main()
