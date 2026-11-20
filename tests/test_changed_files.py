import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / 'scripts/java-quality.py'

class ChangedFilesTest(unittest.TestCase):
    def test_no_java_changes(self):
        with tempfile.TemporaryDirectory() as directory:
            subprocess.run(['git', 'init', '-q', directory], check=True)
            result = subprocess.run(['python3', str(SCRIPT), 'check', '--changed'], cwd=directory,
                                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            self.assertIn('No changed Java files', result.stdout)

if __name__ == '__main__':
    unittest.main()
