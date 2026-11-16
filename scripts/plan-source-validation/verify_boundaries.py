"""通过独立预期验证Top5截取、并列及先筛选后排名。"""
import json
import os
from pathlib import Path
import urllib.request
folder = Path(__file__).resolve().parent
m = json.loads((folder/'boundary-manifest.json').read_text())
base = os.environ.get('REPORT_BASE_URL', 'http://localhost:18082') + '/api/v1/reports/'
filters = dict(startDate='2026-09-15', endDate='2026-09-15', skillIds=[m['skill']])
results = []
def call(body):
    req = urllib.request.Request(base+'plan-sources', data=json.dumps(body).encode(), headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(req, timeout=30) as response:
        assert response.status == 200
        result = json.load(response)
    assert result['code'] == 200, result
    return result['data']['branches']
def check(name, condition, actual):
    assert condition, (name, actual)
    results.append(dict(name=name, actual=actual))
    print(name, 'PASS')
expected = sorted(m['branches'], key=lambda b: (-b['count'], b['id']))
rows = call(filters)
check('seven branches truncated to five', len(rows)==5 and [r['id'] for r in rows]==[b['id'] for b in expected[:5]], rows)
check('independent descending counts', [r['planCount'] for r in rows]==[7,6,5,4,3], rows)
check('tie at cutoff uses ascending id', rows[-1]['id']==min(b['id'] for b in m['branches'] if b['count']==3), rows)
check('three source counts', all((r['myCount'],r['branchCount'],r['presidentCount'])==((r['planCount']+2)//3,(r['planCount']+1)//3,r['planCount']//3) for r in rows), rows)
for i in range(3):
    again=call(filters)
    check('stable ordering '+str(i+1), again==rows, again)
last=expected[-1]
for name, change in [('excluded branch selected',dict(bbkId=last['id'])), ('excluded organization selected',dict(orgId=last['org']))]:
    actual=call(dict(filters, **change))
    check(name, len(actual)==1 and actual[0]['id']==last['id'] and actual[0]['planCount']==1, actual)
actual=call(dict(filters, level='NO_MATCH'))
check('filter no matches', actual==[], actual)
(folder/'boundary-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
print(f'{len(results)}/{len(results)} PASS')
