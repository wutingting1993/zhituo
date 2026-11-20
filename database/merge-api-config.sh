#!/usr/bin/env bash
set -euo pipefail
umask 077

if [[ $# != 6 || ( "$1" != schema && "$1" != data ) ]]; then
  echo '用法: bash database/merge-api-config.sh schema|data 配置库.cnf 配置库名 专家库.cnf 专家库名 目标库.cnf' >&2
  exit 2
fi

phase=$1
config_options=$2
config_schema=$3
expert_options=$4
expert_schema=$5
target_options=$6
target_schema=zhituo

for options in "$config_options" "$expert_options" "$target_options"; do
  if [[ ! -r "$options" ]]; then
    printf '无法读取 MySQL 连接文件：%s\n' "$options" >&2
    exit 2
  fi
done
for schema in "$config_schema" "$expert_schema"; do
  if [[ ! "$schema" =~ ^[a-zA-Z0-9_]+$ ]]; then
    printf '无效的源库名称：%s\n' "$schema" >&2
    exit 2
  fi
done

mysql_query() {
  local options=$1 schema=$2 query=$3
  mysql --defaults-extra-file="$options" --database="$schema" --batch --skip-column-names --execute="$query"
}

table_exists() {
  local options=$1 schema=$2 table=$3
  mysql_query "$options" "$schema" "SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '$table' AND table_type = 'BASE TABLE'"
}

count_rows() {
  local options=$1 schema=$2 table=$3
  mysql_query "$options" "$schema" "SELECT COUNT(1) FROM \`$table\`"
}

dump_tables() {
  local options=$1 schema=$2
  shift 2
  mysqldump --defaults-extra-file="$options" --default-character-set=utf8mb4 \
    --single-transaction --skip-lock-tables --skip-add-drop-table --skip-add-locks \
    --skip-disable-keys --skip-triggers --no-tablespaces --set-gtid-purged=OFF \
    --column-statistics=0 "${dump_mode[@]}" "$schema" "$@"
}

listing=$(mysql_query "$config_options" "$config_schema" "SHOW FULL TABLES WHERE Table_type = 'BASE TABLE'")
config_tables=()
while IFS=$'\t' read -r table table_type; do
  if [[ -z "$table" || "$table" == operation_task ]]; then
    continue
  fi
  if [[ ! "$table" =~ ^[a-zA-Z0-9_]+$ || "$table_type" != 'BASE TABLE' ]]; then
    printf '无法安全迁移源表：%s\n' "$table" >&2
    exit 2
  fi
  config_tables+=("$table")
done <<< "$listing"

if [[ "$(table_exists "$config_options" "$config_schema" api_interface)" != 1 ||
      "$(table_exists "$expert_options" "$expert_schema" operation_task)" != 1 ]]; then
  echo '源库缺少 api_interface 或 operation_task。' >&2
  exit 2
fi

all_tables=("${config_tables[@]}" operation_task)
for table in "${all_tables[@]}"; do
  exists=$(table_exists "$target_options" "$target_schema" "$table")
  if [[ "$phase" == schema && "$exists" != 0 ]]; then
    printf '目标库已有同名表：%s\n' "$table" >&2
    exit 2
  fi
  if [[ "$phase" == data ]]; then
    if [[ "$exists" != 1 || "$(count_rows "$target_options" "$target_schema" "$table")" != 0 ]]; then
      printf '数据阶段要求目标表存在且为空：%s\n' "$table" >&2
      exit 2
    fi
    engine=$(mysql_query "$target_options" "$target_schema" "SELECT engine FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = '$table'")
    if [[ "$engine" != InnoDB ]]; then
      printf '数据阶段要求目标表使用 InnoDB：%s\n' "$table" >&2
      exit 2
    fi
  fi
done

work=$(mktemp -d)
trap 'rm -rf -- "$work"' EXIT
dump_mode=(--no-data)
if [[ "$phase" == data ]]; then
  dump_mode=(--no-create-info --complete-insert)
fi

dump_tables "$config_options" "$config_schema" "${config_tables[@]}" > "$work/config.sql"
dump_tables "$expert_options" "$expert_schema" operation_task > "$work/expert.sql"

{
  if [[ "$phase" == data ]]; then
    printf 'START TRANSACTION;\n'
  fi
  cat "$work/config.sql" "$work/expert.sql"
  if [[ "$phase" == data ]]; then
    printf 'COMMIT;\n'
  fi
} | mysql --defaults-extra-file="$target_options" --default-character-set=utf8mb4 --database="$target_schema"

for table in "${all_tables[@]}"; do
  if [[ "$phase" == schema ]]; then
    [[ "$(table_exists "$target_options" "$target_schema" "$table")" == 1 ]]
    printf '建表完成 %s\n' "$table"
  else
    src_options=$config_options
    src_schema=$config_schema
    if [[ "$table" == operation_task ]]; then
      src_options=$expert_options
      src_schema=$expert_schema
    fi
    source_count=$(count_rows "$src_options" "$src_schema" "$table")
    target_count=$(count_rows "$target_options" "$target_schema" "$table")
    if [[ "$source_count" != "$target_count" ]]; then
      printf '行数校验失败 %s 源=%s 目标=%s\n' "$table" "$source_count" "$target_count" >&2
      exit 1
    fi
    printf '行数校验通过 %s %s\n' "$table" "$target_count"
  fi
done
