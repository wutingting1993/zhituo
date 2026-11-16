"""只读本机验证数据，独立计算执行分析接口预期结果。"""
import json
import os
import subprocess
import sys
from datetime import date, timedelta
from decimal import Decimal, ROUND_HALF_UP
from fractions import Fraction
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
BASE = sys.argv[1].rstrip('/')
RESULT = Path(__file__).with_name('analysis-results.json')
IDS = json.loads((ROOT / 'scripts/report-validation/manifest.json').read_text())
PASSED = []


def check(name, condition):
    if not condition:
        raise AssertionError(name)
    PASSED.append(name)
    print('PASS', name, flush=True)


def api(path, payload, code=200):
    request = Request(BASE + '/api/v1/reports/' + path,
                      data=json.dumps(payload).encode(),
                      headers={'Content-Type': 'application/json'}, method='POST')
    with urlopen(request, timeout=30) as response:
        body = json.load(response)
        assert response.status == 200 and body['code'] == code, (path, payload, body)
        assert body['success'] == (code == 200), body
        if code != 200:
            assert body['data'] is None and body['message'], body
        return body['data']


def read(table, fields):
    expression = ','.join("'%s',%s" % (field, field) for field in fields.split())
    command = ['mysql', '-N', '-B', '--raw', '-h', '127.0.0.1', '-u',
               os.environ.get('MYSQL_USER', 'root'), 'zhituo', '-e',
               'SELECT JSON_OBJECT(' + expression + ') FROM ' + table]
    env = dict(os.environ, MYSQL_PWD=os.environ.get('MYSQL_PASSWORD', 'root'))
    output = subprocess.run(command, env=env, check=True, capture_output=True,
                            text=True).stdout
    return [json.loads(line) for line in output.splitlines()]


def load():
    return {
        'users': read('rm_jkh_user_info', 'user_id first_bbk_id first_bbk_nm org_id org_nm pst_lvl'),
        'plans': read('swe_wealth_plans', 'id deleted created_at'),
        'targets': read('swe_wealth_plan_targets', 'sap_id plan_id deleted'),
        'scenes': read('swe_wealth_plan_scenes', 'plan_id scene_id deleted'),
        'skills': read('swe_marketplace_skills', 'skill_id skill_name source_id'),
        'jobs': read('swe_cron_jobs', 'id tenant_id plan_id skill_ids source_id status'),
        'schedule': read('swe_plan_sence_cron', 'plan_id sence_id execute_day'),
        'executions': read('swe_cron_executions', 'job_id trace_id actual_time is_read'),
        'results': read('swe_cron_result_index', 'job_id skill_id trace_id custuid result_type is_latest_success status'),
        'events': read('swe_html_preview_click_events', 'id trace_id customer_id clicked_at event_type template_type button_type'),
        'contacts': read('swe_skill_contact_detail', 'click_id cust_uid')}


def selected(db, filters):
    scheduled = {(r['plan_id'], r['sence_id'], r['execute_day'][:10])
                 for r in db['schedule']
                 if filters['startDate'] <= r['execute_day'][:10] <= filters['endDate']}
    pairs = {(p, s) for p, s, _ in scheduled}
    plans = {p['id']: p for p in db['plans'] if p['deleted'] == 0}
    skills = {s['skill_id']: s for s in db['skills'] if s['source_id'] == 'RMASSIST'}
    scenes = {(s['plan_id'], s['scene_id']) for s in db['scenes'] if s['deleted'] == 0}
    users = {u['user_id']: u for u in db['users']}
    relations = {}
    for target in db['targets']:
        user = users.get(target['sap_id'])
        plan = plans.get(target['plan_id'])
        if target['deleted'] != 0 or user is None or plan is None:
            continue
        if any(filters.get(k) and filters[k] != user[v]
               for k, v in [('bbkId', 'first_bbk_id'), ('orgId', 'org_id'), ('level', 'pst_lvl')]):
            continue
        for job in db['jobs']:
            pair = (job['plan_id'], job['skill_ids'])
            if (job['plan_id'] != plan['id'] or job['tenant_id'] != user['user_id']
                    or job['source_id'] != 'RMASSIST' or job['status'] in (None, 'deleted')
                    or pair not in pairs or pair not in scenes or job['skill_ids'] not in skills
                    or (filters.get('skillIds') and job['skill_ids'] not in filters['skillIds'])):
                continue
            relations[(job['id'], user['user_id'], job['skill_ids'])] = (job, user, plan)
    executions = []
    for job, user, plan in relations.values():
        latest = {}
        for execution in db['executions']:
            day = execution['actual_time'][:10]
            if (execution['job_id'] != job['id']
                    or (plan['id'], job['skill_ids'], day) not in scheduled
                    or execution['actual_time'] < plan['created_at']):
                continue
            if day not in latest or execution['actual_time'] > latest[day]['actual_time']:
                latest[day] = execution
        executions.extend((job, user, day, execution) for day, execution in latest.items())
    return list(relations.values()), executions


def rate(count, total):
    return None if not total else float((Decimal(count) * 100 / Decimal(total)).quantize(
        Decimal('.01'), rounding=ROUND_HALF_UP))


def metrics(db, executions):
    recommended, viewed, insight, phone, contacts, lists, tasks = (set() for _ in range(7))
    for job, _, day, execution in executions:
        for result in db['results']:
            if ((result['job_id'], result['skill_id'], result['trace_id'])
                    != (job['id'], job['skill_ids'], execution['trace_id'])
                    or result['result_type'] != 'plan' or result['is_latest_success'] != 1
                    or result['status'] != 'SUC'):
                continue
            customer = result['custuid']
            if customer == '':
                lists.add(execution['trace_id'])
            else:
                tasks.add((execution['trace_id'], customer))
                recommended.add(customer)
                if execution['is_read'] == 1:
                    viewed.add(customer)
            for event in db['events']:
                if (event['trace_id'] != execution['trace_id'] or event['customer_id'] != customer
                        or event['clicked_at'][:10] != day):
                    continue
                button = event['event_type'] == 'button_click'
                if customer and button and event['button_type'] == 'insight':
                    insight.add(customer)
                if customer and button and event['button_type'] == 'phone':
                    phone.add(customer)
                if button or (event['event_type'] == 'preview_view' and event['template_type'] == 'sub'):
                    contacts.update(c['cust_uid'] for c in db['contacts']
                                    if c['click_id'] == event['id'] and c['cust_uid'] is not None)
    result = dict(taskCount=len(lists) + len(tasks), recommendedCustomerCount=len(recommended))
    for name, values in [('view', viewed), ('insight', insight), ('call', phone), ('contact', contacts)]:
        result[name + 'CustomerCount'] = len(values)
        result[name + 'Rate'] = rate(len(values), len(recommended))
    return result


def participation(db, filters):
    relations, executions = selected(db, filters)
    assigned, participants, names = {}, {}, {}
    for job, user, _ in relations:
        skill = job['skill_ids']
        assigned.setdefault(skill, set()).add(user['user_id'])
        names[skill] = next(s['skill_name'] for s in db['skills'] if s['skill_id'] == skill)
    for job, user, _, execution in executions:
        if execution['is_read'] == 1:
            participants.setdefault(job['skill_ids'], set()).add(user['user_id'])
    return [dict(skillId=skill, skillName=names[skill], participantCount=len(participants.get(skill, set())),
                 assignedManagerCount=len(users), participationRate=rate(len(participants.get(skill, set())), len(users)))
            for skill, users in assigned.items()]


def attention(db, filters):
    _, executions = selected(db, filters)
    _, baseline = selected(db, {k: v for k, v in filters.items() if k in ('startDate', 'endDate', 'bbkId')})
    base = metrics(db, baseline)
    groups = {}
    for entry in executions:
        user = entry[1]
        groups.setdefault((user['first_bbk_id'], user['org_id']), []).append(entry)
    rows = []
    for (bbk, org), group in groups.items():
        m = metrics(db, group)
        n, bn = m['recommendedCustomerCount'], base['recommendedCustomerCount']
        if not n or not bn:
            continue
        view_low = Fraction(m['viewCustomerCount'], n) < Fraction(base['viewCustomerCount'], bn)
        contact_low = Fraction(m['contactCustomerCount'], n) < Fraction(base['contactCustomerCount'], bn)
        if not (view_low or contact_low):
            continue
        row = {k: m[k] for k in ('recommendedCustomerCount', 'viewCustomerCount', 'contactCustomerCount', 'viewRate', 'contactRate')}
        row.update(bbkId=bbk, orgId=org, diagnosis='bothLow' if view_low and contact_low else 'usageLow' if view_low else 'contactLow')
        row.update(bbkName=group[0][1]['first_bbk_nm'], orgName=group[0][1]['org_nm'])
        for prefix in ('view', 'contact'):
            row['benchmark' + prefix.title() + 'Rate'] = base[prefix + 'Rate']
            delta = (Decimal(m[prefix + 'CustomerCount']) / n - Decimal(base[prefix + 'CustomerCount']) / bn) * 100
            row[prefix + 'RateDifference'] = float(delta.quantize(Decimal('.01'), rounding=ROUND_HALF_UP))
        rows.append(row)
    def order(row):
        usage = row['diagnosis'] == 'usageLow'
        return (usage, Fraction(row['viewCustomerCount'] if usage else row['contactCustomerCount'], row['recommendedCustomerCount']),
                -row['recommendedCustomerCount'], row['bbkId'], row['orgId'])
    return sorted(rows, key=order)


def validate(db):
    common = dict(startDate='2026-11-01', endDate='2026-11-12')
    diagnoses = set()
    cases = [('all', common), ('branch', dict(common, bbkId=IDS['B1'])),
             ('combined', dict(common, bbkId=IDS['B1'], orgId=IDS['O1'], level='L1', skillIds=[IDS['S1']])),
             ('zero-result', dict(common, bbkId=IDS['B1'], skillIds=[IDS['S3']])),
             ('single', dict(common, startDate='2026-11-10', endDate='2026-11-10', bbkId=IDS['B1'])),
             ('empty', dict(common, startDate='2026-01-01', endDate='2026-01-01'))]
    for label, filters in cases:
        _, executions = selected(db, filters)
        points = api('execution-trend', filters)['dailyPoints']
        days = (date.fromisoformat(filters['endDate']) - date.fromisoformat(filters['startDate'])).days + 1
        check(label + ' complete dates', len(points) == days)
        for index, point in enumerate(points):
            day = (date.fromisoformat(filters['startDate']) + timedelta(days=index)).isoformat()
            check(label + ' day ' + day, point == dict(date=day, **metrics(db, [e for e in executions if e[2] == day])))
        expected = participation(db, filters)
        for key in ('participantCount', 'participationRate', 'assignedManagerCount'):
            def order(row):
                value = Fraction(row['participantCount'], row['assignedManagerCount']) if key == 'participationRate' else row[key]
                return (-value, row['skillId'])
            ordered = sorted(expected, key=order)
            for n in (1, 2, 1000):
                actual = api('skill-participation', dict(filters=filters, topN=n, sortKey=key))['rows']
                check(label + ' ' + key + ' top' + str(n), actual == ordered[:n])
        ordered = sorted(expected, key=lambda r: (-r['participantCount'], r['skillId']))
        check(label + ' default top5 and sort', api('skill-participation', dict(filters=filters))['rows'] == ordered[:5])
        check(label + ' explicit null defaults', api('skill-participation', dict(filters=filters, topN=None, sortKey=None))['rows'] == ordered[:5])
        expected_attention = attention(db, filters)
        diagnoses.update(r['diagnosis'] for r in expected_attention)
        actual = api('attention-branches', filters)
        check(label + ' attention total before limit', actual['totalCount'] == len(expected_attention))
        check(label + ' attention rows and exact order', len(actual['rows']) == min(4, len(expected_attention)) and
              all(all(row[k] == expected[k] for k in expected) for row, expected in zip(actual['rows'], expected_attention)))
    relations, executions = selected(db, common)
    assigned = {(job['skill_ids'], user['user_id']) for job, user, _ in relations}
    executed = {(job['skill_ids'], user['user_id']) for job, user, _, _ in executions}
    check('fixture includes assigned but unexecuted personnel', bool(assigned - executed))
    for path in ('execution-trend', 'attention-branches'):
        for label, payload in [('missing dates', {}), ('invalid date', dict(common, startDate='bad')),
                               ('reversed', dict(common, startDate='2026-12-01'))]:
            api(path, payload, 10000)
            check(path + ' rejects ' + label, True)
    for label, payload in [('missing filters', {}), ('null filters', dict(filters=None)),
                           ('missing dates', dict(filters={})), ('reversed', dict(filters=dict(common, startDate='2026-12-01'))),
                           ('invalid date', dict(filters=dict(common, endDate='bad')))]:
        api('skill-participation', payload, 10000)
        check('participation rejects ' + label, True)
    for n in (0, -1, 1.5, 2147483648, 'invalid'):
        api('skill-participation', dict(filters=common, topN=n), 10000)
        check('rejects topN ' + str(n), True)
    for key in ('', 'unknown'):
        api('skill-participation', dict(filters=common, sortKey=key), 10000)
        check('rejects sortKey ' + repr(key), True)
    return dict(skillCount=len(participation(db, common)), attentionCount=len(attention(db, common)),
                unexecutedPersonnelSkills=len(assigned - executed), diagnoses=sorted(diagnoses))


def main():
    report = dict(baseUrl=BASE, database='local zhituo', readOnly=True, passed=[], success=False)
    try:
        db = load()
        report['coverage'] = validate(db)
        report['success'] = True
    finally:
        report['passed'] = PASSED
        report['passedCount'] = len(PASSED)
        RESULT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    main()
