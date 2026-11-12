"""独立临时分行验证流失率并列、三个百分点评价和混合null排序。"""
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
BASE = sys.argv[1].rstrip('/')
FOLDER = Path(__file__).resolve().parent
PASSED = []
TABLES = ['rm_jkh_user_info', 'swe_wealth_plans', 'swe_wealth_plan_targets',
          'swe_wealth_plan_scenes', 'swe_plan_sence_cron', 'swe_cron_jobs',
          'swe_cron_executions', 'swe_cron_result_index',
          'swe_html_preview_click_events', 'swe_skill_contact_detail']


def sql(statement):
    env = dict(os.environ, MYSQL_PWD=os.environ.get('MYSQL_PASSWORD', 'root'))
    command = ['mysql', '-N', '-B', '-h', env.get('MYSQL_HOST', '127.0.0.1'),
               '-P', env.get('MYSQL_PORT', '3306'), '-u', env.get('MYSQL_USER', 'root'),
               env.get('MYSQL_DATABASE', 'zhituo')]
    return subprocess.run(command, input=statement, env=env, check=True,
                          capture_output=True, text=True).stdout.strip()


def quote(value):
    return "'" + str(value).replace("'", "''") + "'"


def insert(table, **values):
    return 'INSERT INTO ' + table + ' (' + ','.join(values) + ') VALUES (' + ','.join(quote(v) for v in values.values()) + ')'


def api(path, body):
    request = Request(BASE + '/api/v1/reports/' + path, data=json.dumps(body).encode(),
                      headers={'Content-Type': 'application/json'}, method='POST')
    with urlopen(request, timeout=30) as response:
        result = json.load(response)
        assert response.status == 200 and result['code'] == 200, result
        return result['data']


def check(name, condition):
    assert condition, name
    PASSED.append(name)
    print('PASS', name, flush=True)


def counts():
    return {table: int(sql('SELECT COUNT(1) FROM ' + table)) for table in TABLES}


def ids():
    # 调用项目CUuid生成业务标识，不用随机数或字符串拼接替代。
    source = ('import com.cmbchina.infrastructure.common.util.CUuid;\n'
              'class GenerateBoundaryIds { public static void main(String[] args) {\n'
              'for (int i=0;i<1600;i++) { System.out.println(CUuid.nextId()); } } }')
    with tempfile.TemporaryDirectory(prefix='zhituo-boundary-') as folder:
        path = Path(folder) / 'GenerateBoundaryIds.java'
        path.write_text(source)
        java = str(Path(os.environ['JAVA_HOME']) / 'bin/java')
        result = subprocess.run([java, '--class-path', str(ROOT / 'target/classes'), str(path)],
                                check=True, capture_output=True, text=True)
    values = result.stdout.splitlines()
    assert len(values) == 1600 and len(set(values)) == 1600
    return iter(values)


def main():
    generated = ids()
    def uid():
        return next(generated)
    bbk, plan = uid(), uid()
    orgs = [uid() for _ in range(4)]
    users = [uid() for _ in range(4)]
    # 复用已有技能，只增加独立分行的规划和执行；不改既有技能记录。
    skill = json.loads((ROOT / 'scripts/report-validation/manifest.json').read_text())['S1']
    assert sql('SELECT COUNT(1) FROM swe_marketplace_skills WHERE skill_id=' + quote(skill)) == '1'
    jobs, traces = [], []
    statements = ['START TRANSACTION']
    statements.append(insert('swe_wealth_plans', id=plan, sap_id=users[0], name='执行边界验证规划', source_id='RMASSIST', created_at='2026-01-01 00:00:00'))
    statements.append(insert('swe_wealth_plan_scenes', plan_id=plan, scene_id=skill, scene_name='边界技能', category='fund', cycle='今日', cron_expr='0 8 * * *'))
    for day in ['2026-11-10', '2026-11-11']:
        statements.append(insert('swe_plan_sence_cron', plan_id=plan, sence_id=skill, execute_day=day))
    for index, (org, user) in enumerate(zip(orgs, users)):
        statements.append(insert('rm_jkh_user_info', first_bbk_id=bbk, first_bbk_nm='边界验证分行', org_id=org, org_nm='边界验证支行' + str(index), user_id=user, user_name='边界验证人员', pst_lvl='L1'))
        statements.append(insert('swe_wealth_plan_targets', plan_id=plan, sap_id=user))
        for read in [1, 0]:
            job, trace = uid(), uid()
            jobs.append(job)
            traces.append(trace)
            statements.append(insert('swe_cron_jobs', id=job, name='边界任务', tenant_id=user, plan_id=plan, skill_ids=skill, source_id='RMASSIST', task_type='agent', cron_expr='0 8 * * *', channel='test'))
            statements.append(insert('swe_cron_executions', job_id=job, tenant_id=user, trace_id=trace, actual_time='2026-11-11 08:00:00', status='success', is_read=read))
            statements.append('SET @boundary_execution=LAST_INSERT_ID()')
            amount = ([53, 47][index] if read else [47, 53][index]) if index < 2 else 0
            for _ in range(amount):
                customer = uid()
                subtask = int(uid(), 16)
                statement = insert('swe_cron_result_index', tenant_id=user, first_bbk_id=bbk, bbk_org_id=org, custuid=customer,
                                   skill_id=skill, job_id=job, execution_id='EXECUTION_PLACEHOLDER', trace_id=trace,
                                   subtask_id=subtask, task_id=uid(), result_type='plan', status='SUC',
                                   execution_at='2026-11-11 08:00:00', expire_at='2026-12-01 00:00:00')
                statements.append(statement.replace("'EXECUTION_PLACEHOLDER'", '@boundary_execution'))
                if read:
                    for button in ['insight', 'phone']:
                        statements.append(insert('swe_html_preview_click_events', trace_id=trace, customer_id=customer, file_url='boundary-validation', file_name=plan, event_type='button_click', button_type=button, clicked_at='2026-11-11 09:00:00'))
                        if button == 'phone':
                            statement = insert('swe_skill_contact_detail', click_id='CLICK_PLACEHOLDER', cust_uid=customer,
                                               cont_srl_nbr=uid(), clicked_at='2026-11-11 09:00:00', cont_dt='2026-11-11',
                                               cont_start_tm='2026-11-11 09:00:00', channel='test')
                            statements.append(statement.replace("'CLICK_PLACEHOLDER'", 'LAST_INSERT_ID()'))
    # 同一规划前一天单独设置8→4→2→1，三段流失率均50%。
    tie_trace = uid()
    traces.append(tie_trace)
    statements.append(insert('swe_cron_executions', job_id=jobs[0], tenant_id=users[0], trace_id=tie_trace, actual_time='2026-11-10 08:00:00', status='success', is_read=1))
    statements.append('SET @boundary_execution=LAST_INSERT_ID()')
    for index in range(8):
        customer = uid()
        statement = insert('swe_cron_result_index', tenant_id=users[0], first_bbk_id=bbk, bbk_org_id=orgs[0], custuid=customer,
                           skill_id=skill, job_id=jobs[0], execution_id='EXECUTION_PLACEHOLDER', trace_id=tie_trace,
                           subtask_id=int(uid(), 16), task_id=uid(), result_type='plan', status='SUC',
                           execution_at='2026-11-10 08:00:00', expire_at='2026-12-01 00:00:00')
        statements.append(statement.replace("'EXECUTION_PLACEHOLDER'", '@boundary_execution'))
        for button, limit in [('insight', 4), ('phone', 2)]:
            if index < limit:
                statements.append(insert('swe_html_preview_click_events', trace_id=tie_trace, customer_id=customer, file_url='boundary-validation', file_name=plan, event_type='button_click', button_type=button, clicked_at='2026-11-10 09:00:00'))
                if button == 'phone' and index == 0:
                    statement = insert('swe_skill_contact_detail', click_id='CLICK_PLACEHOLDER', cust_uid=customer, cont_srl_nbr=uid(),
                                       clicked_at='2026-11-10 09:00:00', cont_dt='2026-11-10', cont_start_tm='2026-11-10 09:00:00', channel='test')
                    statements.append(statement.replace("'CLICK_PLACEHOLDER'", 'LAST_INSERT_ID()'))
    statements.append('COMMIT')
    before = counts()
    cleanup_verified = False
    loaded = False
    # 在写入前保存精确清理条件，供中断恢复使用。
    trace_list = ','.join(map(quote, traces))
    job_list = ','.join(map(quote, jobs))
    cleanup = ['START TRANSACTION',
               'DELETE d FROM swe_skill_contact_detail d JOIN swe_html_preview_click_events c ON d.click_id=c.id WHERE c.trace_id IN (' + trace_list + ')',
               'DELETE FROM swe_html_preview_click_events WHERE trace_id IN (' + trace_list + ')',
               'DELETE FROM swe_cron_result_index WHERE job_id IN (' + job_list + ')',
               'DELETE FROM swe_cron_executions WHERE job_id IN (' + job_list + ')',
               'DELETE FROM swe_cron_jobs WHERE id IN (' + job_list + ')',
               'DELETE FROM swe_plan_sence_cron WHERE plan_id=' + quote(plan),
               'DELETE FROM swe_wealth_plan_scenes WHERE plan_id=' + quote(plan),
               'DELETE FROM swe_wealth_plan_targets WHERE plan_id=' + quote(plan),
               'DELETE FROM swe_wealth_plans WHERE id=' + quote(plan),
               'DELETE FROM rm_jkh_user_info WHERE user_id IN (' + ','.join(map(quote, users)) + ')', 'COMMIT']
    (FOLDER / 'boundary-manifest.json').write_text(json.dumps(dict(bbkId=bbk, planId=plan, orgIds=orgs, userIds=users, jobIds=jobs, traceIds=traces), indent=2))
    (FOLDER / 'boundary-cleanup.sql').write_text(';\n'.join(cleanup) + ';\n')
    try:
        sql(';'.join(statements))
        loaded = True
        filters = dict(startDate='2026-11-11', endDate='2026-11-11', bbkId=bbk)
        rows = api('execution-branches', dict(filters=filters))['rows']
        check('two populated and two zero-result branches', len(rows) == 4 and sum(r['recommendedCustomerCount'] == 0 for r in rows) == 2)
        by_id = {r['orgId']: r for r in rows}
        for metric in ['view', 'insight', 'call', 'contact']:
            check(metric + ' exact +3pp is high', by_id[orgs[0]][metric + 'RateDifference'] == 3 and by_id[orgs[0]][metric + 'RateStatus'] == 'high')
            check(metric + ' exact -3pp is low', by_id[orgs[1]][metric + 'RateDifference'] == -3 and by_id[orgs[1]][metric + 'RateStatus'] == 'low')
            check(metric + ' zero denominator has no evaluation', all(by_id[o][metric + 'RateStatus'] is None for o in orgs[2:]))
        for metric in ['viewRate', 'insightRate', 'callRate', 'contactRate']:
            for direction in ['asc', 'desc']:
                ordered = api('execution-branches', dict(filters=filters, sortKey=metric, sortDir=direction))['rows']
                expected = [orgs[1], orgs[0]] if direction == 'asc' else [orgs[0], orgs[1]]
                expected += sorted(orgs[2:])
                check(metric + ' mixed null last ' + direction, [r['orgId'] for r in ordered] == expected)
        tied = api('execution-overview', dict(startDate='2026-11-10', endDate='2026-11-10', bbkId=bbk))
        check('nonzero loss tie source counts', [tied[k] for k in ['viewCustomerCount', 'insightCustomerCount', 'callCustomerCount', 'contactCustomerCount']] == [8, 4, 2, 1])
        check('50 percent tie selects first stage', tied['largestLoss'] == dict(fromStage='view', toStage='insight', customerDifference=4, lossRate=50))
        # 仅移动本轮新增的一条结果，验证阈值内外；最终统一清理。
        result_id = sql('SELECT id FROM swe_cron_result_index WHERE job_id=' + quote(jobs[0]) + ' AND trace_id=' + quote(traces[0]) + ' ORDER BY id LIMIT 1')
        sql('UPDATE swe_cron_result_index SET job_id=' + quote(jobs[1]) + ',trace_id=' + quote(traces[1]) + ' WHERE id=' + result_id)
        inside = {r['orgId']: r for r in api('execution-branches', dict(filters=filters))['rows']}
        check('inside +3pp remains normal', inside[orgs[0]]['viewRateDifference'] == 2.5 and inside[orgs[0]]['viewRateStatus'] == 'normal')
        check('inside -3pp remains normal', inside[orgs[1]]['viewRateDifference'] == -2.5 and inside[orgs[1]]['viewRateStatus'] == 'normal')
        sql('UPDATE swe_cron_result_index SET job_id=' + quote(jobs[0]) + ',trace_id=' + quote(traces[0]) + ' WHERE id=' + result_id)
        unread_id = sql('SELECT id FROM swe_cron_result_index WHERE job_id=' + quote(jobs[1]) + ' ORDER BY id LIMIT 1')
        sql('UPDATE swe_cron_result_index SET job_id=' + quote(jobs[0]) + ',trace_id=' + quote(traces[0]) + ' WHERE id=' + unread_id)
        outside = {r['orgId']: r for r in api('execution-branches', dict(filters=filters))['rows']}
        check('outside +3pp remains high', outside[orgs[0]]['viewRateDifference'] == 3.5 and outside[orgs[0]]['viewRateStatus'] == 'high')
        check('outside -3pp remains low', outside[orgs[1]]['viewRateDifference'] == -3.5 and outside[orgs[1]]['viewRateStatus'] == 'low')
    finally:
        if loaded:
            clicks = sql('SELECT id FROM swe_html_preview_click_events WHERE trace_id IN (' + trace_list + ')').splitlines()
            cleanup[1] = 'DELETE FROM swe_skill_contact_detail WHERE click_id IN (' + (','.join(clicks) if clicks else '0') + ')'
            (FOLDER / 'boundary-cleanup.sql').write_text(';\n'.join(cleanup) + ';\n')
            sql(';'.join(cleanup))
        after = counts()
        cleanup_verified = before == after
        (FOLDER / 'boundary-results.json').write_text(json.dumps(dict(count=len(PASSED), passed=PASSED, cleanupVerified=cleanup_verified, before=before, after=after), ensure_ascii=False, indent=2))
        assert cleanup_verified, (before, after)
    print(str(len(PASSED)) + ' boundary checks PASS; all temporary rows cleaned')


if __name__ == '__main__':
    main()
