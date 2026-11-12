"""执行视角本机联调；复用存量报表验证数据，临时点击在 finally 中精确清理。"""
import json
import os
import re
import subprocess
import sys
from datetime import datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
IDS = json.loads((ROOT / 'scripts/report-validation/manifest.json').read_text())
BASE = sys.argv[1].rstrip('/')
PASSED = []
RESULT = Path(__file__).with_name('results.json')
MARKER = 'execution-validation-20261111-buttons'


def sql(statement):
    env = dict(os.environ, MYSQL_PWD=os.environ.get('MYSQL_PASSWORD', 'root'))
    command = ['mysql', '-N', '-B', '-h', env.get('MYSQL_HOST', '127.0.0.1'),
               '-P', env.get('MYSQL_PORT', '3306'), '-u', env.get('MYSQL_USER', 'root'),
               env.get('MYSQL_DATABASE', 'zhituo'), '-e', statement]
    return subprocess.run(command, env=env, check=True, capture_output=True, text=True).stdout.strip()


def api(path, payload, code=200):
    req = Request(BASE + '/api/v1/reports/' + path, data=json.dumps(payload).encode(),
                  headers={'Content-Type': 'application/json'}, method='POST')
    with urlopen(req, timeout=30) as response:
        body = json.load(response)
        assert response.status == 200 and body['code'] == code, (path, code, body)
        assert body['success'] == (code == 200), body
        return body['data']


def check(name, condition):
    assert condition, name
    PASSED.append(name)
    print('PASS', name, flush=True)


def filters(**extra):
    return dict(startDate='2026-11-10', endDate='2026-11-11', bbkId=IDS['B1'], **extra)


def rate(count, total):
    return None if not total else float((Decimal(count) * 100 / Decimal(total)).quantize(Decimal('.01'), rounding=ROUND_HALF_UP))


def overview(f):
    return api('execution-overview', f)


def branches(f, **sort):
    return api('execution-branches', dict(filters=f, **sort))


def details(kind, f, org='O1'):
    return api('execution-branches/' + kind, dict(filters=f, scopeBbkId=IDS['B1'], scopeOrgId=IDS[org]))


def validate():
    full = overview(filters())
    check('known fixture totals', (full['taskCount'], full['recommendedCustomerCount'], full['viewCustomerCount'], full['contactCustomerCount']) == (7, 3, 3, 4))
    for label, f in [('full', filters()), ('skill', filters(skillIds=[IDS['S1']])),
                     ('combined', filters(orgId=IDS['O1'], level='L1', skillIds=[IDS['S1']])),
                     ('zero-result', filters(skillIds=[IDS['S3']])),
                     ('empty', dict(startDate='2026-01-01', endDate='2026-01-01', bbkId=IDS['B1']))]:
        current = overview(f)
        old = api('summary', f)
        keys = ['taskCount', 'recommendedCustomerCount', 'viewRate', 'contactRate', 'averageTaskCount', 'averageRecommendedCustomerCount']
        check(label + ' agrees with summary', all(current[k] == old[k] for k in keys))
        for count, percentage in [('viewCustomerCount', 'viewRate'), ('insightCustomerCount', 'insightRate'),
                                  ('callCustomerCount', 'callRate'), ('contactCustomerCount', 'contactRate')]:
            check(label + ' ' + percentage + ' denominator', current[percentage] == rate(current[count], current['recommendedCustomerCount']))
    check('button dedup and exclusions', full['insightCustomerCount'] == 1 and full['callCustomerCount'] == 1)
    check('minimum loss preserves negative value', full['largestLoss'] == dict(fromStage='call', toStage='contact', customerDifference=-3, lossRate=-300))
    zero = overview(filters(skillIds=[IDS['S3']]))
    check('zero denominators produce null', zero['largestLoss'] is None and zero['customersPerTask'] is None and zero['contactRate'] is None)
    tied = overview(dict(startDate='2026-11-11', endDate='2026-11-11', bbkId=IDS['B1']))
    check('all loss denominators zero', tied['largestLoss'] is None)
    branch_data = branches(filters())
    check('branch group count', len(branch_data['rows']) == 2)
    check('baseline equals whole institution', all(branch_data['benchmark'][k] == full[k] for k in ['viewRate', 'insightRate', 'callRate', 'contactRate']))
    for row in branch_data['rows']:
        org = 'O1' if row['orgId'] == IDS['O1'] else 'O2'
        scoped = overview(filters(orgId=row['orgId']))
        keys = ['taskCount', 'recommendedCustomerCount', 'viewRate', 'insightRate', 'callRate', 'contactRate']
        check(org + ' matches independent overview', all(row[k] == scoped[k] for k in keys))
        managers = details('managers', filters(), org)
        skills = details('skills', filters(), org)
        check(org + ' manager summary', all(managers['summary'][k] == scoped[k] for k in ['taskCount', 'recommendedCustomerCount', 'contactRate']))
        check(org + ' skill summary', skills['summary']['taskCount'] == scoped['taskCount'] and skills['summary']['skillCount'] == row['skillCount'])
        check(org + ' skill category explicit null', all('categoryName' in r and r['categoryName'] is None for r in skills['rows']))
        check(org + ' manager order', managers['rows'] == sorted(managers['rows'], key=lambda r: (-r['taskCount'], r['sapId'])))
        check(org + ' skill order', skills['rows'] == sorted(skills['rows'], key=lambda r: (-r['taskCount'], r['skillId'])))
    for direction in ['asc', 'desc']:
        for key in ['skillCount', 'taskCount', 'recommendedCustomerCount', 'viewRate', 'insightRate', 'callRate', 'contactRate']:
            rows = branches(filters(), sortKey=key, sortDir=direction)['rows']
            def order(r):
                value = r[key]
                return (value is None, 0 if value is None else value * (-1 if direction == 'desc' else 1), r['bbkId'], r['orgId'])
            check('sorting ' + key + ' ' + direction, rows == sorted(rows, key=order))
    for kind in ['managers', 'skills']:
        empty = details(kind, dict(startDate='2026-01-01', endDate='2026-01-01', bbkId=IDS['B1']))
        check(kind + ' empty retains organization names', empty['rows'] == [] and bool(empty['bbkName']) and bool(empty['orgName']))
        selected = details(kind, filters(skillIds=[IDS['S1']]))
        if kind == 'skills':
            check('skill drilldown keeps skill filter', all(r['skillId'] == IDS['S1'] for r in selected['rows']))
        else:
            check('manager drilldown keeps skill filter', all(s['skillId'] == IDS['S1'] for r in selected['rows'] for s in r['skills']))
    error_cases = [
        ('missing dates', 'execution-overview', {}),
        ('reverse dates', 'execution-overview', dict(startDate='2026-11-11', endDate='2026-11-10')),
        ('missing filters', 'execution-branches', {}),
        ('invalid sorting', 'execution-branches', dict(filters=filters(), sortKey='unknown')),
        ('invalid direction', 'execution-branches', dict(filters=filters(), sortDir='unknown')),
        ('missing scope', 'execution-branches/managers', dict(filters=filters())),
        ('scope outside filter', 'execution-branches/skills', dict(filters=filters(orgId=IDS['O1']), scopeBbkId=IDS['B1'], scopeOrgId=IDS['O2'])),
        ('mismatched scope', 'execution-branches/managers', dict(filters=filters(), scopeBbkId=IDS['B1'], scopeOrgId='not-an-org'))]
    for name, path, payload in error_cases:
        api(path, payload, code=10000)
        check(name, True)


def main():
    inserted = []
    cleanup = False
    before = int(sql('SELECT COUNT(1) FROM swe_html_preview_click_events'))
    assert sql("SELECT COUNT(1) FROM swe_html_preview_click_events WHERE file_name='" + MARKER + "'") == '0', 'marker already exists'
    trace, customer = IDS['a'], IDS['c1']
    assert int(sql("SELECT COUNT(1) FROM swe_cron_result_index WHERE trace_id='" + trace + "' AND custuid='" + customer + "' AND status='SUC'")) > 0, 'fixture unavailable'
    day = sql("SELECT DATE(actual_time) FROM swe_cron_executions WHERE trace_id='" + trace + "' LIMIT 1")
    next_day = (datetime.strptime(day, '%Y-%m-%d') + timedelta(days=1)).strftime('%Y-%m-%d')
    events = [('button_click', 'insight', day, customer), ('button_click', 'insight', day, customer),
              ('button_click', 'phone', day, customer), ('button_click', 'phone', day, customer),
              ('button_click', 'insight', next_day, customer), ('button_click', 'insight', day, 'not-recommended'),
              ('button_click', 'plan', day, customer), ('module_exposure', 'insight', day, customer),
              ('preview_view', 'phone', day, customer)]
    try:
        statements = ['START TRANSACTION']
        for event, button, click_day, cust in events:
            statements.append("INSERT INTO swe_html_preview_click_events (trace_id,customer_id,file_url,file_name,event_type,button_type,clicked_at,template_type) VALUES ('%s','%s','local-validation','%s','%s','%s','%s 12:00:00','sub')" % (trace, cust, MARKER, event, button, click_day))
            statements.append('SELECT LAST_INSERT_ID()')
        statements.append('COMMIT')
        inserted = [int(x) for x in sql(';'.join(statements)).splitlines()]
        validate()
    finally:
        if inserted:
            sql('DELETE FROM swe_html_preview_click_events WHERE id IN (' + ','.join(map(str, inserted)) + ") AND file_name='" + MARKER + "'")
        cleanup = int(sql('SELECT COUNT(1) FROM swe_html_preview_click_events')) == before
        RESULT.write_text(json.dumps(dict(passed=PASSED, count=len(PASSED), temporaryClickCount=len(inserted), cleanupVerified=cleanup), ensure_ascii=False, indent=2))
        assert cleanup, 'click count not restored'
    print(str(len(PASSED)) + ' checks PASS; temporary clicks cleaned', flush=True)


if __name__ == '__main__':
    main()
