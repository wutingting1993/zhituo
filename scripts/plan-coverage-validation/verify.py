"""通过真实 HTTP 接口核对独立造数场景。"""
import json
import os
from pathlib import Path
import urllib.request

folder = Path(__file__).resolve().parent
ids = json.loads((folder / 'manifest.json').read_text())
base = os.environ.get('REPORT_BASE_URL', 'http://localhost:8081') + '/api/v1/reports/'
filters = dict(startDate='2026-11-10', endDate='2026-11-11', bbkId=ids['B1'])
passed = []
responses = {}


def call(path, body, code=200):
    request = urllib.request.Request(base + path, data=json.dumps(body).encode(), headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=30) as response:
        assert response.status == 200
        result = json.load(response)
    assert result['code'] == code, result
    return result['data']


def check(name, condition, response):
    assert condition, (name, response)
    passed.append(name)
    responses[name] = response
    print(name, 'PASS')


def cover(**changes):
    return call('plan-coverage', dict(filters, **changes))['branches']


def detail(source=None, scope=None, **changes):
    body = dict(filters=dict(filters, **changes), scopeBbkId=ids['B1'])
    if scope:
        body['scopeOrgId'] = ids[scope]
    if source:
        body['source'] = source
    return call('plan-coverage/details', body)


rows = cover()
b = rows[0]
check('branch deduplication across subbranches and days', len(rows) == 1 and b['planCount'] == 3 and sum(c['planCount'] for c in b['children']) == 4, rows)
check('source mapping and exclusions', (b['myCount'], b['branchCount'], b['presidentCount']) == (1, 1, 1), b)
check('covered subbranch count', b['subBranchCount'] == 2 and len(b['children']) == 2, b)
d = detail()
check('detail count equals clicked count', d['planCount'] == 3 and len(d['plans']) == 3, d)
check('aggregate customer deduplication and both task types', (d['recommendedCustomerCount'], d['taskCount'], d['skillCount']) == (1, 20, 2), d)
p = {r['id']: r for r in d['plans']}
check('owner independent from target institution', all(r['ownerId'] == ids['U3'] and r['ownerName'] == '规划负责人' and r['ownerLevel'] == 'L3' for r in p.values()), d)
check('no result execution retained', p[ids['P3']]['taskCount'] == 0 and p[ids['P3']]['recommendedCustomerCount'] == 0, d)
check('no current publication or validity filter', all(r['periodEnd'] == '2020-01-02' for r in p.values()), d)
check('per plan task deduplication', p[ids['P1']]['taskCount'] == 16 and p[ids['P2']]['taskCount'] == 4, d)
r = detail(source='分行关注')
check('source filter leaves summaries unchanged', r['planCount'] == 3 and r['taskCount'] == 20 and [x['id'] for x in r['plans']] == [ids['P2']], r)
r = detail(source='全部')
check('all source reset', len(r['plans']) == 3, r)
r = detail(skillIds=[ids['S2']])
check('only selected skill is displayed', r['planCount'] == 1 and r['taskCount'] == 8 and [s['id'] for s in r['plans'][0]['skills']] == [ids['S2']], r)
r = detail(skillIds=[ids['S1'], ids['S2']])
check('multi skill OR', r['planCount'] == 3 and r['skillCount'] == 2, r)
r = detail(scope='O2')
check('subbranch drill scope', r['planCount'] == 1 and r['taskCount'] == 8, r)
r = detail(orgId=ids['O2'])
check('branch drill retains top subbranch condition', r['planCount'] == 1 and r['taskCount'] == 8, r)
r = detail(level='L2')
check('level filters target not owner', r['planCount'] == 1 and r['taskCount'] == 8 and r['plans'][0]['ownerLevel'] == 'L3', r)
r = detail(endDate='2026-11-10')
check('first date included', r['taskCount'] == 10, r)
r = detail(startDate='2026-11-11')
check('last date included', r['taskCount'] == 10, r)
r = cover(startDate='2026-01-01', endDate='2026-01-01')
check('empty interval', r == [], r)
r = detail(startDate='2026-01-01', endDate='2026-01-01')
check('empty detail', r['plans'] == [] and r['planCount'] == r['taskCount'] == r['recommendedCustomerCount'] == r['skillCount'] == 0, r)
r = detail(source='分行关注', scope='O2')
check('source with no matching plans keeps summary', r['plans'] == [] and r['planCount'] == 1, r)
for name, path, body in [
    ('reversed dates', 'plan-coverage', dict(filters, startDate='2026-11-12')),
    ('missing dates', 'plan-coverage', {}),
    ('mismatched institution', 'plan-coverage', dict(filters, orgId=ids['O3'])),
    ('invalid source', 'plan-coverage/details', dict(filters=filters, scopeBbkId=ids['B1'], source='分行中台')),
    ('out of scope branch', 'plan-coverage/details', dict(filters=filters, scopeBbkId=ids['B2'])),
    ('out of scope subbranch', 'plan-coverage/details', dict(filters=dict(filters, orgId=ids['O1']), scopeBbkId=ids['B1'], scopeOrgId=ids['O2'])),
    ('missing drill institution', 'plan-coverage/details', dict(filters=filters)),
    ('missing nested filters', 'plan-coverage/details', dict(scopeBbkId=ids['B1'])),
    ('invalid nested dates', 'plan-coverage/details', dict(filters={}, scopeBbkId=ids['B1'])),
]:
    call(path, body, 10000)
    check(name, True, {'code': 10000})
(folder / 'results.json').write_text(json.dumps(dict(passed=passed, responses=responses), ensure_ascii=False, indent=2))
print(len(passed), 'scenarios PASS')
