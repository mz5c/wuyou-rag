#!/bin/bash
# 格式: LOCAL_PORT:DESCRIPTION
TUNNELS=(
  "9000:MinIO 对象存储 API"
  "9001:MinIO 管理控制台"
  "3306:MySQL 数据库"
  "5672:RabbitMQ 主端口"
  "15672:RabbitMQ 管理控制台"
  "6379:Redis 缓存"
  "9091:Milvus 健康检查/指标"
  "19530:Milvus 向量数据库 gRPC"
  "3000:Attu Milvus 管理界面"
  "5001:(Embedding 服务端口)"
  "9200:Elasticsearch 搜索引擎"
  "5601:Kibana 日志分析"
)

for entry in "${TUNNELS[@]}"; do
  port="${entry%%:*}"
  desc="${entry#*:}"
  if lsof -i :"$port" -sTCP:LISTEN > /dev/null 2>&1; then
    echo "已连接  $port  $desc"
  else
    ssh -fNL "$port:127.0.0.1:$port" wydev && echo "已建立  $port  $desc"
  fi
done
