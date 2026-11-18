import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

class ArchitectureTest(unittest.TestCase):
    def check_fixture(self, sources, expected):
        with tempfile.TemporaryDirectory() as directory:
            project = Path(directory)
            classes = project / 'target/classes'
            classes.mkdir(parents=True)
            files = []
            for relative, source in sources.items():
                path = project / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(source)
                files.append(str(path))
            compiled = subprocess.run(['javac', '-d', str(classes), *files], capture_output=True, text=True)
            self.assertEqual(compiled.returncode, 0, compiled.stderr)
            result = subprocess.run(['python3', str(ROOT / 'scripts/architecture-check.py'), str(classes)],
                                    capture_output=True, text=True)
            self.assertEqual(result.returncode == 0, expected, result.stdout[-5000:] + result.stderr)
            if not expected:
                self.assertIn('Failures: 1', result.stdout)

    def test_valid_domain_model(self):
        self.check_fixture({'example/domain/model/Order.java':
                            'package example.domain.model; public class Order {}'}, True)

    def test_domain_cannot_depend_on_mapper(self):
        self.check_fixture({
            'example/infrastructure/dao/OrderMapper.java':
                'package example.infrastructure.dao; public interface OrderMapper {}',
            'example/domain/model/Order.java':
                'package example.domain.model; public class Order { public example.infrastructure.dao.OrderMapper mapper; }'
        }, False)

    def test_service_requires_interface(self):
        self.check_fixture({'example/application/service/OrderService.java':
                            'package example.application.service; public class OrderService {}'}, False)

if __name__ == '__main__':
    unittest.main()
