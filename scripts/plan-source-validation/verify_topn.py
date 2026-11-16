"""验证可配置topN与默认值，需装载boundary-seed.sql。"""
import json
import os
from pathlib import Path
import urllib.request
folder = Path(__file__).resolve().parent
m = json.loads((folder/'boundary-manifest.json').read_text())
expected = sorted(m['branches'], key=lambda b: (-b['count'], b['id']))
base = os.environ.get('REPORT_BASE_URL', 'http://localhost:18083')
results = []
for label, value, size in [('omitted', None, 5), ('null', None, 5), ('one', 1, 1), ('three', 3, 3), ('six', 6, 6), ('all', 7, 7), ('above available', 10, 7), ('zero', 0, None), ('negative', -1, None), ('invalid type', 'bad', None)]:
    body = dict(startDate='2026-09-15', endDate='2026-09-15', skillIds=[m['skill']])
    if label != 'omitted':
        body['topN'] = value
    req = urllib.request.Request(base+'/api/v1/reports/plan-sources', data=json.dumps(body).encode(), headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(req, timeout=30) as response:
        assert response.status == 200
        result = json.load(response)
    if size is None:
        assert result['code'] == 10000 and result['data'] is None, result
    else:
        assert result['code'] == 200, result
        rows = result['data']['branches']
        assert len(rows) == size and [r['id'] for r in rows] == [b['id'] for b in expected[:size]], result
    results.append(dict(name=label, response=result))
    print(label, 'PASS')
(folder/'topn-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
print('10/10 PASS')
